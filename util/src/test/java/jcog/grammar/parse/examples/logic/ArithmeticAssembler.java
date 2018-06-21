package jcog.grammar.parse.examples.logic;

import jcog.grammar.parse.Assembly;
import jcog.grammar.parse.IAssembler;
import jcog.grammar.parse.examples.engine.ArithmeticOperator;
import jcog.grammar.parse.examples.engine.ArithmeticTerm;

/**
 * This assembler pops two arithmetic operands, builds an
 * ArithmeticOperator from them, and pushes it. 
 * 
 * @author Steven J. Metsker
 *
 * @version 1.0 
 */
public class ArithmeticAssembler implements IAssembler {
	/**
	 * the character which represents an arithmetic operator
	 */
    private char operator;

	/**
	 * Constructs an assembler that will stack an 
	 * ArithmeticOperator with the specified operator.
	 */
	public ArithmeticAssembler(char operator) {
		this.operator = operator;
	}

	/**
	 * Pop two arithmetic operands, build an ArithmeticOperator 
	 * from them, and push it. 
	 *
	 * @param  Assembly  the assembly to work on
	 */
	public void accept(Assembly a) {
		ArithmeticTerm operand1 = (ArithmeticTerm) a.pop();
		ArithmeticTerm operand0 = (ArithmeticTerm) a.pop();
		a.push(new ArithmeticOperator(operator, operand0, operand1));
	}
}
