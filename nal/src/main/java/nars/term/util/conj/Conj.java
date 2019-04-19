package nars.term.util.conj;

import jcog.TODO;
import jcog.WTF;
import jcog.data.bit.MetalBitSet;
import jcog.data.list.FasterList;
import jcog.data.set.LongObjectArraySet;
import jcog.util.ArrayUtils;
import nars.Op;
import nars.Param;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Bool;
import nars.term.util.builder.TermBuilder;
import nars.term.util.map.ByteAnonMap;
import nars.time.Tense;
import org.eclipse.collections.api.block.predicate.primitive.ByteObjectPredicate;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.iterator.MutableByteIterator;
import org.eclipse.collections.api.set.primitive.ByteSet;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.api.tuple.primitive.LongObjectPair;
import org.eclipse.collections.impl.factory.primitive.ByteSets;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.ByteHashSet;
import org.jetbrains.annotations.Nullable;
import org.roaringbitmap.ImmutableBitmapDataProvider;
import org.roaringbitmap.RoaringBitmap;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.lang.System.arraycopy;
import static nars.Op.*;
import static nars.term.Terms.sorted;
import static nars.term.atom.Bool.*;
import static nars.time.Tense.*;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * representation of conjoined (eternal, parallel, or sequential) events specified in one or more conjunctions,
 * for use while constructing, merging, and/or analyzing
 * <p>
 * https://en.wikipedia.org/wiki/Logical_equivalence
 * https://en.wikipedia.org/wiki/Negation_normal_form
 * https://en.wikipedia.org/wiki/Conjunctive_normal_form
 */
public class Conj extends ByteAnonMap implements ConjBuilder {


    public static final int ROARING_UPGRADE_THRESH = 8;

    /**
     * TermBuilder to use internally
     */
    @Deprecated
    private static final TermBuilder terms =
            //HeapTermBuilder.the;
            Op.terms;
    private static final Predicate<Term> isTemporalComponent = Conj::isSeq;
    private static final Predicate<Term> isEternalComponent = isTemporalComponent.negate();
    public final LongObjectHashMap<Object> event;
    /**
     * state which will be set in a terminal condition, or upon target construction in non-terminal condition
     */
    private Term result = null;

    public Conj() {
        this(2);
    }

    Conj(ObjectByteHashMap<Term> x, FasterList<Term> y) {
        super(x, y);
        event = new LongObjectHashMap<>(2);
    }

    public Conj(int n) {
        super(n);
        event = new LongObjectHashMap<>(n);
    }

    public Conj(long initialEventAt, Term initialEvent) {
        this(1);
        add(initialEventAt, initialEvent);
    }

    /**
     * but events are unique
     */
    static Conj newConjSharingTermMap(Conj x) {
        return new Conj(x.termToId, x.idToTerm);
    }

    public static Conj from(Term t) {
        Conj c = new Conj();
        c.addAuto(t);
        return c;
    }

    private static ConjLazy fromLazy(Term t) {
        return ConjLazy.events(t);
    }

    public static MetalBitSet seqEternalComponents(Subterms x) {
        return x.subsTrue(Conj.isEternalComponent);
    }

    public static boolean containsOrEqualsEvent(Term container, Term x) {
        return container.equals(x) || containsEvent(container, x);
    }

    public static boolean containsEvent(Term container, Term x) {
        if (!x.op().eventable)
            return false;
        if (container.op() != CONJ || container.impossibleSubTerm(x))
            return false;

        if (isSeq(container)) {
            boolean xIsConj = x.op() == CONJ;
            int xdt = xIsConj ? x.dt() : DTERNAL;
            return !container.eventsWhile((when, cc) -> cc == container || !containsOrEqualsEvent(cc, x), //recurse
                    0, !xIsConj || xdt != 0, !xIsConj || xdt != DTERNAL, true);
        } else {
            return container.contains(x);
        }

    }

    public static boolean isEventFirstOrLast(Term container, Term x, boolean firstOrLast) {
        if (!x.op().eventable || container.op() != CONJ || container.impossibleSubTerm(x))
            return false;

        boolean seq = isSeq(container);
        if (!seq) {
            return ConjCommutive.contains(container, x);
        } else {
            return ConjSeq.contains(container, x, firstOrLast);
        }

    }

    public static Term[] preSort(int dt, Term[] u) {

        switch (dt) {
            case 0:
            case DTERNAL:
                return preSorted(u);

            case XTERNAL:
                Term[] v = preSorted(u);
                if (v.length == 1 && !(v[0] instanceof Bool)) {
                    if (/*!(v[0] instanceof Ellipsislike) || */(u.length > 1 && u[0].equals(u[1])))
                        return new Term[]{v[0], v[0]};
                }
                return v;

            default:
                return u;
        }
    }

    //    private static boolean isEventSequence(Term container, Term subseq, boolean neg, boolean firstOrLast) {
//        if (neg)
//            throw new TODO(); //may not even make sense
//
//        for (Term s : subseq.subterms())
//            if (!container.containsRecursively(s))
//                return false;
//
//        int containerDT = container.dt();
//        if (containerDT ==0 || containerDT ==DTERNAL || containerDT ==XTERNAL)
//            return true; //already met requirements since the container is unordered
//
//
//        //compare the correct order and whether it appears in prefix or suffix as appropriate
////        int range = container.eventRange();
//        long elimStart = Long.MAX_VALUE, elimEnd = Long.MIN_VALUE;
//        FasterList<LongObjectPair<Term>> events = container.eventList();
//        elimNext: for (Term s : subseq.subterms()) {
//            int n = events.size();
//            int start = firstOrLast ? 0 : n-1, inc = firstOrLast ? +1 : -1;
//            int k = start;
//            for (int i = 0; i < n; i++) {
//                LongObjectPair<Term> e = events.get(k);
//                if (e.getTwo().equals(s)) {
//                    long ew = e.getOne();
//                    elimStart = Math.min(elimStart, ew);
//                    elimEnd = Math.max(elimEnd, ew);
//                    events.remove(k);
//                    continue elimNext;
//                }
//                k += inc;
//            }
//            return false; //event not found
//        }
//
//        if (events.isEmpty()) {
//            //fully eliminated
//            return false;
//        }
//
//        for (LongObjectPair<Term> remain : events) {
//            long w = remain.getOne();
//            if (firstOrLast && w < elimStart)
//                return false;//there is a prior event
//            else if (!firstOrLast && w > elimEnd)
//                return false;//there is a later event
//        }
//
//        return true;
//    }

    private static Term[] preSorted(Term[] u) {

        for (Term t : u)
            if (t == Bool.Null)
                return Bool.Null_Array;

        int trues = 0;
        for (Term t : u) {
            if (t == Bool.False)
                return Bool.False_Array;
            if (t == Bool.True)
                trues++;
            else if (!t.op().eventable)
                return Null_Array;
        }
        if (trues > 0) {


            int sizeAfterTrueRemoved = u.length - trues;
            switch (sizeAfterTrueRemoved) {
                case 0:
                    return Op.EmptyTermArray;
                case 1: {

                    for (Term uu : u) {
                        if (uu != Bool.True) {
                            //assert (!(uu instanceof Ellipsislike)) : "if this happens, TODO";
                            return new Term[]{uu};
                        }
                    }
                    throw new RuntimeException("should have found non-True target to return");
                }
                default: {
                    Term[] y = new Term[sizeAfterTrueRemoved];
                    int j = 0;
                    for (int i = 0; j < y.length; i++) {
                        Term uu = u[i];
                        if (uu != Bool.True)
                            y[j++] = uu;
                    }
                    u = y;
                }
            }
        }
        return Terms.sorted(u);
    }

    public static Term chooseEvent(Term conj, Random random, boolean decomposeDternal, boolean decomposeParallel, LongObjectPredicate<Term> valid) {

        FasterList<Term> candidates = new FasterList();
        conj.eventsWhile((when, what) -> {
            if (valid.accept(when, what))
                candidates.add(what);
            return true;
        }, 0, decomposeDternal, decomposeParallel, true);

        if (candidates.isEmpty())
            return Null;
        else
            return candidates.get(random);
    }

