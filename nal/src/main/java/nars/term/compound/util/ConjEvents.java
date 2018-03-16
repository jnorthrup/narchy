package nars.term.compound.util;

import jcog.list.FasterList;
import nars.Op;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.util.TermHashMap;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.RoaringBitmap;

import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.IntPredicate;

import static nars.Op.*;
import static nars.term.Terms.sorted;
import static nars.time.Tense.*;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * representation of events specified in one or more conjunctions,
 * for use while constructing, merging, and/or analyzing
 */
public class ConjEvents {

    //capacity of the initial array before upgrading to RoaringBitmap
    private static final int ROARING_UPGRADE_THRESH = 4;

//    private static final byte EMPTY = 0;
//    private static final byte POSITIVE = 1;
//    private static final byte NEGATIVE = 2;


    /**
     * unnegated events
     */
    final TermHashMap<Byte> terms = new TermHashMap();
    final List<Term> termsIndex = new FasterList(8);

//    /**
//     * keys are encoded 8-bits + 8-bits vector of the time,term index
//     *
//     * values are:
//     *      0 00 - not present
//     *      1 01 - positive     (x)
//     *      2 10 - negated  (--, x)
//     *      3 11 - unused
//     *
//     * TODO use a 'ShortCrumbHashMap'
//     *     for compact 4-bit values http://mathworld.wolfram.com/Crumb.html
//     *
//     */
//    public final ShortByteHashMap event = new ShortByteHashMap(2);
//
//    /** occurrences */
//    public final LongArrayList times = new LongArrayList(1);

    public final LongObjectHashMap event = new LongObjectHashMap<>(2);

    /**
     * state which will be set in a terminal condition, or upon term construction in non-terminal condition
     */
    Term term = null;

    public ConjEvents() {

    }


    /**
     * returns false if contradiction occurred, in which case this
     * ConjEvents instance is
     * now corrupt and its result via .term() should be considered final
     */
    public boolean add(Term t, long at) {
        if (term != null)
            throw new RuntimeException("already terminated");

        if (t == True)
            return true; //ignore
        else if (t == False) {
            this.term = False;
            return false;
        } else if (t == Null) {
            this.term = Null;
            return false;
        }


        Op x = t.op();
        boolean polarity;
        if (x == NEG) {
            t = t.unneg();
            polarity = false;
        } else {
            polarity = true;
        }

        int dt;
        if (x == CONJ && (dt = t.dt()) != XTERNAL
                && (dt != DTERNAL || at == ETERNAL)
                && (dt != 0 || at != ETERNAL)) {

//            try {
            return t.eventsWhile((w, e) -> add(e, w),
                    at,
                    (at != ETERNAL),
                    (at == ETERNAL), //only decompose DTERNAL if in the ETERNAL context, otherwise they are embedded as events
                    false, 0);
//            } catch (StackOverflowError e) {
//                System.err.println(t + " " + at + " " + dt);
//                throw new RuntimeException(t + " should not have recursed");
//            }
        } else {

            int id = id(t);
            if (!polarity)
                id = -id;


            if (!addIfValid(at, id)) {
                //contradiction
                term = False;
                return false;
            }

            return true;
        }
    }

    protected boolean addIfValid(long at, int id) {
        Object what = event.getIfAbsentPut(at, () -> new byte[ROARING_UPGRADE_THRESH]);
        if (what instanceof RoaringBitmap) {
            RoaringBitmap r = (RoaringBitmap) what;
            if (!r.contains(-id)) {
                r.add(id);
                return true;
            }
        } else {
            byte[] ii = (byte[]) what;
            if (indexOfZeroTerminated(ii, ((byte) -id)) == -1) {
                int nextSlot = indexOfZeroTerminated(ii, (byte) 0);
                if (nextSlot != -1) {
                    ii[nextSlot] = (byte) id;
                } else {
                    //upgrade to roaring and add
                    RoaringBitmap rb = new RoaringBitmap();
                    for (byte b : ii)
                        rb.add(b);
                    rb.add(id);
                    event.put(at, rb);
                }
                return true;
            }
        }
        return false;
    }

    private int indexOfZeroTerminated(byte[] b, byte val) {
        for (int i = 0; i < b.length; i++) {
            byte bi = b[i];
            if (val == bi) {
                return i;
            } else if (bi == 0) {
                return -1; //terminate early
            }
        }
        return -1;
    }


