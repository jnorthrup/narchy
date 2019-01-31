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
package alice.tuprologx.pj.lib;

import alice.tuprolog.*;
import alice.tuprolog.lib.InvalidObjectIdException;
import alice.tuprolog.lib.OOLibrary;
import alice.tuprologx.pj.annotations.PrologClass;
import alice.tuprologx.pj.annotations.PrologField;
import alice.tuprologx.pj.annotations.PrologMethod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;


/**
 *
 * This class represents a tuProlog library enabling the interaction
 * with the Java environment from tuProlog.
 *
 * Works only with JDK 1.2 (because of setAccessible method)
 *
 * The most specific method algorithm used to find constructors / methods
 * has been inspired by the article
 *     "What Is Interactive Scripting?", by Michael Travers
 *     Dr. Dobb's -- Software Tools for the Professional Programmer
 *     January 2000
 *     CMP  Media Inc., a United News and Media Company
 *
 * Library/Theory Dependency:  BasicLibrary
 *
 *
 *
 */
@SuppressWarnings("serial")
public class PJLibraryNew extends OOLibrary {
	
	/**
	 * library theory
	 */
	@Override
    public String getTheory() {
		return
		
		
		
		":- op(800,xfx,'<-').\n" +
        ":- op(800,xfx,':=').\n" +
        ":- op(850,fy,'returns').\n" +
        ":- op(200,xfx,'as').\n" +
		":- op(600,xfx,'.'). \n" +
		
		
		
		
		
		"java_object_bt(ClassName,Args,Id):- java_object(ClassName,Args,Id).\n" +
		"java_object_bt(ClassName,Args,Id):- destroy_object(Id).\n" +
        "new_object(ClassName,Args,Id):- prolog_class(ClassName), java_object_prolog(ClassName, Args, Id).\n" +
        "new_object(ClassName,Args,Id):- !, java_object_std(ClassName, Args, Id).\n" +
		"Obj <- What :- java_call1(Obj,What,Res), Res \\== false.\n" +
		"Obj <- What returns Res :- java_call1(Obj,What,Res).\n" +
        "java_call1('.'(C,F), setAt(X), Res):-lookup_field(C, F, Field), java_access(C, F, Field, setAt(X), Res).\n" +
        "java_call1('.'(C,F), get(X), Res):-lookup_field(C, F, Field), java_access(C, F, Field, get(X), Res).\n" +
        "java_call1(Obj, What, Res):-java_call2(Obj, What, Res).\n" +
        "java_call2(Obj, What, Res):-lookup_method(Obj, What, Meth), not prolog_method(Meth), !, java_method_call(Obj, Meth, What, Res, false, false).\n" +
        "java_call2(Obj, What, Res):-unmarshal_method(What, M2), lookup_method(Obj, M2, Meth), prolog_call_rest(Obj, Meth, M2, Res).\n" +
        "prolog_call_rest(Obj, Meth, What, Res):-is_iterable(Meth), !, java_method_call(Obj, Meth, What, R2, true, true), R2 <- iterator returns I, next(I, E), marshal(E, Res).\n" +
        "prolog_call_rest(Obj, Meth, What, Res):-!, java_method_call(Obj, Meth, What, R2, true, false), marshal(R2, Res).\n" +
        "java_access(C, F, Field, get(X), Res):-prolog_field(Field), !, java_get(C, F, Y), marshal(Y, X).\n" +
        "java_access(C, F, Field, setAt(X), Res):-prolog_field(Field), !, unmarshal(X, Y), java_set(C, F, Y).\n" +
        "java_access(C, F, Field, get(X), Res):-java_get(C, F, X).\n" +
        "java_access(C, F, Field, setAt(X), Res):-java_set(C, F, X).\n" +
		"java_array_set(Array,Index,Object):-           class('java.lang.reflect.Array') <- setAt(Array as 'java.lang.Object',Index,Object as 'java.lang.Object'),!.\n" +
		"java_array_set(Array,Index,Object):-			java_array_set_primitive(Array,Index,Object).\n"+
		"java_array_get(Array,Index,Object):-           class('java.lang.reflect.Array') <- get(Array as 'java.lang.Object',Index) returns Object,!.\n" +
		"java_array_get(Array,Index,Object):-       java_array_get_primitive(Array,Index,Object).\n"+
        "add_rule(Clause):-this(Obj), pj_assert(Obj, Clause), assert(Clause).\n"+
        "remove_rule(Clause):-this(Obj), pj_retract(Obj, Clause), retract(Clause).\n"+
        "remove_rules(Clause):-this(Obj), pj_retract_all(Obj, Clause), retractAll(Clause).\n"+
        "next(Iterable, Element) :-Iterable <- hasNext, next2(Iterable, Element).\n" +
        "next2(Iterable, Element) :- Iterable <- next returns Element.\n" +
        "next2(Iterable, Element) :- Iterable <- hasNext, next(Iterable, Element).\n" +

		"java_array_length(Array,Length):-              class('java.lang.reflect.Array') <- getLength(Array as 'java.lang.Object') returns Length.\n" +
		"java_object_string(Object,String):-    Object <- toString returns String.    \n";
	}
	
	