    /** TODO make a verison of this which iterates from a Conj instance */
    public static List<LongObjectPair<Term>> match(Term conj, boolean decomposeDternal, boolean decomposeParallel, LongObjectPredicate<Term> valid) {

        FasterList<LongObjectPair<Term>> candidates = new FasterList();

        conj.eventsWhile((when, what) -> {
            if (valid.accept(when, what))
                candidates.add(pair(when,what));
            return true;
        }, 0, decomposeDternal, decomposeParallel, true);

        if (candidates.isEmpty())
            return List.of();
        else
            return candidates;
    }

//    /**
//     * TODO impl levenshtein via byte-array ops
//     */
//    public static StringBuilder sequenceString(Term a, Conj x) {
//        StringBuilder sb = new StringBuilder(4);
//        int range = a.eventRange();
//        final float stepResolution = 16f;
//        float factor = stepResolution / range;
//        a.eventsWhile((when, what) -> {
//            int step = Math.round(when * factor);
//            sb.append((char) step);
//
//            if (what.op() == NEG)
//                sb.append('-'); //since x.addAt(what) will store the unneg id
//            sb.append(((char) x.addAt(what)));
//            return true;
//        }, 0, true, true, false, 0);
//
//        return sb;
//    }


//    /**
//     * TODO improve, unify sub-sequences of events etc
//     */
//    @Nullable
//    public static Term withoutEarlyOrLateUnifies(Term conj, Term event, boolean earlyOrLate, boolean strict, Random rng, int ttl) {
//        int varBits = VAR_DEP.bit | VAR_INDEP.bit;
//
//        if (conj.op() != CONJ || event.volume() > conj.volume())
//            return null;
//
//        boolean eventVars = event.hasAny(varBits);
//        if ((strict && (!conj.hasAny(varBits) && !eventVars /* TODO temporal subterms? */)))
//            return null;
//
//        if (!strict)
//            throw new TODO();
//
//        if (Conj.isSeq(conj)) {
//            Term match = earlyOrLate ? conj.eventFirst() : conj.eventLast(); //TODO look inside parallel conj event
//            if (strict && (match.equals(event) || (!eventVars && !match.hasAny(varBits) /* TODO temporal subterms? */)))
//                return null;
//
//            if (!Terms.possiblyUnifiable(match, event, strict, varBits))
//                return null;
//
//            SubUnify s = new SubUnify(rng);
//            s.ttl = ttl;
//            if (match.unify(event, s)) {
//                //TODO try diferent permutates tryMatch..
//                Term dropped = withoutEarlyOrLate(conj, match, earlyOrLate);
//                Term dropUnified = dropped.replace(s.xy);
//                if (!strict || !dropUnified.equals(dropped)) {
//                    return dropUnified;
//                }
//            }
//        } //else: parallel try sub-events
//
//        return null; //TODO
//    }

    private static int eventCount(Object what) {
        if (what instanceof byte[]) {
            byte[] b = (byte[]) what;
            int i = indexOfZeroTerminated(b, (byte) 0);
            return i == -1 ? b.length : i;
        } else {
            if (what instanceof RoaringBitmap)
                return ((ImmutableBitmapDataProvider) what).getCardinality();
            else
                return 0;
        }
    }

//    /**
//     * returns null if wasnt contained, True if nothing remains after removal
//     */
//    @Nullable
//    public static Term withoutEarlyOrLate(Term conj, Term event, boolean earlyOrLate) {
//
//        Op o = conj.op();
//        if (o == NEG) {
//            Term n = withoutEarlyOrLate(conj, event, earlyOrLate);
//            return n != null ? n.neg() : null;
//        }
//
//        if (o == CONJ && !conj.impossibleSubTerm(event)) {
//            if (isSeq(conj)) {
//                Conj c = Conj.from(conj);
//                //if (c.dropEvent(event, earlyOrLate, filterContradiction))
//                if (c.dropEvent(event, earlyOrLate, false))
//                    return c.term();
//            } else {
//                Term[] csDropped = conj.subterms().subsExcluding(event);
//                if (csDropped != null)
//                    return (csDropped.length == 1) ? csDropped[0] : terms.conj(conj.dt(), csDropped);
//            }
//        }
//
//        return null; //no change
//
//    }

    /**
     * means that the internal represntation of the target is concurrent
     */
    public static boolean concurrentInternal(int dt) {
        switch (dt) {
            case XTERNAL:
            case DTERNAL:
            case 0:
                return true;
        }
        return false;
    }

    /**
     * this refers to an internal concurrent representation but may not be consistent with all cases
     */
    @Deprecated
    public static boolean concurrent(int dt) {
        if (dt == XTERNAL)
            return true; //TEMPORARY
        switch (dt) {
            case XTERNAL:
            case DTERNAL:
            case 0:
                return true;
        }
        return false;
    }

    public static int conjEarlyLate(Term x, boolean earlyOrLate) {
        assert (x.op() == CONJ);
        int dt = x.dt();
        switch (dt) {
            case XTERNAL:
                throw new UnsupportedOperationException();

            case DTERNAL:
            case 0:
                return earlyOrLate ? 0 : 1;

            default: {


                return (dt < 0) ? (earlyOrLate ? 1 : 0) : (earlyOrLate ? 0 : 1);
            }
        }
    }

    public static Term negateEvents(Term x) {
        switch (x.op()) {
            case NEG:
                return negateEvents(x.unneg()).neg();
            case CONJ: {
                ConjBuilder c = Conj.fromLazy(x);
                c.negateEvents();
                return c.term();
            }
            default:
                return x.neg();
        }
    }

//    public static Term dropAnyEvent(Term x, NAR nar) {
//        Op oo = x.op();
//
//        boolean negated = (oo == NEG);
//        if (negated) {
//            x = x.unneg();
//            oo = x.op();
//        }
//
//        if (oo == IMPL) {
//            boolean sNeg = x.sub(0).op()==NEG;
//            Term s = x.sub(0);
//            if (sNeg)
//                s = s.unneg();
//            Term ss = dropAnyEvent(s, nar);
//            Term p = x.sub(1);
//
//            if (ss instanceof Bool || s.equals(ss)) {
//                Term pp = dropAnyEvent(p, nar);
//                if (pp instanceof Bool || p.equals(pp))
//                    return x; //no change
//
//                //use Term.transform to shift any internal dt's appropriately
//                return x.replace(x.sub(1), pp).negIf(negated);
//            } else {
//                //use Term.transform to shift any internal dt's appropriately
//                return x.replace(x.sub(0), ss.negIf(sNeg)).negIf(negated);
//            }
//        }
//
//        if (oo != CONJ)
//            return Null;
//
//
//
//        Random rng = nar.random();
//
//        Term y;
//        int dt = x.dt();
//        if (!Conj.isSeq(x)) {
//            Subterms xx = x.subterms();
//            int ns = xx.subs();
//            if (ns == 2) {
//                //choose which one will remain
//                y = xx.sub(rng.nextInt(ns));
//            } else {
//                y = terms.conj(dt, xx.subsExcluding(rng.nextInt(ns)));
//            }
//        } else {
//            Conj c = from(x);
//            c.distribute();
//            long eventAt;
//            if (c.event.size() == 1) {
//                eventAt = c.event.keysView().longIterator().next();
//            } else {
//                //choose random event
//                long[] events = c.event.keySet().toArray();
//                if (events.length == 0)
//                    throw new WTF();
//                eventAt = events.length == 1 ? events[0] : events[rng.nextInt(events.length)];
//            }
//            Object event = c.event.get(eventAt);
//            if (event instanceof byte[]) {
//                int events = eventCount(event);
//                byte b = ((byte[]) event)[rng.nextInt(events)];
//                boolean removed = c.remove(eventAt, b);
//                assert (removed);
//            } else {
//                throw new TODO();
//            }
//            y = c.target();
////                FasterList<LongObjectPair<Term>> ee = Conj.eventList(x);
////                ee.remove(nar.random().nextInt(ee.size()));
////                y = Conj.conj(ee);
////                if (y.equals(x))
////                    return Null;
////                assert (y != null);
//        }
//        return y.negIf(negated);
//    }

    /**
     * whether the conjunction is a sequence (includes check for factored inner sequence)
     */
    public static boolean isSeq(Term x) {
        if (x.op() != CONJ)
            return false;

        int dt = x.dt();
        if (dt == DTERNAL) {
            return _isSeq(x);
        } else
            return !dtSpecial(dt);
    }

    private static boolean _isSeq(Term x) {
        Subterms xx = x.subterms();
        return xx.hasAny(CONJ) && //inner conjunction
                xx.subs() > 1 &&
                xx.count(Conj::isSeq) == 1 &&
                (!xx.hasAny(NEG)
                        ||
                        /** TODO weird disjunctive seq cases */
                        xx.count(xxx -> xxx.op() == NEG && xxx.unneg().op() == CONJ) == 0);
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
        return seqEternal(ss, ss.subsTrue(isEternalComponent));
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
        Compound t = (Compound) s.subFirst(isTemporalComponent);
        assert (Conj.isSeq(t));
        return t;
    }

