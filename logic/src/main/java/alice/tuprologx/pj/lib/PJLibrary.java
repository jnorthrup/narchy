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

import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.*;
import java.util.*;

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
public class PJLibrary extends Library {
	
	/**
	 * java objects referenced by prolog terms (keys)
	 */
	private HashMap<String,Object> currentObjects = new HashMap<>();
	/**
	 * inverse map useful for implementation issue
	 */
	private IdentityHashMap<Object,Struct> currentObjects_inverse = new IdentityHashMap<>();
	
	private final HashMap<String,Object> staticObjects = new HashMap<>();
	private final IdentityHashMap<Object,Struct> staticObjects_inverse = new IdentityHashMap<>();
	
	/**
	 * progressive conter used to identify registered objects
	 */
	private int id;
	
	
	
	/**
	 * library theory
	 */
	@Override
	public String getTheory() {
		return
		
		
		
		":- op(800,xfx,'<-').\n" +
        ":- op(800,xfx,':=').\n" +
		":- op(850,xfx,'returns').\n" +
		":- op(200,xfx,'as').\n" +
		":- op(600,xfx,'.'). \n" +
		
		
		
		
		
		
		
		
		
		"java_object_bt(ClassName,Args,Id):- java_object(ClassName,Args,Id).\n" +
		"java_object_bt(ClassName,Args,Id):- destroy_object(Id).\n" +
		"Obj <- What :- java_call(Obj,What,Res), Res \\== false.\n" +
		"Obj <- What returns Res :- java_call(Obj,What,Res).\n" +
		"java_array_set(Array,Index,Object):-           class('java.lang.reflect.Array') <- setAt(Array as 'java.lang.Object',Index,Object as 'java.lang.Object'),!.\n" +
		"java_array_set(Array,Index,Object):-			java_array_set_primitive(Array,Index,Object).\n"+
		"java_array_get(Array,Index,Object):-           class('java.lang.reflect.Array') <- get(Array as 'java.lang.Object',Index) returns Object,!.\n" +
		"java_array_get(Array,Index,Object):-       java_array_get_primitive(Array,Index,Object).\n"+
		
		"java_array_length(Array,Length):-              class('java.lang.reflect.Array') <- getLength(Array as 'java.lang.Object') returns Length.\n" +
		"java_object_string(Object,String):-    Object <- toString returns String.    \n";
	}
	
	
	@Override
	public void dismiss() {
		currentObjects.clear();
		currentObjects_inverse.clear();
	}
	
	public void dismissAll() {
		currentObjects.clear();
		currentObjects_inverse.clear();
		staticObjects.clear();
		staticObjects_inverse.clear();
	}
	
	@Override
	public   void onSolveBegin(Term goal) {
		
		currentObjects.clear();
		currentObjects_inverse.clear();
		for(Map.Entry<Object, Struct> en: staticObjects_inverse.entrySet()){
			bindDynamicObject(en.getValue(), en.getKey());
		}
		preregisterObjects();
	}

