package jcog.data.atomic;

import jcog.TODO;
import jcog.Util;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public final class AwesomeAtomicIntegerFieldUpdater<T> extends AtomicIntegerFieldUpdater<T> {
    private static final sun.misc.Unsafe U = Util.unsafe;
    private final long offset;
    //private final Class<?> cclass;
//        private final Class<T> tclass;

    public AwesomeAtomicIntegerFieldUpdater(final Class<T> tclass, final String fieldName) {
        Field field;
//            int modifiers;
        try {
            field = AccessController.doPrivileged((PrivilegedExceptionAction<Field>)
                    () -> tclass.getDeclaredField(fieldName));
//                modifiers = field.getModifiers();
            ////ReflectUtil.ensureMemberAccess(caller, tclass, (Object)null, modifiers);
            //ClassLoader cl = tclass.getClassLoader();
//                ClassLoader ccl = caller.getClassLoader();
            //if (ccl != null && ccl != cl && (cl == null || !isAncestor(cl, ccl))) {
            //  ReflectUtil.checkPackageAccess(tclass);
            //}
        } catch (PrivilegedActionException var8) {
            throw new RuntimeException(var8.getException());
        } catch (Exception var9) {
            throw new RuntimeException(var9);
        }

//            if (field.getType() != Integer.TYPE) {
//                throw new IllegalArgumentException("Must be integer type");
//            } else if (!Modifier.isVolatile(modifiers)) {
//                throw new IllegalArgumentException("Must be volatile type");
//            } else {
        //this.cclass = Modifier.isProtected(modifiers) && tclass.isAssignableFrom(caller) && !isSamePackage(tclass, caller) ? caller : tclass;
//                this.tclass = tclass;
        this.offset = U.objectFieldOffset(field);
//            }
    }


    //        private final void throwAccessCheckException(T obj) {
//            if (this.cclass == this.tclass) {
//                throw new ClassCastException();
//            } else {
//                throw new RuntimeException(new IllegalAccessException("Class " + this.cclass.getName() + " can not access a protected member of class " + this.tclass.getName() + " using an instance of " + obj.getClass().getName()));
//            }
//        }

    public final boolean compareAndSet(T obj, int expect, int update) {

        return U.compareAndSwapInt(obj, this.offset, expect, update);
    }

    public final boolean weakCompareAndSet(T obj, int expect, int update) {

        return U.compareAndSwapInt(obj, this.offset, expect, update);
    }

    public final void set(T obj, int newValue) {

        U.putIntVolatile(obj, this.offset, newValue);
    }

    public final void lazySet(T obj, int newValue) {
        throw new TODO();
//            this.accessCheck(obj);
//            U.putIntRelease(obj, this.offset, newValue);
    }

    public final int get(T obj) {

        return U.getIntVolatile(obj, this.offset);
    }

    public final int getAndSet(T obj, int newValue) {

        return U.getAndSetInt(obj, this.offset, newValue);
    }

    public final int getAndAdd(T obj, int delta) {

        return U.getAndAddInt(obj, this.offset, delta);
    }

    public final int getAndIncrement(T obj) {
        return this.getAndAdd(obj, 1);
    }

    public final int getAndDecrement(T obj) {
        return this.getAndAdd(obj, -1);
    }

    public final int incrementAndGet(T obj) {
        return this.getAndAdd(obj, 1) + 1;
    }

    public final int decrementAndGet(T obj) {
        return this.getAndAdd(obj, -1) - 1;
    }

    public final int addAndGet(T obj, int delta) {
        return this.getAndAdd(obj, delta) + delta;
    }
}
