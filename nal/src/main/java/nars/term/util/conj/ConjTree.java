package nars.term.util.conj;

import jcog.TODO;
import jcog.Util;
import jcog.WTF;
import jcog.data.list.FasterList;
import nars.NAL;
import nars.subterm.DisposableTermList;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.term.atom.IdempotentBool;
import nars.term.util.TermException;
import nars.term.util.TermTransformException;
import nars.term.util.builder.TermBuilder;
import nars.time.Tense;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;

import static nars.Op.CONJ;
import static nars.Op.NEG;
import static nars.term.atom.IdempotentBool.*;
import static nars.time.Tense.*;

/**
 * conj tree node
 */
public class ConjTree implements ConjBuilder {
    private IntObjectHashMap<ConjTree> seq = null;
    final Set<Term> pos = newSet();
    final Set<Term> neg = newSet();
    Term terminal = null;
    private long shift = TIMELESS;


    boolean addParallel(Term x) {


        if (terminal != null)
            throw new UnsupportedOperationException();
            //return false;


        if (x == True)
            return true;
        if (x == False || x == Null) {
            terminate(x);
            return false;
        }

        return result(x instanceof Neg ? addParallelN(x) : addParallelP(x));
    }

    private boolean addParallelNeg(Term x) {
        return addParallel(x.neg());
    }

    private boolean addParallelP(Term p) {
        assert (!(p instanceof Neg));

        if (p instanceof Compound && p.op() == CONJ) {
            if (p.dt() != XTERNAL && !Conj.isSeq(p))
                return ((Compound)p).AND(this::addParallel); //decompose parallel conj
            if (!validConj(p, false)) {
                terminate(False);
                return false;
            }
        } else if (pos.contains(p))
            return true;

//        if (seq!=null) {
//            for (ConjTree ts : seq.values())
//                if (ts.neg.contains(p))
//                    return false;
//        }

        if (!neg.isEmpty()) {
            p = reducePN(p, neg, false); 
            if (p instanceof Neg)
                return addParallelN(p); //became negative

            if (p == True)
                return true; //absorbed
            else if (p == False || p == Null) {
                terminate(p);
                return false; /*conflict */
            }
        }

        //assert (p.op() != NEG);
        pos.add(p);
        return true;
    }

    private boolean validConj(Term conj, boolean invert) {

        //assert(conj.op()==CONJ);

        Set<Term> pos, neg;
        if (invert) {
            pos = this.neg;
            neg = this.pos;
        } else {
            pos = this.pos;
            neg = this.neg;
        }

        return !(!neg.isEmpty() || (conj.hasAny(NEG) && !pos.isEmpty())) ||
                conj.eventsAND(new LongObjectPredicate<Term>() {
                                   @Override
                                   public boolean accept(long when, Term what) {
                                       return what instanceof Neg ? !pos.contains(what.unneg()) : !neg.contains(what);
                                   }
                               },
                        0L, true, true);
    }

    private boolean addParallelN(Term n) {
        assert (n instanceof Neg);
        Term nu = n.unneg();

        if (neg.contains(nu))
            return true;

//        if (seq!=null) {
//            for (ConjTree ts : seq.values())
//                if (ts.pos.contains(nu))
//                    return false;
//        }

        if (!pos.isEmpty()) {
            nu = reducePN(nu, pos, true); 
            if (nu instanceof Neg)
                return addParallelP(nu.unneg()); //became positive
        }

        if (!neg.isEmpty() && !(nu instanceof IdempotentBool)) {
            nu = reduceNegNeg(nu);

            if (nu instanceof Neg)
                return addParallelP(nu.unneg()); //became positive
        }


        if (nu == True)
            return true; //absorbed
        else if (nu == False || nu == Null) {
            terminate(nu);
            return false; /*conflict */
        } else {
            //assert (nu.op() != NEG);
            neg.add(nu);
            return true;
        }
    }

