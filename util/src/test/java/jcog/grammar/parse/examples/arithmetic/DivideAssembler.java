package jcog.grammar.parse.examples.arithmetic;

import jcog.grammar.parse.Assembly;
import jcog.grammar.parse.IAssembler;

/**
 * Pop two numbers from the stack and push the result of
 * dividing the top number into the one below it.
 * 
 * @author Steven J. Metsker
 *
 * @version 1.0
 */
class DivideAssembler implements IAssembler {
	/**
	 * Pop two numbers from the stack and push the result of
	 * dividing the top number into the one below it.
	 * 
	 * @param   Assembly   the assembly whose stack to use
	 */
	public void accept(Assembly a) {
		Double d1 = (Double) a.pop();
		Double d2 = (Double) a.pop();
		Double d3 = new Double(d2.doubleValue() / d1.doubleValue());
		a.push(d3);
	}
}
