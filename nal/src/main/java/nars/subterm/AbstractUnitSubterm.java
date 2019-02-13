package nars.subterm;

import com.google.common.io.ByteArrayDataOutput;
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
    public final void appendTo(ByteArrayDataOutput out) {
        out.writeByte(1);
        sub().appendTo(out);
    }

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
    public boolean ORith(ObjectIntPredicate<Term> p) {
        return p.accept(sub(), 0);
    }
    @Override
    public boolean ANDith(ObjectIntPredicate<Term> p) {
        return p.accept(sub(), 0);
    }

    @Override
    public <X> boolean ORwith(BiPredicate<Term, X> p, X param) {
        return p.test(sub(), param);
    }
    @Override
    public <X> boolean ANDwith(BiPredicate<Term, X> p, X param) {
        return p.test(sub(), param);
    }


    @Override
    public final void forEachWith(ObjectIntProcedure<Term> t) {
        t.accept(sub(), 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Subterms)) return false;
        Subterms s = (Subterms)obj;
        return
                (s.subs() == 1) &&
                (sub().equals(s.sub(0)));
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
    public int hashCode() {
        return Util.hashCombine1(sub());
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


    public final Term[] subsExcluding(int index) {
        assert(index ==0);
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
            return s instanceof NormalizedVariable && ((NormalizedVariable)s).id==1;
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
    public void forEach(Consumer<? super Term> c, int start, int stop) {
        if (start!=0 ||
                stop!=1)
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

}