    private int id(Term t) {
        assert (t != null && !(t instanceof Bool));
        return terms.computeIfAbsent(t, tt -> {
            int s = terms.size();
            termsIndex.add(tt);
            assert (s < Byte.MAX_VALUE);
            return (byte) s;
        }) + 1;
    }

//    private byte id(long w) {
//
//        int i = times.indexOf(w);
//        if (i!=-1) {
//            return (byte) i;
//        } else {
//            int s = times.size();
//            assert(s < Byte.MAX_VALUE);
//            times.add(w);
//            return (byte)s;
//        }
//    }
//
//    short id(Term t, long w) {
//        byte tb = id(t);
//        byte wb = id(w);
//        return (short) ((tb << 8) | wb);
//    }
//
//    byte termIndex(short s) {
//        return (byte) ((s >> 8) & 0xff);
//    }
//    byte timeIndex(short s) {
//        return (byte) (s & 0xff);
//    }

    public Term term() {
        if (term != null)
            return term;


        int numTimes = event.size();
        switch (numTimes) {
            case 0:
                return Null;
            case 1:
                break;
            default:
                break;
        }

        event.compact();


        IntPredicate validator = null;
        Object eternalWhat = event.get(ETERNAL);
        Term eternal = termConj(ETERNAL, eternalWhat);
        if (eternal != null) {

            if (eternal instanceof Bool)
                return this.term = eternal; //override and terminates

            if (numTimes > 1) {
                //temporal components follow, so build the verifier:

                if (eternal.op() == CONJ) {
                    //Subterms eteSub = eternal.subterms();
                    if (eternalWhat instanceof byte[]) {
                        byte[] b = (byte[])eternalWhat;
                        validator = (i) -> indexOfZeroTerminated(b, (byte) -i) == -1;
                    } else {
                        RoaringBitmap b = (RoaringBitmap)eternalWhat;
                        validator = (i) -> !b.contains(-i);
                    }
                } else {
                    Term finalEternal = eternal;
                    validator = (t) -> !finalEternal.equalsNeg(termsIndex.get(Math.abs(t-1)).negIf(t<0));
                }
            }
        }

        if (eternal != null && numTimes == 1)
            return eternal; //done


        FasterList<LongObjectPair<Term>> e = new FasterList(numTimes - (eternal != null ? 1 : 0));
        Iterator<LongObjectPair> ii = event.keyValuesView().iterator();
        while (ii.hasNext()) {
            LongObjectPair next = ii.next();
            long when = next.getOne();
            if (when == ETERNAL)
                continue; //already handled above

            Term wt = termConj(when, next.getTwo(), validator);

            if (wt == True) {
                continue; //canceled out
            } else if (wt == False) {
                return this.term = False; //short-circuit false
            } else if (wt == Null) {
                return this.term = Null; //short-circuit null
            }

            e.add(pair(when, wt));
        }
        assert (!e.isEmpty());

        Term temporal;
        if (e.size() > 1) {
            e.sortThisBy(LongObjectPair::getOne);

            temporal = conjSeq(e);
            if (temporal instanceof Bool)
                return temporal;
        } else {
            temporal = e.get(0).getTwo();
        }

        return eternal != null ?
                //Op.instance(CONJ, DTERNAL, sorted(eternal, temporal))
                CONJ.the(DTERNAL, sorted(eternal, temporal))
                :
                temporal;
    }

    public long shift() {
        long min = Long.MAX_VALUE;
        LongIterator ii = event.keysView().longIterator();
        while (ii.hasNext()) {
            long t = ii.next();
            if (t != DTERNAL) {
                if (t < min)
                    min = t;
            }
        }
        return min;
    }


    private Term termConj(long w) {
        return termConj(w, event.get(w), null);
    }

    private Term termConj(long w, Object what) {
        return termConj(w, what, null);
    }

