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
package alice.tuprolog.lib;

import alice.tuprolog.*;
import alice.util.AbstractDynamicClassLoader;
import alice.util.InspectionUtils;
import alice.util.JavaDynamicClassLoader;
import jcog.TODO;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.IntStream;

/**
 * 
 * This class represents a tuProlog library enabling the interaction with the
 * Java environment from tuProlog.
 * 
 * Warning we use the setAccessible method 
 * 
 * The most specific method algorithm used to find constructors / methods has
 * been inspired by the article "What Is Interactive Scripting?", by Michael
 * Travers Dr. Dobb's -- Software Tools for the Professional Programmer January
 * 2000 CMP Media Inc., a United News and Media Company
 * 
 * Library/Theory Dependency: BasicLibrary
 */
@SuppressWarnings("serial")
public class OOLibrary extends PrologLib {

    public static final Struct STDERR = new Struct("stderr");
    public static final Struct RUNTIME = new Struct("runtime");
    public static final Struct CURRENT_THREAD = new Struct("current_thread");
    public static final Struct STDOUT = new Struct("stdout");
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
         * progressive counter used to identify registered objects
         */
    private int id;
    /**
     * progressive counter used to generate lambda function dinamically
     */
    private int counter;
    
    
    /**
	 * @author Alessio Mercurio
	 * 
	 * used to manage different classloaders.
	 */
    private final AbstractDynamicClassLoader dynamicLoader;
    
    /**
     * library theory
     */
    
    public OOLibrary()
    {






			dynamicLoader = new JavaDynamicClassLoader(new URL[] {}, getClass().getClassLoader());

    }


