package jcog.grammar.parse.examples.logic;

import jcog.grammar.parse.Assembly;
import jcog.grammar.parse.IAssembler;
import jcog.grammar.parse.examples.engine.Atom;
import jcog.grammar.parse.examples.engine.NumberFact;
import jcog.grammar.parse.tokens.Token;

/**
 * Exchanges a token on an assembly's stack with an atom 
 * that has the token's value as its functor. 
 * 
 * @author Steven J. Metsker
 *
 * @version 1.0 
 */
public class AtomAssembler implements IAssembler {
	/**
	 * Exchanges a token on an assembly's stack with an atom 
	 * that has the token's value as its functor. In the case
	 * of a quoted string, this assembler removes the quotes,
	 * so that a string such as "Smith" becomes just Smith. In
	 * the case of a number, this assembler pushes a NumberFact.
	 *
	 * @param  Assembly  the assembly to work on
	 */
	public void accept(Assembly a) {
		Token t = (Token) a.pop();
		
		if (t.isQuotedString()) {
			String s = t.sval();
			a.push(new Atom(s.substring(1, s.length() - 1)));
		} else if (t.isNumber()) {
			a.push(new NumberFact(t.nval()));
		} else {
			a.push(new Atom(t.value()));
		}
	}
}
