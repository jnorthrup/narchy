/*
 * Bool.java
 *
 * Created on March 8, 2007, 5:24 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package alice.tuprologx.pj.model;

/**
 *
 * @author maurizio
 */
public class Bool extends Term<Bool> {
	final Boolean _theBool;
        
	
	@Override
    public <Z> Z toJava() { return uncheckedCast (_theBool); }
	
	public Bool (Boolean b) {_theBool = b;}
        
        @Override
        public alice.tuprolog.Term marshal() {
            return _theBool ? alice.tuprolog.Term.TRUE : alice.tuprolog.Term.FALSE;
        }
        
        static Bool unmarshal(alice.tuprolog.Struct b) {
            if (!matches(b))
                throw new UnsupportedOperationException();
            return b.isEqual(alice.tuprolog.Term.TRUE) ? new Bool(Boolean.TRUE) : new Bool(Boolean.FALSE);
        }
        
        static boolean matches(alice.tuprolog.Term t) {            
            return (!(t instanceof alice.tuprolog.Var) && (t.isEqual(alice.tuprolog.Term.TRUE) || t.isEqual(alice.tuprolog.Term.FALSE)));
        }
        
	public String toString() {
		return "Bool("+_theBool+ ')';
	}

}