package jcog.util;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/** from: https:

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A wrapper for an {@link Object} or {@link Class} upon which reflective calls
 * can be made.
 * <p>
 * An example of using <code>Reflect</code> is <code><pre>
 * 
 * import static org.joor.Reflect.*;
 *
 * 
 * on("java.lang.String")
 * 
 * .create("Hello World")
 * 
 * .call("toString")
 * 
 *
 * @author Lukas Eder
 * @author Irek Matysiewicz
 * @author Thomas Darimont
 */
public class Reflect {

    
    
    

    /**
     * Wrap a class name.
     * <p>
     * This is the same as calling <code>on(Class.forName(name))</code>
     *
     * @param name A fully qualified class name
     * @return A wrapped class object, to be used for further reflection.
     * @throws ReflectException If any reflection exception occurred.
     * @see #on(Class)
     */
    public static Reflect on(String name) throws ReflectException {
        return on(forName(name));
    }

    /**
     * Wrap a class name, loading it via a given class loader.
     * <p>
     * This is the same as calling
     * <code>on(Class.forName(name, classLoader))</code>
     *
     * @param name A fully qualified class name.
     * @param classLoader The class loader in whose context the class should be
     *            loaded.
     * @return A wrapped class object, to be used for further reflection.
     * @throws ReflectException If any reflection exception occurred.
     * @see #on(Class)
     */
    public static Reflect on(String name, ClassLoader classLoader) throws ReflectException {
        return on(forName(name, classLoader));
    }

    /**
     * Wrap a class.
     * <p>
     * Use this when you want to access static fields and methods on a
     * {@link Class} object, or as a basis for constructing objects of that
     * class using {@link #create(Object...)}
     *
     * @param clazz The class to be wrapped
     * @return A wrapped class object, to be used for further reflection.
     */
    public static Reflect on(Class<?> clazz) {
        return new Reflect(clazz);
    }

    /**
     * Wrap an object.
     * <p>
     * Use this when you want to access instance fields and methods on any
     * {@link Object}
     *
     * @param object The object to be wrapped
     * @return A wrapped object, to be used for further reflection.
     */
    public static Reflect on(Object object) {
        return new Reflect(object == null ? Object.class : object.getClass(), object);
    }

    private static Reflect on(Class<?> type, Object object) {
        return new Reflect(type, object);
    }

    /**
     * Conveniently render an {@link AccessibleObject} accessible.
     * <p>
     * To prevent {@link SecurityException}, this is only done if the argument
     * object and its declaring class are non-public.
     *
     * @param accessible The object to render accessible
     * @return The argument object rendered accessible
     */
    public static <T extends AccessibleObject> T accessible(T accessible) {
        if (accessible == null) {
            return null;
        }

        if (accessible instanceof Member) {
            Member member = (Member) accessible;

            if (Modifier.isPublic(member.getModifiers()) &&
                    Modifier.isPublic(member.getDeclaringClass().getModifiers())) {

                return accessible;
            }
        }

        
        if (!accessible.isAccessible()) {
            //accessible.setAccessible(true);
            if (!accessible.trySetAccessible())
                throw new ReflectException("could not set accessible: " + accessible);
        }

        return accessible;
    }

    
    
    

//    /* [java-8] */
//    private static final Constructor<MethodHandles.Lookup> CACHED_LOOKUP_CONSTRUCTOR;
//
//    static {
//        try {
//            CACHED_LOOKUP_CONSTRUCTOR = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
//
//            if (!CACHED_LOOKUP_CONSTRUCTOR.isAccessible())
//                CACHED_LOOKUP_CONSTRUCTOR.setAccessible(true);
//        }
//        catch (Exception e) {
//            throw new IllegalStateException(e);
//        }
//    }
//    /* [/java-8] */

    /**
     * The type of the wrapped object.
     */
    private final Class<?> type;

    /**
     * The wrapped object.
     */
    private final Object   object;

    
    
    

    private Reflect(Class<?> type) {
        this(type, type);
    }

    private Reflect(Class<?> type, Object object) {
        this.type = type;
        this.object = object;
    }

    
    
    

    /**
     * Get the wrapped object
     *
     * @param <T> A convenience generic parameter for automatic unsafe casting
     */
    @SuppressWarnings("unchecked")
    public <T> T get() {
        return (T) object;
    }