    public static boolean prolog_class_1(Term classname) {
        if (!classname.isAtomic())
            return false;
        Class<?> clazz = null;
        try {
             clazz = Class.forName(((Struct)classname.term()).name());
        }
        catch (Throwable ex) {
            return false;
        }
        return clazz != null && clazz.isAnnotationPresent(PrologClass.class);
    }

    public boolean prolog_method_1(Term method) {
        if (!method.isAtomic())
            return false;
        Object o = null;
        try {
             o = getRegisteredDynamicObject((Struct)method.term());
        }
        catch (InvalidObjectIdException ex) {
            return false;
        }
        if (!(o instanceof Method))
            return false;
        else {
            Method m = (Method)o;
            try {
            return m.getDeclaringClass().getSuperclass().getDeclaredMethod(m.getName(),m.getParameterTypes()).isAnnotationPresent(PrologMethod.class);
            }
            catch (Exception e) {
                return false;
            }
        }
    }

    public boolean prolog_field_1(Term method) {
        if (!method.isAtomic())
            return false;
        Object o = null;
        try {
             o = getRegisteredDynamicObject((Struct)method.term());
        }
        catch (InvalidObjectIdException ex) {
            return false;
        }
        if (!(o instanceof Field))
            return false;
        else {
            Field f = (Field)o;
            return f.isAnnotationPresent(PrologField.class);
        }
    }

    public boolean java_method_1(Term method) {
        if (!method.term().isAtomic())
            return false;
        Object o = null;
        try {
             o = getRegisteredDynamicObject((Struct)method.term());
        }
        catch (InvalidObjectIdException ex) {
            return false;
        }
        return o instanceof Method;
    }

    public boolean is_iterable_1(Term method) {
        if (!method.term().isAtomic())
            return false;
        Object o = null;
        try {
             o = getRegisteredDynamicObject((Struct)method.term());
        }
        catch (InvalidObjectIdException ex) {
            return false;
        }
        if (!(o instanceof Method))
            return false;
        else {
            Method m = (Method)o;
            return m.getReturnType() == Iterable.class;
        }
    }

    public boolean java_field_1(Term method) {
        if (!method.term().isAtomic())
            return false;
        Object o = null;
        try {
             o = getRegisteredDynamicObject((Struct)method.term());
        }
        catch (InvalidObjectIdException ex) {
            return false;
        }
        return o instanceof Field;
    }
    
