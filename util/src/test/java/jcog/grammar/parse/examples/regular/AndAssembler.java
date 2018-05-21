package jcog.grammar.parse.examples.regular;

import jcog.grammar.parse.Assembly;
import jcog.grammar.parse.IAssembler;
import jcog.grammar.parse.Parser;
import jcog.grammar.parse.Sequence;

/**
 * Pop two Parsers from the stack and push a new <code>
 * Sequence</code> of them.
 * 
 * @author Steven J. Metsker
 *
 * @version 1.0
 */
public class AndAssembler implements IAssembler {
	/**
	 * Pop two parsers from the stack and push a new 
	 * <code>Sequence</code> of them.
	 * 
	 * @param   Assembly   the assembly whose stack to use
	 */
	public void workOn(Assembly a) {
		Object top = a.pop();
		Sequence s = new Sequence();
		s.add((Parser) a.pop());
		s.add((Parser) top);
		a.push(s);
	}
}
