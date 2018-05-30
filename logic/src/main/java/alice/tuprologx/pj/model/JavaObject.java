/*
 * JavaObject.java
 *
 * Created on July 19, 2007, 11:23 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package alice.tuprologx.pj.model;

import alice.tuprologx.pj.engine.PJ;

/**
 *
 * @author maurizio
 */
public class JavaObject<O> extends Term<JavaObject<O>> {
    
    final O _theObject;
    
    /** Creates a new instance of JavaObject */
    public JavaObject(O o) {
        _theObject = o;
    }

    @Override
    public alice.tuprolog.Term marshal() {
        return PJ.registerJavaObject(_theObject);
    }

    @Override
    public <Z> Z toJava() {
        
        return uncheckedCast(_theObject);
    }
    
    static boolean matches(alice.tuprolog.Term t) {        
        return (t instanceof alice.tuprolog.Struct && PJ.getRegisteredJavaObject((alice.tuprolog.Struct)t) != null);        
    }
    
    static <Z> JavaObject<Z> unmarshalObject(alice.tuprolog.Struct s) {
        if (matches(s)) {           
            
        	Z auxJavaObject = uncheckedCast(PJ.getRegisteredJavaObject(s));
            return new JavaObject<>(auxJavaObject);
        }
        else
            throw new UnsupportedOperationException();
    }

    public String toString() {        
        return "JavaObject{"+_theObject+ '}';
    }
    
    
    
}
