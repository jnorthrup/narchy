package nars.term.compound.util;

import jcog.list.FasterList;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.util.TermHashMap;
import org.eclipse.collections.api.iterator.MutableLongIterator;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.ShortByteHashMap;

import java.util.List;
import java.util.TreeSet;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.XTERNAL;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/** representation of events specified in one or more conjunctions,
 * for use while constructing, merging, and/or analyzing */
public class ConjEvents {

    private static final byte EMPTY = 0;
    private static final byte POSITIVE = 1;
    private static final byte NEGATIVE = 2;

    /** occurrences */
    public final LongArrayList times = new LongArrayList(1);

    /** unnegated events */
    final TermHashMap<Byte> terms = new TermHashMap();
    final List<Term> termsIndex = new FasterList(2);

    /**
     * keys are encoded 8-bits + 8-bits vector of the time,term index
     *
     * values are:
     *      0 00 - not present
     *      1 01 - positive     (x)
     *      2 10 - negated  (--, x)
     *      3 11 - unused
     *
     * TODO use a 'ShortCrumbHashMap'
     *     for compact 4-bit values http://mathworld.wolfram.com/Crumb.html
     *
     */
    public final ShortByteHashMap event = new ShortByteHashMap(2);

    /** state which will be set in a terminal condition, or upon term construction in non-terminal condition */
    Term term = null;

    public ConjEvents() {

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


    /** returns false if contradiction occurred, in which case this
     * ConjEvents instance is
     * now corrupt and its result via .term() should be considered final
     * */
    public boolean add(Term t, long at) {
        if (term!=null)
            throw new RuntimeException("already terminated");

        Op x = t.op();
        byte polarity;
        if (x == NEG) {
            t = t.unneg();
            polarity = NEGATIVE;
        } else {
            polarity = POSITIVE;
        }

        int dt;
        if (x==CONJ && (dt=t.dt())!=XTERNAL && (dt!=DTERNAL || at==ETERNAL)) {
            return t.eventsWhile((w, e) -> add(e, w),
                    at, true,
                    (at==ETERNAL), //only decompose DTERNAL if in the ETERNAL context, otherwise they are embedded as events
                    false, 0);
        } else {

            short id = id(t, at);

            byte existing = event.get(id);
            if (existing!=EMPTY && existing!=polarity) {
                //contradiction
                term = False;
                return false;
            }

            event.put(id, polarity);

            return true;
        }
    }

    private byte id(Term t) {
        return terms.computeIfAbsent(t, tt-> {
            int s = terms.size();
            termsIndex.add(tt);
            assert(s < Byte.MAX_VALUE);
            return (byte) s;
        });
    }

    private byte id(long w) {

        int i = times.indexOf(w);
        if (i!=-1) {
            return (byte) i;
        } else {
            int s = times.size();
            assert(s < Byte.MAX_VALUE);
            times.add(w);
            return (byte)s;
        }
    }

    short id(Term t, long w) {
        byte tb = id(t);
        byte wb = id(w);
        return (short) ((tb << 8) | wb);
    }

    byte termIndex(short s) {
        return (byte) ((s >> 8) & 0xff);
    }
    byte timeIndex(short s) {
        return (byte) (s & 0xff);
    }

    public Term term() {
        if (term != null)
            return term;


        int numTimes = times.size();
        switch (numTimes) {
            case 0: return Null;
            case 1: break;
            default:
                break;
        }

        event.compact();

        Term eternal = null;
        Predicate<Term> eternalWrapValid = null;
        int eteIndex = times.indexOf(ETERNAL);
        if (eteIndex!=-1) {
            eternal = termConj(eteIndex);

            if (eternal instanceof Bool)
                return this.term = eternal; //override and terminates

            if (numTimes > 1) {
                //temporal components follow, so build the verifier

                if (eternal.op()==CONJ) {
                    Subterms eteSub = eternal.subterms();
                    eternalWrapValid = (t) -> {

                        //top-level co-negation test
                        if (eteSub.containsNeg(t))
                            return false;

                        return true;
                    };
                } else {
                    Term finalEternal = eternal;
                    eternalWrapValid = (t) -> {
                        return !finalEternal.equalsNeg(t);
                    };
                }
            }
        }

        if (eternal!=null && numTimes == 1)
            return eternal; //done

        MutableLongIterator ii = times.longIterator();
        int ti = 0;
        FasterList<LongObjectPair<Term>> events = new FasterList(numTimes - (eternal!=null? 1 : 0));
        while (ii.hasNext()) {
            long w = ii.next();
            if (w!=ETERNAL) {
                Term wt = termConj(ti);

                if (wt == True) {
                    continue; //canceled out
                } else if (wt == False) {
                    return this.term = False; //short-circuit false
                } else if (wt == Null) {
                    return this.term = Null; //short-circuit null
                }

                if (eternalWrapValid!=null) {
                    //check for contradiction between ETERNAL and other events
                    if (!eternalWrapValid.test(wt)) {
                        return this.term = False;
                    }
                }
                events.add(pair(w, wt));
            }
            ti++;
        }
        assert(!events.isEmpty());

        if (events.size() > 1)
            events.sortThisBy(LongObjectPair::getOne);

        Term temporal = conjSeq(events);
        if (temporal instanceof Bool)
            return temporal;

        return eternal!=null ? CONJ.the(DTERNAL, eternal, temporal) : temporal;
    }

    public long shift() {
        long min = Long.MAX_VALUE;
        MutableLongIterator ii = times.longIterator();
        while (ii.hasNext()) {
            long t =  ii.next();
            if (t != DTERNAL) {
                if (t < min)
                    min = t;
            }
        }
        return min;
    }

    /** groups events occurring at the specified time index */
    Term termConj(int timeIndex) {
        long w = times.get(timeIndex);

        TreeSet<Term> t = new TreeSet();
        event.forEachKeyValue((s,b)->{
            if (timeIndex(s) == timeIndex) {
                Term c = termsIndex.get(termIndex(s));
                if (b==NEGATIVE)
                    c = c.neg();
                t.add(c);
            }
        });

        switch (t.size()) {
            case 0: throw new RuntimeException("fault");
            case 1: return t.first();
            default:
                return Op.CONJ.the(w == ETERNAL ? DTERNAL : 0, t);
        }

    }
}