    public static Term seqTemporal(Subterms s, MetalBitSet eternalComponents) {
        return s.sub(eternalComponents.next(false, 0, s.subs()));
    }

    public static Term diffOne(Term include, Term exclude) {
        return diffOne(include, exclude, false);
    }

    public static Term diffAll(Term include, Term exclude) {
        return diffAll(include, exclude, false);
    }

    public static Term diffAll(Term include, Term exclude, boolean excludeNeg) {

        if (include.op()==NEG) {
            Term y = diffAll(include.unneg(), exclude);
            return y==True ? True : y.neg();
        }
//        boolean eSeq = Conj.isSeq(exclude);
//        Subterms ii = include.subterms();
//        if (!eSeq && !Conj.isSeq(include)) {
//
//            Subterms es = exclude.subterms();
//            MetalBitSet iei = ii.subsTrue(i -> !es.contains(i));
//            int in = iei.cardinality();
//            if (in < include.subs()) {
//                if (in == 1)
//                    return ii.sub(iei.first(true));
//                else
//                    return terms.conj(include.dt(), ii.subsIncluding(iei));
//            } else {
//                return include; //no change
//            }
//
//        } else {
//
//
//            ConjBuilder x = Conj.fromLazy(include);
//            boolean[] removedSomething = new boolean[]{false};
//
//            long offset = exclude.dt() == DTERNAL && !Conj.isSeq(exclude) ? ETERNAL : 0;
//
//            exclude.eventsWhile((when, what) -> {
//                removedSomething[0] |= when == ETERNAL ? x.removeAll(what) : x.remove(when, what);
//                return true;
//            }, offset, true, true, false);
//
//            return removedSomething[0] ? x.term() : include;
//
//
//        }

        if (exclude.op()==CONJ || Conj.isSeq(include)) {
//            Conj xx = Conj.from(include);
//            if (xx.removeEventsByTerm(exclude, true, excludeNeg)) {
//                return xx.term();
//            } else {
//                return include;
//            }

            ConjBuilder x =
                    //Conj.from(include);
                    Conj.fromLazy(include);

            boolean[] removedSomething = new boolean[]{false};

            long offset = exclude.dt() == DTERNAL && !Conj.isSeq(exclude) ? ETERNAL : 0;

            exclude.eventsWhile((when, what) -> {
                removedSomething[0] |= when == ETERNAL ? x.removeAll(what) : x.remove(when, what);
                //removedSomething[0] |= x.remove(when, what);
                return true;
            }, offset, true, true, false);

            return removedSomething[0] ? x.term() : include;
        } else {
            Subterms s = include.subterms();
            //try positive first
            Term[] ss = Terms.withoutOne(s, t -> t.equals(exclude), ThreadLocalRandom.current());
                    //, s.subsExcluding(exclude);
            if (ss != null) { int dt = include.dt();
                return ss.length > 1 ? terms.conj(dt, ss) : ss[0];
            } else {
                //try negative next
                if (excludeNeg) {
                    Term excludeNegTerm = exclude.neg();
                    ss = Terms.withoutOne(s, t -> t.equals(excludeNegTerm), ThreadLocalRandom.current());
                    if (ss != null) {
                        return terms.conj(include.dt(), ss);
                    }
                }

                return include; //not found
            }

        }
    }

    @Deprecated public static Term diffOne(Term include, Term exclude, @Deprecated boolean excludeNeg) {
        return diffOne(include, exclude, excludeNeg, ThreadLocalRandom.current());
    }

    /**
     * include may be a conjunction or a negation of a conjunction. the result will be polarized accordingly
     */
    public static Term diffOne(Term include, Term exclude, @Deprecated boolean excludeNeg, Random rng) {


        final Op io = include.op();
        if (io == NEG) {
            //negated conjunction
            Term iu = include.unneg();
            if (iu.op()==CONJ) {
                Term r = diffOne(iu, exclude, excludeNeg, rng); //TODO better
                if (r==True)
                    return True;
                else
                    return r.neg();
            }
        }

        final Op eo = exclude.op();

        boolean eitherNeg = io == NEG || eo == NEG;

        if (excludeNeg && eitherNeg) {
            if (include.unneg().equals(exclude.unneg()))
                return True;
        } else {
            if (include.equals(exclude))
                return True;
        }
        if (io != CONJ)
            return include;


//        if (!excludeNeg && eo == CONJ)
//            return diffAll(include, exclude); //HACK

        if (include.impossibleSubTerm(excludeNeg ? exclude.unneg() : exclude))
            return include;




//        if (Conj.isSeq(include)) {


        boolean decomposeDternal = eo!=CONJ && exclude.dt()!=DTERNAL;
        boolean decomposeParallel = eo!=CONJ && exclude.dt()!=0;
        List<LongObjectPair<Term>> ee = Conj.match(include, decomposeDternal, decomposeParallel,
                !excludeNeg ?
                    (when, what) -> what.equals(exclude)
                    :
                    (when, what) -> what.equalsPosOrNeg(exclude)
        );
        LongObjectPair<Term> e;
        switch (ee.size()) {
            case 0: return include; //nothing removed
            case 1: e = ee.get(0); break;
            default: {
                e = ((FasterList<LongObjectPair<Term>>)ee).get(rng);
                break;
            }
        }
        return Conj.remove(include, e);



//        } else { int dt = include.dt();
//            Subterms s = include.subterms();
//            //try positive first
//            Term[] ss = s.subsExcluding(exclude);
//            if (ss != null) {
//                return ss.length > 1 ? terms.conj(dt, ss) : ss[0];
//            } else {
//                //try negative next
//                if (excludeNeg) {
//                    ss = s.subsExcluding(exclude.neg());
//                    if (ss != null) {
//                        return terms.conj(dt, ss);
//                    }
//                }
//
//                return include; //not found
//            }
//
//        }

    }

    public static Term remove(Term include, LongObjectPair<Term> e) {
        int idt = include.dt();
        if (dtSpecial(idt) && e.getTwo().op()!=CONJ) {
            //fast commutive remove
            @Nullable Term[] ss = include.subterms().subsExcluding(e.getTwo());
            if (ss == null)
                return Null; //WTF?

            if (ss.length > 1)
                return CONJ.the(idt, ss);
            else
                return ss[0];
        } else {
            //slow sequence remove
            Conj f = Conj.from(include);
            if (f.remove(e))
                return f.term();
            else
                return Null; //WTF?
        }

    }


    private static int indexOfZeroTerminated(byte[] b, byte val) {
        for (int i = 0; i < b.length; i++) {
            byte bi = b[i];
            if (val == bi)
                return i;
            else if (bi == 0)
                return -1;
        }
        return -1;
    }

    //TODO public static ObjectIntPair<Term> diffX(Term include, Term exclude) { //returns the resulting dt shift, replacing ConjDiff class

    private static Term conjSeq(TermBuilder B, LongObjectArraySet<Term> events) {
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

        int dt = (int) (events.when(center + 1) - firstWhen - left.eventRange());

        return !dtSpecial(dt) ? conjSeqFinal(B, dt, left, right) : conjoin(B, left, right, dt == DTERNAL);
    }

    private static Term conjSeqFinal(TermBuilder b, int dt, Term left, Term right) {
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
        }

        if (left.compareTo(right) > 0) {
            if (dt != DTERNAL)
                dt = -dt;
            Term t = right;
            right = left;
            left = t;
        }

//        if (left.op() == CONJ && right.op() == CONJ) {
//            int ldt = left.dt(), rdt = right.dt();
//            if (ldt != XTERNAL && !concurrent(ldt) && rdt != XTERNAL && !concurrent(rdt)) {
//                int ls = left.subs(), rs = right.subs();
//                if ((ls > 1 + rs) || (rs > ls)) {
//
//                    return terms.conj(dt, left, right);
//                }
//            }
//        }

