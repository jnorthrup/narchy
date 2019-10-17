package jcog.grammar.parse.examples.logic;

import jcog.grammar.parse.Assembly;
import jcog.grammar.parse.IAssembler;
import jcog.grammar.parse.examples.engine.Rule;
import jcog.grammar.parse.examples.engine.Structure;

import java.util.Stack;
import java.util.stream.IntStream;

/**
 * Pops the structures of a rule from an assembly's stack, 
 * and constructs and pushes a rule.
 * 
 * @author Steven J. Metsker
 *
 * @version 1.0
 */
class AxiomAssembler implements IAssembler {
	/**
	 * Pops all of the structures on the stack, builds a rule
	 * from them, and pushes it.
	 *
	 * @param  Assembly  the assembly to work on
	 */
	public void accept(Assembly a) {
		Stack s = a.getStack();
		Structure[] sa = IntStream.range(0, s.size()).mapToObj(i -> (Structure) s.elementAt(i)).toArray(Structure[]::new);
        s.removeAllElements();
		a.push(new Rule(sa));
	}
}
