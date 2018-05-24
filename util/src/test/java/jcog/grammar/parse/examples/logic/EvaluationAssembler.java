package jcog.grammar.parse.examples.logic;

import jcog.grammar.parse.Assembly;
import jcog.grammar.parse.IAssembler;
import jcog.grammar.parse.examples.engine.Evaluation;
import jcog.grammar.parse.examples.engine.Term;

/**
 * Pops two terms, constructs an Evaluation from these terms,
 * and pushes it.
 * 
 * @author Steven J. Metsker
 *
 * @version 1.0
 */
public class EvaluationAssembler implements IAssembler {
	/**
	 * Pops two terms, constructs an Evaluation from these terms,
	 * and pushes it.
	 *
	 * @param  Assembly  the assembly to work on
	 */
	public void accept(Assembly a) {
		Term second = (Term) a.pop();
		Term first = (Term) a.pop();
		a.push(new Evaluation(first, second));
	}
}
