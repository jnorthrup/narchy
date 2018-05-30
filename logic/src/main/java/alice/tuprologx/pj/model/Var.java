/*
 * Var.java
 *
 * Created on March 8, 2007, 5:06 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package alice.tuprologx.pj.model;

import java.lang.reflect.Method;

/**
 *
 * @author maurizio
 */
public class Var<X extends Term<?>> extends Term<X> {
		X _theValue;
        String _theName;
        private static final java.lang.reflect.Method _setLink;
        
        static {
            Method sl;
            try {
                sl = alice.tuprolog.Var.class.getDeclaredMethod("setLink", alice.tuprolog.Term.class);
                sl.setAccessible(true);
            }
            catch(Exception e) {
                sl = null;
                e.printStackTrace();
            }
            _setLink = sl;
        }

	public Var(String name) {_theName = name;}
        
    
    private Var(String name, Term<?> val) {
    	_theName = name; _theValue = uncheckedCast(val);
    }
    		
	@Override
    public <Z> Z toJava() {
		
		return uncheckedCast( _theValue != null ? _theValue.toJava() : this );
	}
        
    public X getValue() {return _theValue;}
    
    @Override
    public alice.tuprolog.Var marshal() {
        try {
            alice.tuprolog.Var v= new alice.tuprolog.Var(_theName);                 
            if (_theValue != null) {
                setLink(v, _theValue.marshal());
            }
            return v;
        }
        catch(Exception e) {
            throw new UnsupportedOperationException(e);
            
        }
    }
    
    static Term<?> unmarshal(alice.tuprolog.Var a) {
        if (!matches(a))
            throw new UnsupportedOperationException();
        
        return a.isBound() ? Term.unmarshal(a.term()) : new Var<>(a.name(), null);
    }
    
    static boolean matches(alice.tuprolog.Term t) {
        return (t instanceof alice.tuprolog.Var);
    }
    
    public String getName() {
        return _theName;
    }
    
    public String toString() {
		return "Var("+_theName+(_theValue != null ? "/"+_theValue : "")+ ')';
	}
        
    private static void setLink(alice.tuprolog.Var v, Object o) {
        try {                
            _setLink.invoke(v,o);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
}