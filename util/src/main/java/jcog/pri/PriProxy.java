package jcog.pri;

import jcog.Util;

import java.lang.ref.SoftReference;
import java.util.function.Supplier;

import static jcog.Texts.n4;

/** prioritized proxy pair, can be used to represent cached memoizable operation with input X and output Y */
public interface PriProxy<X, Y> extends UnitPrioritizable, Supplier<Y> {
    /**
     * 'x', the parameter to the function
     */
    X x();

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
        public boolean equals(Object obj) {
            StrongProxy h = (StrongProxy) obj;
            return hash == h.hash && x.equals(h.x);
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
        public final X x() {
            return x;
        }

        @Override
        public boolean equals(Object obj) {
            SoftProxy h = (SoftProxy) obj;
            return hash == h.hash && x.equals(h.x);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public float pri(float p) {
            if (p == p) {
                p = Util.unitize(p);
            }
            return this.pri = p;
        }

        @Override
        public String toString() {
            return '$' + n4(pri) + ' ' + get();
        }

        @Override
        public boolean delete() {
            if (PriProxy.super.delete()) {
                clear();
                this.pri = Float.NaN;
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
