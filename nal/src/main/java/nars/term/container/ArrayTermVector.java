package nars.term.container;

import jcog.list.ArrayIterator;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.Op.Null;

/**
 * Holds a vector or tuple of terms.
 * Useful for storing a fixed number of subterms

 */
public class ArrayTermVector extends TermVector {

    /*@NotNull*/
    private final Term[] terms;

//    public ArrayTermVector(/*@NotNull */Collection<Term> terms) {
//        this(terms.toArray(new Term[terms.size()]));
//    }

    public ArrayTermVector(/*@NotNull */Term... terms) {
         super(terms);
         this.terms = terms;
    }

    @Override
    public final boolean equals(/*@NotNull*/ Object obj) {
        if (this == obj) return true;

        if (obj instanceof ArrayTermVector) {
            //special case for ArrayTermVector fast compare and sharing
            ArrayTermVector that = (ArrayTermVector) obj;
            Term[] y = that.terms;
            final Term[] x = this.terms;
            if (x == y)
                return true;

            if (hash != that.hash)
                return false;

            int s = x.length;
            if (s != y.length)
                return false;

            //boolean srcXorY = System.identityHashCode(x) < System.identityHashCode(y);
            for (int i = 0; i < s; i++) {
                Term xx = x[i];
                Term yy = y[i];
                if (xx == yy) {
                    continue;
                } else if (!xx.equals(yy)) {
                    return false;
                } else {
//                    //share since subterm is equal
//                    if (srcXorY)
//                        y[i] = xx;
//                    else
//                        x[i] = yy;

                }
            }

            equivalentTo(that);


            return true;

        } else if (obj instanceof TermContainer) {

            TermContainer that = (TermContainer) obj;
            if (hash != that.hashCodeSubTerms())
                return false;

            final Term[] x = this.terms;
            int s = x.length;
            if (s != that.subs())
                return false;
            for (int i = 0; i < s; i++)
                if (!x[i].equals(that.sub(i)))
                    return false;

            if (that instanceof TermVector)
                equivalentTo((TermVector) that);

            return true;
        }
        return false;
    }


    @Override
    /*@NotNull*/ public final Term sub(int i) {
        return terms[i];
        //return terms.length > i ? terms[i] : Null;
    }

    @Override public final Term[] arrayClone() {
        return terms.clone();
    }

    @Override
    public final Term[] arrayShared() {
        return terms;
    }

    @Override
    public final int subs() {
        return terms.length;
    }

    @Override
    public final Iterator<Term> iterator() {
        return new ArrayIterator<>(terms);
    }

    @Override
    public final void forEach(@NotNull Consumer<? super Term> action, int start, int stop) {
        Term[] t = this.terms;
        for (int i = start; i < stop; i++) {
            action.accept(t[i]);
        }
    }

    @Override
    public final void forEach(Consumer<? super Term> action) {
        Term[] t = this.terms;
        for (Term x : t)
            action.accept(x);
    }

    @Override
    public final boolean OR(Predicate<Term> p) {
        Term[] t = this.terms;
        for (Term i : t)
            if (p.test(i))
                return true;
        return false;
    }

    @Override public final boolean AND(Predicate<Term> p) {
        Term[] t = this.terms;
        for (Term i : t)
            if (!p.test(i))
                return false;
        return true;
    }

}