    /**
     * Set a field value.
     * <p>
     * This is roughly equivalent to {@link Field#set(Object, Object)}. If the
     * wrapped object is a {@link Class}, then this will set a value to a static
     * member field. If the wrapped object is any other {@link Object}, then
     * this will set a value to an instance member field.
     * <p>
     * This method is also capable of setting the value of (static) final
     * fields. This may be convenient in situations where no
     * {@link SecurityManager} is expected to prevent this, but do note that
     * (especially static) final fields may already have been inlined by the
     * javac and/or JIT and relevant code deleted from the runtime verison of
     * your program, so setting these fields might not have any effect on your
     * execution.
     * <p>
     * For restrictions of usage regarding setting values on final fields check:
     * <a href=
     * "http:
     * ... and <a href=
     * "http:
     *
     * @param name The field name
     * @param value The new field value
     * @return The same wrapped object, to be used for further reflection.
     * @throws ReflectException If any reflection exception occurred.
     */
    public Reflect set(String name, Object value) throws ReflectException {
        try {
            Field field = field0(name);
            if ((field.getModifiers() & Modifier.FINAL) == Modifier.FINAL) {
                Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true); //TODO trySetAccessible
                modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            }
            field.set(object, unwrap(value));
            return this;
        }
        catch (Exception e) {
            throw new ReflectException(e);
        }
    }

    /**
     * Get a field value.
     * <p>
     * This is roughly equivalent to {@link Field#get(Object)}. If the wrapped
     * object is a {@link Class}, then this will get a value from a static
     * member field. If the wrapped object is any other {@link Object}, then
     * this will get a value from an instance member field.
     * <p>
     * If you want to "navigate" to a wrapped version of the field, use
     * {@link #field(String)} instead.
     *
     * @param name The field name
     * @return The field value
     * @throws ReflectException If any reflection exception occurred.
     * @see #field(String)
     */
    public <T> T get(String name) throws ReflectException {
        return field(name).get();
    }

    /**
     * Get a wrapped field.
     * <p>
     * This is roughly equivalent to {@link Field#get(Object)}. If the wrapped
     * object is a {@link Class}, then this will wrap a static member field. If
     * the wrapped object is any other {@link Object}, then this wrap an
     * instance member field.
     *
     * @param name The field name
     * @return The wrapped field
     * @throws ReflectException If any reflection exception occurred.
     */
    public Reflect field(String name) throws ReflectException {
        try {
            Field field = field0(name);
            return on(field.getType(),
                    object instanceof Class ? field /* the field itself */ : field.get(object));
        } catch (Exception e) {
            throw new ReflectException(e);
        }
    }

    private Field field0(String name) throws ReflectException {
        Class<?> t = type();

        
        try {
            return accessible(t.getField(name));
        } catch (NoSuchFieldException e) {
            do {
                try {
                    return accessible(t.getDeclaredField(name));
                }
                catch (NoSuchFieldException ignore) {}

                t = t.getSuperclass();
            }
            while (t != null);

            throw new ReflectException(e);
        }
    }

    public Map<String, Reflect> fields(boolean instance, boolean statik) {
        return fields(instance, statik, true);
    }

    /**
     * Get a Map containing field names and wrapped values for the fields'
     * values.
     * <p>
     * If the wrapped object is a {@link Class}, then this will return static
     * fields. If the wrapped object is any other {@link Object}, then this will
     * return instance fields.
     * <p>
     * These two calls are equivalent <code><pre>
     * on(object).field("myField");
     * on(object).fields().get("myField");
     * </pre></code>
     *
     * @return A map containing field names and wrapped values.
     */
    public Map<String, Reflect> fields(boolean instance, boolean statik, boolean nonPublic) {

        Map<String, Reflect> result = new LinkedHashMap<>(16);

        Class<?> t = type();

        Function<String, Reflect> fieldResolver = this::field;

        do {
            for (Field field : t.getDeclaredFields()) {
                int mods = field.getModifiers();
                boolean isStatik = Modifier.isStatic(mods);
                if ((isStatik && statik ) || (!isStatik && instance)) {
                    if (!nonPublic && !Modifier.isPublic(mods))
                        continue;

                    result.computeIfAbsent(field.getName(), fieldResolver);
                }
            }

            t = t.getSuperclass();
        }
        while (t != null);

        return result;
    }

    /**
     * Call a method by its name.
     * <p>
     * This is a convenience method for calling
     * <code>call(name, new Object[0])</code>
     *
     * @param name The method name
     * @return The wrapped method result or the same wrapped object if the
     *         method returns <code>void</code>, to be used for further
     *         reflection.
     * @throws ReflectException If any reflection exception occurred.
     * @see #call(String, Object...)
     */
    public Reflect call(String name) throws ReflectException {
        return call(name, new Object[0]);
    }

    /**
     * Call a method by its name.
     * <p>
     * This is roughly equivalent to {@link Method#invoke(Object, Object...)}.
     * If the wrapped object is a {@link Class}, then this will invoke a static
     * method. If the wrapped object is any other {@link Object}, then this will
     * invoke an instance method.
     * <p>
     * Just like {@link Method#invoke(Object, Object...)}, this will try to wrap
     * primitive types or unwrap primitive type wrappers if applicable. If
     * several methods are applicable, by that rule, the first one encountered
     * is called. i.e. when calling <code><pre>
     * on(...).call("method", 1, 1);
     * </pre></code> The first of the following methods will be called:
     * <code><pre>
     * public void method(int param1, Integer param2);
     * public void method(Integer param1, int param2);
     * public void method(Number param1, Number param2);
     * public void method(Number param1, Object param2);
     * public void method(int param1, Object param2);
     * </pre></code>
     * <p>
     * The best matching method is searched for with the following strategy:
     * <ol>
     * <li>public method with exact signature match in class hierarchy</li>
     * <li>non-public method with exact signature match on declaring class</li>
     * <li>public method with similar signature in class hierarchy</li>
     * <li>non-public method with similar signature on declaring class</li>
     * </ol>
     *
     * @param name The method name
     * @param args The method arguments
     * @return The wrapped method result or the same wrapped object if the
     *         method returns <code>void</code>, to be used for further
     *         reflection.
     * @throws ReflectException If any reflection exception occurred.
     */
    public Reflect call(String name, Object... args) throws ReflectException {
        Class<?>[] types = types(args);

        try {
            Method method = exactMethod(name, types);
            return on(method, object, args);
        }

        catch (NoSuchMethodException e) {
            try {
                Method method = similarMethod(name, types);
                return on(method, object, args);
            } catch (NoSuchMethodException e1) {
                throw new ReflectException(e1);
            }
        }
    }

    /**
     * Searches a method with the exact same signature as desired.
     * <p>
     * If a public method is found in the class hierarchy, this method is returned.
     * Otherwise a private method with the exact same signature is returned.
     * If no exact match could be found, we let the {@code NoSuchMethodException} pass through.
     */
    private Method exactMethod(String name, Class<?>[] types) throws NoSuchMethodException {
        Class<?> t = type();

        
        try {
            return t.getMethod(name, types);
        }

        
        catch (NoSuchMethodException e) {
            do {
                try {
                    return t.getDeclaredMethod(name, types);
                }
                catch (NoSuchMethodException ignore) {}

                t = t.getSuperclass();
            }
            while (t != null);

            throw new NoSuchMethodException();
        }
    }

    /**
     * Searches a method with a similar signature as desired using
     * {@link #isSimilarSignature(java.lang.reflect.Method, String, Class[])}.
     * <p>
     * First public methods are searched in the class hierarchy, then private
     * methods on the declaring class. If a method could be found, it is
     * returned, otherwise a {@code NoSuchMethodException} is thrown.
     */
    private Method similarMethod(String name, Class<?>[] types) throws NoSuchMethodException {
        Class<?> t = type();

        
        
        for (Method method : t.getMethods()) {
            if (isSimilarSignature(method, name, types)) {
                return method;
            }
        }

        
        do {
            for (Method method : t.getDeclaredMethods()) {
                if (isSimilarSignature(method, name, types)) {
                    return method;
                }
            }

            t = t.getSuperclass();
        }
        while (t != null);

        throw new NoSuchMethodException("No similar method " + name + " with params " + Arrays.toString(types) + " could be found on type " + type() + '.');
    }

    /**
     * Determines if a method has a "similar" signature, especially if wrapping
     * primitive argument types would result in an exactly matching signature.
     */
    private boolean isSimilarSignature(Method possiblyMatchingMethod, String desiredMethodName, Class<?>[] desiredParamTypes) {
        return possiblyMatchingMethod.getName().equals(desiredMethodName) && match(possiblyMatchingMethod.getParameterTypes(), desiredParamTypes);
    }

    /**
     * Call a constructor.
     * <p>
     * This is a convenience method for calling
     * <code>create(new Object[0])</code>
     *
     * @return The wrapped new object, to be used for further reflection.
     * @throws ReflectException If any reflection exception occurred.
     * @see #create(Object...)
     */
    public Reflect create() throws ReflectException {
        return create(new Object[0]);
    }

    /**
     * Call a constructor.
     * <p>
     * This is roughly equivalent to {@link Constructor#newInstance(Object...)}.
     * If the wrapped object is a {@link Class}, then this will create a new
     * object of that class. If the wrapped object is any other {@link Object},
     * then this will create a new object of the same type.
     * <p>
     * Just like {@link Constructor#newInstance(Object...)}, this will try to
     * wrap primitive types or unwrap primitive type wrappers if applicable. If
     * several constructors are applicable, by that rule, the first one
     * encountered is called. i.e. when calling <code><pre>
     * on(C.class).create(1, 1);
     * </pre></code> The first of the following constructors will be applied:
     * <code><pre>
     * public C(int param1, Integer param2);
     * public C(Integer param1, int param2);
     * public C(Number param1, Number param2);
     * public C(Number param1, Object param2);
     * public C(int param1, Object param2);
     * </pre></code>
     *
     * @param args The constructor arguments
     * @return The wrapped new object, to be used for further reflection.
     * @throws ReflectException If any reflection exception occurred.
     */
    public Reflect create(Object... args) throws ReflectException {
        Class<?>[] types = types(args);

        
        
        try {
            Constructor<?> constructor = type().getDeclaredConstructor(types);
            return on(constructor, args);
        }

        
        
        catch (NoSuchMethodException e) {
            for (Constructor<?> constructor : type().getDeclaredConstructors()) {
                if (match(constructor.getParameterTypes(), types)) {
                    return on(constructor, args);
                }
            }

            throw new ReflectException(e);
        }
    }

