protected XSAttributeUseImpl traverseLocal(Element attrDecl,
                                               XSDocumentInfo schemaDoc,
                                               SchemaGrammar grammar,
                                               XSComplexTypeDecl enclosingCT) {

        // General Attribute Checking
        Object[] attrValues = fAttrChecker.checkAttributes(attrDecl, false, schemaDoc);

        String defaultAtt = (String) attrValues[XSAttributeChecker.ATTIDX_DEFAULT];
        String fixedAtt   = (String) attrValues[XSAttributeChecker.ATTIDX_FIXED];
        String nameAtt    = (String) attrValues[XSAttributeChecker.ATTIDX_NAME];
        QName  refAtt     = (QName)  attrValues[XSAttributeChecker.ATTIDX_REF];
        XInt   useAtt     = (XInt)   attrValues[XSAttributeChecker.ATTIDX_USE];

        // get 'attribute declaration'
        XSAttributeDecl attribute = null;
        if (attrDecl.getAttributeNode(SchemaSymbols.ATT_REF) != null) {
            if (refAtt != null) {
                attribute = (XSAttributeDecl)fSchemaHandler.getGlobalDecl(schemaDoc, XSDHandler.ATTRIBUTE_TYPE, refAtt, attrDecl);

                Element child = DOMUtil.getFirstChildElement(attrDecl);
                if (child != null && DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
                    traverseAnnotationDecl(child, attrValues, false, schemaDoc);
                    child = DOMUtil.getNextSiblingElement(child);
                }

                if (child != null) {
                    reportSchemaError("src-attribute.3.2", new Object[]{refAtt}, child);
                }
                // for error reporting
                nameAtt = refAtt.localpart;
            } else {
                attribute = null;
            }
        } else {
            attribute = traverseNamedAttr(attrDecl, attrValues, schemaDoc, grammar, false, enclosingCT);
        }

        // get 'value constraint'
        short consType = XSConstants.VC_NONE;
        if (defaultAtt != null) {
            consType = XSConstants.VC_DEFAULT;
        } else if (fixedAtt != null) {
            consType = XSConstants.VC_FIXED;
            defaultAtt = fixedAtt;
            fixedAtt = null;
        }

        XSAttributeUseImpl attrUse = null;
        if (attribute != null) {
            if (fSchemaHandler.fDeclPool !=null) {
                attrUse = fSchemaHandler.fDeclPool.getAttributeUse();
            } else {
                attrUse = new XSAttributeUseImpl();
            }
            attrUse.fAttrDecl = attribute;
            attrUse.fUse = useAtt.shortValue();
            attrUse.fConstraintType = consType;
            if (defaultAtt != null) {
                attrUse.fDefault = new ValidatedInfo();
                attrUse.fDefault.normalizedValue = defaultAtt;
            }
        }
        fAttrChecker.returnAttrArray(attrValues, schemaDoc);

        //src-attribute

        // 1 default and fixed must not both be present.
        if (defaultAtt != null && fixedAtt != null) {
            reportSchemaError("src-attribute.1", new Object[]{nameAtt}, attrDecl);
        }

        // 2 If default and use are both present, use must have the actual value optional.
        if (consType == XSConstants.VC_DEFAULT &&
            useAtt != null && useAtt.intValue() != SchemaSymbols.USE_OPTIONAL) {
            reportSchemaError("src-attribute.2", new Object[]{nameAtt}, attrDecl);
        }

        // a-props-correct

        if (defaultAtt != null && attrUse != null) {
            // 2 if there is a {value constraint}, the canonical lexical representation of its value must be valid with respect to the {type definition} as defined in String Valid (3.14.4).
            fValidationState.setNamespaceSupport(schemaDoc.fNamespaceSupport);
            if (!checkDefaultValid(attrUse)) {
                reportSchemaError ("a-props-correct.2", new Object[]{nameAtt, defaultAtt}, attrDecl);
            }

            // 3 If the {type definition} is or is derived from ID then there must not be a {value constraint}.
            if (((XSSimpleType)attribute.getTypeDefinition()).isIDType() ) {
                reportSchemaError ("a-props-correct.3", new Object[]{nameAtt}, attrDecl);
            }

            // check 3.5.6 constraint
            // Attribute Use Correct
            // 2 If the {attribute declaration} has a fixed {value constraint}, then if the attribute use itself has a {value constraint}, it must also be fixed and its value must match that of the {attribute declaration}'s {value constraint}.
            if (attrUse.fAttrDecl.getConstraintType() == XSConstants.VC_FIXED &&
                attrUse.fConstraintType != XSConstants.VC_NONE) {
                if (attrUse.fConstraintType != XSConstants.VC_FIXED ||
                    !((XSSimpleType)attrUse.fAttrDecl.getTypeDefinition()).isEqual(attrUse.fAttrDecl.getValInfo().actualValue,
                                                     attrUse.fDefault.actualValue)) {
                    reportSchemaError ("au-props-correct.2", new Object[]{nameAtt}, attrDecl);
                }
            }
        }

        return attrUse;
    }