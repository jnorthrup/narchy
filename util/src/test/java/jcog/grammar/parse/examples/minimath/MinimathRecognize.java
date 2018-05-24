package jcog.grammar.parse.examples.minimath;

import jcog.grammar.parse.Repetition;
import jcog.grammar.parse.Sequence;
import jcog.grammar.parse.tokens.Num;
import jcog.grammar.parse.tokens.Symbol;
import jcog.grammar.parse.tokens.TokenAssembly;

/**
 * Show how to build a parser to recognize elements
 * of the language "Minimath", whose rules are:
 * 
 * <blockquote><pre>	
 *     e = Num m*;
 *     m = '-' Num;
 * </pre></blockquote>
 *
 * @author Steven J. Metsker
 *
 * @version 1.0 
 */
public class MinimathRecognize {
	/**
	 * Just a little demo.
	 */
	public static void main(String args[]) {
		Sequence e = new Sequence();

		e.get(new Num());

		Sequence m = new Sequence();
		m.get(new Symbol('-'));
		m.get(new Num());

		e.get(new Repetition(m));

		System.out.println(e.completeMatch(new TokenAssembly("25 - 16 - 9")));
	}
}