    private static Set<Term> newSet() {
    //    return new ArrayHashSet<>(4);
//        return new ArrayHashSet<>(4) {
//            @Override
//            public boolean add(Term x) {
//                System.out.println("+ " + x);
//                return super.add(x);
//            }
//
//            /** incomplete */
//            @Override public boolean remove(Object x) {
//                System.out.println("- " + x);
//                return super.remove(x);
//            }
//        };

        //return new LinkedHashSet<>(4, 0.99f);
        return new UnifiedSet<>(2, 0.99f);
        //return new HashSet<>(4);
    }


    /**
     * @param nx
     * @param neg
     * @return
     */
    private Term reduceNegNeg(Term nx) {
        //assert (nx.op() != NEG);


        boolean xConj = nx.opID() == (int) CONJ.id;

        FasterList<Term> toAdd = null;
        Term nxn = null;
        for (Iterator<Term> nyi = neg.iterator(); nyi.hasNext(); ) {
            Term ny = nyi.next();
            boolean yConj = ny.opID() == (int) CONJ.id;
            //int nys = ny.structure();
            //disj
            //                //Robbins Algebra / Huntington Reduction
            //                if (xConj && (nx.hasAny(NEG) || ny.hasAny(NEG)) && Term.commonStructure(nx.subterms().structure(), ny.subterms().structure())) {
            //                    ConjList nxe = ConjList.events(nx);
            //                    ConjList nye = ConjList.events(ny);
            //                    //symmetric difference
            //                    if (nxe.removeIf((LongObjectPredicate<Term>) nye::removeNeg)) {
            //                        if (nxe.isEmpty()) {
            //                            //XOR ??? TODO check that this shouldnt be eliminated
            //                        } else {
            //                            nyi.remove();
            //
            //                            nx = nxe.term();  nxs = nx.structure(); //the intersection
            //
            //                            if (nx == False || nx == Null)
            //                                return nx; //shouldnt happen
            //
            //                            long nxshift = nxe.shift();
            //                            if (nxs == ETERNAL || nxshift == 0) {
            //                                //continue, adding at present time
            //                            } else {
            //                                //add at shifted time
            //                                if (!add(nxshift, nx))
            //                                    return False;
            //                                break;
            //                            }
            //                        }
            //                    }
            //                }
            //
            if (yConj) {
                if (Conj.eventOf(ny, nx)) {
                    //short-circuit
                    nyi.remove();
                    continue;
                }
                if (nxn == null) nxn = nx.neg();

                if (Conj.eventOf(ny, nxn)) {
                    //prune
                    nyi.remove();
                    if (toAdd == null) toAdd = new FasterList(1);
                    Term z = Conj.diffAll(ny, nxn);
                    if (z == True) {
                        //eliminated y
                    } else if (z == False || z == Null) {
                        terminate(z);
                        return z;
                    } else
                        toAdd.add(z);
                    continue;
                }
            }
            if (xConj) {
                ///return True; //absorbed necessary
                if (Conj.eventOf(nx, ny))
                    return ny; //reduce nx to ny, so that it can be added at the correct sequence position.  for parallel this isnt needed and could return True?
                Term nyn = ny.neg();

                if (nx.dt()==XTERNAL) {
                    //HACK because ConjList.events doesnt decompose XTERNAL
                    Term nx2 = Conj.diffPar(nx, nyn);
                    if (!nx.equals(nx2)) {
                        nx = nx2;
                        if (nx instanceof IdempotentBool)
                            return nx;
                        Term x = reinsertNN(nx, toAdd, 0L);
                        if (x != null)
                            return x;
                    }
                } else if (Conj.eventOf(nx, nyn)) {
                    ConjList nxe = ConjList.events(nx);
                    nxe.removeAll(nyn);
                    nx = nxe.term();
                    nxn = null; //invalidate
                    if (nx instanceof IdempotentBool)
                        return nx;
                    long nxshift = nxe.shift();
                    Term x = reinsertNN(nx, toAdd, nxshift);
                    if (x != null)
                        return x;
                }

            }

        }
        if (toAdd != null) if (!toAdd.allSatisfy(this::addParallelNeg))
            return False;
        return nx;
    }

