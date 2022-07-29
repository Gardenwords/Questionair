public static SearchPattern createPattern(IJavaElement element, int limitTo, int matchRule) {
	SearchPattern searchPattern = null;
	int lastDot;
	boolean ignoreDeclaringType = false;
	boolean ignoreReturnType = false;
	int maskedLimitTo = limitTo & ~(IJavaSearchConstants.IGNORE_DECLARING_TYPE+IJavaSearchConstants.IGNORE_RETURN_TYPE);
	if (maskedLimitTo == IJavaSearchConstants.DECLARATIONS || maskedLimitTo == IJavaSearchConstants.ALL_OCCURRENCES) {
		ignoreDeclaringType = (limitTo & IJavaSearchConstants.IGNORE_DECLARING_TYPE) != 0;
		ignoreReturnType = (limitTo & IJavaSearchConstants.IGNORE_RETURN_TYPE) != 0;
	}
	if ((matchRule = validateMatchRule(null, matchRule)) == -1) {
		return null;
	}
	char[] declaringSimpleName = null;
	char[] declaringQualification = null;
	switch (element.getElementType()) {
		case IJavaElement.FIELD :
			IField field = (IField) element;
			if (!ignoreDeclaringType) {
				IType declaringClass = field.getDeclaringType();
				declaringSimpleName = declaringClass.getElementName().toCharArray();
				declaringQualification = declaringClass.getPackageFragment().getElementName().toCharArray();
				char[][] enclosingNames = enclosingTypeNames(declaringClass);
				if (enclosingNames.length > 0) {
					declaringQualification = CharOperation.concat(declaringQualification, CharOperation.concatWith(enclosingNames, '.'), '.');
				}
			}
			char[] name = field.getElementName().toCharArray();
			char[] typeSimpleName = null;
			char[] typeQualification = null;
			String typeSignature = null;
			if (!ignoreReturnType) {
				try {
					typeSignature = field.getTypeSignature();
					char[] signature = typeSignature.toCharArray();
					char[] typeErasure = Signature.toCharArray(Signature.getTypeErasure(signature));
					CharOperation.replace(typeErasure, '$', '.');
					if ((lastDot = CharOperation.lastIndexOf('.', typeErasure)) == -1) {
						typeSimpleName = typeErasure;
					} else {
						typeSimpleName = CharOperation.subarray(typeErasure, lastDot + 1, typeErasure.length);
						typeQualification = CharOperation.subarray(typeErasure, 0, lastDot);
						if (!field.isBinary()) {
							// prefix with a '*' as the full qualification could be bigger (because of an import)
							typeQualification = CharOperation.concat(IIndexConstants.ONE_STAR, typeQualification);
						}
					}
				} catch (JavaModelException e) {
					return null;
				}
			}
			// Create field pattern
			searchPattern =
				new FieldPattern(
					name,
					declaringQualification,
					declaringSimpleName,
					typeQualification,
					typeSimpleName,
					typeSignature,
					limitTo,
					matchRule);
			break;
		case IJavaElement.IMPORT_DECLARATION :
			String elementName = element.getElementName();
			lastDot = elementName.lastIndexOf('.');
			if (lastDot == -1) return null; // invalid import declaration
			IImportDeclaration importDecl = (IImportDeclaration)element;
			if (importDecl.isOnDemand()) {
				searchPattern = createPackagePattern(elementName.substring(0, lastDot), maskedLimitTo, matchRule);
			} else {
				searchPattern =
					createTypePattern(
						elementName.substring(lastDot+1).toCharArray(),
						elementName.substring(0, lastDot).toCharArray(),
						null,
						null,
						null,
						maskedLimitTo,
						matchRule);
			}
			break;
		case IJavaElement.LOCAL_VARIABLE :
			LocalVariable localVar = (LocalVariable) element;
			searchPattern = new LocalVariablePattern(localVar, limitTo, matchRule);
			break;
		case IJavaElement.TYPE_PARAMETER:
			ITypeParameter typeParam = (ITypeParameter) element;
			boolean findParamDeclarations = true;
			boolean findParamReferences = true;
			switch (maskedLimitTo) {
				case IJavaSearchConstants.DECLARATIONS :
					findParamReferences = false;
					break;
				case IJavaSearchConstants.REFERENCES :
					findParamDeclarations = false;
					break;
			}
			searchPattern =
				new TypeParameterPattern(
					findParamDeclarations,
					findParamReferences,
					typeParam,
					matchRule);
			break;
		case IJavaElement.METHOD :
			IMethod method = (IMethod) element;
			boolean isConstructor;
			try {
				isConstructor = method.isConstructor();
			} catch (JavaModelException e) {
				return null;
			}
			IType declaringClass = method.getDeclaringType();
			if (ignoreDeclaringType) {
				if (isConstructor) declaringSimpleName = declaringClass.getElementName().toCharArray();
			} else {
				declaringSimpleName = declaringClass.getElementName().toCharArray();
				declaringQualification = declaringClass.getPackageFragment().getElementName().toCharArray();
				char[][] enclosingNames = enclosingTypeNames(declaringClass);
				if (enclosingNames.length > 0) {
					declaringQualification = CharOperation.concat(declaringQualification, CharOperation.concatWith(enclosingNames, '.'), '.');
				}
			}
			char[] selector = method.getElementName().toCharArray();
			char[] returnSimpleName = null;
			char[] returnQualification = null;
			String returnSignature = null;
			if (!ignoreReturnType) {
				try {
					returnSignature = method.getReturnType();
					char[] signature = returnSignature.toCharArray();
					char[] returnErasure = Signature.toCharArray(Signature.getTypeErasure(signature));
					CharOperation.replace(returnErasure, '$', '.');
					if ((lastDot = CharOperation.lastIndexOf('.', returnErasure)) == -1) {
						returnSimpleName = returnErasure;
					} else {
						returnSimpleName = CharOperation.subarray(returnErasure, lastDot + 1, returnErasure.length);
						returnQualification = CharOperation.subarray(returnErasure, 0, lastDot);
						if (!method.isBinary()) {
							// prefix with a '*' as the full qualification could be bigger (because of an import)
							CharOperation.concat(IIndexConstants.ONE_STAR, returnQualification);
						}
					}
				} catch (JavaModelException e) {
					return null;
				}
			}
			String[] parameterTypes = method.getParameterTypes();
			int paramCount = parameterTypes.length;
			char[][] parameterSimpleNames = new char[paramCount][];
			char[][] parameterQualifications = new char[paramCount][];
			String[] parameterSignatures = new String[paramCount];
			for (int i = 0; i < paramCount; i++) {
				parameterSignatures[i] = parameterTypes[i];
				char[] signature = parameterSignatures[i].toCharArray();
				char[] paramErasure = Signature.toCharArray(Signature.getTypeErasure(signature));
				CharOperation.replace(paramErasure, '$', '.');
				if ((lastDot = CharOperation.lastIndexOf('.', paramErasure)) == -1) {
					parameterSimpleNames[i] = paramErasure;
					parameterQualifications[i] = null;
				} else {
					parameterSimpleNames[i] = CharOperation.subarray(paramErasure, lastDot + 1, paramErasure.length);
					parameterQualifications[i] = CharOperation.subarray(paramErasure, 0, lastDot);
					if (!method.isBinary()) {
						// prefix with a '*' as the full qualification could be bigger (because of an import)
						CharOperation.concat(IIndexConstants.ONE_STAR, parameterQualifications[i]);
					}
				}
			}

			// Create method/constructor pattern
			if (isConstructor) {
				searchPattern =
					new ConstructorPattern(
						declaringSimpleName,
						declaringQualification,
						parameterQualifications,
						parameterSimpleNames,
						parameterSignatures,
						method,
						limitTo,
						matchRule);
			} else {
				searchPattern =
					new MethodPattern(
						selector,
						declaringQualification,
						declaringSimpleName,
						returnQualification,
						returnSimpleName,
						returnSignature,
						parameterQualifications,
						parameterSimpleNames,
						parameterSignatures,
						method,
						limitTo,
						matchRule);
			}
			break;
		case IJavaElement.TYPE :
			IType type = (IType)element;
			searchPattern = 	createTypePattern(
						type.getElementName().toCharArray(),
						type.getPackageFragment().getElementName().toCharArray(),
						ignoreDeclaringType ? null : enclosingTypeNames(type),
						null,
						type,
						maskedLimitTo,
						matchRule);
			break;
		case IJavaElement.PACKAGE_DECLARATION :
		case IJavaElement.PACKAGE_FRAGMENT :
			searchPattern = createPackagePattern(element.getElementName(), maskedLimitTo, matchRule);
			break;
	}
	if (searchPattern != null)
		MatchLocator.setFocus(searchPattern, element);
	return searchPattern;
}