        Term t = b.theCompound(CONJ, dt, left, right);
        //HACK temporary
//        if (((left instanceof Compound && right instanceof Compound && left.hasAny(CONJ) && right.hasAny(CONJ))
//                )
//                && t.anon().volume()!=t.volume()) {
//            try {
//                t = terms.conj(dt, left, right); //factorization, etc
//            } catch (StackOverflowError e) {
//                System.out.println("conj fail: (&&,dt, " + left + " , " + right + ')');
//                if (Param.DEBUG)
//                    throw new WTF(); //TEMPORARY
//            }
//        }


//        if (t.op()==CONJ && t.OR(tt -> tt.op()==NEG && tt.unneg().op()==CONJ)) {
//            //HACK verify
//            Term u = t.anon();
//            if (u!=t && (u.op()!=CONJ || u.volume()!=t.volume())) {
//                t = terms.conj(dt, left, right);
//            }
//        }

        //HACK sometimes this seems to happen
        if (t.hasAny(BOOL)) {
            if (t.contains(False))
                return False;
            if (t.contains(Null))
                return Null;
        }
        return t;
    }

    private static int conflictOrSame(Object e, byte id) {
        if (e instanceof byte[]) {
            byte[] b = (byte[]) e;
            for (byte bi : b) {
                if (bi == -id)
                    return -1;
                else if (bi == id)
                    return +1;
                else if (bi == 0)
                    break; //null terminator
            }
        } else if (e instanceof RoaringBitmap) {
            RoaringBitmap r = (RoaringBitmap) e;
            if (r.contains(-id))
                return -1;
            else if (r.contains(id))
                return +1;
        }
        return 0;
    }

    /**
     * merge an incoming target with a disjunctive sub-expression (occurring at same event time) reductions applied:
     * ...
     */
    private static Term disjunctify(Term existing, Term incoming, boolean eternal) {
        Term existingUnneg = existing.unneg();
        Term incomingUnneg = incoming.unneg();
        if (incoming.op() == NEG && incomingUnneg.op() == CONJ) {
            return disjunctionVsDisjunction(existingUnneg, incomingUnneg, eternal);
        } else {
            return disjunctionVsNonDisjunction(existingUnneg, incoming, eternal);
        }
    }

    private static Term disjunctionVsNonDisjunction(Term conjUnneg, Term incoming, boolean eternal) {
//        if (incoming.op()==CONJ)
//            throw new WTF(incoming + " should have been decomposed further");

        assert (conjUnneg.op() == CONJ);

        final Term[] result = new Term[1];
        conjUnneg.eventsWhile((when, what) -> {
            if (eternal || when == 0) {
                if (incoming.equalsNeg(what)) {
                    //overlap with the option so annihilate the entire disj
                    result[0] = True;
                    return false; //stop iterating
                } else if (incoming.equals(what)) {
                    //contradiction
                    result[0] = False;
                    //keep iterating, because possible total annihilation may follow.
                }
            }
            return eternal || when == 0;
        }, 0, true, true, eternal);


        if (result[0] == True) {
            return incoming; //disjunction totally annihilated by the incoming condition
        }

        int dt = eternal ? DTERNAL : 0;

        if (result[0] == False) {
            //removing the matching subterm from the disjunction and reconstruct it
            //then merge the incoming target


            if (!isSeq(conjUnneg)) {
                Term newConj = Conj.diffAll(conjUnneg, incoming);
                if (newConj.equals(conjUnneg))
                    return True; //no change

                if (newConj.equals(incoming)) //quick test
                    return False;

                return terms.conj(dt, newConj.neg(), incoming);

            } else {
                ConjBuilder c = new Conj();
                if (eternal) c.addAuto(conjUnneg);
                else c.add(0, conjUnneg);
                //c.factor();
                if (!eternal) {
                    boolean removed;
                    if (!(removed = c.remove(dt, incoming))) {
//                        //possibly absorbed in a factored eternal component TODO check if this is always the case
//                        if (c.eventOccurrences() > 1 && c.eventCount(ETERNAL) > 0) {
//                            //try again after distributing to be sure:
//                            //c.distribute();
//                            if (!(removed = c.remove(dt, incoming))) {
//                                return Null; //return True;
//                            }
//                        } else {
//                            return Null; //return True;
//                            //return null; //compatible
//                        }
                        return null;
                    }
                } else {
                    if (!c.removeAll(incoming)) {
                        return null; //compatible
                    }
                }
                Term newConjUnneg = c.term();
                long shift = c.shiftOrZero();

                Term newConj = newConjUnneg.neg();
                if (shift != 0) {
                    if (dt == DTERNAL)
                        dt = 0;

                    ConjLazy d = new ConjLazy(2);
                    d.add((long) dt, incoming);
                    d.add(shift, newConj);
                    return d.term();

                } else {
                    return conjoin(terms, incoming, newConj, eternal);
                }

            }


        }

        return null; //no interaction

    }

    private static Term disjunctionVsDisjunction(Term a, Term b, boolean eternal) {
        Conj aa = new Conj();
        if (eternal) aa.addAuto(a);
        else aa.add(0, a);
        aa.factor();
        //aa.distribute();

        Conj bb = newConjSharingTermMap(aa);
        if (eternal) bb.addAuto(b);
        else bb.add(0, b);
        bb.factor();
        //bb.distribute();

        Conj cc = intersect(aa, bb);
        if (cc == null) {
            //perfectly disjoint; OK
            return null;
        } else {
            Term A = aa.term();
            if (a == Null)
                return Null;
            Term B = bb.term();
            if (b == Null)
                return Null;
            long as = aa.shiftOrZero(), bs = bb.shiftOrZero();
            long abShift = Math.min(as, bs);
            ConjBuilder dd = newConjSharingTermMap(cc);
            if (eternal && as == 0 && bs == 0) {
                as = bs = ETERNAL;
            }
            if (dd.add(as, A.neg()))
                dd.add(bs, B.neg());

            Term D = dd.term();
            if (D == Null)
                return Null;

            if (cc.eventOccurrences() == 0)
                return D.neg();
            else {
//                if (cc.eventCount(ETERNAL) > 0 && !Conj.isSeq(A) && !Conj.isSeq(B))
//                    abShift = ETERNAL;
                if (eternal && as == ETERNAL && bs == ETERNAL)
                    abShift = ETERNAL;
                cc.add(abShift, D.neg());
                return cc.term().neg();
            }
        }
    }

    /**
     * produces a Conj instance containing the intersecting events.
     * null if no events in common and x and y were not modified.
     * erases contradiction (both cases) between both so may modify X and Y.  probably should .distribute() x and y first
     */
    @Nullable
    private static Conj intersect(Conj x, Conj y) {

        assert (x.termToId == y.termToId) : "x and y should share target map";

        MutableLongSet commonEvents = x.event.keySet().select(y.event::containsKey);
        if (commonEvents.isEmpty())
            return null;

        Conj c = newConjSharingTermMap(x);
        final boolean[] modified = {false};
        commonEvents.forEach(e -> {
            ByteSet xx = x.eventSet(e);
            ByteSet yy = y.eventSet(e);
            ByteSet common = xx.select(yy::contains);
            ByteSet contra = xx.select(xxx -> yy.contains((byte) -xxx));
            if (!common.isEmpty()) {
                common.forEach(cc -> {
                    c.add(e, x.unindex(cc));
                    boolean xr = x.remove(e, cc);
                    boolean yr = y.remove(e, cc);
                    assert (xr && yr);
                    modified[0] = true;
                });
            }
            if (!contra.isEmpty()) {
                contra.forEach(cc -> {
                    boolean xr = x.remove(e, cc, (byte) -cc);
                    boolean yr = y.remove(e, cc, (byte) -cc);
                    assert (xr && yr);
                    modified[0] = true;
                });
            }
        });
        return (!modified[0] && c.event.isEmpty()) ? null : c;
    }

    private static Term conjoinify(Term existing /* conj */, Term incoming, boolean eternal, boolean create) {

        int existingDT = existing.dt();
        int incomingDT = incoming.dt();

        if (existingDT == XTERNAL || incomingDT == XTERNAL)
            return null; //one or two xternal's, no way to know how they combine or not

//        if (existingDT == 0 && incomingDT == 0) {
//            assert(eternal);
//            //promote to parallel
//            eternal = false;
//        }

        int outerDT = eternal ? DTERNAL : 0;

        if (incoming.op() == CONJ) {

            if (eternal && (incomingDT != DTERNAL && existingDT != DTERNAL))
                return null; //dont change non-DTERNAL components in eternity

            if (incomingDT == DTERNAL || incomingDT == 0) {

                if (incomingDT == outerDT || existingDT == outerDT) {
                    //at least one of the terms has a DT matching the outer
                    return terms.conj(outerDT, existing, incoming);
                } else if (incomingDT == existing.dt()) {
                    if (outerDT == 0 && ((incomingDT == 0) || (incomingDT == DTERNAL))) {
                        //promote a parallel of two eternals or two parallels to one parallel
                        return terms.conj(incomingDT, existing, incoming);
                    }
                }
            }


            //two sequence-likes. maybe some preprocessing that can be applied here
            //otherwise just add the new event
            if (!Conj.isSeq(existing) && !Conj.isSeq(incoming))
                return null;

        }

        //quick contradiction test
        if (eternal || existingDT == 0) {
            if (existing.containsNeg(incoming))
                return False;
            if (incoming.op() != CONJ && existing.contains(incoming)) {
                return existing;
            }
        }

        ConjBuilder c = new ConjLazy();

        boolean ok = existing.eventsWhile((whn, wht) -> {
            Term ww = terms.conj(outerDT, wht, incoming);
            if (ww == Null)
                throw new WTF();
            else if (ww == False) {
                c.add(ETERNAL, False);
                return false;
            } else if (ww == True)
                return true;

//            try {
            return c.add(whn, ww);
//            } catch(StackOverflowError e) {
//                throw new WTF(); //TEMPORARY
//            }

        }, eternal ? ETERNAL : 0, true, true, false);
        if (!ok)
            return False;

        Term d = c.term();
        if (create)
            return d;
        else {
            if (d.op() != CONJ)
                return d;
            else
                return null; //potentially factor
        }
    }

    /**
     * @param existing
     * @param incoming
     * @param eternalOrParallel * True - absorb and ignore the incoming
     *                          * Null/False - short-circuit annihilation due to contradiction
     *                          * non-null - merged value.  if merged equals current item, as-if True returned. otherwise remove the current item, recurse and add the new merged one
     *                          * null - do nothing, no conflict.  add x to the event time
     * @param create            - whether this is being used for testing potential construction, or to actually create the conjunction.
     *                          can be used as a hint whether to proceed with full conjunction construction or not.
     */
    private static Term merge(Term existing, Term incoming, boolean eternalOrParallel, boolean create) {


        boolean incomingPolarity = incoming.op() != NEG;
        Term incomingUnneg = incomingPolarity ? incoming : incoming.unneg();
        Term existingUnneg = existing.unneg();
        boolean iConj = incomingUnneg.op() == CONJ;
        boolean eConj = existingUnneg.op() == CONJ;

        if (!eConj && !iConj)
            return null;  //OK neither a conj/disj

        boolean existingPolarity = existing.op() != NEG;

        if (!Term.commonStructure(existingUnneg.structure() & (~CONJ.bit), incomingUnneg.structure() & (~CONJ.bit)))
            return null; //OK no potential for interaction

        Term base;
        if (eConj && !iConj)
            base = existing;
        else if (iConj && !eConj)
            base = incoming;
        else if (!existingPolarity && incomingPolarity)
            base = existing; //forces disjunctify
        else if (!incomingPolarity && existingPolarity)
            base = incoming; //forces disjunctify
        else { //if (eConj && iConj) {
            assert (eConj && iConj && existingPolarity == incomingPolarity);
            //decide which is larger, swap for efficiency
            boolean swap = ((existingPolarity == incomingPolarity) && incoming.volume() > existing.volume());
            base = swap ? incoming : existing;
        }
//        else if (eConj && !iConj) {
//            base = existing;
//        } else /*if (xConj && !eConj)*/ {
//            base = incoming;
//        }

        boolean conjPolarity = base == existing ? existingPolarity : incomingPolarity;

        Term x = base == existing ? incoming : existing;
        Term result = conjPolarity ? conjoinify(base, x, eternalOrParallel, create) : disjunctify(base, x, eternalOrParallel);

        if (result != null && result.equals(existing))
            result = existing; //same value

        return result;
    }

    /**
     * stateless/fast 2-ary conjunction in either eternity (dt=DTERNAL) or parallel(dt=0) modes
     */
    public static Term conjoin(TermBuilder B, Term x, Term y, boolean eternalOrParallel) {

        if (x == Null || y == Null) return Null;

        if (x == False || y == False) return False;

        if (x == True) return y;
        if (y == True) return x;

        if (x.equals(y))
            return x; //exact same
        if (x.equalsNeg(y))
            return False; //contradiction

        Term xy = merge(x, y, eternalOrParallel, true);

        //decode result target
        if (xy == True) {
            return x; //x absorbs y
        } else if (xy == null) {
            return B.theCompound(CONJ, eternalOrParallel ? DTERNAL : 0, sorted(x, y));
        } else {
            //failure or some particular merge result
            return xy;
        }
    }

    private static boolean eventsContains(byte[] events, byte b) {
        return ArrayUtils.contains(events, b);
    }

    private static void events(byte[] events, ByteProcedure each) {
        for (byte e : events) {
            if (e != 0) {
                each.value(e);
            } else
                break; //null-terminator
        }
    }

    private static boolean eventsAND(Object events, BytePredicate each) {
        if (events instanceof byte[])
            return eventsAND((byte[]) events, each);
        else
            throw new TODO();
    }

