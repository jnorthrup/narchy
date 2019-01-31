/*
 * JavaTerm.java
 *
 * Created on May 7, 2007, 11:04 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package alice.tuprologx.pj.model;

import alice.tuprologx.pj.annotations.Termifiable;

import java.util.HashMap;
import java.util.Vector;
/**
 *
 * @author maurizio
 */
public class JavaTerm<O> extends Compound<JavaTerm<O>> {

    public static final HashMap<String, Class<?>> hashtable = new HashMap<>();

    @SuppressWarnings("serial")
	static class TermifiableStruct<O> extends alice.tuprolog.Struct {
        JavaTerm<O> _term;

        TermifiableStruct(String name, alice.tuprolog.Term... arr) {
            super(name, arr);
        }

        TermifiableStruct<O> setJavaTerm(JavaTerm<O> term) {
            _term = term;
            return this;
        }

        JavaTerm<O> getJavaTerm() {
            return _term;
        }
    }

    final Class<?> _class;
    final java.util.Collection<Term<?>> _properties;

    public JavaTerm(O o) {        
        this(o.getClass(), getProperties(o));
    }

    public JavaTerm(Class<?> _class, java.util.Collection<Term<?>> properties) {
        this._class = _class;
        _properties = properties;
    }
    
    /** Creates a new instance of JavaTerm */
    /*private static List<Compound2<Atom,Term<?>>> getTermList(Object po) {
        try {
            java.util.Vector<Compound2<Atom,Term<?>>> termArr = new java.util.Vector<Compound2<Atom,Term<?>>>();
            java.beans.BeanInfo binfo = java.beans.Introspector.getBeanInfo(po.getClass());
            for (java.beans.PropertyDescriptor pdesc : binfo.getPropertyDescriptors()) {
                
                if (pdesc.getReadMethod()!=null && pdesc.getWriteMethod()!=null) { 
                    Object o = pdesc.getReadMethod().invoke(po);
                    Atom propertyName = new Atom(pdesc.getName());
                    Term<?> propertyValue = (o != null) ? Term.fromJava(o) : new Var(((String)propertyName.toJava()).toUpperCase());
                    termArr.addAt(new Compound2<Atom,Term<?>>("_property",propertyName, propertyValue));
                }
            }
            System.out.println(termArr);
            return new List<Compound2<Atom,Term<?>>>(termArr);            
        }                      
        catch (Exception e) {
            e.printStackTrace();
            throw new UnsupportedOperationException(e);
        }
    }*/
    private static Vector<Term<?>>  getProperties(Object _object) {
        Vector<Term<?>> termArr = null;
        try {
            termArr = new java.util.Vector<>();
            java.beans.BeanInfo binfo = java.beans.Introspector.getBeanInfo(_object.getClass());
            int count = 0;
            for (java.beans.PropertyDescriptor pdesc : binfo.getPropertyDescriptors()) {
                
                if (pdesc.getReadMethod()!=null && pdesc.getWriteMethod()!=null) {
                    Object o = pdesc.getReadMethod().invoke(_object);
                    Term<?> t = o != null ? Term.fromJava(o) : new Var<>("X" + count);
                    termArr.add(t);
                    count++;
                }
            }
            
            
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new UnsupportedOperationException(e);
        }
        if (termArr == null || termArr.isEmpty()) {
                throw new IllegalArgumentException();

            }
        return termArr;
    }

    /** Creates a new instance of JavaTerm */
    @Override
    public alice.tuprolog.Struct marshal() {
        try {
            
            alice.tuprolog.Term[] termArr = new alice.tuprolog.Term[_properties.size()];
            int count = 0;
            for (Term<?> term : _properties) {
                
                termArr[count] = term.marshal();
                count++;
            }
            
            hashtable.put(getName(), this._class);
            return new TermifiableStruct<O>(getName(), termArr).setJavaTerm(this);
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new UnsupportedOperationException(e);
        }
    }
    /*
    private static String getName(Object po) {
        try {
            java.util.Vector<Compound2<Atom,Term<?>>> termArr = new java.util.Vector<Compound2<Atom,Term<?>>>();
            java.beans.BeanInfo binfo = java.beans.Introspector.getBeanInfo(po.getClass());
            return binfo.getBeanDescriptor().getBeanClass().getName();
        }        
        catch (Exception e) {
            e.printStackTrace();
            throw new UnsupportedOperationException(e);
        }
    }
    */

    @Override
    public String getName() {
        return _class.getAnnotation(Termifiable.class).predicate();
    }

    public Class<?> getKlass() {
        return _class;
    }

    @Override
    public int arity() {
        return _properties.size();
    }

    @Override
    public <Z> Z toJava() {
        try {                
            Object po = _class.getConstructor().newInstance();
            java.beans.BeanInfo binfo = java.beans.Introspector.getBeanInfo(_class);
            
            java.util.Iterator<Term<?>> it = _properties.iterator();
            for (java.beans.PropertyDescriptor pdesc : binfo.getPropertyDescriptors()) {
                if (pdesc.getReadMethod()!=null && pdesc.getWriteMethod()!=null) {
                    Term<?> property = it.next();
                    /* ED 2013-05-21 */ Var<Term<?>> auxProperty = uncheckedCast(property);
                    
                    if (!((property instanceof Var) && (auxProperty).getValue()==null)) {
                        
                        pdesc.getWriteMethod().invoke(po, property.toJava());
                    }
                }
            }            
            
            return uncheckedCast(po);
        }
        catch (Exception e) {
            throw new UnsupportedOperationException(e);
        }
    }

    static boolean matches(alice.tuprolog.Term t) {






        return (t instanceof TermifiableStruct<?>) || ((t.term() instanceof alice.tuprolog.Struct) && hashtable.containsKey(((alice.tuprolog.Struct)t.term()).name()));
    }
    
    static <Z> JavaTerm<Z> unmarshalObject(alice.tuprolog.Struct s) {
        if (!matches(s))
            throw new UnsupportedOperationException();
        Class<?> termKlass = hashtable.get(s.name());
        Vector<Term<?>> terms = new Vector<>();
        for (int i = 0; i < s.subs() ; i ++) {
            terms.add(Term.unmarshal(s.sub(i)));
        }
        return new JavaTerm<>(termKlass, terms);
    }

    public String toString() {
        return getName() + _properties;
    }
}
