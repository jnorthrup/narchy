package nars.term.compound;

import jcog.data.bit.MetalBitSet;
import jcog.util.ArrayUtil;
import nars.Op;
import nars.subterm.ArrayTermVector;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Interval;
import nars.term.util.TermException;
import nars.term.util.builder.TermBuilder;
import nars.time.Tense;
import nars.unify.Unify;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

import static nars.Op.CONJ;
import static nars.time.Tense.*;

/** conjunction sequence implementation with internal Interval instance and Event accessor for it */
public class Sequence extends CachedCompound.TemporalCachedCompound {

    private final Interval times;

    public Sequence(Subterms s) {
        super(CONJ, DTERNAL, s);
        this.times = (Interval) s.sub(s.subs()-1);
        if (times.keyCount()+1!=s.subs()-1)
            throw new TermException("interval subterm mismatch", this);
    }

    /** expects unique to be sorted in the final canonical unique ordering */
    public Sequence(Term[] unique, Interval times) {
        this(new ArrayTermVector(ArrayUtil.add(unique, times)) /* TODO use better facade/wrapper */ {


            @Override
            public @Nullable Subterms transformSubs(UnaryOperator<Term> f, Op superOp) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Subterms transformSub(int which, UnaryOperator<Term> f) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Term[] removing(MetalBitSet toRemove) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Term[] removing(int index) {
                throw new UnsupportedOperationException();
            }
        });
    }


    @Override
    public Term dt(int nextDT, TermBuilder b) {
        if (nextDT == XTERNAL)
            return CONJ.the(b, XTERNAL, events());
        else
            throw new UnsupportedOperationException();
    }

    @Override
    public boolean unify(Term y, Unify u) {
        if (this == y) return true;

        if (!(y instanceof Sequence))
            return false; //TODO

        if (this.equals(y)) return true;

        Subterms a = subterms(), b = y.subterms();
        int s = a.subs();
        if (s !=b.subs())
            return false;
        Interval st = ((Sequence)y).times;
        if (!st.equals(times))
            return false; //TODO
        return Subterms.unifyCommute(events(), ((Sequence)y).events(), u);
    }

    public Subterms events() {
        Subterms ss = subterms();
        return new TermList(ss, 0, ss.subs()-1);
    }

    @Override
    public Term eventFirst() {
        return times.key(0, subterms());
    }
    @Override
    public Term eventLast() {
        return times.key(times.size()-1, subterms());
    }

    @Override
    public boolean subTimesWhile(Term match, IntPredicate each) {
        int n = times.size();
        Subterms ss = subterms();
        return IntStream.range(0, n).filter(i -> times.key(i, ss).equals(match)).allMatch(i -> each.test(occToDT(times.value(i))));
    }

//    /** transform each sub component-wise so that a remapping can be determined before constructing new ConjSeq*/
//     @Override public Term transform(RecursiveTermTransform f, Op newOp, int ydt) {
//
//        if (newOp!=null && (newOp!=CONJ || ydt!=dt()))
//            throw new UnsupportedOperationException();
//
//        Subterms s = subterms();
//        int ss = s.subs();
//        int ee = ss - 1;
//        Term[] r = new Term[ee];
//        boolean modified = false;
//        //TODO modifiedTemporally; //to elide complete resequencing, detect when an inner term becomes a sequence or changes in some way that would change sequence's temporality
//        for (int i = 0; i < ee; i++) {
//            Term x = s.sub(i);
//            Term y = f.apply(x);
//            if (x!=y)
//                modified = true;
//            if (y == Null)
//                return Null;
//            if (y == False)
//                return False;
//            r[i] = y;
//        }
//        if (!modified)
//            return this;
//
//
//        ConjList l = new ConjList(ee);
//        int t = times.size();
//        for (byte i = 0; i < t; i++)
//            l.add((long)this.times.value(i), this.times.key(i, r));
//
//        return l.term();
//    }

    @Override
    public int eventRange() {
        return Tense.occToDT(times.valueLast()-times.valueFirst());
    }

    @Override
    public boolean hasXternal() {
        return false; //TODO test that subterms do not
    }

    @Override
    public boolean eventsAND(LongObjectPredicate<Term> each, long offset, boolean decomposeConjDTernal, boolean decomposeXternal) {
        int n = times.size();
        Subterms ss = subterms();
        for (int i = 0; i < n; i++) {
            long o = offset != ETERNAL ? times.value(i) + offset : ETERNAL;
            Term x = times.key(i, ss);
            if (x instanceof Compound && (decomposeConjDTernal || decomposeXternal) && x.op()==CONJ) {
                int xdt = x.dt();
                if ((decomposeConjDTernal && xdt ==DTERNAL) || (decomposeXternal && xdt == XTERNAL)) {
                    for (Term xx : x.subterms()) {
                        if (!each.accept(o, xx))
                            return false;
                    }
                    continue;
                }

            }

            if (!each.accept(o, x))
                return false;
        }
        return true;
    }


}