//    public boolean addAt(Term t, long start, long end, int maxSamples, int minSegmentLength) {
//        if ((start == end) || start == ETERNAL) {
//            return addAt(start, t);
//        } else {
//            if (maxSamples == 1) {
//
//                return addAt((start + end) / 2L, t);
//            } else {
//
//                long dt = Math.max(minSegmentLength, (end - start) / maxSamples);
//                long x = start;
//                while (x < end) {
//                    if (!addAt(x, t))
//                        return false;
//                    x += dt;
//                }
//                return true;
//            }
//        }
//    }

    private static boolean eventsOR(Object events, BytePredicate each) {
        if (events instanceof byte[])
            return eventsOR((byte[]) events, each);
        else
            throw new TODO();
    }

    private static boolean eventsAND(byte[] events, BytePredicate each) {
        for (byte e : events) {
            if (e != 0) {
                if (!each.accept(e))
                    return false;
            } else
                break; //null-terminator
        }
        return true;
    }

    private static <X> boolean eventsANDwith(byte[] events, ByteObjectPredicate<X> each, X x) {
        for (byte e : events) {
            if (e != 0) {
                if (!each.accept(e, x))
                    return false;
            } else
                break; //null-terminator
        }
        return true;
    }

    private static <X> boolean eventsORwith(byte[] events, ByteObjectPredicate<X> each, X x) {
        for (byte e : events) {
            if (e != 0) {
                if (each.accept(e, x))
                    return true;
            } else
                break; //null-terminator
        }
        return false;
    }

    private static boolean eventsOR(byte[] events, BytePredicate each) {
        for (byte e : events) {
            if (e != 0) {
                if (each.accept(e))
                    return true;
            } else
                break; //null-terminator
        }
        return false;
    }

    private static boolean todoOrFalse() {
        if (Param.DEBUG)
            throw new TODO();
        else
            return false;
    }

    @Override
    public int eventCount(long when) {
        Object e = event.get(when);
        return e != null ? Conj.eventCount(e) : 0;
    }

    private boolean dropEvent(final Term event, boolean earlyOrLate, boolean filterContradiction) {
    /* check that event.neg doesnt occurr in the result.
        for use when deriving goals.
         since it would be absurd to goal the opposite just to reach the desired later
         */
        byte id = get(event);
        if (id == Byte.MIN_VALUE)
            return false; //not found


        long targetTime;
        if (this.event.size() == 1) {

            targetTime = this.event.keysView().longIterator().next();
        } else if (earlyOrLate) {
            Object eternalTemporarilyRemoved = this.event.remove(ETERNAL); //HACK
            targetTime = this.event.keysView().min();
            if (eternalTemporarilyRemoved != null) this.event.put(ETERNAL, eternalTemporarilyRemoved); //UNDO HACK
        } else {
            targetTime = this.event.keysView().max();
        }
        assert (targetTime != XTERNAL);

        if (filterContradiction) {


            byte idNeg = (byte) -id;

            final boolean[] contradiction = {false};
            this.event.forEachKeyValue((w, wh) -> {
                if (w == targetTime || contradiction[0])
                    return; //HACK should return early via predicate method

                if ((wh instanceof byte[] && ArrayUtils.indexOf((byte[]) wh, idNeg) != -1)
                        ||
                        (wh instanceof RoaringBitmap && ((RoaringBitmap) wh).contains(idNeg)))
                    contradiction[0] = true;
            });
            if (contradiction[0])
                return false;
        }

        return this.remove(targetTime, event);
    }

    @Override
    public void negateEvents() {
        event.forEachValue(x -> {
            if (!(x instanceof byte[]))
                throw new TODO();
            byte[] bx = (byte[]) x;
            for (int i = 0; i < bx.length; i++) {
                byte b = bx[i];
                if (b == 0) break; //null terminator
                bx[i] = (byte) -b;
            }
        });
    }

    /**
     * note this doesnt remove the terms which only appeared in the target time being removed
     */
    boolean removeAll(long when) {
        if (event.remove(when) != null) {
            this.result = null;
            return true;
        }
        return false;
    }

