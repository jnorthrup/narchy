package nars.term.util.conj;

import jcog.WTF;
import jcog.data.bit.MetalBitSet;
import jcog.data.set.LongObjectArraySet;
import nars.Task;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.util.TermException;
import nars.term.util.builder.TermBuilder;
import nars.time.Tense;

import java.util.Arrays;
import java.util.TreeSet;

import static nars.Op.CONJ;
import static nars.Op.NEG;
import static nars.term.atom.Bool.*;
import static nars.time.Tense.*;

/** utilities for working with conjunction sequences (raw sequences, and factored sequences) */
public enum ConjSeq { ;

    /** TODO TermBuilder */
    public static Term sequence(TermBuilder B, int dt, Term[] u) {
        if (u.length != 2)
            throw new TermException("temporal conjunction with n!=2 subterms", CONJ, dt, u);

        return (dt >= 0) ?
                sequence(u[0], 0, u[1], +dt + u[0].eventRange(), B) :
                sequence(u[1], 0, u[0], -dt + u[1].eventRange(), B);
    }

    static public Term sequence(Term a, long aStart, Term b, long bStart, TermBuilder B) {

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

//        if (a.hasAny(Op.CONJ) || b.hasAny(Op.CONJ)) {
            ConjBuilder c = new ConjTree();
            if (c.add(aStart, a))
                c.add(bStart, b);
            return c.term(B);
//        } else {
//            if (aStart > bStart)
//            assert (aStart < bStart);
//            //simple construction
//            LongObjectArraySet<Term> ab = new LongObjectArraySet(2);
//            ab.add(0L, a);
//            ab.add(bStart - aStart, b);
//            return ConjSeq.conjSeq(B, ab);
//        }
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
                        xx.count(xxx -> xxx.op() == NEG && xxx.unneg().op() == CONJ) == 0)
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

    public static Term conjSeq(TermBuilder B, LongObjectArraySet<Term> events) {
        events.sortThis();
        return conjSeq(B, events, 0, events.size());
    }

    /**
     * constructs a correctly merged conjunction from a list of events, in the sublist specified by from..to (inclusive)
     * assumes that all of the event terms have distinct occurrence times
     */
    private static Term conjSeq(TermBuilder B, LongObjectArraySet<Term> events, int start, int end) {

        Term first = events.get(start);
        long firstWhen = events.when(start);
        int ee = end - start;
        switch (ee) {
            case 0:
                throw new NullPointerException("should not be called with empty events list");
            case 1:
                return first;
            case 2: {
                Term second = events.get(end - 1);
                long secondWhen = events.when(end - 1);
                return conjSeqFinal(B,
                        Tense.occToDT(secondWhen - firstWhen),
                        /* left */ first, /* right */ second);
            }
        }

        int center = start + (end - 1 - start) / 2;


        Term left = conjSeq(B, events, start, center + 1);
        if (left == Null) return Null;
        if (left == False) return False;

        Term right = conjSeq(B, events, center + 1, end);
        if (right == Null) return Null;
        if (right == False) return False;

        int dt = Tense.occToDT((events.when(center + 1) - firstWhen - left.eventRange()));

        return dtSpecial(dt) ?
                Conj.conjoin(B, left, right, dt == DTERNAL) :
                conjSeqFinal(B, dt, left, right);
    }

    static Term conjSeqFinal(TermBuilder b, int dt, Term left, Term right) {
        assert (dt != XTERNAL);

        if (left == Null) return Null;
        if (right == Null) return Null;

        if (left == False) return False;
        if (right == False) return False;

        if (left == True) return right;
        if (right == True) return left;

        if (!left.op().eventable || !right.op().eventable)
            return Null;

        if (dt == 0 || dt == DTERNAL) {
            if (left.equals(right))
                return left;
            else if (left.equalsNeg(right))
                return False;
        } else {
            if (left!=right && left.equals(right))
                right = left;
            //TODO equalsNeg identity?
        }

        if (left.compareTo(right) > 0) {
            if (dt != DTERNAL)
                dt = -dt;
            Term t = right;
            right = left;
            left = t;
        }

        if (left.op()==CONJ && right.op()==CONJ && dtSpecial(left.dt()) && left.dt()==right.dt() && !left.equals(right)) {
            //factorize any comment subevents
            Subterms ll = left.subterms();
            Subterms rr = right.subterms();
            if (Term.commonStructure(ll, rr)) {
                java.util.Set<Term> common = null;
                java.util.Set<Term> l = ll.toSetSorted(), r = new TreeSet();
                for (Term x : rr) {
                    if (l.remove(x)) {
                        if (common == null) common = new TreeSet();
                        common.add(x);
                    } else
                        r.add(x);
                }
                if (common != null && !r.isEmpty() /*&& !l.isEmpty()*/) {
                    Term c = CONJ.the(b, common);
                    left = CONJ.the(l);
                    right = CONJ.the(r);
                    return b.conj(DTERNAL, c, b.theCompound(CONJ, dt, left, right));
                }
            }
        }
        return b.theCompound(CONJ, dt, left, right);
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
