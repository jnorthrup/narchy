package jcog.pri;

import jcog.Util;

import java.lang.ref.SoftReference;
import java.util.function.Supplier;

import static jcog.Texts.*;

/** prioritized proxy pair, can be used to represent cached memoizable operation with input X and output Y */
public interface PriProxy<X, Y> extends UnitPrioritizable, Supplier<Y> {
    /**
     * 'x', the parameter to the function
     */
    X x();

    default boolean xEquals(Object y, int kHash) {
        return x().equals(y);
    }

    /** equaity/hash on X, supplies Y */
    final class StrongProxy<X, Y> extends PLink<Y> implements PriProxy<X, Y> {

        public final X x;
        private final int hash;

        public StrongProxy(X x, Y y, float pri) {
            super(y, pri);
            this.x = x;
            this.hash = x.hashCode();
        }

        @Override
        public boolean xEquals(Object y, int kHash) {
            return hash == kHash && x.equals(y);
        }

        @Override
        public boolean equals(Object obj) {
            StrongProxy h = (StrongProxy) obj;
            return xEquals(h.x, h.hash);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public X x() {
            return x;
        }
    }

    /** TODO needs tested for correct behavior on reclamation.  equailty/hash on X, supplies Y
     *  TODO needs ScalarValue.AtomicScalarValue support.  this doesnt have it by extending SoftReference already cant extend Pri like the Strong impl
     * */
    final class SoftProxy<X, Y> extends SoftReference<Y> implements PriProxy<X, Y> {

        public final X x;
        private final int hash;
        private volatile float pri;

        public SoftProxy(X x, Y y, float pri) {
            super(y);
            this.x = x;
            this.hash = x.hashCode();
            this.pri = pri;
        }

        @Override
        public boolean xEquals(Object y, int kHash) {
            return hash == kHash && x.equals(y);
        }

        @Override
        public final X x() {
            return x;
        }

        @Override
        public boolean equals(Object obj) {
            SoftProxy h = (SoftProxy) obj;
            return xEquals(h.x, h.hash);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public ScalarValue pri(float p) {
            if (p == p) {
                p = Util.unitize(p);
            }
            this.pri = p;
            return this;
        }

        @Override
        public String toString() {
            return '$' + INSTANCE.n4(pri) + ' ' + get();
        }

        @Override
        public boolean delete() {
            if (PriProxy.super.delete()) {
                this.pri = Float.NaN;
                clear();
                return true;
            }
            return false;
        }

        @Override
        public Y get() {
            Y y = super.get();
            if (y == null) {
                this.pri = Float.NaN;
                return null;
            }
            return y;
        }


        @Override
        public final float pri() {
            return pri;
        }

    }
}
