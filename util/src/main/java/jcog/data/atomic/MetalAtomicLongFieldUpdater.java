package jcog.data.atomic;

import jcog.Util;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

public final class MetalAtomicLongFieldUpdater<T> extends AtomicLongFieldUpdater<T> {
	private static final Unsafe U = Util.unsafe;

	private final long offset;
	private final Class<?> cclass;
//        private final Class<T> tclass;

	public MetalAtomicLongFieldUpdater(final Class<T> tclass, final String fieldName) {

//            try {
//                field = (Field) AccessController.doPrivileged(new PrivilegedExceptionAction<Field>() {
//                    public Field run() throws NoSuchFieldException {
		Field field = null;
		try {
			field = tclass.getDeclaredField(fieldName);
			field.trySetAccessible();
			this.cclass = tclass; //Modifier.isProtected(modifiers) && tclass.isAssignableFrom(caller) && !isSamePackage(tclass, caller) ? caller : tclass;
//				this.tclass = tclass;
			this.offset = U.objectFieldOffset(field);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
//                    }
//                });
//                int modifiers = field.getModifiers();
//                ReflectUtil.ensureMemberAccess(caller, tclass, (Object)null, modifiers);
//                ClassLoader cl = tclass.getClassLoader();
//                ClassLoader ccl = caller.getClassLoader();
//                if (ccl != null && ccl != cl && (cl == null || !isAncestor(cl, ccl))) {
//                    ReflectUtil.checkPackageAccess(tclass);
//                }
//            } catch (PrivilegedActionException var8) {
//                throw new RuntimeException(var8.getException());
//            } catch (Exception var9) {
//                throw new RuntimeException(var9);
//            }

//            if (field.getType() != Long.TYPE) {
//                throw new IllegalArgumentException("Must be long type");
//            } else if (!Modifier.isVolatile(modifiers)) {
//                throw new IllegalArgumentException("Must be volatile type");
//            } else {

//            }
	}

//        private final void accessCheck(T obj) {
//            if (!this.cclass.isInstance(obj)) {
//                this.throwAccessCheckException(obj);
//            }
//
//        }

	//        private final void throwAccessCheckException(T obj) {
//            if (this.cclass == this.tclass) {
//                throw new ClassCastException();
//            } else {
//                throw new RuntimeException(new IllegalAccessException("Class " + this.cclass.getName() + " can not access a protected member of class " + this.tclass.getName() + " using an instance of " + obj.getClass().getName()));
//            }
//        }
//
	public final boolean compareAndSet(T obj, long expect, long update) {
//            this.accessCheck(obj);
		return U.compareAndSwapLong(obj, this.offset, expect, update);
	}

	//
	public final boolean weakCompareAndSet(T obj, long expect, long update) {
//            this.accessCheck(obj);
		return U.compareAndSwapLong(obj, this.offset, expect, update);
	}

	public final void set(T obj, long newValue) {
//            this.accessCheck(obj);
		U.putLongVolatile(obj, this.offset, newValue);
	}

	public final void lazySet(T obj, long newValue) {
//            this.accessCheck(obj);
		U.putOrderedLong(obj, this.offset, newValue);
	}

	public final long get(T obj) {
//            this.accessCheck(obj);
		return U.getLongVolatile(obj, this.offset);
	}

	public final long getAndSet(T obj, long newValue) {
//            this.accessCheck(obj);
		return U.getAndSetLong(obj, this.offset, newValue);
	}

	public final long getAndAdd(T obj, long delta) {
//            this.accessCheck(obj);
		return U.getAndAddLong(obj, this.offset, delta);
	}

	public final long getAndIncrement(T obj) {
		return this.getAndAdd(obj, 1L);
	}

	public final long getAndDecrement(T obj) {
		return this.getAndAdd(obj, -1L);
	}

	public final long incrementAndGet(T obj) {
		return this.getAndAdd(obj, 1L) + 1L;
	}

	public final long decrementAndGet(T obj) {
		return this.getAndAdd(obj, -1L) - 1L;
	}

	public final long addAndGet(T obj, long delta) {
		return this.getAndAdd(obj, delta) + delta;
	}
}

//    private static final class LockedUpdater<T> extends AtomicLongFieldUpdater<T> {
//        private static final Unsafe U = Unsafe.getUnsafe();
//        private final long offset;
//        private final Class<?> cclass;
//        private final Class<T> tclass;
//
//        LockedUpdater(final Class<T> tclass, final String fieldName, Class<?> caller) {
//            Field field;
//            int modifiers;
//            try {
//                field = (Field)AccessController.doPrivileged(new PrivilegedExceptionAction<Field>() {
//                    public Field run() throws NoSuchFieldException {
//                        return tclass.getDeclaredField(fieldName);
//                    }
//                });
//                modifiers = field.getModifiers();
//                ReflectUtil.ensureMemberAccess(caller, tclass, (Object)null, modifiers);
//                ClassLoader cl = tclass.getClassLoader();
//                ClassLoader ccl = caller.getClassLoader();
//                if (ccl != null && ccl != cl && (cl == null || !isAncestor(cl, ccl))) {
//                    ReflectUtil.checkPackageAccess(tclass);
//                }
//            } catch (PrivilegedActionException var8) {
//                throw new RuntimeException(var8.getException());
//            } catch (Exception var9) {
//                throw new RuntimeException(var9);
//            }
//
//            if (field.getType() != Long.TYPE) {
//                throw new IllegalArgumentException("Must be long type");
//            } else if (!Modifier.isVolatile(modifiers)) {
//                throw new IllegalArgumentException("Must be volatile type");
//            } else {
//                this.cclass = Modifier.isProtected(modifiers) && tclass.isAssignableFrom(caller) && !isSamePackage(tclass, caller) ? caller : tclass;
//                this.tclass = tclass;
//                this.offset = U.objectFieldOffset(field);
//            }
//        }
//
//        private final void accessCheck(T obj) {
//            if (!this.cclass.isInstance(obj)) {
//                throw this.accessCheckException(obj);
//            }
//        }
//
//        private final RuntimeException accessCheckException(T obj) {
//            return (RuntimeException)(this.cclass == this.tclass ? new ClassCastException() : new RuntimeException(new IllegalAccessException("Class " + this.cclass.getName() + " can not access a protected member of class " + this.tclass.getName() + " using an instance of " + obj.getClass().getName())));
//        }
//
//        public final boolean compareAndSet(T obj, long expect, long update) {
//            this.accessCheck(obj);
//            synchronized(this) {
//                long v = U.getLong(obj, this.offset);
//                if (v != expect) {
//                    return false;
//                } else {
//                    U.putLong(obj, this.offset, update);
//                    return true;
//                }
//            }
//        }
//
//        public final boolean weakCompareAndSet(T obj, long expect, long update) {
//            return this.compareAndSet(obj, expect, update);
//        }
//
//        public final void set(T obj, long newValue) {
//            this.accessCheck(obj);
//            synchronized(this) {
//                U.putLong(obj, this.offset, newValue);
//            }
//        }
//
//        public final void lazySet(T obj, long newValue) {
//            this.set(obj, newValue);
//        }
//
//        public final long get(T obj) {
//            this.accessCheck(obj);
//            synchronized(this) {
//                return U.getLong(obj, this.offset);
//            }
//        }
//    }
