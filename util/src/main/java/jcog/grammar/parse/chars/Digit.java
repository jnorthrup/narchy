package jcog.grammar.parse.chars;

import jcog.grammar.parse.Parser;
import jcog.grammar.parse.Terminal;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/*
 * Copyright (c) 1999 Steven J. Metsker. All Rights Reserved.
 * 
 * Steve Metsker makes no representations or warranties about
 * the fitness of this software for any particular purpose, 
 * including the implied warranty of merchantability.
 */

/**
 * A Digit matches a digit from a character assembly.
 * 
 * @author Steven J. Metsker
 * 
 * @version 1.0
 */
public class Digit extends Terminal {

	/**
	 * Returns true if an assembly's next element is a digit.
	 * 
	 * @param object
	 *            an element from an assembly
	 * 
	 * @return true, if an assembly's next element is a digit
	 */
	@Override
	public boolean qualifies(Object o) {
        Character c = (Character) o;
		return Character.isDigit(c);
	}

	/**
	 * Create a set with one random digit.
	 */
	@Override
	public List<String> randomExpansion(int maxDepth, int depth) {
        char c = (char) (10.0 * Math.random() + (double) '0');
		List<String> v = new ArrayList<>();
		v.add(new String(new char[] { c }));
		return v;
	}

	/**
	 * Returns a textual description of this parser.
	 * 
	 * @param vector
	 *            a list of parsers already printed in this description
	 * 
	 * @return string a textual description of this parser
	 * 
	 * @see Parser#toString()
	 */
	@Override
	public String unvisitedString(Set<Parser> visited) {
		return "D";
	}
}
