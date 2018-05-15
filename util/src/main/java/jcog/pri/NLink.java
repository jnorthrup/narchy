package jcog.pri;

import static jcog.Texts.n4;

/** immutable object + mutable number pair;
 * considered in a 'deleted' state when the value is NaN */
public class NLink<X> extends Pri implements PriReference<X> {

    public final X id;

    public NLink(X x, float v) {
        super(v);
        this.id = x;
    }
    @Override
    public boolean equals(Object that) {
        return (this == that) || id.equals(((NLink) that).get());
    }
    @Override
    public boolean isDeleted() {
        float p = pri;
        return p!=p; //fast NaN check
    }
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    final public X get() {
        return id;
    }

//    @Override
//    public boolean equals(/*@NotNull*/ Object that) {
//        if (this == that) return true;
//
////        return //hashCode()==that.hashCode() &&
////                Objects.equals(get(),
////                    (that instanceof Supplier) ? ((Supplier)that).get() : that);
//
//        Object y = ((NLink)that).get();
//        if (y == null) return false;
//        Object x = get();
//        if (x == null) return false;
//        return x.equals(y);
//
////        return
////            (x!=null)
////                &&
////            (
////                x.equals(that)
////                    ||
////                ((that instanceof Supplier) && x.equals(((Supplier) that).get()))
////            );
//    }


    @Override
    public String toString() {
        return n4(pri()) + " " + get();
    }


}