    private @Nullable Term reinsertNN(Term nx, FasterList<Term> toAdd, long nxshift) {
        //add at shifted time
        if (nxshift == ETERNAL || nxshift == 0L) {
            //continue, adding at present parallel time
        } else {
            if (!addEvent(nxshift, nx.neg()))
                return False;
            else {
                assert(toAdd == null): "TODO";
                return True;
            }
        }
        return null;
    }

    private Term reducePN(Term x, Collection<Term> y, boolean nP_or_pN) {

        //assert (x.op() != NEG);

        if (y.contains(x))
            return False; //contradiction


        boolean xConj = x instanceof Compound && x.opID() == (int) CONJ.id;
        if (xConj && nP_or_pN) {
            for (Term yy : y)
                if (Conj.eventOf(x, yy, -1))
                    return yy.neg();
                else {
                    Term z = Conj.diffAll(x, yy);
                    if (!z.equals(x)) {
                        if (z instanceof IdempotentBool)
                            return z.neg();
                        x = z;
                        if (x.op()!=CONJ)
                            break;
                    }
                }
        }


        if (!nP_or_pN) {

            Term xn = null;
            FasterList<Term> add = null;
            for (Iterator<Term> iterator = y.iterator(); iterator.hasNext(); ) {
                Term yy = iterator.next();
                // && yy.containsRecursively(x)) {
                //short-circuit
                //                    }
                if (yy.op() == CONJ) {
                    if (xn == null) xn = x.neg();
                    if (Conj.eventOf(yy, xn))
                        iterator.remove();
                    else {
                        //impossibility
                        Term z = Conj.diffAll(yy, x);
                        if (!z.equals(yy)) {

                            //if (Conj.eventOf(yy, b)) {
                            iterator.remove();
                            //Term z = Conj.diffAll(yy, b);
                            if (z == True) {
                                //only remove
                            } else if (z == False || z == Null)
                                return z;
                            else {
                                if (add == null) add = new FasterList<>(1);
                                add.add(z);
                            }

                        }
                    }
                }

            }

            if (add != null) if (!add.allSatisfy(this::addParallelNeg))
                return False;
        }

        return x;
    }

    Term terminate(Term t) {
        Term x = terminal;
        if (t==Null) {
            x = terminal = Null;
        } else if (t == False) {
            if (x != Null)
                x = terminal = False;
        } else
            throw new WTF();
        return x;
    }

    @Override
    public boolean addEvent(long at, Term x) {
        return terminal == null && (at == ETERNAL ? addParallel(x) : addAt(at, x));
    }


    /**
     * this can not eliminate matching parallel pos/neg content until after a sequence is given a chance to be defined
     * otherwise it could erase the sequence before it even becomes factorable.
     */
    private boolean addAt(long at, Term x) {
        if (!(x instanceof Neg)) {

            if (neg.contains(x)) {
                terminate(False); //contradicted
                return false;
            }
                if (!validConj(x,false)) {
                    terminate(False); //contradicted
                    return false;
                }
//            if (!neg.isEmpty() && !(x instanceof Bool)) {
//                x = reducePN(x, neg, false);
//            }

        } else {
            Term _xu = x.unneg();
            Term xu = _xu;
//
            if (pos.contains(xu)) {
                terminate(False); //contradict
                return false;
            }

//            if (!validConj(xu,true)) {
//                terminate(False); //contradicted
//                return false;
//            }

            //                if (xu.op()==NEG)
            //                    return addAt(at, xu.unneg()); //inverted
            if (!pos.isEmpty() && !(xu instanceof IdempotentBool)) {
                xu = reducePN(xu, pos, true); 
                if (xu instanceof Neg) {
                    return addAt(at,xu.unneg());
                }
            }

            if (!neg.isEmpty() && !(xu instanceof IdempotentBool)) {
                xu = reduceNegNeg(xu);
            }

            if (_xu != xu)
                x = xu.neg(); //HACK
        }

        if (x == True)
            return true; //absorbed
        else if (x == False || x == Null) {
            terminate(x);
            return false; /*conflict */
        }

        if (seq == null) seq = new IntObjectHashMap<>(2);

        return result(seq.getIfAbsentPut(Tense.occToDT(at), new Function0<ConjTree>() {
            @Override
            public ConjTree value() {
                shift = TIMELESS; //invalidate
                return new ConjTree();
            }
        }).addParallel(x));
    }

