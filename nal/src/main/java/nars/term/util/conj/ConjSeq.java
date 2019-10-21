package nars.term.util.conj;

import jcog.WTF;
import jcog.data.bit.MetalBitSet;
import nars.Op;
import nars.Task;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.term.atom.IdempotentBool;
import nars.term.atom.Interval;
import nars.term.compound.Sequence;
import nars.term.util.builder.TermBuilder;
import nars.term.util.map.ByteAnonMap;
import nars.time.Tense;

import java.util.Arrays;
import java.util.TreeSet;
import java.util.function.Predicate;

import static nars.Op.CONJ;
import static nars.Op.NEG;
import static nars.term.atom.IdempotentBool.*;
import static nars.time.Tense.*;

/** utilities for working with conjunction sequences (raw sequences, and factored sequences) */
public enum ConjSeq { ;


    public static Sequence sequenceFlat(ConjList list) {


        int n = list.size();
        assert(n > 2);

        list.trimToSize();
        list.sortThis();
        list.shift(0L);


        TreeSet<Term> ordered = new TreeSet(list);

        ByteAnonMap m = new ByteAnonMap(n);
        for (Term x : ordered)
            m.intern(x);

        Term[] oa = ordered.toArray(Op.EmptyTermArray);

        byte[] subterm = new byte[n];
        int[] value = new int[n];
        for (int i = 0; i < n; i++) {
            subterm[i] = (byte) ((int) m.interned(list.get(i)) -1);
            value[i] = Tense.occToDT(list.when(i));
        }

        Interval times = Interval.the(subterm, value);

        return new Sequence(oa, times);
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

            return sequenceLeafPair(Tense.occToDT(bStart-aStart), a, b, B);
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
        return xx.subs() == 2 &&
                xx.hasAny(CONJ) && //inner conjunction
                xx.countEquals(Conj::isSeq, 1)
                && (!xx.hasAny(NEG)
                        ||
                        /** TODO weird disjunctive seq cases */
                        xx.countEquals(new Predicate<Term>() {
                            @Override
                            public boolean test(Term xxx) {
                                return xxx instanceof Neg && xxx.unneg().opID() == (int) CONJ.id;
                            }
                        }, 0))
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
                if (e instanceof IdempotentBool)
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
    static Term sequenceBalancedTree(TermBuilder B, ConjList events, int start, int end) {

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
                int center = ConjList.centerByIndex(start, end);
                //int center = events.centerByVolume(start, end);
                left = sequenceBalancedTree(B, events, start, center + 1);
                if (left == Null) return Null;
                if (left == False)
                    return False;
                right = sequenceBalancedTree(B, events, center + 1, end);
                long firstWhen = events.when(start);
                dt = Tense.occToDT((events.when(center + 1) - firstWhen - (long) left.eventRange()));
                break;
            }

        }



        return sequenceLeafPair(dt, left, right, B);
    }


    /** TODO add support for supersampling to include task.end() features */
    public static Term sequence(Task[] events, int ditherDT, TermBuilder B) {
        int eventsSize = events.length;
        switch (eventsSize) {
            case 0:
                return True;
            case 1:
                return sequenceTerm(events[0]);
            case 2: {
                //optimized 2-ary case
                Task a = events[0];
                if (a.op() != CONJ) {
                    Task b = events[1];
                    if (b.op() != CONJ) {
                        long as = a.start(), bs = b.start();
                        assert (bs != ETERNAL && as != ETERNAL);
                        return B.conjAppend(sequenceTerm(a), Tense.occToDT(Tense.dither(bs - as, ditherDT)), sequenceTerm(b));
                    }
                }
                break;
            }
        }

        ConjBuilder ce = new ConjTree();
        for (Task o : events)
            if (!ce.add(Tense.dither(o.start(), ditherDT), sequenceTerm(o))) {
                break;
        }

        return ce.term(B);
    }
    private static Term sequenceTerm(Task o) {
        return o.term().negIf(o.isNegative());
    }


    private static Term sequenceLeafPair(int dt, Term left, Term right, TermBuilder B) {
//        if (dt == 0 || dt == DTERNAL) {
//            return B.conj(DTERNAL, left, right);
//        }
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


        int lr = left.compareTo(right);
        if (lr > 0) {
            dt = -dt;
            Term t = right;
            right = left;
            left = t;
        } else if (lr == 0) {
            //equal
            //sequence of repeating terms
            right = left; //share identity
            if (dt < 0) {
                dt = -dt;

            }
        }
//        //HACK
//        if ((left.unneg().op() == CONJ || right.unneg().op() == CONJ) && !left.equals(right)) {
//               /* if (eventsCommon(left.unneg(), right.unneg())) */{
//
////                    try {
//                        //attempt reconsolidation if possible because refactorization can be necessary
//                        ConjList c =
//                            new ConjList();
//
//                        c.addConjEvent(0, left);
//                        c.addConjEvent(dt + left.eventRange(), right);
//
//                        ConjTree t = new ConjTree();
//                        c.factor(t);
//                        if (!t.isEmpty()) {
//                            //something factored. i knew it
//                            //add again.. this sux
//                            t.clear();
//                            if (t.addConjEvent(0, left))
//                                t.addConjEvent(dt + left.eventRange(), right);
//                            return t.term(B);
//                        }
////                    } catch (StackOverflowError ee) {
////                        throw new TermException("conj seq stack overflow", CONJ, dt, left, right);
////                    }
//                }
//        }

        return B.newCompound(CONJ, dt, left, right);
//        return B.conj(dt, left, right);

    }

}
