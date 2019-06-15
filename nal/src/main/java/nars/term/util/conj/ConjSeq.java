package nars.term.util.conj;

import jcog.WTF;
import jcog.data.bit.MetalBitSet;
import jcog.util.ArrayUtil;
import nars.Task;
import nars.subterm.ArrayTermVector;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.atom.Interval;
import nars.term.compound.CachedCompound;
import nars.term.util.TermException;
import nars.term.util.builder.TermBuilder;
import nars.term.util.map.ByteAnonMap;
import nars.time.Tense;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;

import java.util.Arrays;
import java.util.TreeSet;
import java.util.function.IntPredicate;

import static nars.Op.CONJ;
import static nars.Op.NEG;
import static nars.term.atom.Bool.*;
import static nars.term.util.conj.ConjUnify.eventsCommon;
import static nars.time.Tense.*;

/** utilities for working with conjunction sequences (raw sequences, and factored sequences) */
public enum ConjSeq { ;


    public static ConjSequence the(ConjList list) {


        int n = list.size();
        assert(n > 2);

        list.trimToSize();
        list.sortThis();
        list.shift(0);


        TreeSet<Term> ordered = new TreeSet();
        for (Term x : list)
            ordered.add(x);

        ByteAnonMap m = new ByteAnonMap(n);
        for (Term x : ordered)
            m.intern(x);

        Term[] oa = ordered.toArray(new Term[ordered.size()]);

        byte[] subterm = new byte[n];
        int[] value = new int[n];
        for (int i= 0; i < n; i++) {
            subterm[i] = (byte) (m.interned(list.get(i))-1);
            value[i] = Tense.occToDT(list.when(i));
        }

        Interval times = Interval.the(subterm, value);

        return new ConjSequence(oa, times);
    }

    public static class ConjSequence extends CachedCompound.TemporalCachedCompound {


        @Deprecated private final Interval times;

        public ConjSequence(Subterms s) {
            super(CONJ, XTERNAL, s);
            this.times = (Interval) s.sub(s.subs()-1);
            if (times.keyCount()+1!=s.subs()-1)
                throw new TermException("interval subterm mismatch", this);
        }

        /** expects unique to be sorted in the final canonical unique ordering */
        private ConjSequence(Term[] unique, Interval times) {
            this(new ArrayTermVector(ArrayUtil.add(unique, times)) /* TODO use better facade/wrapper */);
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
            for (int i = 0; i < n; i++) {
                if (times.key(i, ss).equals(match))
                    if (!each.test(Tense.occToDT(times.value(i))))
                        return false;
            }
            return true;
        }

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


    /** TODO make method of B: TermBuilder.conjSequence(..) */
    public static Term sequence(Term a, long aStart, Term b, long bStart, TermBuilder B) {

        if (a==Null || b == Null)
            return Null;

        if (!a.op().eventable || !b.op().eventable)
            return Null;

        if (a == False || b == False) return False;

        if (a == True) return b;
        if (b == True) return a;


        /*assert(bStart == aStart);*/
        if (aStart == TIMELESS) {
            assert(bStart==TIMELESS);
            return B.conj(XTERNAL, a, b);
        }
        if (aStart==bStart) {
            //quick tests
            if (a.equals(b)) return a;
            else if (a.equalsNeg(b)) return False;
        }

        if (a.unneg().op() == CONJ || b.unneg().op()==CONJ) {
            ConjBuilder c = new ConjTree();
            if (c.add(aStart, a))
                c.add(bStart, b);
            return c.term(B);
        } else {
//            //simple construction

            return conjSeqFinal(Tense.occToDT(bStart-aStart), a, b, B);
//            if (aStart > bStart)
//            assert (aStart < bStart);
//            LongObjectArraySet<Term> ab = new LongObjectArraySet(2);
//            ab.add(0L, a);
//            ab.add(bStart - aStart, b);
//            return ConjSeq.conjSeq(B, ab);
        }
    }

    public static MetalBitSet seqEternalComponents(Subterms x) {
        return x.indicesOfBits(ConjBuilder.isEternalComponent);
    }

    static boolean _isSeq(Term x) {
        Subterms xx = x.subterms();
        return xx.hasAny(CONJ) && //inner conjunction
                xx.subs() == 2 &&
                xx.count(Conj::isSeq) == 1
                && (!xx.hasAny(NEG)
                        ||
                        /** TODO weird disjunctive seq cases */
                        xx.count(xxx -> xxx instanceof Neg && xxx.unneg().op() == CONJ) == 0)
                ;
    }

    public static boolean isFactoredSeq(Term x) {
        return x.dt() == DTERNAL && _isSeq(x);
    }

    /**
     * extracts the eternal components of a seq. assumes the conj actually has been determined to be a sequence
     */
    public static Term seqEternal(Term seq) {
        assert (seq.op() == CONJ && seq.dt() == DTERNAL);
        return seqEternal(seq.subterms());
    }

    private static Term seqEternal(Subterms ss) {
        return seqEternal(ss, ss.indicesOfBits(ConjBuilder.isEternalComponent));
    }

    public static Term seqEternal(Subterms ss, MetalBitSet m) {
        switch (m.cardinality()) {
            case 0:
                throw new WTF();
            case 1:
                return ss.sub(m.first(true));
            default:
                Term[] cc = ss.subsIncluding(m);
                Term e = CONJ.the(cc);
                if (e instanceof Bool)
                    throw new WTF("&&(" + Arrays.toString(cc) + ") => " + e);
                return e;
        }
    }

