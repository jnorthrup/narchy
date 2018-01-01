package jcog.pri;

import java.util.Objects;
import java.util.function.Supplier;

import static jcog.Texts.n4;

public abstract class AbstractPLink<X> extends Pri implements PriReference<X> {

    protected AbstractPLink(float p) {
        super(p);
    }


    @Override
    public boolean equals(/*@NotNull*/ Object that) {
        if (this == that) return true;

        return hashCode()==that.hashCode() &&
                Objects.equals(get(),
                    (that instanceof Supplier) ? ((Supplier)that).get() : that);
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