//    /**
//     * stage 2: a and b are both the inner conjunctions of disjunction terms being compared for contradiction or factorable commonalities.
//     * a is the existing disjunction which can be rewritten.  b is the incoming
//     */
//    private static Term OLD_disjunctionVsDisjunction(Term a, Term b, boolean eternal) {
//        int adt = a.dt(), bdt = b.dt();
//        boolean bothCommute = (adt == 0 || adt == DTERNAL) && (bdt == 0 || bdt == DTERNAL);
//        if (bothCommute) {
//            if ((adt == bdt || adt == DTERNAL || bdt == DTERNAL)) {
//                //factor out contradicting subterms
//                MutableSet<Term> aa = a.subterms().toSet();
//                MutableSet<Term> bb = b.subterms().toSet();
//                Iterator<Term> bbb = bb.iterator();
//                boolean change = false;
//                while (bbb.hasNext()) {
//                    Term bn = bbb.next();
//                    if (aa.remove(bn.neg())) {
//                        bbb.remove(); //both cases allowed; annihilate both
//                        change = true;
//                    }
//                }
//                if (change) {
//                    //reconstitute the two terms, glue them together as a new super-disjunction to replace the existing (and interrupt adding the incoming)
//
//                    Term A = terms.conj(adt, aa.toArray(EmptyTermArray)).neg();
//                    if (A == False || A == Null || (adt == bdt && aa.equals(bb)))
//                        return A;
//
//                    Term B = terms.conj(bdt, bb.toArray(EmptyTermArray)).neg();
//                    if (B == False || B == Null || A.equals(B))
//                        return B;
//
//                    return terms.conj(eternal ? DTERNAL : 0, A, B);
//                }
//            }
//            //return null; //no change
//            return Null;
//        } else {
////            //TODO factor out the common events, and rewrite the disjunction as an extra CONJ target with the two options (if they are compatible)
////            //HACK simple case
////            if (a.subs()==2 && b.subs()==2 && !a.subterms().hasAny(CONJ) && !b.subterms().hasAny(CONJ)){
////                Term common = a.sub(0);
////                if (common.equals(b.sub(0))) {
////                    Term ar = Conj.without(a, common, false);
////                    if (ar!=null) {
////                        int ac = a.subTimeFirst(common);
////                        if (ac!=DTERNAL) {
////                            int ao = a.subTimeFirst(ar);
////                            if (ao != DTERNAL) {
////                                Term br = Conj.without(b, common, false);
////                                if (br != null) {
////                                    int bc = b.subTimeFirst(common);
////                                    if (bc!=DTERNAL) {
////                                        int bo = b.subTimeFirst(br);
////                                        if (bo != DTERNAL) {
////                                            Term arOrBr = terms.conj((bo-bc) - (ao-ac), ar.neg(), br.neg()).neg();
////                                            Term y = terms.conj(-ac, common, arOrBr).neg(); //why neg
////                                            return y;
////                                        }
////                                    }
////                                }
////                            }
////                        }
////                    }
////                }
////            }
//
////            //HACK if the set of components is disjoint, allow
////            if (!a.subterms().hasAny(CONJ) && !b.subterms().hasAny(CONJ)) {
////                Subterms bs = b.subterms();
////                if (!a.subterms().OR(aa -> bs.contains(aa) || bs.containsNeg(aa)))
////                    return null;//allow
////            }
//
//            //TODO finer grained disjoint event comparison
//
//            //TODO sequence conditions
//            //throw new TODO(a + " vs " + b);
//            return Null; //disallow for now. the complexity might be excessive
//            //return null; //OK
//        }
//    }

    /**
     * TODO batch Term... variant
     */
    public boolean removeAll(Term what) {
        byte id = get(what);
        if (id != Byte.MIN_VALUE) {
            long[] events = event.keySet().toArray(); //create an array because removal will interrupt direct iteration of the keySet
            boolean removed = false;
            for (long e : events) {
                removed |= remove(e, id);
            }
            if (removed)
                this.result = null;
            return removed;
        }
        return false;
    }

    public void clear() {
        super.clear();
        event.clear();
        result = null;
    }

    int conflictOrSame(long at, byte what) {
        return conflictOrSame(event.get(at), what);
    }

    public final ConjBuilder with(long at, Term x) {
        add(at, x);
        return this;
    }

