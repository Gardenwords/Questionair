private void format(FieldDeclaration fieldDeclaration, ASTVisitor visitor, MethodScope scope, boolean isChunkStart, boolean isFirstClassBodyDeclaration) {
		
		if (isFirstClassBodyDeclaration) {
			int newLinesBeforeFirstClassBodyDeclaration = this.preferences.blank_lines_before_first_class_body_declaration;
			if (newLinesBeforeFirstClassBodyDeclaration > 0) {
				this.scribe.printEmptyLines(newLinesBeforeFirstClassBodyDeclaration);
			}
		} else {
			int newLineBeforeChunk = isChunkStart ? this.preferences.blank_lines_before_new_chunk : 0;
			if (newLineBeforeChunk > 0) {
				this.scribe.printEmptyLines(newLineBeforeChunk);
			}
			final int newLinesBeforeField = this.preferences.blank_lines_before_field;
			if (newLinesBeforeField > 0) {
				this.scribe.printEmptyLines(newLinesBeforeField);
			}
		}
		Alignment memberAlignment = this.scribe.getMemberAlignment();
	
        this.scribe.printComment();
		this.scribe.printModifiers(fieldDeclaration.annotations, this, ICodeFormatterConstants.ANNOTATION_ON_MEMBER);
		this.scribe.space();
		/*
		 * Field type
		 */
		fieldDeclaration.type.traverse(this, scope);
		
		/*
		 * Field name
		 */
		this.scribe.alignFragment(memberAlignment, 0);
	
		this.scribe.printNextToken(TerminalTokens.TokenNameIdentifier, true);
	
		/*
		 * Check for extra dimensions
		 */
		int extraDimensions = getDimensions();
		if (extraDimensions != 0) {
			 for (int i = 0; i < extraDimensions; i++) {
			 	this.scribe.printNextToken(TerminalTokens.TokenNameLBRACKET);
			 	this.scribe.printNextToken(TerminalTokens.TokenNameRBRACKET);
			 }
		}
	
		/*
		 * Field initialization
		 */
		final Expression initialization = fieldDeclaration.initialization;
		if (initialization != null) {
			this.scribe.alignFragment(memberAlignment, 1);
			this.scribe.printNextToken(TerminalTokens.TokenNameEQUAL, this.preferences.insert_space_before_assignment_operator);
			if (this.preferences.insert_space_after_assignment_operator) {
				this.scribe.space();
			}
			Alignment assignmentAlignment = this.scribe.createAlignment("fieldDeclarationAssignmentAlignment", this.preferences.alignment_for_assignment, Alignment.R_OUTERMOST, 1, this.scribe.scanner.currentPosition); //$NON-NLS-1$
			this.scribe.enterAlignment(assignmentAlignment);
			boolean ok = false;
			do {
				try {
					this.scribe.alignFragment(assignmentAlignment, 0);
					initialization.traverse(this, scope);
					ok = true;
				} catch(AlignmentException e){
					this.scribe.redoAlignment(e);
				}
			} while (!ok);		
			this.scribe.exitAlignment(assignmentAlignment, true);			
		}
		
		this.scribe.printNextToken(TerminalTokens.TokenNameSEMICOLON, this.preferences.insert_space_before_semicolon);

		if (memberAlignment != null) {
			this.scribe.alignFragment(memberAlignment, 2);
			this.scribe.printTrailingComment();
		} else {
			this.scribe.space();
			this.scribe.printTrailingComment();
		}
	}