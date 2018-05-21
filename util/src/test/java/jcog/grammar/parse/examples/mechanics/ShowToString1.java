package jcog.grammar.parse.examples.mechanics;

import jcog.grammar.parse.Alternation;
import jcog.grammar.parse.Assembly;
import jcog.grammar.parse.Repetition;
import jcog.grammar.parse.Sequence;
import jcog.grammar.parse.tokens.Literal;
import jcog.grammar.parse.tokens.TokenAssembly;

/**
 * Show how a simple composite prints itself.
 * 
 * @author Steven J. Metsker
 *
 * @version 1.0 
 */
public class ShowToString1 {
	/**
	 * Show how a simple composite prints itself.
	 */
	public static void main(String[] args) {

		Alternation adjective = new Alternation();
		adjective.add(new Literal("steaming"));
		adjective.add(new Literal("hot"));

		Sequence good = new Sequence();
		good.add(new Repetition(adjective));
		good.add(new Literal("coffee"));

		String s = "hot hot steaming hot coffee";
		Assembly a = new TokenAssembly(s);
		System.out.println(good.bestMatch(a));
		System.out.println(good);
	}
}
