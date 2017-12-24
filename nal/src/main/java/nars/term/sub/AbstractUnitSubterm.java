package nars.term.sub;

import jcog.Util;
import nars.term.Term;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

abstract public class AbstractUnitSubterm implements Subterms {

    abstract public Term sub();

    @Override
    public String toString() {
        return "(" + sub() + ')';
    }

    @Override
    public Term[] arrayClone() {
        return new Term[] { sub() };
    }

    @Override
    public boolean OR(Predicate<Term> p) {
        return p.test(sub());
    }
    @Override
    public boolean AND(Predicate<Term> p) {
        return p.test(sub());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Subterms)) return false;
        Subterms s = (Subterms)obj;
        return (hashCodeSubterms() == s.hashCodeSubterms()) &&
                (s.subs() == 1) &&
                (sub().equals(s.sub(0)));
    }

    @Override
    public int hashCode() {
        return Util.hashCombine1(sub().hashCode());
    }

    @Override
    public int volume() {
        return 1+sub().volume();
    }

    @Override
    public int complexity() {
        return 1+sub().complexity();
    }

    @Override
    public int structure() {
        return sub().structure();
    }

    @Override
    public final Term sub(int i) {
        if (i!=0) throw new ArrayIndexOutOfBoundsException();
        return sub();
    }


    @Override
    public final int subs() {
        return 1;
    }

    @Override
    public final void forEach(Consumer<? super Term> c) {
        c.accept(sub());
    }

    @Override
    public void forEach(Consumer<? super Term> c, int start, int stop) {
        if (start!=0 && stop!=1)
            throw new ArrayIndexOutOfBoundsException();
        c.accept(sub());
    }

    @Override
    public Stream<Term> subStream() {
        return Stream.of(sub());
    }

    @Override
    public Iterator<Term> iterator() {
        return com.google.common.collect.Iterators.singletonIterator(sub());
    }

    @Override
    public int intify(IntObjectToIntFunction<Term> reduce, int v) {
        return sub().intify(reduce, v);
    }

    @Override
    public int intifyShallow(IntObjectToIntFunction<Term> reduce, int v) {
        return reduce.intValueOf(v, sub());
    }

}