    public boolean marshal_2(Term term, Term marshalledTerm) {
        if (!term.isAtomic())
            return false;
        Object o = null;
        try {
             o = getRegisteredDynamicObject((Struct)term.term());
        }
        catch (InvalidObjectIdException ex) {
            return false;
        }
        if (!(o instanceof alice.tuprologx.pj.model.Term<?>))
            return unify(marshalledTerm, term);
        else {
            alice.tuprologx.pj.model.Term<?> t = (alice.tuprologx.pj.model.Term<?>)o;
            return unify(marshalledTerm, t.marshal());
        }            
    }
    
    public boolean unmarshal_2(Term term, Term unmarshalledTerm) {
        Object o = alice.tuprologx.pj.model.Term.unmarshal(term.term());
        return unify(unmarshalledTerm, registerDynamic(o));
    }

    public boolean unmarshal_method_2(Term term, Term unmarshalledTerm) {
        if (! (term.term() instanceof Struct) )
            return false;
        Struct methodInfo = (Struct)term.term();
        Term[] terms = new Term[methodInfo.subs()];
        for (int i = 0; i < methodInfo.subs() ; i ++) {
            terms[i] = registerDynamic(alice.tuprologx.pj.model.Term.unmarshal(methodInfo.subResolve(i).term()));
        }
        return unify(unmarshalledTerm, new Struct(methodInfo.name(), terms));
    }

    

    public boolean lookup_field_3(Term receiver, Term name, Term result) {
        if (!receiver.isAtomic())
            return false;
        Object o = null;
        try {
             o = getRegisteredDynamicObject((Struct)receiver.term());
        }
        catch (InvalidObjectIdException ex) {
            return false;
        }
        if (o == null)
            return false;
        else {
            String fname = ((Struct)name.term()).name();
            try {
                return bindDynamicObject(result, o.getClass().getField(fname));
            }
            catch (Exception e) {
                return false;
            }
        }
    }

    public boolean lookup_method_3(Term receiver, Term method, Term result) {
        if (!receiver.isAtomic())
            return false;
        Object o = null;
        try {
             o = getRegisteredDynamicObject((Struct)receiver.term());
        }
        catch (InvalidObjectIdException ex) {
            return false;
        }
        if (o == null)
            return false;
        else {
            Struct methodInfo = (Struct)method.term();
            String mname = methodInfo.name();
            Signature sig = parseArg(methodInfo);
            Method m = null;
            try {
                m = new MethodFinder(o.getClass()).findMethod(mname, sig.getTypes());
            }
            catch (Exception e2) {
                return false;
            }
            return bindDynamicObject(result, m);
        }
    }

    private static Object[] getArrayFromList(Struct list) {
		Object args[] = new Object[list.listSize()];
		Iterator<? extends Term> it = list.listIterator();
		int count = 0;
		while (it.hasNext()) {
			args[count++] = it.next();
		}
		return args;
	}


    public boolean java_object_std_3(Term className, Term args, Term id) {
        if (!className.isAtomic() && !args.isList())
            return false;
        String clazz = ((Struct)className.term()).name();
        Signature sig = parseArg(getArrayFromList((Struct)args.term()));
        Object o = null;
        try {
            Constructor<?> c = new MethodFinder(Class.forName(clazz)).findConstructor(sig.getTypes());
            o = c.newInstance(sig.values);
        }
        catch (Exception e2) {
            return false;
        }
        return bindDynamicObject(id, o);
    }

    public boolean java_object_prolog_3(Term className, Term args, Term id) {
        if (!className.isAtomic() && !args.isList())
            return false;
        Signature sig = parseArg(getArrayFromList((Struct)args.term()));
        assert sig.types.length == 0;
        Object o = null;
        try {
            Class<?> clazz = Class.forName(((Struct) className.term()).name());
            o = alice.tuprologx.pj.engine.PJ.newInstance(clazz);
        }
        catch (Exception e2) {
            return false;
        }
        return bindDynamicObject(id, o);
    }

