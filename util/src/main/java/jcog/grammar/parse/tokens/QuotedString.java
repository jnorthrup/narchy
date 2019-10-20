package jcog.grammar.parse.tokens;

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
 * A QuotedString matches a quoted string, like "this one" from a token
 * assembly.
 * 
 * @author Steven J. Metsker
 * 
 * @version 1.0
 */
public class QuotedString extends Terminal {
	/**
	 * Returns true if an assembly's next element is a quoted string.
	 * 
	 * @param object
	 *            an element from a assembly
	 * 
	 * @return true, if a assembly's next element is a quoted string, like
	 *         "chubby cherubim".
	 */
	@Override
	protected boolean qualifies(Object o) {
		var t = (Token) o;
		return t.isQuotedString();
	}

	/**
	 * Create a set with one random quoted string (with 2 to 6 characters).
	 */
	@Override
	public List<String> randomExpansion(int maxDepth, int depth) {
		var n = (int) (5.0 * Math.random());

		var letters = new char[n + 2];
		letters[0] = '"';
		letters[n + 1] = '"';

		for (var i = 0; i < n; i++) {
			var c = (int) (26.0 * Math.random()) + 'a';
			letters[i + 1] = (char) c;
		}

		List<String> v = new ArrayList<>();
		v.add(new String(letters));
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
		return "QuotedString";
	}
}