    private Term termConj(long w, Object what, IntPredicate validator) {

        if (what == null) return null;

        final RoaringBitmap rb;
        final byte[] b;
        int n;
        if (what instanceof byte[]) {
            b = (byte[]) what;
            rb = null;
            n = indexOfZeroTerminated(b, (byte) 0);
            if (n == 1) {
                //simplest case
                return sub(b[0], null, validator);
            }
        } else {
            rb = (RoaringBitmap) what;
            b = null;
            n = rb.getCardinality();
        }


        final boolean[] negatives = {false};
        TreeSet<Term> t = new TreeSet();
        if (b != null) {
            for (byte x : b) {
                if (x == 0)
                    break; //done
                t.add(sub(x, negatives, validator));
            }
        } else {
            rb.forEach((int termIndex) -> {
                t.add(sub(termIndex, negatives, validator));
            });
        }

        if (negatives[0] && n > 1) {
            //annihilate common terms inside and outside of disjunction
            //      ex:
            //          -X &&  ( X ||  Y)
            //          -X && -(-X && -Y)  |-   -X && Y
            Iterator<Term> oo = t.iterator();
            List<Term> csa = null;
            while (oo.hasNext()) {
                Term x = oo.next();
                if (x.hasAll(NEG.bit | CONJ.bit)) {
                    if (x.op() == NEG) {
                        Term x0 = x.sub(0);
                        if (x0.op() == CONJ && CONJ.commute(x0.dt(), x0.subs())) { //DISJUNCTION
                            Term disj = x.unneg();
                            SortedSet<Term> disjSubs = disj.subterms().toSetSorted();
                            //factor out occurrences of the disj's contents outside the disjunction, so remove from inside it
                            if (disjSubs.removeAll(t)) {
                                //reconstruct disj if changed
                                oo.remove();

                                if (!disjSubs.isEmpty()) {
                                    if (csa == null)
                                        csa = new FasterList(1);
                                    csa.add(
                                            CONJ.the(disj.dt(), sorted(disjSubs)).neg()
                                    );
                                }
                            }
                        }
                    }
                }
            }
            if (csa != null)
                t.addAll(csa);
        }

        int ts = t.size();
        switch (ts) {
            case 0:
                throw new RuntimeException("fault");
            case 1:
                return t.first();
            default:
                return
                        Op.instance(CONJ,
                                w == ETERNAL ? DTERNAL : 0,
                                sorted(t));
        }
    }

    private Term sub(int termIndex, @Nullable boolean[] negatives, IntPredicate validator) {
        assert (termIndex != 0);

        boolean neg = false;
        if (termIndex < 0) {
            termIndex = -termIndex;
            neg = true;
        }

        if (validator!=null && !validator.test(termIndex))
            return False;

        Term c = termsIndex.get(termIndex - 1);
        if (neg) {
            c = c.neg();
            if (negatives != null)
                negatives[0] = true;
        }
        return c;
    }

    public static Term conjSeq(FasterList<LongObjectPair<Term>> events) {
        return conjSeq(events, 0, events.size());
    }

    /**
     * constructs a correctly merged conjunction from a list of events, in the sublist specified by from..to (inclusive)
     * assumes that all of the event terms have distinct occurrence times
     */
    public static Term conjSeq(List<LongObjectPair<Term>> events, int start, int end) {

        LongObjectPair<Term> first = events.get(start);
        int ee = end - start;
        switch (ee) {
            case 0:
                throw new NullPointerException("should not be called with empty events list");
            case 1:
                return first.getTwo();
            case 2:
                LongObjectPair<Term> second = events.get(end - 1);
                return conjSeqFinal(
                        (int) (second.getOne() - first.getOne()),
                        /* left */ first.getTwo(), /* right */ second.getTwo());
        }

        int center = start + (end - 1 - start) / 2;


        Term left = conjSeq(events, start, center + 1);
        if (left == Null) return Null;
        if (left == False) return False; //early fail shortcut

        Term right = conjSeq(events, center + 1, end);
        if (right == Null) return Null;
        if (right == False) return False; //early fail shortcut

        int dt = (int) (events.get(center + 1).getOne() - first.getOne() - left.dtRange());

        return conjSeqFinal(dt, left, right);
    }

    private static Term conjSeqFinal(int dt, Term left, Term right) {
        assert (dt != XTERNAL);

        if (left == False) return False;
        if (left == Null) return Null;
        if (left == True) return right;

        if (right == False) return False;
        if (right == Null) return Null;
        if (right == True) return left;

        if (dt == 0 || dt == DTERNAL) {
            if (left.equals(right)) return left;
            if (left.equalsNeg(right)) return False;

            //return CONJ.the(dt, left, right); //send through again
        }


        //System.out.println(left + " " + right + " " + left.compareTo(right));
        //return CONJ.the(dt, left, right);
        if (left.compareTo(right) > 0) {
            //larger on left
            dt = -dt;
            Term t = right;
            right = left;
            left = t;
        }

        if (left.op() == CONJ && right.op() == CONJ) {
            int ldt = left.dt(), rdt = right.dt();
            if (ldt != XTERNAL && !concurrent(ldt) && rdt != XTERNAL && !concurrent(rdt)) {
                int ls = left.subs(), rs = right.subs();
                if ((ls > 1 + rs) || (rs > ls)) {
                    //seq imbalance; send through again
                    return CONJ.the(dt, left, right);
                }
            }
        }


        return Op.implInConjReduce(instance(CONJ, dt, left, right));
    }

}