    @Override
    public String getTheory() {
        return
        
        
        
        ":- op(800,xfx,'<-').\n"
                + ":- op(850,xfx,'returns').\n"
                + ":- op(200,xfx,'as').\n"
                + ":- op(600,xfx,'.'). \n"
                + 
                "new_object_bt(ClassName,Args,Id):- new_object(ClassName,Args,Id).\n"
                + "new_object_bt(ClassName,Args,Id):- destroy_object(Id).\n"
                
                + "Obj <- What :- java_call(Obj,What,Res), Res \\== false.\n"
                + "Obj <- What returns Res :- java_call(Obj,What,Res).\n"
                
                + "array_set(Array,Index,Object):- class('java.lang.reflect.Array') <- setAt(Array as 'java.lang.Object',Index,Object as 'java.lang.Object'), !.\n"
                + "array_set(Array,Index,Object):- java_array_set_primitive(Array,Index,Object).\n"
                + "array_get(Array,Index,Object):- class('java.lang.reflect.Array') <- get(Array as 'java.lang.Object',Index) returns Object,!.\n"
                + "array_get(Array,Index,Object):- java_array_get_primitive(Array,Index,Object).\n"
                
				+ "array_length(Array,Length):- class('java.lang.reflect.Array') <- getLength(Array as 'java.lang.Object') returns Length.\n"

                
                + 
                "java_object_bt(ClassName,Args,Id):- java_object(ClassName,Args,Id).\n"
                + "java_object_bt(ClassName,Args,Id):- destroy_object(Id).\n"
                
                + "java_array_set(Array,Index,Object):- class('java.lang.reflect.Array') <- setAt(Array as 'java.lang.Object',Index,Object as 'java.lang.Object'), !.\n"
                + "java_array_set(Array,Index,Object):- java_array_set_primitive(Array,Index,Object).\n"
                + "java_array_get(Array,Index,Object):- class('java.lang.reflect.Array') <- get(Array as 'java.lang.Object',Index) returns Object,!.\n"
                + "java_array_get(Array,Index,Object):- java_array_get_primitive(Array,Index,Object).\n"
                
                + "java_array_length(Array,Length):- class('java.lang.reflect.Array') <- getLength(Array as 'java.lang.Object') returns Length.\n"
                + "java_object_string(Object,String):- Object <- toString returns String.    \n"
                +
                "java_catch(JavaGoal, List, Finally) :- call(JavaGoal), call(Finally).\n";
        		
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
    public void onSolveBegin(Term goal) {
        currentObjects.clear();
        currentObjects_inverse.clear();
        for (var en : staticObjects_inverse.entrySet()) {
            bindDynamicObject(en.getValue(), en.getKey());
        }
        preregisterObjects();
    }

    /**
     * objects actually pre-registered in order to be available since the
     * beginning of demonstration
     */
    protected void preregisterObjects() {
//        try {
            bindDynamicObject(STDOUT, System.out);
            bindDynamicObject(STDERR, System.err);
            bindDynamicObject(RUNTIME, Runtime.getRuntime());
            bindDynamicObject(CURRENT_THREAD, Thread.currentThread());
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
    }

     /**
     * Deprecated from tuProlog 3.0 use new_object
     */
    public boolean java_object_3(Term className, Term argl, Term id) throws JavaException {
    	return new_object_3(className, argl,id);
    }
    
    /**
     * Creates of a java object - not backtrackable case
     * @param className
     * @param argl
     * @param id
     * @return
     * @throws JavaException
     */
    public boolean new_object_3(Term className, Term argl, Term id) throws JavaException {
        className = className.term();
        var arg = (Struct) argl.term();
        id = id.term();
        try {
            if (!className.isAtomic()) {
                throw new JavaException(new ClassNotFoundException(
                        "Java class not found: " + className));
            }
            var clName = ((Struct) className).name();
            
            if (clName.endsWith("[]")) {
                var list = getArrayFromList(arg);
                var nargs = ((NumberTerm) list[0]).intValue();
                if (java_array(clName, nargs, id))
                    return true;
                else
                    throw new JavaException(new Exception());
            }
            var args = parseArg(getArrayFromList(arg));
            if (args == null) {
                throw new IllegalArgumentException(
                        "Illegal constructor arguments  " + arg);
            }
            
            try {
                var cl = Class.forName(clName, true, dynamicLoader);
                var args_value = args.getValues();
                var co = lookupConstructor(cl, args.getTypes(),args_value);
                if (co == null) {
                    Prolog.warn("Constructor not found: class " + clName);
                    throw new JavaException(new NoSuchMethodException(
                            "Constructor not found: class " + clName));
                }

                var obj = co.newInstance(args_value);
                if (bindDynamicObject(id, obj))
                    return true;
                else
                    throw new JavaException(new Exception());
            } catch (ClassNotFoundException ex) {
                Prolog.warn("Java class not found: " + clName);
                throw new JavaException(ex);
            } catch (InvocationTargetException ex) {
                Prolog.warn("Invalid constructor arguments.");
                throw new JavaException(ex);
            } catch (NoSuchMethodException ex) {
                Prolog.warn("Constructor not found: " + Arrays.toString(args.getTypes()));
                throw new JavaException(ex);
            } catch (InstantiationException ex) {
                Prolog.warn(
                        "Objects of class " + clName
                                + " cannot be instantiated");
                throw new JavaException(ex);
            } catch (IllegalArgumentException ex) {
                Prolog.warn("Illegal constructor arguments  " + args);
                throw new JavaException(ex);
            }
        } catch (Exception ex) {
            throw new JavaException(ex);
        }
    }
    
    /**
     * @author Roberta Calegari
     * 
     * Creates of a lambda object - not backtrackable case
     * @param interfaceName represent the name of the target interface i.e. 'java.util.function.Predicate<String>'
     * @param implementation contains the function implementation i.e. 's -> s.length()>4 '
     * @param id represent the identification_name of the created object function i.e. MyLambda
     * 
     * @throws JavaException, Exception
     */
    @SuppressWarnings("unchecked")
	public static boolean new_lambda_3(Term interfaceName, Term implementation, Term id) {































        throw new TODO();
    }

    /**
     * Destroy the link to a java object - called not directly, but from
     * predicate java_object (as second choice, for backtracking)
     * 
     * @throws JavaException
     */
    public boolean destroy_object_1(Term id) throws JavaException {
        id = id.term();
        try {
            if (id.isGround()) {
                unregisterDynamic((Struct) id);
            }
            return true;
        } catch (Exception ex) {
            throw new JavaException(ex);
        }
    }

    /**
     * Deprecated from tuProlog 3.0 use new_class
     * 
     * @throws JavaException
     */
    public boolean java_class_4(Term clSource, Term clName, Term clPathes,Term id) throws JavaException {
    	return new_class_4(clSource,  clName,  clPathes, id);
    }
    
    /**
     * The java class/4 creates, compiles and loads a new Java class from a source text
     * @param clSource: is a string representing the text source of the new Java class
     * @param clName: full class name
     * @param clPathes: is a (possibly empty) Prolog list of class paths that may be required for a successful dynamic compilation of this class
     * @param id: reference to an instance of the meta-class java.lang.Class rep- resenting the newly-created class
     * @return boolean: true if created false otherwise
     * @throws JavaException
     */
	public boolean new_class_4(Term clSource, Term clName, Term clPathes,Term id) throws JavaException {
        var classSource = (Struct) clSource.term();
        var className = (Struct) clName.term();
        var classPathes = (Struct) clPathes.term();
		id = id.term();
		try {
            var fullClassName = alice.util.Tools.removeApostrophes(className.toString());

            Iterator<? extends Term> it = classPathes.listIterator();
            var cp = "";
            while (it.hasNext()) {
                if (!cp.isEmpty()) {
                    cp += ";";
                }
                cp += alice.util.Tools.removeApostrophes(it.next()
                        .toString());
            }
            if (!cp.isEmpty()) {
                cp = " -classpath " + cp;
            }

            var text = alice.util.Tools.removeApostrophes(classSource.toString());
            var fullClassPath = fullClassName.replace('.', '/');
            try {
                var file = new FileWriter(fullClassPath + ".java");
                file.write(text);
                file.close();
            } catch (IOException ex) {
                Prolog.warn("Compilation of java sources failed");
                Prolog.warn(
                        "(creation of " + fullClassPath + ".java fail failed)");
                throw new JavaException(ex);
            }
            var cmd = "javac " + cp + ' ' + fullClassPath + ".java";

            try {
                var jc = Runtime.getRuntime().exec(cmd);
                var res = jc.waitFor();
                if (res != 0) {
                    Prolog.warn("Compilation of java sources failed");
                    Prolog.warn(
                            "(java compiler (javac) has stopped with errors)");
                    throw new IOException("Compilation of java sources failed");
                }
            } catch (IOException ex) {
                Prolog.warn("Compilation of java sources failed");
                Prolog.warn("(java compiler (javac) invocation failed)");
                throw new JavaException(ex);
            }
            try 
            {

                /**
            	 * @author Alessio Mercurio
            	 * 
            	 * On Dalvik VM we can only use the DexClassLoader.
            	 */

                var the_class = Class.forName(fullClassName, true, "Dalvik".equals(System.getProperty("java.vm.name")) ? dynamicLoader : new ClassLoader());

                if (bindDynamicObject(id, the_class))
                    return true;
                else
                    throw new JavaException(new Exception());
            } catch (ClassNotFoundException ex) {
                Prolog.warn("Compilation of java sources failed");
                Prolog.warn(
                        "(Java Class compiled, but not created: "
                                + fullClassName + " )");
                throw new JavaException(ex);
            }
        } catch (Exception ex) {
            throw new JavaException(ex);
        }
    }

    /**
	 * 
	 * Calls a method of a Java object
	 * 
	 * @throws JavaException
	 * 
	 */
	public boolean java_call_3(Term objId, Term method_name, Term idResult)
			throws JavaException {
		objId = objId.term();
		idResult = idResult.term();
        var method = (Struct) method_name.term();
        Signature args = null;
		String methodName = null;
		try {
			methodName = method.name();
			if (!objId.isAtomic()) {
				if (objId instanceof Var) {
					throw new JavaException(new IllegalArgumentException(objId
							.toString()));
				}
                var sel = (Struct) objId;
				if (".".equals(sel.name()) && sel.subs() == 2
						&& method.subs() == 1) {
                    switch (methodName) {
                        case "setAt":
                            return java_set(sel.subResolve(0), sel.subResolve(1), method
                                    .subResolve(0));
                        case "get":
                            return java_get(sel.subResolve(0), sel.subResolve(1), method
                                    .subResolve(0));
                    }
				}
			}
			args = parseArg(method);
			
			if (objId instanceof Var)
				throw new JavaException(new IllegalArgumentException(objId
						.toString()));
			if (args == null) {
				throw new JavaException(new IllegalArgumentException());
			}
            var objName = alice.util.Tools.removeApostrophes(objId.toString());
            var obj = staticObjects.containsKey(objName) ? staticObjects.get(objName) : currentObjects.get(objName);
            Object res = null;

			if (obj != null) {
                var cl = obj.getClass();
                var args_values = args.getValues();
                var m = lookupMethod(cl, methodName, args.getTypes(),args_values);
				if (m != null) {
					try {
						m.setAccessible(true);
						res = m.invoke(obj, args_values);
					} catch (IllegalAccessException ex) {
						Prolog.warn("Method invocation failed: " + methodName+ "( signature: " + args + " )");
						throw new JavaException(ex);
					}
				} else {
					Prolog.warn("Method not found: " + methodName + "( signature: "+ args + " )");
					throw new JavaException(new NoSuchMethodException("Method not found: " + methodName + "( signature: "+ args + " )"));
				}
			} else {
				if (objId.isCompound()) {
                    var id = (Struct) objId;

					if (id.subs() == 1 && "class".equals(id.name())) {
						try {
                            var clName = alice.util.Tools
									.removeApostrophes(id.sub(0).toString());
                            var cl = Class.forName(clName, true, dynamicLoader);

                            var m = InspectionUtils.searchForMethod(cl, methodName, args.getTypes());
							m.setAccessible(true);
							res = m.invoke(null, args.getValues());
						} catch (ClassNotFoundException ex) {
							
							
							Prolog.warn("Unknown class.");
							throw new JavaException(ex);
						}
					}
					else {

                        var m = java.lang.String.class.getMethod(methodName, args.getTypes());
						m.setAccessible(true);
						res = m.invoke(objName, args.getValues());
					}
				} else {

                    var m = java.lang.String.class.getMethod(methodName,
							args.getTypes());
					m.setAccessible(true);
					res = m.invoke(objName, args.getValues());
				}
			}
			if (parseResult(idResult, res))
				return true;
			else
				throw new JavaException(new Exception());
		} catch (InvocationTargetException ex) {
			Prolog.warn(
					"Method failed: " + methodName + " - ( signature: " + args
					+ " ) - Original Exception: "
					+ ex.getTargetException());
			throw new JavaException(new IllegalArgumentException());
		} catch (NoSuchMethodException ex) {
			Prolog.warn(
					"Method not found: " + methodName + " - ( signature: "
							+ args + " )");
			throw new JavaException(ex);
		} catch (IllegalArgumentException ex) {
			Prolog.warn(
					"Invalid arguments " + args + " - ( method: " + methodName
					+ " )");
			throw new JavaException(ex);
		} catch (Exception ex) {
			Prolog
			.warn("Generic error in method invocation " + methodName);
			throw new JavaException(ex);
		}
	}
	
    /**
     * @author Michele Mannino
     * 
     * Set global classpath
     * 
     * @throws JavaException
     * 
     */
    public boolean set_classpath_1(Term paths) throws JavaException
    {
    	try {
    		paths = paths.term();
        	if(!paths.isList())
        		throw new IllegalArgumentException();
            var listOfPaths = getStringArrayFromStruct((Struct) paths);
        	dynamicLoader.removeAllURLs();
        	dynamicLoader.addURLs(getURLsFromStringArray(listOfPaths));
        	return true;
    	}catch(IllegalArgumentException e)
        {
        	Prolog.warn("Illegal list of paths " + paths);
            throw new JavaException(e);
        }
        catch (Exception e) {
        	throw new JavaException(e);
		}
    }
    
    /**
     * @author Michele Mannino
     * 
     * Get global classpath
     * 
     * @throws JavaException
     * 
     */
    
	public boolean get_classpath_1(Term paths) throws JavaException
    {
    	try {
    		paths = paths.term();
    		if(!(paths instanceof Var))
    			throw new IllegalArgumentException();
            var urls = dynamicLoader.getURLs();
        	String stringURLs = null;
            if(urls.length > 0)
        	{
	        	stringURLs = "[";
	     
	        	for (var url : urls) {
                    var file = new File(java.net.URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8));
	        		stringURLs = stringURLs + '\'' + file.getPath() + "',";
				}
	        	
	        	stringURLs = stringURLs.substring(0, stringURLs.length() - 1);
                stringURLs += ']';
        	}
        	else
        		stringURLs = "[]";
            var pathTerm = Term.term(stringURLs);
            return unify(paths, pathTerm);
    	}catch(IllegalArgumentException e)
        {
        	Prolog.warn("Illegal list of paths " + paths);
            throw new JavaException(e);
        }
        catch (Exception e) {
        	throw new JavaException(e);
		}
    }
	