    public static Compound seqTemporal(Term seq) {
        assert (seq.op() == CONJ && seq.dt() == DTERNAL);
        return seqTemporal(seq.subterms());
    }

    private static Compound seqTemporal(Subterms s) {
        Compound t = (Compound) s.subFirst(ConjBuilder.isTemporalComponent);
        assert (Conj.isSeq(t));
        return t;
    }

    public static Term seqTemporal(Subterms s, MetalBitSet eternalComponents) {
        return s.sub(eternalComponents.next(false, 0, s.subs()));
    }


    /**
     * constructs a correctly merged conjunction from a list of events, in the sublist specified by from..to (inclusive)
     * assumes that all of the event terms have distinct occurrence times
     */
    static Term conjSeq(TermBuilder B, ConjList events, int start, int end) {

//        if (end-start > 2 && start == 0 && end == events.size() && events.eventOccurrences()>2) { //HACK
//            return events.term(B);
//        }

        Term first = events.get(start);
        int ee = end - start;

        int dt;
        Term left, right;
        switch (ee) {
            case 0:
                throw new NullPointerException("should not be called with empty events list");
            case 1:
                return first;
            case 2: {
                left = first;
                right = events.get(end - 1);
                long firstWhen = events.when(start);
                dt = Tense.occToDT(events.when(end - 1) - firstWhen);
                break;
            }
            default: {
                int center = events.centerByIndex(start, end);
                //int center = events.centerByVolume(start, end);
                left = conjSeq(B, events, start, center + 1);
                if (left == Null) return Null;
                right = conjSeq(B, events, center + 1, end);
                long firstWhen = events.when(start);
                dt = Tense.occToDT((events.when(center + 1) - firstWhen - left.eventRange()));
                break;
            }

        }



        return conjSeqFinal(dt, left, right, B);
    }

    private static Term conjSeqFinal(int dt, Term left, Term right, TermBuilder B) {
        if (dt == 0 || dt == DTERNAL) {
            return B.conj(DTERNAL, left, right);
        }
//        assert (dt != XTERNAL);

        if (left == Null) return Null;
        if (left == False) return False;


        if (right == Null) return Null;
        if (right == False) return False;

        if (left == True)
            return right;
        else if (right == True)
            return left;

        if (!left.op().eventable || !right.op().eventable)
            return Null;

        if ((left.op() == CONJ && right.op() == CONJ) && !left.equals(right)) {
            if (!Conj.isSeq(left) && !Conj.isSeq(right)) {
                if (eventsCommon(left, right)) {
                    //attempt reconsolidation if possible because factorization can be necessary
                    ConjTree c = new ConjTree();
                    c.addConjEvent(0, left);
                    c.addConjEvent(dt, right);
                    try {
                        return c.term(B);
                    } catch (StackOverflowError ee) {
                        throw new TermException("conj seq stack overflow", CONJ, dt, left, right);
                    }
                }
            }
        }
        int lr = left.compareTo(right);

        if (lr!=0) {
            //not equal
            if (dt == 0 || dt == DTERNAL) {
                if (left.equalsNeg(right))
                    return False; //contradiction
            }
        }

        if (lr > 0) {
            if (dt != DTERNAL)
                dt = -dt;
            Term t = right;
            right = left;
            left = t;
        } else if (lr == 0) {
            //equal
            if (dt == 0 || dt == DTERNAL) {
                //parallel duplicate
                return left;
            } else {
                //sequence of repeating terms
                right = left; //share identity
                if (dt < 0)
                    dt = Math.abs(dt); //use positive dt only
            }
        }



        if (dt == 0)
            dt = DTERNAL; //HACK

        return B.newCompound(CONJ, dt, left, right);

    }

    /** TODO add support for supersampling to include task.end() features */
    public static Term sequence(Task[] events, int ditherDT) {
        int eventsSize = events.length;
        switch (eventsSize) {
            case 0:
                return True;
            case 1:
                return sequenceTerm(events[0]);
        }

        ConjBuilder ce = new ConjTree();
        for (Task o : events) {
            if (!ce.add(Tense.dither(o.start(), ditherDT), sequenceTerm(o))) {
                break;
            }
        }

        return ce.term();
    }
    private static Term sequenceTerm(Task o) {
        return o.term().negIf(o.isNegative());
    }

//    public static boolean contains(Term container, Term x, boolean firstOrLast) {
//        final long[] last = {-1}, found = {-1};
//
//        int xdt = x.dt();
//        container.eventsWhile((when, subEvent) -> {
//
//            if (subEvent == container) return true; //HACK
//
//            if (Conj.containsOrEqualsEvent(subEvent, x)) { //recurse
//
//                if (!firstOrLast || when == 0) {
//                    found[0] = when; //a later event was found
//
//                    if (firstOrLast) {
//                        assert (when == 0);
//                        return false; //done
//                    }
//                }
//
//            }
//            last[0] = when;
//            return true; //continue looking for last event
//        }, 0, xdt!=0, xdt!=DTERNAL, false);
//
//        return firstOrLast ? found[0] == 0 : found[0] == last[0];
//
//    }
}
