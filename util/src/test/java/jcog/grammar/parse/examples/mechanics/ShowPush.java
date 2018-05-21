package jcog.grammar.parse.examples.mechanics;

import jcog.grammar.parse.Assembly;
import jcog.grammar.parse.Parser;
import jcog.grammar.parse.Sequence;
import jcog.grammar.parse.tokens.Num;
import jcog.grammar.parse.tokens.Symbol;
import jcog.grammar.parse.tokens.TokenAssembly;

import java.util.Stack;

/**
 * Show the value of not pushing the element a terminal 
 * matches.
 * 
 * @author Steven J. Metsker
 *
 * @version 1.0 
 */
public class ShowPush {
	/**
	 * Show the value of not pushing the element a terminal 
	 * matches.
	 */
	public static void main(String[] args) {

		Parser open = new Symbol('(').discard();
		Parser close = new Symbol(')').discard();
		Parser comma = new Symbol(',').discard();

		Num num = new Num();

		Parser coord = new Sequence().add(open).add(num).add(comma).add(num).add(comma).add(num).add(close);

		Assembly out = coord.bestMatch(new TokenAssembly("(23.4, 34.5, 45.6)"));

		Stack<? extends Object> s = out.getStack();
		while (!s.empty()) {
			System.out.println(s.pop());
		}
	}
}