    /**
     * set the field value of an object
     */
    private boolean java_set(Term objId, Term fieldTerm, Term what) {
        what = what.term();
        if (!fieldTerm.isAtomic() || what instanceof Var)
            return false;
        var fieldName = ((Struct) fieldTerm).name();
        try {
            Class<?> cl = null;
            Object obj = null;
            if(objId.isCompound() && "class".equals(((Struct) objId).name()))
            {
            	String clName = null;
            	
            	if(((Struct) objId).subs() == 1)
            		 clName = alice.util.Tools.removeApostrophes(((Struct) objId).sub(0).toString());
            	if(clName != null)
            	{
            		try {
                        cl = Class.forName(clName, true, dynamicLoader);
                    } catch (ClassNotFoundException ex) {
                        Prolog.warn("Java class not found: " + clName);
                        return false;
                    } catch (Exception ex) {
                        Prolog.warn(
                                "Static field "
                                        + fieldName
                                        + " not found in class "
                                        + alice.util.Tools
                                                .removeApostrophes(((Struct) objId)
                                                        .sub(0).toString()));
                        return false;
                    }
            	}
            }
            else {
                var objName = alice.util.Tools
                        .removeApostrophes(objId.toString());
                obj = currentObjects.get(objName);
                if (obj != null) {
                    cl = obj.getClass();
                } else {
                    return false;
                }
            }


            var field = cl.getField(fieldName);
            if (what instanceof NumberTerm) {
                var wn = (NumberTerm) what;
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
            } else {
                var what_name = alice.util.Tools.removeApostrophes(what
                        .toString());
                var obj2 = currentObjects.get(what_name);
                if (obj2 != null) {
                    field.set(obj, obj2);
                } else {
                    
                    field.set(obj, what_name);
                }
            }
            return true;
        } catch (NoSuchFieldException ex) {
            Prolog.warn(
                    "Field " + fieldName + " not found in class " + objId);
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * get the value of the field
     */
    private boolean java_get(Term objId, Term fieldTerm, Term what) {
        if (!fieldTerm.isAtomic()) {
            return false;
        }
        var fieldName = ((Struct) fieldTerm).name();
        try {
            Class<?> cl = null;
            Object obj = null;
            if(objId.isCompound() && "class".equals(((Struct) objId).name()))
            {
            	String clName = null;
            	if(((Struct) objId).subs() == 1)
            		 clName = alice.util.Tools.removeApostrophes(((Struct) objId).sub(0).toString());
            	if(clName != null)
            	{
            		try {
                        cl = Class.forName(clName, true, dynamicLoader);
                    } catch (ClassNotFoundException ex) {
                        Prolog.warn("Java class not found: " + clName);
                        return false;
                    } catch (Exception ex) {
                        Prolog.warn(
                                "Static field "
                                        + fieldName
                                        + " not found in class "
                                        + alice.util.Tools
                                                .removeApostrophes(((Struct) objId)
                                                        .sub(0).toString()));
                        return false;
                    }
            	}
            }
            else {
                var objName = alice.util.Tools.removeApostrophes(objId.toString());
                obj = currentObjects.get(objName);
                if (obj == null) {
                    return false;
                }
                cl = obj.getClass();
            }

            var field = cl.getField(fieldName);
            var fc = field.getType();
            field.setAccessible(true);
            if (fc == Integer.TYPE || fc == Byte.TYPE) {
                var value = field.getInt(obj);
                return unify(what, new NumberTerm.Int(value));
            } else if (fc == Long.TYPE) {
                var value = field.getLong(obj);
                return unify(what, new NumberTerm.Long(value));
            } else if (fc == Float.TYPE) {
                var value = field.getFloat(obj);
                return unify(what, new NumberTerm.Float(value));
            } else if (fc == Double.TYPE) {
                var value = field.getDouble(obj);
                return unify(what, new NumberTerm.Double(value));
            } else {

                var res = field.get(obj);
                return bindDynamicObject(what, res);
            }
            
        } catch (NoSuchFieldException ex) {
            Prolog.warn(
                    "Field " + fieldName + " not found in class " + objId);
            return false;
        } catch (Exception ex) {
            Prolog.warn("Generic error in accessing the field");
            return false;
        }
    }
    
    public boolean java_array_set_primitive_3(Term obj_id, Term i, Term what)
            throws JavaException {
        var objId = (Struct) obj_id.term();
        var index = (NumberTerm) i.term();
        what = what.term();
        if (!index.isInteger()) {
            throw new JavaException(new IllegalArgumentException(index
                    .toString()));
        }
        try {
            var objName = alice.util.Tools.removeApostrophes(objId.toString());
            var obj = currentObjects.get(objName);
            Class<?> cl = null;
            if (obj != null) {
                cl = obj.getClass();
            } else {
                throw new JavaException(new IllegalArgumentException(objId
                        .toString()));
            }

            if (!cl.isArray()) {
                throw new JavaException(new IllegalArgumentException(objId
                        .toString()));
            }
            var name = cl.toString();
            switch (name) {
                case "class [I": {
                    if (!(what instanceof NumberTerm)) {
                        throw new JavaException(new IllegalArgumentException(what
                                .toString()));
                    }
                    var v = (byte) ((NumberTerm) what).intValue();
                    Array.setInt(obj, index.intValue(), v);
                    break;
                }
                case "class [D": {
                    if (!(what instanceof NumberTerm)) {
                        throw new JavaException(new IllegalArgumentException(what
                                .toString()));
                    }
                    var v = ((NumberTerm) what).doubleValue();
                    Array.setDouble(obj, index.intValue(), v);
                    break;
                }
                case "class [F": {
                    if (!(what instanceof NumberTerm)) {
                        throw new JavaException(new IllegalArgumentException(what
                                .toString()));
                    }
                    var v = ((NumberTerm) what).floatValue();
                    Array.setFloat(obj, index.intValue(), v);
                    break;
                }
                case "class [L": {
                    if (!(what instanceof NumberTerm)) {
                        throw new JavaException(new IllegalArgumentException(what
                                .toString()));
                    }
                    var v = ((NumberTerm) what).longValue();
                    Array.setFloat(obj, index.intValue(), v);
                    break;
                }
                case "class [C": {
                    var s = what.toString();
                    Array.setChar(obj, index.intValue(), s.charAt(0));
                    break;
                }
                case "class [Z":
                    var s = what.toString();
                    switch (s) {
                        case "true":
                            Array.setBoolean(obj, index.intValue(), true);
                            break;
                        case "false":
                            Array.setBoolean(obj, index.intValue(), false);
                            break;
                        default:
                            throw new JavaException(new IllegalArgumentException(what
                                    .toString()));
                    }
                    break;
                case "class [B": {
                    if (!(what instanceof NumberTerm)) {
                        throw new JavaException(new IllegalArgumentException(what
                                .toString()));
                    }
                    var v = ((NumberTerm) what).intValue();
                    Array.setByte(obj, index.intValue(), (byte) v);
                    break;
                }
                case "class [S":
                    if (!(what instanceof NumberTerm)) {
                        throw new JavaException(new IllegalArgumentException(what
                                .toString()));
                    }
                    var v = (short) ((NumberTerm) what).intValue();
                    Array.setShort(obj, index.intValue(), v);
                    break;
                default:
                    throw new JavaException(new Exception());
            }
            return true;
        } catch (Exception ex) {
            throw new JavaException(ex);
        }
    }
    
    
    /**
     * Sets the value of the field 'i' with 'what'
     * @param obj_id
     * @param i
     * @param what
     * @return
     * @throws JavaException
     */
    public boolean java_array_get_primitive_3(Term obj_id, Term i, Term what) throws JavaException {
        var objId = (Struct) obj_id.term();
        var index = (NumberTerm) i.term();
        what = what.term();
        if (!index.isInteger()) {
            throw new JavaException(new IllegalArgumentException(index.toString()));
        }
        try {
            var objName = alice.util.Tools.removeApostrophes(objId.toString());
            var obj = currentObjects.get(objName);
            Class<?> cl = null;
            if (obj != null) {
                cl = obj.getClass();
            } else {
                throw new JavaException(new IllegalArgumentException(objId.toString()));
            }

            if (!cl.isArray()) {
                throw new JavaException(new IllegalArgumentException(objId.toString()));
            }
            var name = cl.toString();
            switch (name) {
                case "class [I": {
                    Term value = new NumberTerm.Int(Array.getInt(obj, index.intValue()));
                    if (unify(what, value))
                        return true;
                    else
                        throw new JavaException(new IllegalArgumentException(what.toString()));
                }
                case "class [D": {
                    Term value = new NumberTerm.Double(Array.getDouble(obj, index.intValue()));
                    if (unify(what, value))
                        return true;
                    else
                        throw new JavaException(new IllegalArgumentException(what
                                .toString()));
                }
                case "class [F": {
                    Term value = new NumberTerm.Float(Array.getFloat(obj, index
                            .intValue()));
                    if (unify(what, value))
                        return true;
                    else
                        throw new JavaException(new IllegalArgumentException(what
                                .toString()));
                }
                case "class [L": {
                    Term value = new NumberTerm.Long(Array.getLong(obj, index
                            .intValue()));
                    if (unify(what, value))
                        return true;
                    else
                        throw new JavaException(new IllegalArgumentException(what
                                .toString()));
                }
                case "class [C": {
                    Term value = new Struct(String.valueOf(Array.getChar(obj, index.intValue())));
                    if (unify(what, value))
                        return true;
                    else
                        throw new JavaException(new IllegalArgumentException(what
                                .toString()));
                }
                case "class [Z":
                    var b = Array.getBoolean(obj, index.intValue());
                    if (b) {
                        if (unify(what, Term.TRUE))
                            return true;
                        else
                            throw new JavaException(new IllegalArgumentException(
                                    what.toString()));
                    } else {
                        if (unify(what, Term.FALSE))
                            return true;
                        else
                            throw new JavaException(new IllegalArgumentException(
                                    what.toString()));
                    }
                case "class [B": {
                    Term value = new NumberTerm.Int(Array.getByte(obj, index
                            .intValue()));
                    if (unify(what, value))
                        return true;
                    else
                        throw new JavaException(new IllegalArgumentException(what
                                .toString()));
                }
                case "class [S":
                    Term value = new NumberTerm.Int(Array.getInt(obj, index
                            .intValue()));
                    if (unify(what, value))
                        return true;
                    else
                        throw new JavaException(new IllegalArgumentException(what
                                .toString()));
                default:
                    throw new JavaException(new Exception());
            }
        } catch (Exception ex) {
            
            throw new JavaException(ex);
        }

    }

    private boolean java_array(String type, int nargs, Term id) {
        try {
            Object array = null;
            var obtype = type.substring(0, type.length() - 2);

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
                    var cl = Class.forName(obtype, true, dynamicLoader);
                    array = Array.newInstance(cl, nargs);
                    break;
            }
            return bindDynamicObject(id, array);
        } catch (Exception ex) {
            
            return false;
        }
    }

    /**
     * Returns an URL array from a String array
     *
     * @throws JavaException
     */
    private static URL[] getURLsFromStringArray(String... paths) throws MalformedURLException
    {
    	URL[] urls = null;
    	if(paths != null)
    	{
	    	urls = new URL[paths.length];
			
			for (var i = 0; i < paths.length; i++)
			{
				if(paths[i] == null)
					continue;
				if(paths[i].contains("http") || paths[i].contains("https") || paths[i].contains("ftp"))
					urls[i] = new URL(paths[i]);
				else{
                    var file = new File(paths[i]);
					urls[i] = (file.toURI().toURL());
				}
			}
    	}
		return urls;
    }
    
    /**
     * Returns a String array from a Struct contains a list
     *
     * @throws JavaException
     */
    
    private static String[] getStringArrayFromStruct(Struct list) {
        var args = new String[list.listSize()];
        Iterator<? extends Term> it = list.listIterator();
        var count = 0;
        while (it.hasNext()) {
            var path = alice.util.Tools.removeApostrophes(it.next().toString());
            args[count++] = path;
        }
        return args;
    }
    
    
    /**
     * creation of method signature from prolog data
     */
    private Signature parseArg(Struct method) {
        var values = new Object[method.subs()];
        Class<?>[] types = new Class[method.subs()];
        for (var i = 0; i < method.subs(); i++) {
            if (!parse_arg(values, types, i, method.subResolve(i)))
                return null;
        }
        return new Signature(values, types);
    }

    private Signature parseArg(Object... objs) {
        var values = new Object[objs.length];
        Class<?>[] types = new Class[objs.length];
        for (var i = 0; i < objs.length; i++) {
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
                var name = alice.util.Tools.removeApostrophes(term.toString());
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
                        var obj = currentObjects.get(name);
                        values[i] = obj == null ? name : obj;
                        types[i] = values[i].getClass();
                        break;
                }
            } else if (term instanceof NumberTerm) {
                var t = (NumberTerm) term;
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

                var tc = (Struct) term;
                if ("as".equals(tc.name())) {
                    return parse_as(values, types, i, tc.subResolve(0), tc
                            .subResolve(1));
                } else {
                    var obj = currentObjects.get(alice.util.Tools
                            .removeApostrophes(tc.toString()));
                    values[i] = obj == null ? alice.util.Tools
                            .removeApostrophes(tc.toString()) : obj;
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
     * parsing 'as' operator, which makes it possible to define the specific
     * class of an argument
     * 
     */
    private boolean parse_as(Object[] values, Class<?>[] types, int i,
            Term castWhat, Term castTo) {
        try {
            if (!(castWhat instanceof NumberTerm)) {
                var castTo_name = alice.util.Tools
                        .removeApostrophes(((Struct) castTo).name());
                var castWhat_name = alice.util.Tools.removeApostrophes(castWhat
                        .term().toString());
                
                if ("java.lang.String".equals(castTo_name)
                        && "true".equals(castWhat_name)) {
                    values[i] = "true";
                    types[i] = String.class;
                    return true;
                } else if ("java.lang.String".equals(castTo_name)
                        && "false".equals(castWhat_name)) {
                    values[i] = "false";
                    types[i] = String.class;
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
                            castTo_name = "[L"
                                    + castTo_name.substring(0,
                                    castTo_name.length() - 2) + ';';
                            break;
                    }
                }
                if (!"null".equals(castWhat_name)) {
                    var obj_to_cast = currentObjects.get(castWhat_name);
                    if (obj_to_cast == null) {
                        if ("boolean".equals(castTo_name)) {
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
                            types[i] = Class.forName(castTo_name, true, dynamicLoader);
                        } catch (ClassNotFoundException ex) {
                            Prolog.warn(
                                    "Java class not found: " + castTo_name);
                            return false;
                        }
                    }
                } else {
                    values[i] = null;
                    switch (castTo_name) {
                        case "byte":
                            types[i] = java.lang.Byte.TYPE;
                            break;
                        case "short":
                            types[i] = java.lang.Short.TYPE;
                            break;
                        case "char":
                            types[i] = java.lang.Character.TYPE;
                            break;
                        case "int":
                            types[i] = java.lang.Integer.TYPE;
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
                            types[i] = java.lang.Boolean.TYPE;
                            break;
                        default:
                            try {
                                types[i] = Class.forName(castTo_name, true, dynamicLoader);
                            } catch (ClassNotFoundException ex) {
                                Prolog.warn(
                                        "Java class not found: " + castTo_name);
                                return false;
                            }
                            break;
                    }
                }
            } else {
                var num = (NumberTerm) castWhat;
                var castTo_name = ((Struct) castTo).name();
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
                        types[i] = Long.TYPE;
                        break;
                    case "float":
                        values[i] = num.floatValue();
                        types[i] = Float.TYPE;
                        break;
                    case "double":
                        values[i] = num.doubleValue();
                        types[i] = Double.TYPE;
                        break;
                    default:
                        return false;
                }
            }
        } catch (Exception ex) {
            Prolog.warn(
                    "Casting " + castWhat + " to " + castTo + " failed");
            return false;
        }
        return true;
    }

    /**
     * parses return value of a method invokation
     */
    private boolean parseResult(Term id, Object obj) {
        if (obj == null) {
            
            return unify(id, new Var());
        }
        try {
            if (obj instanceof Boolean) {
                return unify(id, (Boolean) obj ? Term.TRUE : Term.FALSE);
            } else if (obj instanceof Byte) {
                return unify(id, new NumberTerm.Int(((Byte) obj).intValue()));
            } else if (obj instanceof Short) {
                return unify(id, new NumberTerm.Int(((Short) obj).intValue()));
            } else if (obj instanceof Integer) {
                return unify(id, new NumberTerm.Int((Integer) obj));
            } else if (obj instanceof Long) {
                return unify(id, new NumberTerm.Long((Long) obj));
            } else if (obj instanceof Float) {
                return unify(id, new NumberTerm.Float(
                        (Float) obj));
            } else if (obj instanceof Double) {
                return unify(id, new NumberTerm.Double(
                        (Double) obj));
            } else if (obj instanceof String) {
                return unify(id, new Struct((String) obj));
            } else if (obj instanceof Character) {
                return unify(id, new Struct(obj.toString()));
            } else {
                return bindDynamicObject(id, obj);
            }
        } catch (Exception ex) {
            
            return false;
        }
    }

    private static Object[] getArrayFromList(Struct list) {
        var args = new Object[list.listSize()];
        Iterator<? extends Term> it = list.listIterator();
        var count = 0;
        while (it.hasNext()) {
            args[count++] = it.next();
        }
        return args;
    }

    /**
     * Register an object with the specified id. The life-time of the link to
     * the object is engine life-time, available besides the individual query.
     * 
     * 
     * @param id
     *            object identifier
     * @param obj
     *            the object
     * @return true if the operation is successful
     * @throws InvalidObjectIdException
     *             if the object id is not valid
     */
    public boolean register(Struct id, Object obj)
            throws InvalidObjectIdException {
        /*
         * note that this method act on the staticObject and
         * staticObject_inverse hashmaps
         */
        if (!id.isGround()) {
            throw new InvalidObjectIdException();
        }
        
        synchronized (staticObjects) {
            Object aKey = staticObjects_inverse.get(obj);

            if (aKey != null) {
                
                return false;
            } else {
                var raw_name = alice.util.Tools.removeApostrophes(id.term()
                        .toString());
                staticObjects.put(raw_name, obj);
                staticObjects_inverse.put(obj, id);
                return true;
            }
        }
    }
    
    /**
     * Register an object with the specified id. The life-time of the link to
     * the object is engine life-time, available besides the individual query.
     * 
     * The identifier must be a ground object.
     * 
     * @param id
     *            object identifier
     *            
     * @return true if the operation is successful
     * @throws JavaException
     *             if the object id is not valid
     */
    public boolean register_1(Term id) throws JavaException
    {
    	id = id.term();
        try
        {
            var obj = getRegisteredDynamicObject((Struct) id);
            return register((Struct)id, obj);
        }catch(InvalidObjectIdException e)
        {
        	Prolog.warn("Illegal object id " + id);
            throw new JavaException(e);
        }
    }
    
    /**
     * Unregister an object with the specified id.
     * 
     * The identifier must be a ground object.
     * 
     * @param id
     *            object identifier
     *            
     * @return true if the operation is successful
     * @throws JavaException
     *             if the object id is not valid
     */
    public boolean unregister_1(Term id) throws JavaException
    {
    	id = id.term();
    	try
        {
        	return unregister((Struct)id);
        }catch(InvalidObjectIdException e)
        {
        	Prolog.warn("Illegal object id " + id);
            throw new JavaException(e);
        }
    }
    
    /**
     * Registers an object, with automatic creation of the identifier.
     * 
     * If the object is already registered, its identifier is returned
     * 
     * @param obj
     *            object to be registered.
     * @return fresh id
     */
    public Struct register(Object obj) {
    	
        synchronized (staticObjects) {
            var aKey = staticObjects_inverse.get(obj);
            if (aKey != null) {
                return aKey;
            } else {
                var id = generateFreshId();
                staticObjects.put(id.name(), obj);
                staticObjects_inverse.put(obj, id);
                return id;
            }
        }
    }

    /**
     * Gets the reference to an object previously registered
     * 
     * @param id
     *            object id
     * @return the object, if present
     * @throws InvalidObjectIdException
     */
    public Object getRegisteredObject(Struct id)
            throws InvalidObjectIdException {
        if (!id.isGround()) {
            throw new InvalidObjectIdException();
        }
        synchronized (staticObjects) {
            return staticObjects.get(alice.util.Tools.removeApostrophes(id
                    .toString()));
        }
    }

    /**
     * Unregisters an object, given its identifier
     * 
     * 
     * @param id
     *            object identifier
     * @return true if the operation is successful
     * @throws InvalidObjectIdException
     *             if the id is not valid (e.g. is not ground)
     */
    public boolean unregister(Struct id) throws InvalidObjectIdException {
        if (!id.isGround()) {
            throw new InvalidObjectIdException();
        }
        synchronized (staticObjects) {
            var raw_name = alice.util.Tools.removeApostrophes(id.toString());
            var obj = staticObjects.remove(raw_name);
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
     * @param id
     *            object identifier
     * @param obj
     *            object
     */
    public void registerDynamic(Struct id, Object obj) {
        synchronized (currentObjects) {
            var raw_name = alice.util.Tools.removeApostrophes(id.toString());
            currentObjects.put(raw_name, obj);
            currentObjects_inverse.put(obj, id);
        }
    }

    /**
     * Registers an object for the query life-time, with the automatic
     * generation of the identifier.
     * 
     * If the object is already registered, its identifier is returned
     * 
     * @param obj
     *            object to be registered
     * @return identifier
     */
    public Struct registerDynamic(Object obj) {
        

        
        synchronized (currentObjects) {
            var aKey = currentObjects_inverse.get(obj);
            if (aKey != null) {
                return aKey;
            } else {
                var id = generateFreshId();
                currentObjects.put(id.name(), obj);
                currentObjects_inverse.put(obj, id);
                return id;
            }
        }
    }

    /**
     * Gets a registered dynamic object (returns null if not presents)
     */
    public Object getRegisteredDynamicObject(Struct id)
            throws InvalidObjectIdException {
        if (!id.isGround()) {
            throw new InvalidObjectIdException();
        }
        synchronized (currentObjects) {
            return currentObjects.get(alice.util.Tools.removeApostrophes(id
                    .toString()));
        }
    }

    /**
     * Unregister the object, only for dynamic case
     * 
     * @param id
     *            object identifier
     * @return true if the operation is successful
     */
    public boolean unregisterDynamic(Struct id) {
        synchronized (currentObjects) {
            var raw_name = alice.util.Tools.removeApostrophes(id.toString());
            var obj = currentObjects.remove(raw_name);
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
        
        synchronized (currentObjects) {
            var aKey = currentObjects_inverse.get(obj);
            if (aKey != null) {
                return unify(id, aKey);
            } else {
                
                if (id instanceof Var) {

                    var idTerm = generateFreshId();
                    unify(id, idTerm);
                    registerDynamic(idTerm, obj);
                    
                    return true;
                } else {

                    var raw_name = alice.util.Tools.removeApostrophes(id
                            .term().toString());
                    var linkedobj = currentObjects.get(raw_name);
                    if (linkedobj == null) {
                        registerDynamic((Struct) (id.term()), obj);
                        
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
     * 
     * @return
     */
    protected Struct generateFreshId() {
        return new Struct("$obj_" + id++);
    }

    /**
     * handling writeObject method is necessary in order to make the library
     * serializable, 'nullyfing' eventually objects registered in maps
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        var bak00 = currentObjects;
        var bak01 = currentObjects_inverse;
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
     * handling readObject method is necessary in order to have the library
     * reconstructed after a serialization
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException,
            ClassNotFoundException {
        in.defaultReadObject();
        currentObjects = new HashMap<>();
        currentObjects_inverse = new IdentityHashMap<>();
        preregisterObjects();
    }

    

    private static Method lookupMethod(Class<?> target, String name,
                                       Class<?>[] argClasses, Object... argValues)
            throws NoSuchMethodException {
        
        try {
            return target.getMethod(name, argClasses);
        } catch (NoSuchMethodException e) {
            if (argClasses.length == 0) { 
                
                return null;
            }
        }


        var methods = target.getMethods();
        var goodMethods = new Vector<Method>();
        for (var i = 0; i != methods.length; i++) {
            if (name.equals(methods[i].getName())
                    && matchClasses(methods[i].getParameterTypes(), argClasses))
                goodMethods.addElement(methods[i]);
        }
        switch (goodMethods.size()) {
        case 0:
            
            
            
            
            
            
            

            for (var i = 0; i != methods.length; i++) {
                if (name.equals(methods[i].getName())) {
                    var types = methods[i].getParameterTypes();
                    var val = matchClasses(types, argClasses, argValues);
                    if (val != null) {
                        
                        
                        for (var j = 0; j < types.length; j++) {
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

    private static Constructor<?> lookupConstructor(Class<?> target,
                                                    Class<?>[] argClasses, Object... argValues)
            throws NoSuchMethodException {
        
        try {
            return target.getConstructor(argClasses);
        } catch (NoSuchMethodException e) {
            if (argClasses.length == 0) { 
                
                return null;
            }
        }


        var constructors = target.getConstructors();
        var goodConstructors = new Vector<Constructor<?>>();
        for (var i = 0; i != constructors.length; i++) {
            if (matchClasses(constructors[i].getParameterTypes(), argClasses))
                goodConstructors.addElement(constructors[i]);
        }
        switch (goodConstructors.size()) {
        case 0:
            
            
            
            
            
            
            

            for (var i = 0; i != constructors.length; i++) {
                var types = constructors[i].getParameterTypes();
                var val = matchClasses(types, argClasses, argValues);
                if (val != null) {
                    
                    
                    for (var j = 0; j < types.length; j++) {
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
            return IntStream.range(0, mclasses.length).allMatch(i -> matchClass(mclasses[i], pclasses[i]));
        }
        return false;
    }

    private static boolean matchClass(Class<?> mclass, Class<?> pclass) {
        var assignable = mclass.isAssignableFrom(pclass);
        if (assignable) {
            return true;
        } else {
            return mclass == Long.TYPE
                    && (pclass == Integer.TYPE);
        }
    }

    private static Method mostSpecificMethod(Vector<Method> methods)
            throws NoSuchMethodException {
        for (var i = 0; i != methods.size(); i++) {
            for (var j = 0; j != methods.size(); j++) {
                if ((i != j)
                        && (moreSpecific(methods.elementAt(i),
                        methods.elementAt(j)))) {
                    methods.removeElementAt(j);
                    if (i > j)
                        i--;
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
        var p1 = c1.getParameterTypes();
        var p2 = c2.getParameterTypes();
        var n = p1.length;
        return IntStream.range(0, n).allMatch(i -> matchClass(p2[i], p1[i]));
    }

    private static Constructor<?> mostSpecificConstructor(Vector<Constructor<?>> constructors)
            throws NoSuchMethodException {
        var cs = constructors.size();
        for (var i = 0; i != cs; i++) {
            for (var j = 0; j != cs; j++) {
                if ((i != j) && (moreSpecific(constructors.elementAt(i), constructors.elementAt(j)))) {
                    constructors.removeElementAt(j);
                    cs--;
                    if (i > j)
                        i--;
                    j--;
                }
            }
        }
        if (cs == 1)
            return constructors.elementAt(0);
        else
            throw new NoSuchMethodException(">1 most specific constructor");
    }

    
    private static boolean moreSpecific(Constructor<?> c1, Constructor<?> c2) {
        var p1 = c1.getParameterTypes();
        var p2 = c2.getParameterTypes();
        var n = p1.length;
        return IntStream.range(0, n).allMatch(i -> matchClass(p2[i], p1[i]));
    }

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    private static Object[] matchClasses(Class<?>[] mclasses, Class<?>[] pclasses,
                                         Object... values) {
        if (mclasses.length == pclasses.length) {
            var newvalues = new Object[mclasses.length];

            for (var i = 0; i != mclasses.length; i++) {
                var assignable = mclasses[i].isAssignableFrom(pclasses[i]);
                if (assignable
                        || (mclasses[i] == Long.TYPE && pclasses[i] == Integer.TYPE)) {
                    newvalues[i] = values[i];
                } else if (mclasses[i] == Float.TYPE
                        && pclasses[i] == Double.TYPE) {
                    
                    
                    newvalues[i] = ((Double) values[i]).floatValue();
                } else if (mclasses[i] == Float.TYPE
                        && pclasses[i] == Integer.TYPE) {
                    
                    
                    newvalues[i] = (float) (Integer) values[i];
                } else if (mclasses[i] == Double.TYPE
                        && pclasses[i] == Integer.TYPE) {
                    
                    
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
 * Signature class mantains information about type and value of a method
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
        var st = "";
        for (var i = 0; i < types.length; i++) {
            st = st + "\n  Argument " + i + " -  VALUE: " + values[i]
                    + " TYPE: " + types[i];
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