    public boolean java_method_call_6(Term objId, Term method, Term method_info, Term idResult, Term isProlog, Term isReentrant) {
        if (!method.isAtomic())
            return false;
        Object o = null;
        try {
             o = getRegisteredDynamicObject((Struct)method.term());
        }
        catch (InvalidObjectIdException ex) {
            return false;
        }
        if (!(o instanceof Method)) {
            Prolog.warn("Method not found: " + method);
            return false;
        }
        else {
            Method m = (Method)o;
            Object receiver = null;
            try {
                receiver = getRegisteredDynamicObject((Struct)objId.term());
            }
            catch (Exception e) {}
            Object res = null;
            try {
                Object[] args = parseArg((Struct)method_info.term()).getValues();
                if (isProlog.isAtomic() && ((Struct)isProlog).name().equals("true")) {
                    boolean reentrant = isProlog.isAtomic() && ((Struct)isReentrant).name().equals("true");
                    res = alice.tuprologx.pj.engine.PJ.call(receiver, m, args, reentrant);
                }
                else {
                    res = m.invoke(receiver, args);
                }
            } catch (Throwable ex) {
                Prolog.warn("Method invocation failed: " + method);
                ex.printStackTrace();
                return false;
            }
            return parseResult(idResult, res);
        }
    }
	