    /**
	 * objects actually pre-registered in order to be
	 * available since the beginning of demonstration
	 */
	protected void preregisterObjects() {
		try {
			bindDynamicObject(new Struct("stdout"), System.out);
			bindDynamicObject(new Struct("stderr"), System.err);
			bindDynamicObject(new Struct("runtime"), Runtime.getRuntime());
			bindDynamicObject(new Struct("current_thread"), Thread.currentThread());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	
	
	/**
	 * Creates of a java object - not backtrackable case
	 */
	public boolean java_object_3(Term className, Term argl, Term id) {
		className = className.term();
		Struct arg = (Struct) argl.term();
		id = id.term();
		try {
			if (!className.isAtomic()) {
				return false;
			}
			String clName = ((Struct) className).name();
			
			if (clName.endsWith("[]")) {
				Object[] list = getArrayFromList(arg);
				int nargs = ((NumberTerm) list[0]).intValue();
				return java_array(clName, nargs, id);
			}
			Signature args = parseArg(getArrayFromList(arg));
			if (args == null) {
				return false;
			}
			
			try {
				Class<?> cl = Class.forName(clName);
				Object[] args_value = args.getValues();
				
				
				Constructor<?> co = lookupConstructor(cl, args.getTypes(), args_value);
				
				
				if (co==null){
					Prolog.warn("Constructor not found: class " + clName);
					return false;
				}
				
				Object obj = co.newInstance(args_value);
				return bindDynamicObject(id, obj);
			} catch (ClassNotFoundException ex) {
				Prolog.warn("Java class not found: " + clName);
				return false;
			} catch (InvocationTargetException ex) {
				Prolog.warn("Invalid constructor arguments.");
				return false;
			} catch (NoSuchMethodException ex) {
				Prolog.warn("Constructor not found: " + Arrays.toString(args.getTypes()));
				return false;
			} catch (InstantiationException ex) {
				Prolog.warn("Objects of class " + clName + " cannot be instantiated");
				return false;
			} catch (IllegalArgumentException ex) {
				Prolog.warn("Illegal constructor arguments  " + args);
				return false;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}
	
	
	/**
	 * Destroy the link to a java object - called not directly, but from
	 * predicate java_object (as second choice, for backtracking)
	 */
	public   boolean destroy_object_1(Term id) {
		id = id.term();
		try {
			if (id.isGround()) {
				unregisterDynamic((Struct)id);
			}
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}
	
	
	/**
	 * Creates of a java class
	 */
	public   boolean java_class_4(Term clSource, Term clName, Term clPathes, Term id) {
		Struct classSource = (Struct) clSource.term();
		Struct className = (Struct) clName.term();
		Struct classPathes = (Struct) clPathes.term();
		id = id.term();
		try {
			String fullClassName = alice.util.Tools.removeApostrophes(className.toString());

			Iterator<? extends Term> it = classPathes.listIterator();
			String cp = "";
			while (it.hasNext()) {
				if (!cp.isEmpty()) {
					cp += ";";
				}
				cp += alice.util.Tools.removeApostrophes(it.next().toString());
			}
			if (!cp.isEmpty()) {
				cp = " -classpath " + cp;
			}
			
			String text = alice.util.Tools.removeApostrophes(classSource.toString());
			
			
			
			
			String fullClassPath = fullClassName.replace('.', '/');
			try {
				FileWriter file = new FileWriter(fullClassPath + ".java");
				file.write(text);
				file.close();
			} catch (IOException ex) {
				Prolog.warn("Compilation of java sources failed");
				Prolog.warn("(creation of " + fullClassPath + ".java fail failed)");
				return false;
			}
			String cmd = "javac " + cp + ' ' + fullClassPath + ".java";
			
			try {
				Process jc = Runtime.getRuntime().exec(cmd);
				int res = jc.waitFor();
				if (res != 0) {
					Prolog.warn("Compilation of java sources failed");
					Prolog.warn("(java compiler (javac) has stopped with errors)");
					return false;
				}
			} catch (IOException ex) {
				Prolog.warn("Compilation of java sources failed");
				Prolog.warn("(java compiler (javac) invocation failed)");
				return false;
			}
			try {
				Class<?> the_class = Class.forName(fullClassName, true, new ClassLoader());
				return bindDynamicObject(id, the_class);
			} catch (ClassNotFoundException ex) {
				Prolog.warn("Compilation of java sources failed");
				Prolog.warn("(Java Class compiled, but not created: " + fullClassName + " )");
				return false;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}
	
	
	/**
	 *
	 * Calls a method of a Java object
	 *
	 */
	public boolean java_call_3(Term objId, Term method_name, Term idResult) {
		objId = objId.term();
		idResult = idResult.term();
		Struct method = (Struct) method_name.term();
		Signature args = null;
		String methodName = null;
		try {
			methodName = method.name();
			
			
			
			if (!objId.isAtomic()) {
				if (objId instanceof Var) {
					return false;
				}
				Struct sel = (Struct) objId;
				if (sel.name().equals(".") && sel.subs() == 2 && method.subs() == 1) {
					if (methodName.equals("setAt"))
						return java_set(sel.subResolve(0), sel.subResolve(1), method.subResolve(0));
					else if (methodName.equals("get"))
						return java_get(sel.subResolve(0), sel.subResolve(1), method.subResolve(0));
				}
			}
			
			args = parseArg(method);
			
			
			if (objId instanceof Var || args == null)
				return false;			
            
			
			String objName = alice.util.Tools.removeApostrophes(objId.toString());
			Object obj = currentObjects.get(objName);
			Object res = null;
			
			if (obj != null) {                
				Class<?> cl = obj.getClass();
				
				
				Object[] args_values = args.getValues();                
				Method m = lookupMethod(cl, methodName, args.getTypes(), args_values);                
				
				
                if (m == null) {                    
                    Object[] newValues = new Object[args_values.length];
                    Class<?>[] newTypes = new Class<?>[args_values.length];
                    
                    for (int i = 0; i < method.subs(); i++) {
                        newValues[i] = alice.tuprologx.pj.model.Term.unmarshal(method.sub(i));
                        newTypes[i] = newValues[i].getClass();                        
                    }                    
                    m = lookupMethod(cl, methodName, newTypes, newValues);
                    if (m != null)
                        args_values = newValues;
                }
				if (m != null) {
					try {
						
						
						res = m.invoke(obj, args_values);
						
					} catch (IllegalAccessException ex) {
						Prolog.warn("Method invocation failed: " + methodName+ "( signature: " + args + " )");
						ex.printStackTrace();
						return false;                        
					}                    
				} else {
					Prolog.warn("Method not found: " + methodName+ "( signature: " + args + " )");
					return false;
				}
			} else {
				if (objId.isCompound()) {
					Struct id = (Struct) objId;
					if (id.subs() == 1 && id.name().equals("class")) {
						try {
							Class<?> cl = Class.forName(alice.util.Tools.removeApostrophes(id.sub(0).toString()));
							Method m = cl.getMethod(methodName, args.getTypes());
							m.setAccessible(true);
							res = m.invoke(null, args.getValues());
						} catch (ClassNotFoundException ex) {
							
							Prolog.warn("Unknown class.");
							ex.printStackTrace();
							return false;
						}
					} else {
						
						Method m = java.lang.String.class.getMethod(methodName, args.getTypes());
						m.setAccessible(true);
						res = m.invoke(objName, args.getValues());
					}
				} else {
					
					Method m = java.lang.String.class.getMethod(methodName, args.getTypes());
					m.setAccessible(true);
					res = m.invoke(objName, args.getValues());
				}
			}
			return parseResult(idResult, res);
		} catch (InvocationTargetException ex) {
			Prolog.warn("Method failed: " + methodName + " - ( signature: " + args +
					" ) - Original Exception: "+ex.getTargetException());
			ex.printStackTrace();
			return false;
		} catch (NoSuchMethodException ex) {
			ex.printStackTrace();
			Prolog.warn("Method not found: " + methodName+ " - ( signature: " + args + " )");
			return false;
		} catch (IllegalArgumentException ex) {
			ex.printStackTrace();
			Prolog.warn("Invalid arguments " + args+ " - ( method: " + methodName + " )");
			
			return false;
		} catch (Exception ex) {
			ex.printStackTrace();
			Prolog.warn("Generic error in method invocation " + methodName);
			return false;
		}
	}
	
	
	/*
	 * set the field value of an object
	 */
	private boolean java_set(Term objId, Term fieldTerm, Term what) {
		
		what = what.term();
		if (!fieldTerm.isAtomic() || what instanceof Var)
			return false;
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
				String objName = alice.util.Tools.removeApostrophes(objId.toString());
				obj = currentObjects.get(objName);
				if (obj != null) {
					cl = obj.getClass();
				} else {
					return false;
				}
			}
			
			
			Field field = cl.getField(fieldName);            
            if (field.isAnnotationPresent(alice.tuprologx.pj.annotations.PrologField.class)) {                
                alice.tuprologx.pj.model.Term<?> t = alice.tuprologx.pj.model.Term.unmarshal(what);
                field.set(obj, t);
                return true;
            }
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
                    String what_name = alice.util.Tools.removeApostrophes(what.toString());
                    Object obj2 = currentObjects.get(what_name);
                    if (obj2 != null) {
                        field.set(obj, obj2);
                    } else {
                        
                        field.set(obj, what_name);
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
	
	/*
	 * get the value of the field
	 */
	private boolean java_get(Term objId, Term fieldTerm, Term what) {
		
		if (!fieldTerm.isAtomic()) {
			return false;
		}
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
				String objName = alice.util.Tools.removeApostrophes(objId.toString());
				obj = currentObjects.get(objName);
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
	
	public boolean java_array_set_primitive_3(Term obj_id, Term i, Term what) {
		Struct objId = (Struct) obj_id.term();
		NumberTerm index = (NumberTerm) i.term();
		what = what.term();
		
		if (!index.isInteger()){
			return false;
		}
		try {
			String objName = alice.util.Tools.removeApostrophes(objId.toString());
			Object obj = currentObjects.get(objName);
			Class<?> cl = null;
			if (obj != null) {
				cl = obj.getClass();
			} else {
				return false;
			}
			
			if (!cl.isArray()){
				return false;
			}
			String name = cl.toString();
            switch (name) {
                case "class [I": {
                    if (!(what instanceof NumberTerm)) {
                        return false;
                    }
                    byte v = (byte) ((NumberTerm) what).intValue();
                    Array.setInt(obj, index.intValue(), v);
                    break;
                }
                case "class [D": {
                    if (!(what instanceof NumberTerm)) {
                        return false;
                    }
                    double v = ((NumberTerm) what).doubleValue();
                    Array.setDouble(obj, index.intValue(), v);
                    break;
                }
                case "class [F": {
                    if (!(what instanceof NumberTerm)) {
                        return false;
                    }
                    float v = ((NumberTerm) what).floatValue();
                    Array.setFloat(obj, index.intValue(), v);
                    break;
                }
                case "class [L": {
                    if (!(what instanceof NumberTerm)) {
                        return false;
                    }
                    long v = ((NumberTerm) what).longValue();
                    Array.setFloat(obj, index.intValue(), v);
                    break;
                }
                case "class [C": {
                    String s = what.toString();
                    Array.setChar(obj, index.intValue(), s.charAt(0));
                    break;
                }
                case "class [Z":
                    String s = what.toString();
                    switch (s) {
                        case "true":
                            Array.setBoolean(obj, index.intValue(), true);
                            break;
                        case "false":
                            Array.setBoolean(obj, index.intValue(), false);
                            break;
                        default:
                            return false;
                    }
                    break;
                case "class [B": {
                    if (!(what instanceof NumberTerm)) {
                        return false;
                    }
                    int v = ((NumberTerm) what).intValue();
                    Array.setByte(obj, index.intValue(), (byte) v);
                    break;
                }
                case "class [S":
                    if (!(what instanceof NumberTerm)) {
                        return false;
                    }
                    short v = (short) ((NumberTerm) what).intValue();
                    Array.setShort(obj, index.intValue(), v);
                    break;
                default:
                    return false;
            }
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}
	
	public   boolean java_array_get_primitive_3(Term obj_id, Term i, Term what) {
		Struct objId = (Struct) obj_id.term();
		NumberTerm index = (NumberTerm) i.term();
		what = what.term();
		
		if (!index.isInteger()) {
			return false;
		}
		try {
			String objName = alice.util.Tools.removeApostrophes(objId.toString());
			Object obj = currentObjects.get(objName);
			Class<?> cl = null;
			if (obj != null) {
				cl = obj.getClass();
			} else {
				return false;
			}
			
			if (!cl.isArray()){
				return false;
			}
			String name = cl.toString();
            switch (name) {
                case "class [I": {
                    Term value = new NumberTerm.Int(Array.getInt(obj, index.intValue()));
                    return unify(what, value);
                }
                case "class [D": {
                    Term value = new NumberTerm.Double(Array.getDouble(obj, index.intValue()));
                    return unify(what, value);
                }
                case "class [F": {
                    Term value = new NumberTerm.Float(Array.getFloat(obj, index.intValue()));
                    return unify(what, value);
                }
                case "class [L": {
                    Term value = new NumberTerm.Long(Array.getLong(obj, index.intValue()));
                    return unify(what, value);
                }
                case "class [C": {
                    Term value = new Struct(String.valueOf(Array.getChar(obj, index.intValue())));
                    return unify(what, value);
                }
                case "class [Z":
                    boolean b = Array.getBoolean(obj, index.intValue());
					return b ? unify(what, Term.TRUE) : unify(what, Term.FALSE);
                case "class [B": {
                    Term value = new NumberTerm.Int(Array.getByte(obj, index.intValue()));
                    return unify(what, value);
                }
                case "class [S":
                    Term value = new NumberTerm.Int(Array.getInt(obj, index.intValue()));
                    return unify(what, value);
                default:
                    return false;
            }
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
		
	}
	
	
	private boolean java_array(String type, int nargs, Term id) {
		try {
			Object array = null;
			String obtype = type.substring(0, type.length() - 2);
            switch (obtype) {
                case "boolean":
                    array = new boolean[nargs];
                    break;
                case "byte":
                    array = new byte[nargs];
                    break;
                case "char":
                    array = new char[nargs];
                    break;
                case "short":
                    array = new short[nargs];
                    break;
                case "int":
                    array = new int[nargs];
                    break;
                case "long":
                    array = new long[nargs];
                    break;
                case "float":
                    array = new float[nargs];
                    break;
                case "double":
                    array = new double[nargs];
                    break;
                default:
                    Class<?> cl = Class.forName(obtype);
                    array = Array.newInstance(cl, nargs);
                    break;
            }
			return bindDynamicObject(id, array);
		} catch (Exception ex) {
			
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
                        Object obj = currentObjects.get(name);
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
					Object obj = currentObjects.get(alice.util.Tools.removeApostrophes(tc.toString()));
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
	 *
	 * parsing 'as' operator, which makes it possible
	 * to define the specific class of an argument
	 *
	 */
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
					Object obj_to_cast = currentObjects.get(castWhat_name);
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
	
	private static Object[] getArrayFromList(Struct list) {
		Object args[] = new Object[list.listSize()];
		Iterator<? extends Term> it = list.listIterator();
		int count = 0;
		while (it.hasNext()) {
			args[count++] = it.next();
		}
		return args;
	}
	
	/**
	 * Register an object with the specified id.
	 * The life-time of the link to the object is engine life-time,
	 * available besides the individual query.
	 * 
	 * The identifier must be a ground object.   
	 * 
	 * @param id object identifier 
	 * @param obj the object
	 * @return true if the operation is successful 
	 * @throws InvalidObjectIdException if the object id is not valid
	 */
	public   boolean register(Struct id, Object obj) throws InvalidObjectIdException {
		/*
		 * note that this method act on the staticObject
		 * and staticObject_inverse hashmaps
		 */
		if (!id.isGround()) {
			throw new InvalidObjectIdException();
		}
		
		synchronized (staticObjects){
			Object aKey = staticObjects_inverse.get(obj);
			
			if (aKey != null) {
				
				return false;
			} else {
				String raw_name = alice.util.Tools.removeApostrophes(id.term().toString());
				staticObjects.put(raw_name, obj);
				staticObjects_inverse.put(obj, id);
				return true;
			}
		}
	}
	
	/**
	 * Registers an object, with automatic creation of the identifier.
	 * 
	 * If the object is already registered,
	 * its identifier is returned
	 * 
	 * @param obj object to be registered. 
	 * @return fresh id
	 */	
	public   Struct register(Object obj) {
		
		
		
		synchronized (staticObjects){
			Object aKey = staticObjects_inverse.get(obj);
			if (aKey != null) {
				
				
				
				return (Struct) aKey;
			} else {
				Struct id = generateFreshId();
				staticObjects.put(id.name(), obj);
				staticObjects_inverse.put(obj, id);
				return id;
			}
		}
	}
	
	/**
	 * Gets the reference to an object previously registered
	 * 
	 * @param id object id
	 * @return the object, if present
	 * @throws InvalidObjectIdException
	 */
	public Object getRegisteredObject(Struct id) throws InvalidObjectIdException {
		if (!id.isGround()) {
			throw new InvalidObjectIdException();
		}
		synchronized (staticObjects){
			return staticObjects.get(alice.util.Tools.removeApostrophes(id.toString()));
		}
	}
	
	/**
	 * Unregisters an object, given its identifier 
	 * 
	 * 
	 * @param id object identifier
	 * @return true if the operation is successful
	 * @throws InvalidObjectIdException if the id is not valid (e.g. is not ground)
	 */
	public   boolean unregister(Struct id) throws InvalidObjectIdException {
		if (!id.isGround()) {
			throw new InvalidObjectIdException();
		}
		synchronized (staticObjects){
			String raw_name = alice.util.Tools.removeApostrophes(id.toString());
			Object obj = staticObjects.remove(raw_name);
			if (obj != null) {
				staticObjects_inverse.remove(obj);
				return true;
			} else {
				return false;
			}
		}
	}
	
	
	
	/**
	 * Registers an object only for the running query life-time
	 * 
	 * @param id object identifier
	 * @param obj object 
	 */
	public void registerDynamic(Struct id, Object obj) {
		synchronized (currentObjects){
			String raw_name = alice.util.Tools.removeApostrophes(id.toString());
			currentObjects.put(raw_name, obj);
			currentObjects_inverse.put(obj, id);
		}
	}
	
	/**
	 * Registers an object for the query life-time, 
	 * with the automatic generation of the identifier.
	 * 
	 * If the object is already registered,
	 * its identifier is returned
	 * 
	 * @param obj object to be registered
	 * @return identifier
	 */
	public   Struct registerDynamic(Object obj) {
		
		
		
		synchronized (currentObjects){
			Object aKey = currentObjects_inverse.get(obj);
			if (aKey != null) {
				
				
				
				return (Struct) aKey;
			} else {
				Struct id = generateFreshId();
				currentObjects.put(id.name(), obj);
				currentObjects_inverse.put(obj, id);
				return id;
			}
		}
	}
	
	/**
	 * Gets a registered dynamic object
	 * (returns null if not presents)
	 */
	public   Object getRegisteredDynamicObject(Struct id) throws InvalidObjectIdException {
		if (!id.isGround()) {
			throw new InvalidObjectIdException();
		}
		synchronized (currentObjects){
			return currentObjects.get(alice.util.Tools.removeApostrophes(id.toString()));
		}
	}
	
	/**
	 * Unregister the object, only for dynamic case
	 * 
	 * @param id object identifier
	 * @return true if the operation is successful
	 */
	public boolean unregisterDynamic(Struct id) {
		synchronized (currentObjects){
			String raw_name = alice.util.Tools.removeApostrophes(id.toString());
			Object obj = currentObjects.remove(raw_name);
			if (obj != null) {
				currentObjects_inverse.remove(obj);
				return true;
			} else {
				return false;
			}
		}
	}
	
	/**
	 * Tries to bind specified id to a provided java object.
	 * 
	 * Term id can be a variable or a ground term.
	 */
	protected boolean bindDynamicObject(Term id, Object obj) {
		
		if (obj == null) {
			return unify(id, new Var());
		}
        /*if (obj instanceof alice.tuprologx.pj.model.Term<?>) {
            alice.tuprologx.pj.model.Term<?> t = (alice.tuprologx.pj.model.Term<?>)obj;
            return unify(id, t.marshal());
        }*/
		
		synchronized (currentObjects){
			Object aKey = currentObjects_inverse.get(obj);
			if (aKey != null) {
				
				
				
				return unify(id, (Term) aKey);
			} else {
				
				if (id instanceof Var) {
					
					Struct idTerm = generateFreshId();
					unify(id, idTerm);
					registerDynamic(idTerm, obj);
					
					return true;
				} else {
					
					String raw_name = alice.util.Tools.removeApostrophes(id.term().toString());
					Object linkedobj = currentObjects.get(raw_name);
					if (linkedobj == null) {
						registerDynamic((Struct)(id.term()), obj);
						
						return true;
					} else {
						
						
						return obj == linkedobj;
					}
				}
			}
		}
	}
	
	/**
	 * Generates a fresh numeric identifier
	 * @return
	 */
	protected Struct generateFreshId() {
		return new Struct("$obj_" + id++);        
	}
	
	/**
	 *  handling writeObject method is necessary in order to
	 *  make the library serializable, 'nullyfing'  eventually
	 *  objects registered in maps
	 */
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		HashMap<String,Object> bak00 = currentObjects;
		IdentityHashMap<Object,Struct> bak01 = currentObjects_inverse;
		try {
			currentObjects = null;
			currentObjects_inverse = null;
			out.defaultWriteObject();
		} catch (IOException ex) {
			currentObjects = bak00;
			currentObjects_inverse = bak01;
			throw new IOException();
		}
		currentObjects = bak00;
		currentObjects_inverse = bak01;
	}
	
	/**
	 *  handling readObject method is necessary in order to
	 *  have the library reconstructed after a serialization
	 */
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		currentObjects = new HashMap<>();
		currentObjects_inverse = new IdentityHashMap<>();
		preregisterObjects();
	}
	
	
	
	private static Method lookupMethod(Class<?> target, String name,
                                       Class<?>[] argClasses, Object... argValues) throws NoSuchMethodException {
		
		try {
			return target.getMethod(name, argClasses);
		} catch (NoSuchMethodException e) {
			if (argClasses.length == 0) { 
				return null;
			}
		}
		
		
		Method[] methods = target.getMethods();
		Vector<Method> goodMethods = new Vector<>();
		for (int i = 0; i != methods.length; i++) {
			if (name.equals(methods[i].getName()) &&
					matchClasses(methods[i].getParameterTypes(), argClasses))
				goodMethods.addElement(methods[i]);
		}
		switch (goodMethods.size()) {
		case 0:
			
			
			
			
			
			
			
			
			for (int i = 0; i != methods.length; i++) {
				if (name.equals(methods[i].getName())) {
					Class<?>[] types = methods[i].getParameterTypes();
					Object[] val = matchClasses(types, argClasses, argValues);
					if (val != null) {
						
						
						for (int j = 0; j < types.length; j++) {
							argClasses[j] = types[j];
							argValues[j] = val[j];
						}
						return methods[i];
					}
				}
			}
			
			return null;
		case 1:
			return goodMethods.firstElement();
		default:
			return mostSpecificMethod(goodMethods);
		}
	}
	
	private static Constructor<?> lookupConstructor(Class<?> target, Class<?>[] argClasses, Object... argValues) throws NoSuchMethodException {
		
		try {
			return target.getConstructor(argClasses);
		} catch (NoSuchMethodException e) {
			if (argClasses.length == 0) { 
				return null;
			}
		}
		
		
		Constructor<?>[] constructors = target.getConstructors();
		Vector<Constructor<?>> goodConstructors = new Vector<>();
		for (int i = 0; i != constructors.length; i++) {
			if (matchClasses(constructors[i].getParameterTypes(), argClasses))
				goodConstructors.addElement(constructors[i]);
		}
		switch (goodConstructors.size()) {
		case 0:
			
			
			
			
			
			
			
			
			for (int i = 0; i != constructors.length; i++) {
				Class<?>[] types = constructors[i].getParameterTypes();
				Object[] val = matchClasses(types, argClasses, argValues);
				if (val != null) {
					
					
					for (int j = 0; j < types.length; j++) {
						argClasses[j] = types[j];
						argValues[j] = val[j];
					}
					return constructors[i];
				}
			}
			
			return null;
		case 1:
			return goodConstructors.firstElement();
		default:
			return mostSpecificConstructor(goodConstructors);
		}
	}
	
	
	private static boolean matchClasses(Class<?>[] mclasses, Class<?>... pclasses) {
		if (mclasses.length == pclasses.length) {
			for (int i = 0; i != mclasses.length; i++) {
				if (!matchClass(mclasses[i], pclasses[i])) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	private static boolean matchClass(Class<?> mclass, Class<?> pclass) {
		boolean assignable = mclass.isAssignableFrom(pclass);
		if (assignable) {
			return true;
		} else {
			if (mclass.equals(java.lang.Long.TYPE) && (pclass.equals(java.lang.Integer.TYPE))) {
				return true;
			}
            else return pclass.isPrimitive() && mclass.equals(Object.class);
		}
    }
	
	private static Method mostSpecificMethod(Vector<Method> methods) throws NoSuchMethodException {
		for (int i = 0; i != methods.size(); i++) {
			for (int j = 0; j != methods.size(); j++) {
				if ((i != j) &&
						(moreSpecific(methods.elementAt(i), methods.elementAt(j)))) {
					methods.removeElementAt(j);
					if (i > j) i--;
					j--;
				}
			}
		}
		if (methods.size() == 1)
			return methods.elementAt(0);
		else
			throw new NoSuchMethodException(">1 most specific method");
	}
	
	
	private static boolean moreSpecific(Method c1, Method c2) {
		Class<?>[] p1 = c1.getParameterTypes();
		Class<?>[] p2 = c2.getParameterTypes();
		int n = p1.length;
		for (int i = 0; i != n; i++) {
			if (!matchClass(p2[i], p1[i])) {
				return false;
			}
		}
		return true;
	}
	
	private static Constructor<?> mostSpecificConstructor(Vector<Constructor<?>> constructors) throws NoSuchMethodException {
		for (int i = 0; i != constructors.size(); i++) {
			for (int j = 0; j != constructors.size(); j++) {
				if ((i != j) &&
						(moreSpecific(constructors.elementAt(i), constructors.elementAt(j)))) {
					constructors.removeElementAt(j);
					if (i > j) i--;
					j--;
				}
			}
		}
		if (constructors.size() == 1)
			return constructors.elementAt(0);
		else
			throw new NoSuchMethodException(">1 most specific constructor");
	}
	
	
	private static boolean moreSpecific(Constructor<?> c1, Constructor<?> c2) {
		Class<?>[] p1 = c1.getParameterTypes();
		Class<?>[] p2 = c2.getParameterTypes();
		int n = p1.length;
		for (int i = 0; i != n; i++) {
			if (!matchClass(p2[i], p1[i])) {
				return false;
			}
		}
		return true;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	private static Object[] matchClasses(Class<?>[] mclasses, Class<?>[] pclasses, Object... values) {
		if (mclasses.length == pclasses.length) {
			Object[] newvalues = new Object[mclasses.length];
			
			for (int i = 0; i != mclasses.length; i++) {
				boolean assignable = mclasses[i].isAssignableFrom(pclasses[i]);
				if (assignable ||
						(mclasses[i].equals(java.lang.Long.TYPE) && pclasses[i].equals(java.lang.Integer.TYPE))) {
					newvalues[i] = values[i];
				} else if (mclasses[i].equals(java.lang.Float.TYPE) &&
						pclasses[i].equals(java.lang.Double.TYPE)) {
					
					
					newvalues[i] = ((java.lang.Double) values[i]).floatValue();
				} else if (mclasses[i].equals(java.lang.Float.TYPE) &&
						pclasses[i].equals(java.lang.Integer.TYPE)) {
					
					
					newvalues[i] = (float) (Integer) values[i];
				} else if (mclasses[i].equals(java.lang.Double.TYPE) &&
						pclasses[i].equals(java.lang.Integer.TYPE)) {
					
					
					newvalues[i] = ((Integer) values[i]).doubleValue();
				} else if (values[i] == null && !mclasses[i].isPrimitive()) {
					newvalues[i] = null;
				} else {
					return null;
				}
			}
			return newvalues;
		} else {
			return null;
		}
	}
	
	
}

/**
 * Signature class mantains information
 * about type and value of a method
 * arguments
 */
@SuppressWarnings("serial")
class Signature implements Serializable {
	final Class<?>[] types;
	final Object[] values;
	
	public Signature(Object[] v, Class<?>... c) {
		values = v;
		types = c;
	}
	
	public Class<?>[] getTypes() {
		return types;
	}
	
	Object[] getValues() {
		return values;
	}
	
	public String toString() {
		String st = "";
		for (int i = 0; i < types.length; i++) {
			st = st + "\n  Argument " + i + " -  VALUE: " + values[i] + " TYPE: " + types[i];
		}
		return st;
	}
}

/** used to load new classes without touching system class loader */
class ClassLoader extends java.lang.ClassLoader {
}

/**
 * Information about an EventListener
 */
@SuppressWarnings("serial")
class ListenerInfo implements Serializable {
	public final String listenerInterfaceName;
	public final EventListener listener;
	
	public final String eventFullClass;
	
	public ListenerInfo(EventListener l, String eventClass, String n) {
		listener = l;
		
		this.eventFullClass = eventClass;
		listenerInterfaceName = n;
	}
}



