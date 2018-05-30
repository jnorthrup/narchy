/*
 * Int.java
 *
 * Created on March 8, 2007, 5:25 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package alice.tuprologx.pj.model;

import alice.tuprolog.NumberTerm;

/**
 *
 * @author maurizio
 */
public class Int extends Term<Int> {
	final Integer _theInt;

	@Override
    public <Z> Z/*Integer*/ toJava() {
		
		return uncheckedCast(_theInt);
	}
	
	public Int (Integer i) {_theInt = i;}
        
        @Override
        public NumberTerm.Int marshal() {
            return new NumberTerm.Int(_theInt);
        }
        
        static Int unmarshal(NumberTerm.Int i) {
            if (!matches(i))
                throw new UnsupportedOperationException();
            return new Int(i.intValue());
        }
        
        static boolean matches(alice.tuprolog.Term t) {
            return (t instanceof NumberTerm.Int);
        }
        
	public String toString() {
		return "Int("+_theInt+ ')';
	}

}