/*Copyright 2023 by Beverly A Sanders
 * 
 * This code is provided for solely for use of students in COP4020 Programming Language Concepts at the 
 * University of Florida during the spring semester 2023 as part of the course project.  
 * 
 * No other use is authorized. 
 * 
 * This code may not be posted on a public web site either during or after the course.  
 */

package edu.ufl.cise.plcsp23.ast;

import edu.ufl.cise.plcsp23.IToken;
import edu.ufl.cise.plcsp23.PLCException;

public class Ident extends AST {

	NameDef def = null;
	String name;

	public Ident(IToken firstToken) {
		super(firstToken);
		name = firstToken.getTokenString();
	}

	@Override
	public Object visit(ASTVisitor v, Object arg) throws PLCException {
			return v.visitIdent(this, arg);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Ident [getName()=" + getName();
	}

}