    private boolean result(boolean result) {
        if (!result)
            terminate(False);
        return result;
    }

    @Override
    public boolean remove(long at, Term t) {
        if (at == ETERNAL) {

            if (t instanceof Neg ? negRemove(t.unneg()) : posRemove(t)) {
                shift = TIMELESS;
                return true;
            }

        } else {
            int aat = occToDT(at);
            ConjTree s = seq.get(aat);
            if (s == null) return false;
            if (s.remove(ETERNAL, t)) {
                if (s.isEmpty()) {
                    seq.remove(aat);
                    shift = TIMELESS; //invalidate
                    if (seq.isEmpty()) seq = null;
                }
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    boolean posRemove(Term t) {
        return pos.remove(t);
    }

    boolean negRemove(Term t) {
        return neg.remove(t);
    }

    @Override
    public int eventOccurrences() {
        return ((!pos.isEmpty() || !neg.isEmpty()) ? 1 : 0) + (seq != null ? seq.size() : 0);
    }

    @Override
    public int eventCount(long when) {
        if (when == ETERNAL)
            return (!pos.isEmpty() ? pos.size() : 0) + (!neg.isEmpty() ? neg.size() : 0);
        else {
            if (seq != null) {
                ConjTree s = seq.get(occToDT(when));
                if (s != null)
                    return s.size();
            }
            return 0;
        }
    }

    @Override
    public long shift() {
        if (shift == TIMELESS)
            shift = (seq == null || seq.isEmpty()) ? (!pos.isEmpty() || !neg.isEmpty() ? ETERNAL : TIMELESS) : (long) seq.keysView().min();
        return shift;
    }

    @Override
    public boolean removeAll(Term term) {
        boolean removed = removeParallel(term);

        if (seq != null) if (!removed) removed = seq.countWith(ConjTree::removeAll, term) > 0;
        else
            seq.forEachWith(ConjTree::removeAll, term);

        return removed;
    }

    private boolean removeParallel(Term term) {
        if (terminal != null)
            throw new UnsupportedOperationException();

        return term instanceof Neg ? negRemove(term) : posRemove(term);
    }

    @Override
    public void negateEvents() {
        throw new TODO();
    }


    private int size() {
        return pos.size() +
                neg.size() +
                (seq != null ? (int) seq.sumOfInt(ConjTree::size) : 0);
    }

    @Override
    public Term term(TermBuilder B) {
        if (terminal != null)
            return terminal;

        if (seq != null && !seq.isEmpty()) {


            Term s = termSeq(B);

            shift(); //cache the shift before clearing seq
            seq = null;
            //seq.clear();

            if (terminal != null)
                return terminal;

            return s;
        } else
            return termCom(B);

    }

    @Override
    public void clear() {
        pos.clear();
        neg.clear();
        seq = null;
        terminal = null;
        shift = TIMELESS;
    }


    private Term termCom(TermBuilder B) {

        DisposableTermList PN = null;

        if (!pos.isEmpty()) {
            int pp = pos.size();
            if (neg.isEmpty() && pp == 1)
                return pos.iterator().next();
            else {
                PN = new DisposableTermList(pos.size() + (!neg.isEmpty() ? neg.size() : 0));
                PN.addAll(pos);
            }
        }
        if (!neg.isEmpty()) {
            int nn = neg.size();
            if (pos.isEmpty() && nn == 1)
                return neg.iterator().next().neg();
            else {
                if (PN == null) PN = new DisposableTermList(neg.size());
                for (Term N : neg)
                    PN.add(N.neg());
            }
        }


        if (PN == null)
            return True;

        Term[] q = PN.arrayKeep();

        switch (q.length) {
            case 0:
                return True;
            case 1:
                return q[0];
            default: {
//                if (!neg.isEmpty()) {
//                    Term d = ConjPar.disjunctiveFactor(q, DTERNAL, B);
//                    if (d!=null)
//                        return d; //specially disjunctive reduction
//                }

                boolean hasConj = false, hasDisj = false;

                for (Term x : q) {
                    if (x instanceof Compound) {
                        if (x instanceof Neg) {
                            Term xu = x.unneg();
                            if (xu.op()==CONJ) hasDisj = true;
                            for (Term y : q) {
                                if (y != x && !(y instanceof Neg) && y.equals(xu))
                                    return False; //simple conflict avoided
                            }
                        } else if (x.op() == CONJ)
                            hasConj = true;
                    }
                }
                boolean simple = true;

                if (hasConj) {
                    ready:
                    for (Term x : q) {
                        x = x.unneg();
                        if (x instanceof Compound && x.op() == CONJ) {
                            int xv = x.volume();
                            for (Term y : q)
                                if (y != x && y.volume() < xv && x.hasAll(y.unneg().structure())) {
                                    //TODO test for disjunctive sequence contradictions
                                    boolean[] fail = {false};
                                    simple = !x.eventsOR(new LongObjectPredicate<Term>() {
                                        @Override
                                        public boolean accept(long when, Term xx) {
                                            if (xx.equalsNeg(y)) {
                                                fail[0] = true;
                                                return true;
                                            }
                                            if (xx.equals(y))
                                                return true;
                                            if (xx instanceof Neg) {
                                                Term xu = xx.unneg();
                                                if (xu instanceof Compound && xu.op() == CONJ) {
                                                    return Conj.eventOf(xu, y, +1) || Conj.eventOf(xu, y, -1);
                                                }
                                            }
                                            return false;
                                        }
                                    }, ETERNAL, true, true);
                                    if (fail[0])
                                        return False; //conflict in sequence avoided
                                    if (!simple)
                                        break ready;

//                                    if (Conj.eventOf(x, y, ETERNAL, +1)) {
//                                        simple = false;
//                                        break ready;
//                                    }
                                }
                        }
                    }
                }

                if (hasDisj) {
                    @Nullable Term qd = ConjPar.disjunctiveFactor(q, DTERNAL, B);
                    if (qd != null)
                        return qd;
                }


                if (!simple && Util.or(new Predicate<Term>() {
                    @Override
                    public boolean test(Term qq) {
                        return qq.op() == CONJ && qq.dt() == XTERNAL;
                    }
                }, q)) {
                    simple = true; //TODO refine
                }

                Term y;
                if (simple) {
                    Arrays.sort(q);
                    y = B.newCompound(CONJ, DTERNAL, q);

                    //post-verify HACK
                    if (y.op()==CONJ && (y.subStructure()&CONJ.bit)!=0) {
                        //test factorization exhaustively
                        try {
                            y.eventsAND(new LongObjectPredicate<Term>() {
                                @Override
                                public boolean accept(long when, Term whta) {
                                    return true;
                                }
                            }, 0L, false, true);
                        } catch (TermTransformException tte) {
                            if (NAL.DEBUG)
                                throw tte;
                        }
                    }

                } else {
                    try {
                        y = ConjPar.the(B, DTERNAL, true, q);
                    } catch (StackOverflowError e) {
                        throw new TermException("non-simple conjunction stack overflow", CONJ, q);
                        //return Null;
                    }
                }

                return y;
            }
        }

    }

    /**
     * construct the sequence component of the Conj
     */
    private @Nullable Term termSeq(TermBuilder B) {

        int ss = seq.size();

        Term s;
        if (ss == 1) {
            //special case: degenerate sequence of 1 time point (probably @ 0)


            IntObjectPair<ConjTree> only = seq.keyValuesView().getOnly();
            ConjTree x = only.getTwo();

            shift = (long) seq.keySet().min();

            if (x.seq==null) {
                //flatten point
                if (!addAllAt(only.getOne(), x)) {
                    terminate(False);
                    return terminal;
                }
                s = null;

            } else
                s = x.term(B);




//            s = (seq!=null && !seq.isEmpty()) ? termSeq(B) /* recurse */ : null;

        } else {

            boolean pp = !pos.isEmpty(), nn = !neg.isEmpty();
            //actual sequence
            ConjList events = new ConjList(ss * (1 + (pp ? pos.size() : 0) + (nn ? neg.size() : 0)));

            for (IntObjectPair<ConjTree> wc : seq.keyValuesView()) {
                Term x = wc.getTwo().term(B);
                if (x == False || x == Null) {
                    terminate(x);
                    break;
                }

                long W = (long) wc.getOne();
                if (!events.add(W, x) || (pp && !events.addAll(W, pos)) || (nn && !events.addAllNeg(W, neg))) {
                    terminate(False);
                    break;
                }
            }

            if (pp) pos.clear();
            if (nn) neg.clear();

            if (terminal != null)
                return terminal;

            if (!events.isEmpty()) {

                events.factor(this);
                if (terminal != null)
                    return terminal;

                if (!events.condense(B)) {
                    terminate(False);
                    return terminal;
                }

                assert(terminal == null);

                int es = events.size();
                switch (es) {
                    case 0:
                        s = null; break;
                    case 1:
                        s = events.get(0); break;
                    default: {
//                        final int SEQ_THRESH =
//                                //2;
//                                //3;
//                                //4;
//                                //5;
//                                8;
//                        if (es >= SEQ_THRESH)
//                            s = ConjSeq.sequenceFlat(events);
//                        else
                            s =  ConjSeq.sequenceBalancedTree(B, events, 0, es);
                        break;
                    }
                }

            } else
                s = null;
        }

        if (s == False || s == Null) {
            terminate(s);
            return s;
        }
        if (s == True)
            s = null;

        if (pos.isEmpty() && neg.isEmpty()) return s;
        else {
            if (s != null) if (!addParallel(s))
                return terminal;
            return termCom(B);
        }



    }

    private boolean addAllAt(int at, ConjTree x) {
        if (x.seq!=null)
            return addAt((long) at, x.term());
        else {
            if (!x.pos.isEmpty()) {
                for (Term p : x.pos) {
                    if (!addParallel(p))
                        return false;
                }
            }
            if (!x.neg.isEmpty()) {
                for (Term term : x.neg) {
                    if (!addParallelNeg(term)) {
                        return false;
                    }
                }
                return true;
            }
        }
//        if (x.seq!=null) {
//
//            if (!x.seq.keyValuesView().allSatisfy(ww -> {
//                return addAt(ww.getOne() + at, ww.getTwo());
//            })) {
//                terminate(False);
//                return false;
//            }
//        }
        return true;
    }


//    private boolean addTo(TermList t, Term term) {
//        if (term == True)
//            return true;
//        else if (term == False || term == Null) {
//            terminate(term);
//            return false;
//        } else {
//
//            if (false) {
//                terminate(False);
//                return false;
//            } else {
//                t.add(term);
//                return true;
//            }
//        }
//    }

    @Override
    public LongIterator eventOccIterator() {
        throw new TODO();
    }


    public boolean removeParallel(Iterable<Term> t) {
        boolean b = false;
        for (Term x : t) {
            if (terminal != null)
                return b;
            b |= removeParallel(x);
        }
        return b;
    }
}
