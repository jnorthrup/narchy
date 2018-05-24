package jcog.grammar.parse.examples.logic;

import jcog.grammar.parse.Assembly;
import jcog.grammar.parse.IAssembler;
import jcog.grammar.parse.examples.engine.Variable;
import jcog.grammar.parse.tokens.Token;

/**
 * Pops a string like "X" or "Person" from an assembly's stack
 * and pushes a variable with that name.
 * 
 * @author Steven J. Metsker
 *
 * @version 1.0
 */
public class VariableAssembler implements IAssembler {
	/**
	 * Pops a string like "X" or "Person" from an assembly's stack
	 * and pushes a variable with that name.
	 *
	 * @param  Assembly  the assembly to work on
	 */
	public void accept(Assembly a) {
		Token t = (Token) a.pop();
		String name = t.sval();
		a.push(new Variable(name));
	}
}
