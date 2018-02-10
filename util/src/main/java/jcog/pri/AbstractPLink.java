package jcog.pri;

import static jcog.Texts.n4;

public abstract class AbstractPLink<X> extends Pri implements PriReference<X> {

    protected AbstractPLink() {
        super();
    }

    protected AbstractPLink(float p) {
        super(p);
    }


    @Override
    public boolean equals(/*@NotNull*/ Object that) {
        if (this == that) return true;

//        return //hashCode()==that.hashCode() &&
//                Objects.equals(get(),
//                    (that instanceof Supplier) ? ((Supplier)that).get() : that);

        Object y = ((AbstractPLink)that).get();
        if (y == null) return false;
        Object x = get();
        if (x == null) return false;
        return x.equals(y);

//        return
//            (x!=null)
//                &&
//            (
//                x.equals(that)
//                    ||
//                ((that instanceof Supplier) && x.equals(((Supplier) that).get()))
//            );
    }


    @Override
    public abstract int hashCode();

    @Override
    abstract public X get();

    @Override
    public String toString() {
        return "$" + n4(pri()) + " " + get();
    }

}
