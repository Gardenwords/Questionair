public void javadocInvalidConstructor(Statement statement, MethodBinding targetConstructor, int modifiers) {

	if (!javadocVisibility(this.options.reportInvalidJavadocTagsVisibility, modifiers)) {
		return;
	}
//	boolean insideDefaultConstructor = 
//		(this.referenceContext instanceof ConstructorDeclaration)
//			&& ((ConstructorDeclaration)this.referenceContext).isDefaultConstructor();
//	boolean insideImplicitConstructorCall =
//		(statement instanceof ExplicitConstructorCall)
//			&& (((ExplicitConstructorCall) statement).accessMode == ExplicitConstructorCall.ImplicitSuper);

	int id = IProblem.JavadocUndefinedConstructor; //default...
	switch (targetConstructor.problemId()) {
		case NotFound :
//			if (insideDefaultConstructor){
//				id = IProblem.JavadocUndefinedConstructorInDefaultConstructor;
//			} else if (insideImplicitConstructorCall){
//				id = IProblem.JavadocUndefinedConstructorInImplicitConstructorCall;
//			} else {
				id = IProblem.JavadocUndefinedConstructor;
//			}
			break;
		case NotVisible :
//			if (insideDefaultConstructor){
//				id = IProblem.JavadocNotVisibleConstructorInDefaultConstructor;
//			} else if (insideImplicitConstructorCall){
//				id = IProblem.JavadocNotVisibleConstructorInImplicitConstructorCall;
//			} else {
				id = IProblem.JavadocNotVisibleConstructor;
//			}
			break;
		case Ambiguous :
//			if (insideDefaultConstructor){
//				id = IProblem.AmbiguousConstructorInDefaultConstructor;
//			} else if (insideImplicitConstructorCall){
//				id = IProblem.AmbiguousConstructorInImplicitConstructorCall;
//			} else {
				id = IProblem.JavadocAmbiguousConstructor;
//			}
			break;
		case NoError : // 0
		default :
			needImplementation(); // want to fail to see why we were here...
			break;
	}

	this.handle(
		id,
		new String[] {new String(targetConstructor.declaringClass.readableName()), parametersAsString(targetConstructor.parameters, false)},
		new String[] {new String(targetConstructor.declaringClass.shortReadableName()), parametersAsString(targetConstructor.parameters, true)},
		statement.sourceStart,
		statement.sourceEnd);
}