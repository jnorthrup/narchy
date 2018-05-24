package jcog.grammar.parse.examples.logic;

import jcog.grammar.parse.Assembly;
import jcog.grammar.parse.IAssembler;
import jcog.grammar.parse.examples.engine.Comparison;
import jcog.grammar.parse.examples.engine.ComparisonTerm;
import jcog.grammar.parse.tokens.Token;

/**
 * Pops two comparison terms and an operator, builds 
 * the comparison, and pushes it.
 * 
 * @author Steven J. Metsker
 *
 * @version 1.0 
 */
public class ComparisonAssembler implements IAssembler {
	/**
	 * Pops two comparison terms and an operator, builds 
	 * the comparison, and pushes it.
	 *
	 * @param  Assembly  the assembly to work on
	 */
	public void accept(Assembly a) {
		ComparisonTerm second = (ComparisonTerm) a.pop();
		ComparisonTerm first = (ComparisonTerm) a.pop();
		Token t = (Token) a.pop();
		a.push(new Comparison(t.sval(), first, second));
	}
}
