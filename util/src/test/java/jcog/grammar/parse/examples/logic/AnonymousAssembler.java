package jcog.grammar.parse.examples.logic;

import jcog.grammar.parse.Assembly;
import jcog.grammar.parse.IAssembler;
import jcog.grammar.parse.examples.engine.Anonymous;

/**
 * Pushes an anonymous variable onto an assembly's stack.
 * 
 * @author Steven J. Metsker
 *
 * @version 1.0
 */
public class AnonymousAssembler implements IAssembler {
	/**
	 * Pushes an anonymous variable onto an assembly's stack.
	 *
	 * @param  Assembly  the assembly to work on
	 */
	public void accept(Assembly a) {
		a.push(new Anonymous());
	}
}
