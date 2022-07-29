private int addDTDDefaultAttributes(QName element, XMLAttrList attrList, int attrIndex, boolean validationEnabled, boolean standalone) throws Exception {


      //
      // Check after all specified attrs are scanned
      // (1) report error for REQUIRED attrs that are missing (V_TAGc)
      // (2) check that FIXED attrs have matching value (V_TAGd)
      // (3) add default attrs (FIXED and NOT_FIXED)
      //

      int elementIndex = fGrammar.getElementDeclIndex(element, -1);

      if (elementIndex == -1) {
         return attrIndex;
      }

      fGrammar.getElementDecl(elementIndex,fTempElementDecl);


      int elementNameIndex = fTempElementDecl.name.rawname;
      int attlistIndex = fGrammar.getFirstAttributeDeclIndex(elementIndex);
      int firstCheck = attrIndex;
      int lastCheck = -1;
      while (attlistIndex != -1) {

         fGrammar.getAttributeDecl(attlistIndex, fTempAttDecl);

         // TO DO: For ericye Debug only
         /***
         if (fTempAttDecl != null) {
             XMLElementDecl element = new XMLElementDecl();
             fGrammar.getElementDecl(elementIndex, element);
             System.out.println("element: "+fStringPool.toString(element.name.localpart));
             System.out.println("attlistIndex " + attlistIndex + "\n"+
                 "attName : '"+fStringPool.toString(fTempAttDecl.name.localpart) + "'\n"
                                + "attType : "+fTempAttDecl.type + "\n"
                                + "attDefaultType : "+fTempAttDecl.defaultType + "\n"
                                + "attDefaultValue : '"+fTempAttDecl.defaultValue + "'\n"
                                + attrList.getLength() +"\n"
                                );
         }
         /***/

         int attPrefix = fTempAttDecl.name.prefix;
         int attName = fTempAttDecl.name.rawname;
         int attLocalpart = fTempAttDecl.name.localpart;
         int attType = attributeTypeName(fTempAttDecl);
         int attDefType =fTempAttDecl.defaultType;
         int attValue = -1 ;
         if (fTempAttDecl.defaultValue != null ) {
            attValue = fStringPool.addSymbol(fTempAttDecl.defaultValue);
         }
         boolean specified = false;
         boolean required = (attDefType & XMLAttributeDecl.DEFAULT_TYPE_REQUIRED)>0;


         /****
         if (fValidating && fGrammar != null && fGrammarIsDTDGrammar && attValue != -1) {
             normalizeAttValue(null, fTempAttDecl.name,
                               attValue,attType,fTempAttDecl.list, 
                               fTempAttDecl.enumeration);
         }
         /****/

         if (firstCheck != -1) {
            boolean cdata = attType == fCDATASymbol;
            if (!cdata || required || attValue != -1) {
               int i = attrList.getFirstAttr(firstCheck);
               while (i != -1 && (lastCheck == -1 || i <= lastCheck)) {

                  if ( attrList.getAttrName(i) == fTempAttDecl.name.rawname ) {

                     if (validationEnabled && (attDefType & XMLAttributeDecl.DEFAULT_TYPE_FIXED) > 0) {
                        int alistValue = attrList.getAttValue(i);
                        if (alistValue != attValue &&
                            !fStringPool.toString(alistValue).equals(fStringPool.toString(attValue))) {
                           Object[] args = { fStringPool.toString(elementNameIndex),
                              fStringPool.toString(attName),
                              fStringPool.toString(alistValue),
                              fStringPool.toString(attValue)};
                           fErrorReporter.reportError(fErrorReporter.getLocator(),
                                                      XMLMessages.XML_DOMAIN,
                                                      XMLMessages.MSG_FIXED_ATTVALUE_INVALID,
                                                      XMLMessages.VC_FIXED_ATTRIBUTE_DEFAULT,
                                                      args,
                                                      XMLErrorReporter.ERRORTYPE_RECOVERABLE_ERROR);
                        }
                     }
                     specified = true;
                     break;
                  }
                  i = attrList.getNextAttr(i);
               }
            }
         }

         if (!specified) {
            if (required) {
               if (validationEnabled) {
                  Object[] args = { fStringPool.toString(elementNameIndex),
                     fStringPool.toString(attName)};
                  fErrorReporter.reportError(fErrorReporter.getLocator(),
                                             XMLMessages.XML_DOMAIN,
                                             XMLMessages.MSG_REQUIRED_ATTRIBUTE_NOT_SPECIFIED,
                                             XMLMessages.VC_REQUIRED_ATTRIBUTE,
                                             args,
                                             XMLErrorReporter.ERRORTYPE_RECOVERABLE_ERROR);
               }
            } else if (attValue != -1) {
               if (validationEnabled && standalone ){
                  if ( fGrammarIsDTDGrammar 
                       && ((DTDGrammar) fGrammar).getAttributeDeclIsExternal(attlistIndex) ) {

                     Object[] args = { fStringPool.toString(elementNameIndex),
                        fStringPool.toString(attName)};
                     fErrorReporter.reportError(fErrorReporter.getLocator(),
                                                XMLMessages.XML_DOMAIN,
                                                XMLMessages.MSG_DEFAULTED_ATTRIBUTE_NOT_SPECIFIED,
                                                XMLMessages.VC_STANDALONE_DOCUMENT_DECLARATION,
                                                args,
                                                XMLErrorReporter.ERRORTYPE_RECOVERABLE_ERROR);
                  }
               }
               if (validationEnabled) {
                    if (attType == fIDREFSymbol) {
                        this.fValIDRef.validate( fStringPool.toString(attValue), null );
                    }
                    else if (attType == fIDREFSSymbol) {
                        this.fValIDRefs.validate( fStringPool.toString(attValue), null );
                    }
               }
               if (attrIndex == -1) {
                  attrIndex = attrList.startAttrList();
               }

               fTempQName.setValues(attPrefix, attLocalpart, attName, fTempAttDecl.name.uri);
               int newAttr = attrList.addAttr(fTempQName, 
                                              attValue, attType, 
                                              false, false);
               if (lastCheck == -1) {
                  lastCheck = newAttr;
               }
            }
         }
         attlistIndex = fGrammar.getNextAttributeDeclIndex(attlistIndex);
      }
      return attrIndex;

   } // addDTDDefaultAttributes(int,XMLAttrList,int,boolean,boolean):int