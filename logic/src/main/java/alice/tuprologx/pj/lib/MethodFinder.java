/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package alice.tuprologx.pj.lib;

/*
 * This file is part of Domingo
 * an Open Source Java-API to Lotus Notes/Domino
 * hosted at http:
 *
 * Copyright (c) 2003-2007 Beck et al. projects GmbH Munich, Germany (http:
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

/* Origin:
 * From <http:
 * Original license- public domain? code published in article
 * with changes by Ronny Brandt, see: <http:
 * dresden-ocl is licensed GNU LGPL 2.1 or any later version
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Finds methods and constructors that can be invoked by reflection.
 * Attempts to address some of the limitations of the JDK's
 * Class.getMethod() and Class.getConstructor(), and other JDK
 * reflective facilities.
 */
public final class MethodFinder {

    /**
	 * The target class to look for methods and constructors in.
	 */
    private final Class<?> clazz;

    /**
	 * Mapping from method name to the Methods in the target class with that name.
	 */
    private final Map<String,List<Member>> methodMap = new HashMap<>();

    /**
	 * List of the Constructors in the target class.
	 */
    private final List<Member> ctorList = new ArrayList<>();

    /**
	 * Mapping from a Constructor or Method object to the Class objects representing its formal parameters.
	 */
    private final Map<Member,Class<?>[]> paramMap = new HashMap<>();