//    /**
//     * Create a proxy for the wrapped object allowing to typesafely invoke
//     * methods on it using a custom interface
//     *
//     * @param proxyType The interface type that is implemented by the proxy
//     * @return A proxy for the wrapped object
//     */
//    @SuppressWarnings("unchecked")
//    public <P> P as(final Class<P> proxyType) {
//        final boolean isMap = (object instanceof Map);
//        final InvocationHandler handler = (proxy, method, args) -> {
//            String name = method.getName();
//
//
//            try {
//                return on(type, object).call(name, args).get();
//            }
//
//
//            catch (ReflectException e) {
//                if (isMap) {
//                    Map<String, Object> map = (Map<String, Object>) object;
//                    int length = (args == null ? 0 : args.length);
//
//                    if (length == 0 && name.startsWith("get")) {
//                        return map.get(property(name.substring(3)));
//                    }
//                    else if (length == 0 && name.startsWith("is")) {
//                        return map.get(property(name.substring(2)));
//                    }
//                    else if (length == 1 && name.startsWith("setAt")) {
//                        map.put(property(name.substring(3)), args[0]);
//                        return null;
//                    }
//                }
//
//                /* [java-8] */
//                if (method.isDefault()) {
//                    return CACHED_LOOKUP_CONSTRUCTOR
//                            .newInstance(proxyType)
//                            .unreflectSpecial(method, proxyType)
//                            .bindTo(proxy)
//                            .invokeWithArguments(args);
//                }
//                /* [/java-8] */
//
//                throw e;
//            }
//        };
//
//        return (P) as(proxyType, handler);
//    }

    public Object as(Class proxyType, InvocationHandler handler) {
        return Proxy.newProxyInstance(proxyType.getClassLoader(), new Class[] { proxyType }, handler);
    }

    /**
     * Get the POJO property name of an getter/setter
     */
    private static String property(String string) {
        int length = string.length();

        if (length == 0) {
            return "";
        }
        else if (length == 1) {
            return string.toLowerCase();
        }
        else {
            return string.substring(0, 1).toLowerCase() + string.substring(1);
        }
    }

    
    
    

    /**
     * Check whether two arrays of types match, converting primitive types to
     * their corresponding wrappers.
     */
    private boolean match(Class<?>[] declared, Class<?>[] actual) {
        if (declared == actual)
            return true;

        if (declared.length == actual.length) {
            for (int i = 0; i < actual.length; i++) {
                if (actual[i] == NULL.class)
                    continue;

                if (wrapper(declared[i]).isAssignableFrom(wrapper(actual[i])))
                    continue;

                return false;
            }

            return true;
        }
        else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return object.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Reflect) {
            return object.equals(((Reflect) obj).get());
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return object.toString();
    }

    
    
    

    /**
     * Wrap an object created from a constructor
     */
    private static Reflect on(Constructor<?> constructor, Object... args) throws ReflectException {
        try {
            return on(constructor.getDeclaringClass(), accessible(constructor).newInstance(args));
        }
        catch (Exception e) {
            throw new ReflectException(e);
        }
    }

    /**
     * Wrap an object returned from a method
     */
    private static Reflect on(Method method, Object object, Object... args) throws ReflectException {
        try {
            accessible(method);

            if (method.getReturnType() == void.class) {
                method.invoke(object, args);
                return on(object);
            }
            else {
                return on(method.invoke(object, args));
            }
        }
        catch (Exception e) {
            throw new ReflectException(e);
        }
    }

    /**
     * Unwrap an object
     */
    private static Object unwrap(Object object) {
        if (object instanceof Reflect) {
            return ((Reflect) object).get();
        }

        return object;
    }

    /**
     * Get an array of types for an array of objects
     *
     * @see Object#getClass()
     */
    private static Class<?>[] types(Object... values) {
        if (values == null) {
            return new Class[0];
        }

        Class<?>[] result = new Class[values.length];

        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            result[i] = value == null ? NULL.class : value.getClass();
        }

        return result;
    }

    /**
     * Load a class
     *
     * @see Class#forName(String)
     */
    private static Class<?> forName(String name) throws ReflectException {
        try {
            return Class.forName(name);
        }
        catch (Exception e) {
            throw new ReflectException(e);
        }
    }

    private static Class<?> forName(String name, ClassLoader classLoader) throws ReflectException {
        try {
            return Class.forName(name, true, classLoader);
        }
        catch (Exception e) {
            throw new ReflectException(e);
        }
    }

    /**
     * Get the type of the wrapped object.
     *
     * @see Object#getClass()
     */
    public Class<?> type() {
        return type;
    }

    /**
     * Get a wrapper type for a primitive type, or the argument type itself, if
     * it is not a primitive type.
     */
    public static Class<?> wrapper(Class<?> type) {
        if (type == null) {
            return null;
        }
        else if (type.isPrimitive()) {
            if (boolean.class == type) {
                return Boolean.class;
            }
            else if (int.class == type) {
                return Integer.class;
            }
            else if (long.class == type) {
                return Long.class;
            }
            else if (short.class == type) {
                return Short.class;
            }
            else if (byte.class == type) {
                return Byte.class;
            }
            else if (double.class == type) {
                return Double.class;
            }
            else if (float.class == type) {
                return Float.class;
            }
            else if (char.class == type) {
                return Character.class;
            }
            else if (void.class == type) {
                return Void.class;
            }
        }

        return type;
    }

    private static class NULL {}

//
//    /**
//     * A unchecked wrapper for any of Java's checked reflection exceptions:
//     * <p>
//     * These exceptions are
//     * <ul>
//     * <li> {@link ClassNotFoundException}</li>
//     * <li> {@link IllegalAccessException}</li>
//     * <li> {@link IllegalArgumentException}</li>
//     * <li> {@link InstantiationException}</li>
//     * <li> {@link InvocationTargetException}</li>
//     * <li> {@link NoSuchMethodException}</li>
//     * <li> {@link NoSuchFieldException}</li>
//     * <li> {@link SecurityException}</li>
//     * </ul>
//     *
//     * @author Lukas Eder
//     */
    final static class ReflectException extends RuntimeException {

        ReflectException(String msg) {
            super(msg);
        }

        ReflectException(Throwable cause) {
            super(cause);
        }
    }
}