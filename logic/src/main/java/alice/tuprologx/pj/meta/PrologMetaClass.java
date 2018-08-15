/*
 * MetaPrologClass.java
 *
 * Created on 5 aprile 2007, 9.16
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package alice.tuprologx.pj.meta;

import alice.tuprologx.pj.annotations.PrologClass;
import alice.tuprologx.pj.annotations.PrologField;
import alice.tuprologx.pj.annotations.PrologMethod;
import alice.tuprologx.pj.model.Theory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Vector;
/**
 *
 * @author Maurizio
 */
public class PrologMetaClass {
    
    private final Class<?> _theClass;
    
    private PrologMetaField[] _fields;
    private PrologMetaMethod[] _methods;
    private Theory _theory;
    
    /** Creates a new instance of MetaPrologClass */
    public PrologMetaClass(Class<?> cl) {
        _theClass = cl.getSuperclass();
        
        initTheory();
        initPrologFields();
        initPrologMethods();        
    }
    
    private void initPrologFields() {        
        Vector<PrologMetaField> temp = new Vector<>();
        Field[] fields = _theClass.getFields();
        for (Field f : fields) {
            if (f.isAnnotationPresent(PrologField.class)) {
                temp.add(new PrologMetaField(this,f));
            }
        }
        _fields = new PrologMetaField[temp.size()];
        _fields = temp.toArray(_fields);
    }
    
    private void initPrologMethods() {
        Vector<PrologMetaMethod> temp = new Vector<>();
        Method[] methods = _theClass.getMethods();
        for (Method m : methods) {
            if (m.isAnnotationPresent(PrologMethod.class)) {
                temp.add(new PrologMetaMethod(this,m));
            }
        }
        _methods = new PrologMetaMethod[temp.size()];
        _methods = temp.toArray(_methods);
    }
    
    public void initTheory() {
        String theory = "";
        Class<?> cl = _theClass;
        while (!cl.getName().equals("java.lang.Object")) {
            PrologClass pa = cl.getAnnotation(PrologClass.class);
            if (pa != null) {
                String[] clauses = pa.clauses();
                for (String clause : clauses) {
                    theory += clause + '\n';
                }                                   
            }
            cl = cl.getSuperclass(); 
        }                
        _theory = new Theory(theory);		                
    }
    
    public PrologMetaField[] getPrologFields() {
        return _fields;
    }
    
    public PrologMetaField getPrologField(Field jf) {
        for (PrologMetaField f : _fields) {
            if (f.getJavaField() == jf) {
                return f;
            }
        }
        return null;
    }
    
    public PrologMetaMethod[] getPrologMethods() {
        return _methods;
    }
    
    public PrologMetaMethod getPrologMethod(Method jm) {                
        for (PrologMetaMethod m : _methods) {            
            if (jm.equals(m.getJavaMethod()))
                return m;
        }
        return null;
    }
    
    public Theory getTheory() {
        return _theory;
    }
    
    public void setTheory(Theory t) {
        _theory = t;
        for (PrologMetaMethod pmm : getPrologMethods()) {
            pmm.initClauses();
        }
    }
    
    public Class<?> getJavaClass() {
        return _theClass;
    }
}