    /**
     * @param  clazz  Class in which I will look for methods and constructors
     * throws  IllegalArgumentException if clazz is null, or represents a primitive, or represents an array type
     */
    public MethodFinder(final Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("null Class parameter");
        }
        if (clazz.isPrimitive()) {
            throw new IllegalArgumentException("primitive Class parameter");
        }
        if (clazz.isArray()) {
            throw new IllegalArgumentException("array Class parameter");
        }
        this.clazz = clazz;
        loadMethods();
        loadConstructors();
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            MethodFinder other = (MethodFinder) o;
            return clazz.equals(other.clazz);
        }
    }

    /**
     * Returns the most specific public constructor in my target class that
     * accepts the number and type of parameters in the given Class array in a
     * reflective invocation. <p> A null value or Void.TYPE in parameterTypes
     * matches a corresponding Object or array reference in a constructor's
     * formal parameter list, but not a primitive formal parameter.
     *
     * @param parameterTypes array representing the number and types of
     *            parameters to look for in the constructor's signature. A null
     *            array is treated as a zero-length array.
     * @return Constructor object satisfying the conditions
     * @throws NoSuchMethodException if no constructors match the criteria,
     *                or if the reflective call is ambiguous based on the
     *                parameter types
     */
    public Constructor<?> findConstructor(final Class<?>... parameterTypes) throws NoSuchMethodException {
        return (Constructor<?>) findMemberIn(ctorList, parameterTypes == null ? new Class[0] : parameterTypes);
    }

    /**
     * Basis of findConstructor() and findMethod(). The member list fed to this
     * method will be either all Constructor objects or all Method objects.
     */
    private Member findMemberIn(final List<Member> memberList, final Class<?>... parameterTypes) throws NoSuchMethodException {
        List<Member> matchingMembers = new ArrayList<>();
        for (Member member: memberList) {
            Class<?>[] methodParamTypes = paramMap.get(member);

            if (Arrays.equals(methodParamTypes, parameterTypes)) {
                return member;
            }
            if (ClassUtilities.compatibleClasses(methodParamTypes, parameterTypes)) {
                matchingMembers.add(member);
            }
        }
        if (matchingMembers.isEmpty()) {
            throw new NoSuchMethodException("no member in " + clazz.getName() + " matching given args");
        }
        if (matchingMembers.size() == 1) {
            return matchingMembers.get(0);
        }
        return findMostSpecificMemberIn(matchingMembers);
    }

    /**
     * Returns the most specific public method in my target class that has the
     * given name and accepts the number and type of parameters in the given
     * Class array in a reflective invocation. <p> A null value or Void.TYPE in
     * parameterTypes will match a corresponding Object or array reference in a
     * method's formal parameter list, but not a primitive formal parameter.
     *
     * @param methodName name of the method to search for
     * @param parameterTypes array representing the number and types of
     *            parameters to look for in the method's signature. A null array
     *            is treated as a zero-length array.
     * @return Method object satisfying the conditions
     * @throws NoSuchMethodException if no methods match the criteria, or if
     *                the reflective call is ambiguous based on the parameter
     *                types, or if methodName is null
     */
    public Method findMethod(final String methodName, final Class<?>... parameterTypes) throws NoSuchMethodException {
        List<Member> methodList = methodMap.get(methodName);
        if (methodList == null) {
            throw new NoSuchMethodException("no method named " + clazz.getName() + '.' + methodName);
        }
        return (Method) findMemberIn(methodList, parameterTypes == null ? new Class[0] : parameterTypes);
    }

    /**
     * @param a List of Members (either all Constructors or all Methods)
     * @return the most specific of all Members in the list
     * @throws NoSuchMethodException if there is an ambiguity as to which is
     *                most specific
     */
    private Member findMostSpecificMemberIn(final List<Member> memberList) throws NoSuchMethodException {
        List<Member> mostSpecificMembers = new ArrayList<>();

        for (Member member: memberList) {

            if (mostSpecificMembers.isEmpty()) {
                
                mostSpecificMembers.add(member);
            } else {
                boolean moreSpecific = true;
                boolean lessSpecific = false;

                
                
                for (Member moreSpecificMember: mostSpecificMembers) {

                    if (!memberIsMoreSpecific(member, moreSpecificMember)) {
                        /*
                         * Can't be more specific than the whole set. Bail out,
                         * and mark whether member is less specific than the
                         * member under consideration. If it is less specific,
                         * it need not be added to the ambiguity set. This is no
                         * guarantee of not getting added to the ambiguity
                         * set...we're just not clever enough yet to make that
                         * assessment.
                         */

                        moreSpecific = false;
                        lessSpecific = memberIsMoreSpecific(moreSpecificMember, member);
                        break;
                    }
                }

                if (moreSpecific) {
                    
                    mostSpecificMembers.clear();
                    mostSpecificMembers.add(member);
                } else if (!lessSpecific) {
                    
                    mostSpecificMembers.add(member);
                }
            }
        }

        if (mostSpecificMembers.size() > 1) {
            throw new NoSuchMethodException("Ambiguous request for member in " + this.clazz.getName() + " matching given args");
        }

        return mostSpecificMembers.get(0);
    }

    /**
     * @param args an Object array
     * @return an array of Class objects representing the classes of the objects
     *         in the given Object array. If args is null, a zero-length Class
     *         array is returned. If an element in args is null, then Void.TYPE
     *         is the corresponding Class in the return array.
     */
    public static Class<?>[] getParameterTypesFrom(final Object... args) {
        if (args == null) {
            return new Class[0];
        }
        Class<?>[] argTypes = new Class[args.length];
        for (int i = 0; i < args.length; ++i) {
            argTypes[i] = (args[i] == null) ? Void.TYPE : args[i].getClass();
        }
        return argTypes;
    }

    /**
     * @param classNames String array of fully qualified names (FQNs) of classes
     *            or primitives. Represent an array type by using its JVM type
     *            descriptor, with dots instead of slashes (e.g. represent the
     *            type int[] with "[I", and Object[][] with
     *            "[[Ljava.lang.Object;").
     * @return an array of Class objects representing the classes or primitives
     *         named by the FQNs in the given String array. If the String array
     *         is null, a zero-length Class array is returned. If an element in
     *         classNames is null, the empty string, "void", or "null", then
     *         Void.TYPE is the corresponding Class in the return array. If any
     *         classes require loading because of this operation, the loading is
     *         done by the ClassLoader that loaded this class. Such classes are
     *         not initialized, however.
     * @throws ClassNotFoundException if any of the FQNs name an unknown
     *                class
     */
    public static Class<?>[] getParameterTypesFrom(final String... classNames) throws ClassNotFoundException {
        return getParameterTypesFrom(classNames, MethodFinder.class.getClassLoader());
    }

    /**
     * @param classNames String array of fully qualified names (FQNs) of classes
     *            or primitives. Represent an array type by using its JVM type
     *            descriptor, with dots instead of slashes (e.g. represent the
     *            type int[] with "[I", and Object[][] with
     *            "[[Ljava.lang.Object;").
     * @param loader a ClassLoader
     * @return an array of Class objects representing the classes or primitives
     *         named by the FQNs in the given String array. If the String array
     *         is null, a zero-length Class array is returned. If an element in
     *         classNames is null, the empty string, "void", or "null", then
     *         Void.TYPE is the corresponding Class in the return array. If any
     *         classes require loading because of this operation, the loading is
     *         done by the given ClassLoader. Such classes are not initialized,
     *         however.
     * @throws ClassNotFoundException if any of the FQNs name an unknown
     *                class
     */
    public static Class<?>[] getParameterTypesFrom(final String[] classNames, final java.lang.ClassLoader loader)
            throws ClassNotFoundException {
        if (classNames == null) {
            return new Class[0];
        }
        Class<?>[] types = new Class[classNames.length];
        for (int i = 0; i < classNames.length; ++i) {
            types[i] = ClassUtilities.classForNameOrPrimitive(classNames[i], loader);
        }
        return types;
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return clazz.hashCode();
    }

    /**
     * Loads up the data structures for my target class's constructors.
     */
    private void loadConstructors() {
        Constructor<?>[] ctors = clazz.getConstructors();
        for (Constructor<?> ctor : ctors) {
            ctorList.add(ctor);
            paramMap.put(ctor, ctor.getParameterTypes());
        }
    }

    /**
     * Loads up the data structures for my target class's methods.
     */
    private void loadMethods() {
        
        List<Member> allMethods = getAllMethods();
        Method[] methods = allMethods.toArray(new Method[0]);
        for (Method method : methods) {
            Method m = method;
            String methodName = m.getName();
            Class<?>[] paramTypes = m.getParameterTypes();
            List<Member> list = methodMap.computeIfAbsent(methodName, k -> new ArrayList<>());
            if (!ClassUtilities.classIsAccessible(clazz)) {
                m = ClassUtilities.getAccessibleMethodFrom(clazz, methodName, paramTypes);
            }
            if (m != null) {
                list.add(m);
                paramMap.put(m, paramTypes);
            }
        }
    }

    private List<Member> getAllMethods() {
        List<Member> allMethods = new ArrayList<>();
        Class<?> c = clazz;
        while ((c != null)) {
            Method[] methods = c.getDeclaredMethods();
            List<? extends Member> list = null;
            if (methods != null) {
                list = Arrays.asList(methods);
            }
            if (list != null) {
                allMethods.addAll(list);
            }
            c = c.getSuperclass();
        }
        return allMethods;
    }

    /**
     * @param  first  a Member
     * @param  second  a Member
     * @return  true if the first Member is more specific than the second,
     * false otherwise.  Specificity is determined according to the
     * procedure in the Java Language Specification, section 15.12.2.
     */
    private boolean memberIsMoreSpecific(final Member first, final Member second) {
        Class<?>[] firstParamTypes = paramMap.get(first);
        Class<?>[] secondParamTypes = paramMap.get(second);
        return ClassUtilities.compatibleClasses(secondParamTypes, firstParamTypes);
    }
}