	/*
	 * set the field value of an object
	 */
	public boolean java_set_3(Term objId, Term fieldTerm, Term what) {
		
		what = what.term();
		if (!fieldTerm.isAtomic() || what instanceof Var)
			return false;
        fieldTerm = fieldTerm.term();
        objId = objId.term();
		String fieldName = ((Struct) fieldTerm).name();
        try {
			Class<?> cl = null;
            Object obj = null;
            if (objId.isCompound() &&
					((Struct) objId).subs() == 1 && ((Struct) objId).name().equals("class")) {
				String clName = alice.util.Tools.removeApostrophes(((Struct) objId).sub(0).toString());
				try {
					cl = Class.forName(clName);
				} catch (ClassNotFoundException ex) {
					Prolog.warn("Java class not found: " + clName);
					return false;
				} catch (Exception ex) {
					Prolog.warn("Static field " + fieldName + " not found in class " + alice.util.Tools.removeApostrophes(((Struct) objId).sub(0).toString()));
					return false;
				}
			} else {				
				obj = getRegisteredDynamicObject((Struct)objId.term());
				if (obj != null) {
					cl = obj.getClass();
				} else {
					return false;
				}
			}
			
			
			Field field = cl.getField(fieldName);
			if (what instanceof NumberTerm) {
				NumberTerm wn = (NumberTerm) what;
				if (wn instanceof NumberTerm.Int) {
					field.setInt(obj, wn.intValue());
				} else if (wn instanceof NumberTerm.Double) {
					field.setDouble(obj, wn.doubleValue());
				} else if (wn instanceof NumberTerm.Long) {
					field.setLong(obj, wn.longValue());
				} else if (wn instanceof NumberTerm.Float) {
					field.setFloat(obj, wn.floatValue());
				} else {
					return false;
				}
			} 
            else {                    
                    Object obj2 = getRegisteredDynamicObject((Struct)what.term());
                    if (obj2 != null) {
                        field.set(obj, obj2);
                    } else {
                        
                        field.set(obj, what.toString());
                    }
                }
			
			return true;
		} catch (NoSuchFieldException ex) {
			Prolog.warn("Field " + fieldName + " not found in class " + objId);
			return false;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}

    public boolean pj_assert_2(Term obj, Term clause) {
        if (!obj.isAtomic())
            return false;
        Object o = null;
        try {
             o = getRegisteredDynamicObject((Struct)obj.term());
        }
        catch (InvalidObjectIdException ex) {
            return false;
        }
        if (!(o instanceof alice.tuprologx.pj.engine.PrologObject))
            return false;
        else {
            alice.tuprologx.pj.engine.PJ.assertClause((alice.tuprologx.pj.engine.PrologObject)o, clause);
            return true;
        }
    }

    public boolean pj_retract_2(Term obj, Term clause) {
        if (!obj.isAtomic())
            return false;
        Object o = null;
        try {
             o = getRegisteredDynamicObject((Struct)obj.term());
        }
        catch (InvalidObjectIdException ex) {
            return false;
        }
        if (!(o instanceof alice.tuprologx.pj.engine.PrologObject))
            return false;
        else {
            alice.tuprologx.pj.engine.PJ.retractClause((alice.tuprologx.pj.engine.PrologObject)o, clause);
            return true;
        }
    }

    public boolean pj_retract_all_2(Term obj, Term clause) {
        if (!obj.isAtomic())
            return false;
        Object o = null;
        try {
             o = getRegisteredDynamicObject((Struct)obj.term());
        }
        catch (InvalidObjectIdException ex) {
            return false;
        }
        if (!(o instanceof alice.tuprologx.pj.engine.PrologObject))
            return false;
        else {
            alice.tuprologx.pj.engine.PJ.retractAllClauses((alice.tuprologx.pj.engine.PrologObject)o, clause);
            return true;
        }
    }
	
	/*
	 * get the value of the field
	 */
	public boolean java_get_3(Term objId, Term fieldTerm, Term what) {
		
		if (!fieldTerm.isAtomic()) {
			return false;
		}
        fieldTerm = fieldTerm.term();
        objId = objId.term();

		String fieldName = ((Struct) fieldTerm).name();
        try {
			Class<?> cl = null;
            Object obj = null;
            if (objId.isCompound() &&
					((Struct) objId).subs() == 1 && ((Struct) objId).name().equals("class")) {
				String clName = alice.util.Tools.removeApostrophes(((Struct) objId).sub(0).toString());
				try {
					cl = Class.forName(clName);
				} catch (ClassNotFoundException ex) {
					Prolog.warn("Java class not found: " + clName);
					return false;
				} catch (Exception ex) {
					Prolog.warn("Static field " + fieldName + " not found in class " + alice.util.Tools.removeApostrophes(((Struct) objId).sub(0).toString()));
					return false;
				}
			} else {
				obj = getRegisteredDynamicObject((Struct)objId.term());
				if (obj == null) {
					return false;
				}
				cl = obj.getClass();
			}
			
			Field field = cl.getField(fieldName);
			Class<?> fc = field.getType();
			
			field.setAccessible(true);
			
			
			if (fc.equals(Integer.TYPE) || fc.equals(Byte.TYPE)) {
				int value = field.getInt(obj);
				return unify(what, new NumberTerm.Int(value));
			} else if (fc.equals(java.lang.Long.TYPE)) {
				long value = field.getLong(obj);
				return unify(what, new NumberTerm.Long(value));
			} else if (fc.equals(java.lang.Float.TYPE)) {
				float value = field.getFloat(obj);
				return unify(what, new NumberTerm.Float(value));
			} else if (fc.equals(java.lang.Double.TYPE)) {
				double value = field.getDouble(obj);
				return unify(what, new NumberTerm.Double(value));
			} else {
				
				Object res = field.get(obj);
				return bindDynamicObject(what, res);
			}
			
			
			
			
		} catch (NoSuchFieldException ex) {
			Prolog.warn("Field " + fieldName + " not found in class " + objId);
			return false;
		} catch (Exception ex) {
			Prolog.warn("Generic error in accessing the field");
			
			return false;
		}
	}	
	
	/**
	 * creation of method signature from prolog data
	 */
	private Signature parseArg(Struct method) {
		Object[] values = new Object[method.subs()];
		Class<?>[] types = new Class[method.subs()];
		for (int i = 0; i < method.subs(); i++) {
			if (!parse_arg(values, types, i, method.subResolve(i)))
				return null;
		}
		return new Signature(values, types);
	}

    private Signature parseArg(Object... objs) {
		Object[] values = new Object[objs.length];
		Class<?>[] types = new Class[objs.length];
		for (int i = 0; i < objs.length; i++) {
			if (!parse_arg(values, types, i, (Term) objs[i]))
				return null;
		}
		return new Signature(values, types);
	}
	
	private boolean parse_arg(Object[] values, Class<?>[] types, int i, Term term) {
		try {
			if (term == null) {
				values[i] = null;
				types[i] = null;
			} else if (term.isAtomic()) {
				String name = alice.util.Tools.removeApostrophes(term.toString());
                switch (name) {
                    case "true":
                        values[i] = Boolean.TRUE;
                        types[i] = Boolean.TYPE;
                        break;
                    case "false":
                        values[i] = Boolean.FALSE;
                        types[i] = Boolean.TYPE;
                        break;
                    default:
                        Object obj = getRegisteredDynamicObject((Struct) term.term());
                        values[i] = obj == null ? name : obj;
                        types[i] = values[i].getClass();
                        break;
                }
			} else if (term instanceof NumberTerm) {
				NumberTerm t = (NumberTerm) term;
				if (t instanceof NumberTerm.Int) {
					values[i] = t.intValue();
					types[i] = java.lang.Integer.TYPE;
				} else if (t instanceof NumberTerm.Double) {
					values[i] = t.doubleValue();
					types[i] = java.lang.Double.TYPE;
				} else if (t instanceof NumberTerm.Long) {
					values[i] = t.longValue();
					types[i] = java.lang.Long.TYPE;
				} else if (t instanceof NumberTerm.Float) {
					values[i] = t.floatValue();
					types[i] = java.lang.Float.TYPE;
				}
			} else if (term instanceof Struct) {
				
				Struct tc = (Struct) term;
				if (tc.name().equals("as")) {
					return parse_as(values, types, i, tc.subResolve(0), tc.subResolve(1));
				} else {
					Object obj = getRegisteredDynamicObject((Struct)tc.term());
                    values[i] = obj == null ? alice.util.Tools.removeApostrophes(tc.toString()) : obj;
					types[i] = values[i].getClass();
				}
			} else if (term instanceof Var && !((Var) term).isBound()) {
				values[i] = null;
				types[i] = Object.class;
			} else {
				return false;
			}
		} catch (Exception ex) {
			return false;
		}
		return true;
	}
	
	/**
	 *  parses return value
	 *  of a method invokation
	 */
	private boolean parseResult(Term id, Object obj) {
		if (obj == null) {
			
			return unify(id, new Var());
		}
		try {
			if (Boolean.class.isInstance(obj)) {
                return (Boolean) obj ? unify(id, Term.TRUE) : unify(id, Term.FALSE);
			} else if (Byte.class.isInstance(obj)) {
				return unify(id, new NumberTerm.Int(((Byte) obj).intValue()));
			} else if (Short.class.isInstance(obj)) {
				return unify(id, new NumberTerm.Int(((Short) obj).intValue()));
			} else if (Integer.class.isInstance(obj)) {
				return unify(id, new NumberTerm.Int((Integer) obj));
			} else if (java.lang.Long.class.isInstance(obj)) {
				return unify(id, new NumberTerm.Long((java.lang.Long) obj));
			} else if (java.lang.Float.class.isInstance(obj)) {
				return unify(id, new NumberTerm.Float((java.lang.Float) obj));
			} else if (java.lang.Double.class.isInstance(obj)) {
				return unify(id, new NumberTerm.Double((java.lang.Double) obj));
			} else if (String.class.isInstance(obj)) {
				return unify(id, new Struct((String) obj));
			} else if (Character.class.isInstance(obj)) {
				return unify(id, new Struct(obj.toString()));
			} else {
				return bindDynamicObject(id, obj);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}

    private boolean parse_as(Object[] values, Class<?>[] types, int i, Term castWhat, Term castTo) {
		try {
			if (!(castWhat instanceof NumberTerm)) {
				String castTo_name = alice.util.Tools.removeApostrophes(((Struct) castTo).name());
				String castWhat_name = alice.util.Tools.removeApostrophes(castWhat.term().toString());
				
				if (castTo_name.equals("java.lang.String") &&
						castWhat_name.equals("true")){
					values[i]="true";
					types[i]=String.class;
					return true;
				} else if (castTo_name.equals("java.lang.String") &&
						castWhat_name.equals("false")){
					values[i]="false";
					types[i]=String.class;
					return true;
				} else if (castTo_name.endsWith("[]")) {
                    switch (castTo_name) {
                        case "boolean[]":
                            castTo_name = "[Z";
                            break;
                        case "byte[]":
                            castTo_name = "[B";
                            break;
                        case "short[]":
                            castTo_name = "[S";
                            break;
                        case "char[]":
                            castTo_name = "[C";
                            break;
                        case "int[]":
                            castTo_name = "[I";
                            break;
                        case "long[]":
                            castTo_name = "[L";
                            break;
                        case "float[]":
                            castTo_name = "[F";
                            break;
                        case "double[]":
                            castTo_name = "[D";
                            break;
                        default:
                            castTo_name = "[L" + castTo_name.substring(0, castTo_name.length() - 2) + ';';
                            break;
                    }
				}
				if (!castWhat_name.equals("null")) {
					Object obj_to_cast = getRegisteredDynamicObject((Struct)castWhat.term());
					if (obj_to_cast == null) {
						if (castTo_name.equals("boolean")) {
                            switch (castWhat_name) {
                                case "true":
                                    values[i] = Boolean.TRUE;
                                    break;
                                case "false":
                                    values[i] = Boolean.FALSE;
                                    break;
                                default:
                                    return false;
                            }
							types[i] = Boolean.TYPE;
						} else {
							
							return false;
						}
					} else {
						values[i] = obj_to_cast;
						try {
							types[i] = (Class.forName(castTo_name));
						} catch (ClassNotFoundException ex) {
							Prolog.warn("Java class not found: " + castTo_name);
							return false;
						}
					}
				} else {
					values[i] = null;
                    switch (castTo_name) {
                        case "byte":
                            types[i] = Byte.TYPE;
                            break;
                        case "short":
                            types[i] = Short.TYPE;
                            break;
                        case "char":
                            types[i] = Character.TYPE;
                            break;
                        case "int":
                            types[i] = Integer.TYPE;
                            break;
                        case "long":
                            types[i] = java.lang.Long.TYPE;
                            break;
                        case "float":
                            types[i] = java.lang.Float.TYPE;
                            break;
                        case "double":
                            types[i] = java.lang.Double.TYPE;
                            break;
                        case "boolean":
                            types[i] = Boolean.TYPE;
                            break;
                        default:
                            try {
                                types[i] = (Class.forName(castTo_name));
                            } catch (ClassNotFoundException ex) {
                                Prolog.warn("Java class not found: " + castTo_name);
                                return false;
                            }
                            break;
                    }
				}
			} else {
				NumberTerm num = (NumberTerm) castWhat;
				String castTo_name = ((Struct) castTo).name();
                switch (castTo_name) {
                    case "byte":
                        values[i] = (byte) num.intValue();
                        types[i] = Byte.TYPE;
                        break;
                    case "short":
                        values[i] = (short) num.intValue();
                        types[i] = Short.TYPE;
                        break;
                    case "int":
                        values[i] = num.intValue();
                        types[i] = Integer.TYPE;
                        break;
                    case "long":
                        values[i] = num.longValue();
                        types[i] = java.lang.Long.TYPE;
                        break;
                    case "float":
                        values[i] = num.floatValue();
                        types[i] = java.lang.Float.TYPE;
                        break;
                    case "double":
                        values[i] = num.doubleValue();
                        types[i] = java.lang.Double.TYPE;
                        break;
                    default:
                        return false;
                }
			}
		} catch (Exception ex) {
			Prolog.warn("Casting " + castWhat + " to " + castTo + " failed");
			return false;
		}
		return true;
	}
}