//    static boolean eventable(Term t) {
//        return !t.op().isAny(BOOL.bit | INT.bit | IMG.bit | NEG.bit);
//    }

    @Override
    public boolean add(long at, Term x) {
        if (this.result != null)
            throw new RuntimeException("already concluded: " + result);

        return added(ConjBuilder.super.add(at, x));
    }

    private boolean added(boolean success) {
        if (success)
            return true;
        else {
            if (result == null)
                result = False;
            return false;
        }
    }

    @Override
    public boolean addEvent(long at, Term x) {
//        if (Param.DEBUG) {
//            if (at == DTERNAL) //HACK
//                throw new WTF("probably meant at=ETERNAL not DTERNAL");
//        }

        if (x instanceof Bool) {
            //short circuits
            if (x == True)
                return true;
            else if (x == False) {
                this.result = False;
                return false;
            } else if (x == Null) {
                this.result = Null;
                return false;
            }
        }

//        if (x.op()==CONJ && x.dt()!=XTERNAL)
//            throw new WTF("why adding entire CONJ as event. should be decomposed");

        //test this first
        boolean polarity = x.op() != NEG;
        Term xUnneg = polarity ? x : x.unneg();

        if (at != ETERNAL && !polarity && xUnneg.op() == CONJ && xUnneg.dt() == DTERNAL) {
            //convert a sequenced negated eternal conjunction to parallel
            xUnneg = ((Compound) xUnneg).dt(0);
            x = xUnneg.neg();
        }

        byte id = add(xUnneg);
        if (!polarity) id = (byte) -id;

        //quick test for conflict with existing ETERNALs
        int c = conflictOrSame(ETERNAL, id);
        if (c > 0)
            return true;
        else if (c < 0) {
            result = False;
            return false;
        }

        switch (filterAdd(at, id, x)) {
            case +1:
                return true; //ignore and continue
            case 0:
                return addEvent(at, id, x); //continue
            case -1:
                return false; //reject and fail
            default:
                throw new UnsupportedOperationException();
        }

    }

    private boolean addEvent(long at, byte id) {
        Term xx = unindex(id);
        return addEvent(at, id, xx);
    }

    private boolean addEvent(long at, byte id, Term x) {

        Object events = event.getIfAbsentPut(at, () -> new byte[ROARING_UPGRADE_THRESH]);
        if (events instanceof byte[]) {
            byte[] b = (byte[]) events;

            //quick test for exact absorb/contradict
            if (b[0] != 0) {
                for (byte bi : b) {
                    if (bi == 0)
                        break;
                    if (id == -bi)
                        return false; //contradiction
                    if (id == bi)
                        return true; //found existing
                }
            }

            for (int i = 0; i < b.length; i++) {
                byte bi = b[i];
                if (bi == 0) {
                    //empty slot, take
                    //  assert(ArrayUtils.indexOf(b, (byte)-id)==-1); basic verification test

                    b[i] = id;
                    return true;
                } else {

                    Term result = merge(unindex(bi), x, at == ETERNAL, false);

                    if (result != null) {
                        if (result == True)
                            return true; //absorbed input
                        if (result == False || result == Null) {
                            this.result = result; //failure
                            return false;
                        } else {
                            if (i < b.length - 1) {
                                arraycopy(b, i + 1, b, i, b.length - 1 - i);
                                i--; //compactify
                            } else
                                b[i] = 0; //erase, continue comparing. the result remains eligible for add

                            return add(at, result);
                        }
                    }
                }
            }

            //no remaining capacity, upgrade to RoaringBitmap

            RoaringBitmap rb = new RoaringBitmap();
            for (byte bb : b)
                rb.add(bb);
            rb.add(id);
            event.put(at, rb);


            return true;
        } else {
            return todoOrFalse();
        }
    }

    /**
     * allows subclass implement different behavior.
     * <p>
     * return:
     * -1: ignore and fail the conjunction
     * 0: default, continue
     * +1: ignore and continue
     */
    int filterAdd(long at, byte id, Term x) {
        return 0;
    }

    /**
     * # of unique event occurrence times
     */
    @Override
    public int eventOccurrences() {
        return event.size();
    }

    private ByteSet eventSet(long e) {
        Object ee = event.get(e);
        if (ee == null)
            return ByteSets.immutable.empty();
        if (!(ee instanceof byte[]))
            throw new TODO();
        byte[] eee = (byte[]) ee;
        int ec = eventCount(eee);
        assert (ec > 0);
        if (ec == 1)
            return ByteSets.immutable.of(eee[0]);
        else if (ec == 2)
            return ByteSets.immutable.of(eee[0], eee[1]);
        else {
            ByteHashSet b = new ByteHashSet(ec);
            events(eee, b::add);
            return b;
        }
    }

    /**
     * @return non-zero byte value
     */
    private byte add(Term t) {
//        if (!(t != null && eventable(t))) //eventable
//            throw new WTF(t + " is not valid event in " + Conj.class);

        return termToId.getIfAbsentPutWithKey(t, tt -> {
            int s = idToTerm.addAndGetSize(tt);
            assert (s < Byte.MAX_VALUE);
            return (byte) s;
        });
    }

    /**
     * returns index of an item if it is present, or -1 if not
     */
    private byte index(Term t) {
        return termToId.getIfAbsent(t.unneg(), (byte) -1);
    }

    private Term unindex(byte id) {
        Term x = idToTerm.get(Math.abs(id) - 1);
        if (x == null)
            throw new NullPointerException();
        return x.negIf(id < 0);
    }

    private byte get(Term x) {
        boolean neg;
        if (neg = (x.op() == NEG))
            x = x.unneg();
        byte index = index(x);
        if (index == -1)
            return Byte.MIN_VALUE;

        if (neg)
            index = (byte) (-index);

        return index;
    }

    @Override
    public boolean remove(long at, Term t) {
        if (t.op()!=CONJ) {
            byte i = get(t);
            if (i == Byte.MIN_VALUE)
                return false;
            return remove(at, i);
        } else {
            return t.eventsWhile((when,what)->{
                assert(!t.equals(what)); //prevent infinite recursion, hopefully this cant happen
                return remove(when, what); //fails on error
            }, at, true, true, false);
        }
    }

    private boolean remove(long at, byte... what) {

        Object o = event.get(at);
        if (o == null)
            return false;


        if (removeFromEvent(at, o, true, what) != 0) {
            result = null;
            return true;
        }
        return false;
    }

    /**
     * returns:
     * +2 removed, and now this event time is empty
     * +1 removed
     * +0 not removed
     */
    private int removeFromEvent(long at, Object o, boolean autoRemoveIfEmpty, byte... i) {
        if (o instanceof RoaringBitmap) {
            boolean b = false;
            RoaringBitmap oo = (RoaringBitmap) o;
            for (int ii : i)
                b |= oo.checkedRemove(ii);
            if (!b) return 0;
            if (oo.isEmpty()) {
                if (autoRemoveIfEmpty)
                    event.remove(at);
                return 2;
            } else {
                return 1;
            }
        } else {
            byte[] b = (byte[]) o;

            int num = ArrayUtils.indexOf(b, (byte) 0);
            if (num == -1) num = b.length;

            int removals = 0;
            for (int ii : i) {
                assert (ii != 0);
                int bi = ArrayUtils.indexOf(b, (byte) ii, 0, num);
                if (bi != -1) {
                    //if (b[bi] != 0) {
                    b[bi] = 0;
                    removals++;
                    //}
                }
            }

            if (removals == 0)
                return 0;
            else if (removals == num) {
                if (autoRemoveIfEmpty)
                    event.remove(at);
                return 2;
            } else {


                //sort all zeros to the end
                ArrayUtils.sort(b, 0, num - 1, (byte x) -> x == 0 ? Byte.MIN_VALUE : x);

//                MetalBitSet toRemove = MetalBitSet.bits(b.length);
//
//                for (int zeroIndex = 0; zeroIndex < b.length; zeroIndex++) {
//                    if (b[zeroIndex] == 0)
//                        toRemove.setAt(zeroIndex);
//                }
//
//                b = ArrayUtils.removeAll(b, toRemove);
//                event.put(at, b);
                return 1;
            }
        }
    }

    public boolean removeEventsByTerm(Term t, boolean pos, boolean neg) {

        boolean negateInput;
        if (t.op() == NEG) {
            negateInput = true;
            t = t.unneg();
        } else {
            negateInput = false;
        }

        byte i = get(t);
        if (i == Byte.MIN_VALUE)
            return false;

        byte[] ii;
        if (pos && neg) {
            ii = new byte[]{i, (byte) -i};
        } else if (pos) {
            ii = new byte[]{negateInput ? (byte) -i : i};
        } else if (neg) {
            ii = new byte[]{negateInput ? i : (byte) -i};
        } else {
            throw new UnsupportedOperationException();
        }


        final boolean[] removed = {false};
        LongArrayList eventsToRemove = new LongArrayList(4);
        event.forEachKeyValue((when, o) -> {
            int result = removeFromEvent(when, o, false, ii);
            if (result == 2) {
                eventsToRemove.add(when);
            }
            removed[0] |= result > 0;
        });

        if (!eventsToRemove.isEmpty()) {
            eventsToRemove.forEach(event::remove);
        }

        if (removed[0]) {
            result = null;
            return true;
        }
        return false;
    }

    @Override
    public Term term(TermBuilder B) {
        if (result != null)
            return result;

        if (event.isEmpty())
            return True;

        factor();

        int numOcc = eventOccurrences();
        int numTmpOcc = numOcc;


        List<Term> tmp = new FasterList(2);
        Term eternal;
        if (event.containsKey(ETERNAL)) {
            numTmpOcc--;
            eternal = term(ETERNAL, tmp);
            if (eternal != null) {
                if (eternal == False || eternal == Null)
                    return this.result = eternal;
            }
        } else {
            eternal = null;
        }

        Term temporal;
        Term ci;
        switch (numTmpOcc) {
            case 0:
                temporal = null;
                break;
//            case 1:
//                //one other non-eternal time
//                LongObjectPair<Object> e = event.keyValuesView().select(x -> x.getOne() != ETERNAL).getFirst();
//                Term t = target(e.getOne(), e.getTwo(), tmp);
//                if (t == null || t == True) {
//                    t = null;
//                } else if (t == False) {
//                    return this.result = False;
//                } else if (t == Null) {
//                    return this.result = Null;
//                }
//                if (t != null) {
//                    if (eternal != null && e.getOne() == 0) {
//                        boolean econj = eternal.op() == CONJ;
//                        if (!econj || eternal.dt() == DTERNAL) {
//                            //promote eternal to parallel
//                            if (!econj) {
//                                return ConjCommutive.the(0, eternal, t);
//                            } else {
//                                List<Term> ecl = new FasterList(eternal.subs() + 1);
//                                eternal.subterms().addTo(ecl);
//                                ecl.addAt(t);
//                                return ConjCommutive.the(0, ecl);
//                            }
//                        }
//                    }
//                }
//                temporal = t;
//                break;
            default:
                LongObjectArraySet<Term> temporals = null;

                int occSkipped = 0;
                for (LongObjectPair<?> next : event.keyValuesView()) {
                    long when = next.getOne();
                    if (when == ETERNAL) {
                        occSkipped++;
                    } else {

                        Term wt = term(when, next.getTwo(), tmp);
                        if (wt == False || wt == Null) {
                            return this.result = wt;
                        } else if (wt != True && wt != null) {

                            if (temporals == null)
                                temporals = new LongObjectArraySet<>((numOcc - occSkipped) * 2 /* estimate */);

                            temporals.add(when, wt);

                        } else {
                            occSkipped++;
                        }
                    }
                }
                if (temporals != null) {

                    int ee = temporals.size();
                    switch (ee) {
                        case 0:
                            temporal = null;
                            break;
                        case 1:
                            temporal = temporals.get(0);
                            break;
                        default:
                            temporals.sortThis();
                            temporal = conjSeq(B, temporals);
                            break;
                    }
                } else
                    temporal = null;
                break;
        }


        if (eternal != null && temporal != null) {
            if (eternal == True)
                ci = temporal;
            else {

                if (temporal.equals(eternal))
                    return eternal;

                if (temporal.equalsNeg(eternal))
                    return False;

//                //both conj or disj sequences?
//                if (((eternal.op()==CONJ && eternal.dt()!=DTERNAL) && Conj.isSeq(temporal.unneg())))
//                    return Null; //too complex

                //temporal disjunction sequence AND eternal component?
//                if (Conj.isSeq(temporal.unneg()) && (eternal.unneg().op()==CONJ || temporal.containsRecursively(eternal.unneg())))
//                    return Null; //too complex

//                if (temporal.op() == CONJ && Term.commonStructure(eternal, temporal)) {
//                    if (Conj.isSeq(temporal) || eternal.op() == CONJ) {
//
//                        if (!eternal.eventsWhile((ewhen, ewhat) -> {
//                            //exhaustively check events for conflict
//                            Term nEternal = ewhat.neg();
//                            return temporal.eventsWhile((when, what) -> {
//                                return !what.equals(nEternal);
//                            }, 0, true, true, true);
//                        }, 0, true, true, true))
//                            return False;
//
//                    } else {
//                        if (temporal.containsNeg(eternal))
//                            return False; //HACK
//                        else if (temporal.contains(eternal))
//                            return temporal;  //HACK
//                    }
//                }


                int tdt = temporal.dt();
                int edt = eternal.dt();
                if ((temporal.unneg().op() == CONJ && (tdt == DTERNAL || tdt == 0)) || (eternal.op() == CONJ && (edt == DTERNAL || edt == 0)) || temporal.containsRecursively(eternal.unneg()))
                    return B.conj(DTERNAL, temporal, eternal); //needs flatten
                else {
                    return B.theCompound(CONJ, DTERNAL, sorted(temporal, eternal));
                }
            }
        } else if (eternal == null) {
            ci = temporal;
        } else /*if (temporal == null)*/ {
            ci = eternal;
        }

        if (ci == null)
            return True;

        return ci;
    }
