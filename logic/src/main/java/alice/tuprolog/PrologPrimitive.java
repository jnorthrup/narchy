/*
 * tuProlog - Copyright (C) 2001-2002  aliCE team at deis.unibo.it
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package alice.tuprolog;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;


/**
 * Primitive class
 * referring to a builtin predicate or functor
 *
 * @see Struct
 */
public class PrologPrimitive {
    
    public final static int DIRECTIVE  = 0;
    public final static int PREDICATE  = 1;
    public final static int FUNCTOR    = 2;
    
    public final int type;
    /**
	 * method to be call when evaluating the built-in
	 */
    private final Method method;
    /**
	 * lib object where the builtin is defined
	 */
    public final Library source;
    public final int arity;
    /**
	 * for optimization purposes
	 */
    //private final Object[] primitive_args;
    public final String key;
    private final MethodHandle mh;


    public PrologPrimitive(int type, String key, Library lib, Method m, int arity) throws NoSuchMethodException {
        if (m==null) {
            throw new NoSuchMethodException();
        }
        this.type = type;
        this.key = key;
        source = lib;
        method = m;
        try {
            m.setAccessible(true);
            if (Modifier.isStatic(m.getModifiers()))
                mh = MethodHandles.lookup().unreflect(m);
            else
                mh = MethodHandles.lookup().unreflect(m).bindTo(source);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        this.arity = arity;
    }

    private Object[] newArgs() { return new Object[arity]; }


    /**
     * evaluates the primitive as a directive
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws Exception if invocation directive failure
     */
    public void evalAsDirective(Struct g) throws Throwable {
        Object[] primitive_args = newArgs();
        for (int i=0; i<primitive_args.length; i++) {
            primitive_args[i] = g.subResolve(i);
        }
        //method.invoke(source,primitive_args);
        try {
            mh.invokeWithArguments(primitive_args);
        } catch (Throwable throwable) {
            throw throwable.getCause();
        }
    }


    /**
     * evaluates the primitive as a predicate
     * @throws Exception if invocation primitive failure
     */
    public boolean evalAsPredicate(Struct g) throws Throwable {
        Object[] primitive_args = newArgs();
        for (int i=0; i<primitive_args.length; i++) {
            primitive_args[i] = g.sub(i);
        }
        try {
        	//System.out.println("PRIMITIVE INFO evalAsPredicate sto invocando metodo "+method.getName());
            return (boolean)mh.invokeWithArguments(primitive_args);
            //return (Boolean) method.invoke(source, primitive_args);
        } catch (InvocationTargetException e) {
            // throw new Exception(e.getCause());
            throw //new JavaException
                    (e.getCause());
        }
    }


    /**
     * evaluates the primitive as a functor
     * @throws Throwable
     */
    public Term evalAsFunctor(Struct g) throws Throwable {
//        try {
        Object[] primitive_args = newArgs();
            for (int i=0; i<primitive_args.length; i++) {
                primitive_args[i] = g.subResolve(i);
            }
            return (Term)mh.invokeWithArguments(primitive_args);
            //return ((Term)method.invoke(source,primitive_args));
//        } catch (Exception ex) {
//            //throw ex.getCause();
//            throw ex;
//        }
    }



    public String toString() {
        return "[ primitive: method "+method.getName()+" - "
                //+primitive_args+" - N args: "+primitive_args.length+" - "
                +source.getClass().getName()+" ]\n";
    }
    
}