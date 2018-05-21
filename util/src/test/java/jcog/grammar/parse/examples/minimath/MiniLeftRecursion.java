package jcog.grammar.parse.examples.minimath;

import jcog.grammar.parse.Alternation;
import jcog.grammar.parse.Sequence;
import jcog.grammar.parse.tokens.Num;
import jcog.grammar.parse.tokens.Symbol;
import jcog.grammar.parse.tokens.TokenAssembly;

/**
 * This class uses a problematic grammar for Minimath. For
 * a better grammar, see class <code>MinimathCompute</code>. 
 * Here, the grammar is:
 * 
 * <blockquote><pre>	
 *     e = Num | e '-' Num;
 * </pre></blockquote>
 *
 * Writing a parser directly from this grammar shows
 * that left recusion will hang a parser.
 *
 * @author Steven J. Metsker
 *
 * @version 1.0 
 */
public class MiniLeftRecursion {
	/**
	 * Demonstrates an infinite loop.
	 */
	public static void main(String args[]) {
		Alternation e = new Alternation();
		Num n = new Num();

		Sequence s = new Sequence();
		s.add(e);
		s.add(new Symbol('-').discard());
		s.add(n);

		e.add(n);
		e.add(s);

		// now hang (or crash)
		e.completeMatch(new TokenAssembly("25 - 16 - 9"));
	}
}