//
//    private static void flattenInto(Collection<Term> ee, Term ex, int dt) {
//        if (ex.op() == CONJ && ex.dt() == dt)
//            ex.subterms().forEach(eee -> flattenInto(ee, eee, dt));
//        else
//            ee.addAt(ex);
//    }

    /**
     * factor common temporal event components to an ETERNAL component
     * returns true if possibly changed
     */
    public boolean factor() {

        if (eventOccurrences() <= 1)
            return false;

        List<LongObjectPair<Object>> events = event.keyValuesView().toList();

        int numTemporalCompoundEvents = 0, numTemporalEvents = 0;

        for (Iterator<LongObjectPair<Object>> iterator = events.iterator(); iterator.hasNext(); ) {
            LongObjectPair<Object> l = iterator.next();
            boolean temporal = l.getOne() != ETERNAL;
            if (temporal) {
                numTemporalEvents++;

                if (eventCount(l.getTwo()) > 1)
                    numTemporalCompoundEvents++;
            } else {
                iterator.remove();
            }
        }
        if ((numTemporalCompoundEvents <= 1) || (numTemporalCompoundEvents != numTemporalEvents))
            return false;


        ByteHashSet common = null;
        ByteProcedure commonsAdd = null;
        //TODO if this is iterated in order of least # of events at each time first, it is optimal
        for (LongObjectPair<Object> whenWhat : events) {
            long when = whenWhat.getOne();
            if (when == ETERNAL)
                return true;
            Object what = whenWhat.getTwo();
            if (what instanceof byte[]) {
                byte[] bWhat = (byte[]) what;
                if (common == null || common.isEmpty()) {
                    //add the first set of events
                    if (common == null) {
                        common = new ByteHashSet(bWhat.length);
                        commonsAdd = common::add;
                    }
                    events(bWhat, commonsAdd);
                } else {
                    if (common.removeIf(c -> !eventsContains(bWhat, c))) {
                        //done
                        if (common.isEmpty())
                            return false; //all are eliminated
                                // break;
                    }
                }

            } else {
                throw new TODO();
            }
        }

        int commonCount = common!=null ? common.size() : 0;
        if (0 == commonCount)
            return false;

        BytePredicate commonContains = common::contains;


        int e = 0;
        for (LongObjectPair<Object> whenWhat : events) {
            long when = whenWhat.getOne();  //assert(when!=ETERNAL);
            Object what = whenWhat.getTwo();

            if (what instanceof byte[]) {
                if (eventsAND(((byte[]) what), commonContains)) {
                    return false; //all would be eliminated at this time slot
                }
            } else
                throw new TODO();
        }


        MutableByteIterator ii = common.byteIterator();
        while (ii.hasNext()) {
            byte f = ii.next();
            boolean added = addEvent(ETERNAL, f); assert(added);
            for (LongObjectPair<Object> whenWhat : events) {
                boolean removed = remove(whenWhat.getOne(), f); assert(removed);
            }
        }

        return true;
    }

    @Override
    public LongIterator eventOccIterator() {
        return event.keysView().longIterator();
    }

    public Term term(long when) {
        return term(when, new FasterList(1));
    }


//    private Term target(long when, Object what) {
//        return target(when, what, null);
//    }

    private Term term(long when, List<Term> tmp) {
        return term(when, event.get(when), tmp);
    }

    private int term(Object what, Consumer<Term> each) {
        if (what == null)
            return 0;

        if (what instanceof byte[]) {
            //rb = null;
            final byte[] b = (byte[]) what;
            int k = 0;
            for (byte x : b) {
                if (x == 0)
                    break; //null-terminator reached
                each.accept(unindex(x));
                k++;
            }
            return k;
        } else {
            throw new TODO();
        }

    }

    private Term term(long when, Object what, List<Term> tmp) {

        tmp.clear();

        int n = term(what, tmp::add);
        if (n == 0)
            return null;

        int ts = tmp.size();
        switch (ts) {
            case 0:
                return null;
            case 1:
                return tmp.get(0);
            default: {
//                if (when==ETERNAL && ((FasterList<Term>)tmp).count(Conj::isSeq)>1)
//                    return Null; //too complex

                return terms.theSortedCompound(CONJ, when == ETERNAL ? DTERNAL : 0, tmp);
            }

        }
    }

    private Term unindex(int termIndex, @Nullable boolean[] negatives) {
        assert (termIndex != 0);

        boolean neg = termIndex < 0;
        if (neg)
            termIndex = -termIndex;

        Term c = idToTerm.get(termIndex - 1);
        if (neg) {
            c = c.neg();
            if (negatives != null)
                negatives[0] = true;
        }
        return c;
    }

//    public boolean addDithered(Term target, long start, long end, int maxSamples, int minSegmentLength, NAR nar) {
//        if (start != ETERNAL) {
//            int d = nar.timeResolution.intValue();
//            if (d != 1) {
//                start = Tense.dither(start, d);
//                end = Tense.dither(end, d);
//            }
//        }
//        return addAt(target, start, end, maxSamples, minSegmentLength);
//    }

    /**
     * opposite of factor; 'multiplies' all temporal components with any eternal components
     */
    public void distribute() {

        int occ = eventOccurrences();
        if (occ <= 1)
            return;

        if (eventCount(ETERNAL) == 0)
            return;

        Term ete = term(ETERNAL);

        removeAll(ETERNAL);

        //replace all terms (except the eternal components removed) with the distributed forms

        this.idToTerm.replaceAll((x) -> {
            if (!x.equals(ete)) {
                Term y = CONJ.the(x, ete);
                if (!y.equals(x)) {
                    byte id = termToId.removeKeyIfAbsent(x, Byte.MIN_VALUE);
                    assert (id != Byte.MIN_VALUE);
                    termToId.put(y, id);
                    return y;
                }
            }
            return x;
        });
    }


//    /**
//     * inverse of addAt
//     */
//    public void subtract(long w, Term x) {
//        this.result = null;
//
//        if (x.op()==CONJ) {
//            x.eventsWhile((ww, xx)->{
//                remove(ww, xx);
//                return true;
//            }, w, true, true, false);
//        } else {
//            remove(w, x);
//        }
//    }

    //    private static void addAllTo(ByteHashSet common, Object o) {
//        if (o instanceof byte[])
//            common.addAll((byte[]) o);
//        else {
//            RoaringBitmap r = (RoaringBitmap) o;
//            r.forEach((int x) -> common.addAt((byte) x));
//        }
//    }


}
