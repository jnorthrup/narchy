package nars.subterm;

import jcog.Util;
import nars.Op;
import nars.term.Term;
import nars.term.Variable;
import nars.term.var.NormalizedVariable;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.api.block.predicate.primitive.ObjectIntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ObjectIntProcedure;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

abstract class AbstractUnitSubterm implements Subterms {

    protected abstract Term sub();

    @Override
    public final String toString() {
        return Subterms.toString(sub());
    }

    @Override
    public final Term[] arrayClone() {
        return new Term[]{sub()};
    }

    @Override
    public final boolean OR(Predicate<Term> p) {
        return AND(p);
    }

    @Override
    public final int volume() {
        return 1 + sub().volume();
    }

    @Override
    public final int complexity() {
        return 1 + sub().complexity();
    }

    @Override
    public final float voluplexity() {
        return 1 + sub().voluplexity();
    }

    @Override
    public final boolean AND(Predicate<Term> p) {
        return p.test(sub());
    }

    @Override
    public final boolean ORith(ObjectIntPredicate<Term> p) {
        return ANDith(p);
    }

    @Override
    public final boolean ANDith(ObjectIntPredicate<Term> p) {
        return p.accept(sub(), 0);
    }

    @Override
    public final <X> boolean ORwith(BiPredicate<Term, X> p, X param) {
        return ANDwith(p, param);
    }

    @Override
    public final <X> boolean ANDwith(BiPredicate<Term, X> p, X param) {
        return p.test(sub(), param);
    }

    @Override
    public final <X> boolean ANDwithOrdered(BiPredicate<Term, X> p, X param) {
        return ANDwith(p, param);
    }

    @Override
    public @Nullable Term subSub(int start, int end, byte[] path) {
        byte a = path[start];
        if (a != 0)
            return null;
        else {
            Term s = sub();
            return end - start == 1 ?
                    s :
                    s.subPath(start + 1, end);
        }
    }

    @Override
    public final void forEachI(ObjectIntProcedure<Term> t) {
        t.value(sub(), 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Subterms)) return false;
        Subterms s = (Subterms) obj;
        return
                (s.subs() == 1) &&
                        (sub().equals(s.sub(0)));
    }


    @Override
    public int hashCode() {
        return Util.hashCombine1(sub());
    }

    @Override
    public int hashCodeSubterms() {
        return Subterms.hash(sub());
    }

    @Override
    public int structure() {
        return sub().structure();
    }

    @Override
    public final Term sub(int i) {
        if (i != 0) throw new ArrayIndexOutOfBoundsException();
        return sub();
    }


    public final Term[] removing(int index) {
        assert (index == 0);
        return Op.EmptyTermArray;
    }

    @Override
    public final int subs() {
        return 1;
    }

    @Override
    public boolean isNormalized() {
        Term s = sub();
        if (s instanceof Variable) {
            return s instanceof NormalizedVariable && ((NormalizedVariable) s).id() == 1;
        }
        return s.isNormalized();
    }

    @Override
    public void setNormalized() {
        sub().subterms().setNormalized();
    }

    @Override
    public final void forEach(Consumer<? super Term> c) {
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
    public int sum(ToIntFunction<Term> value) {
        return value.applyAsInt(sub());
    }

    @Override
    public final int intifyRecurse(IntObjectToIntFunction<Term> reduce, int v) {
        return sub().intifyRecurse(reduce, v);
    }

    @Override
    public final int intifyShallow(IntObjectToIntFunction<Term> reduce, int v) {
        return reduce.intValueOf(v, sub());
    }

//    @Override
//    public @Nullable Term subSub(int start, int end, byte[] path) {
//        byte a = path[start];
//        if (a != 0)
//            return null;
//        else {
//            Term s = sub();
//            return end - start == 1 ?
//                    s :
//                    s.subPath(start + 1, end);
//        }
//    }

}
