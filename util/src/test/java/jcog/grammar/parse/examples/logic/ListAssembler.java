package jcog.grammar.parse.examples.logic;

import jcog.grammar.parse.AssemblerHelper;
import jcog.grammar.parse.Assembly;
import jcog.grammar.parse.IAssembler;
import jcog.grammar.parse.examples.engine.Structure;
import jcog.grammar.parse.tokens.Token;

/*
 * Copyright (c) 2000 Steven J. Metsker. All Rights Reserved.
 * 
 * Steve Metsker makes no representations or warranties about
 * the fitness of this software for any particular purpose, 
 * including the implied warranty of merchantability.
 */

/**
 * Pops the terms of a list from an assembly's stack, builds the list, and
 * pushes it.
 * 
 * @author Steven J. Metsker
 * 
 * @version 1.0
 */
class ListAssembler implements IAssembler {

	private static final Token fence = new Token('[');

	/**
	 * Pops the terms of a list from an assembly's stack, builds the list, and
	 * pushes it.
	 * <p>
	 * This method expects a series of terms to lie on top of a stack, with an
	 * open bracket token lying beneath. If there is no '[' marker, this class
	 * will throw an <code>EmptiStackException</code>.
	 * 
	 * @param Assembly
	 *            the assembly to work on
	 */
	public void accept(Assembly a) {
		a.push(Structure.list(
			StructureWithTermsAssembler.vectorReversedIntoTerms(
				AssemblerHelper.elementsAbove(a, fence)
		)));
	}
}
