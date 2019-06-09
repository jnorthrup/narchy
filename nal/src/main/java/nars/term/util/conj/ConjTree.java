package nars.term.util.conj;

import jcog.TODO;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import nars.subterm.DisposableTermList;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.util.builder.TermBuilder;
import nars.time.Tense;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;

import java.util.Iterator;
import java.util.Set;

import static nars.Op.CONJ;
import static nars.Op.NEG;
import static nars.term.atom.Bool.*;
import static nars.time.Tense.*;

/**
 * conj tree node
 */
public class ConjTree implements ConjBuilder {
    IntObjectHashMap<ConjTree> seq = null;
    Set<Term> pos = null;
    Set<Term> neg = null;
    Term terminal = null;
    private long shift = TIMELESS;



    boolean addParallel(Term x) {


        if (terminal != null)
            throw new UnsupportedOperationException();

        if (x == True)
            return true;
        if (x == False || x == Null) {
            terminate(x);
            return false;
        }

        try {
            if (x instanceof Neg) {
                return addParallelN(x);
            } else {
                return addParallelP(x);
            }
        } finally {
            if (pos!=null && pos.isEmpty())
                pos = null; //HACK
            if (neg!=null && neg.isEmpty())
                neg = null; //HACK
        }

    }

    private boolean addParallelNeg(Term x) {
        return addParallel(x.neg());
    }

    private boolean addParallelP(Term p) {
        if (p instanceof Compound) {
            assert (p.op() != NEG);

            if (p.op() == CONJ) {
                if (p.dt() == XTERNAL || Conj.isSeq(p)) {
//                    for (Term what : p.subterms()) {
//                        if (validate(what))
//                            return false; //contradiction
//                    }
                    if (pos!=null || neg!=null) {
                        if (!p.eventsWhile((when, what) -> validatePosNeg(what), 0, true, true)) {
                            terminate(False);
                            return false;
                        }
                    }
                    //continue below
                } else {
                    //decompose parallel conj
                    if (!Conj.isSeq(p)) {
                        return p.subterms().AND(this::addParallel);
                    }
                }
            }
        }

        if (pos!=null && pos.contains(p))
            return true;
        if (neg!=null && neg.contains(p)) {
            terminate(False);
            return false;
        }

        if (neg != null) {
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

        if (pos == null) pos = newSet();
        assert(p.op()!=NEG);
        pos.add(p);
        return true;
    }

    private boolean validatePosNeg(Term what) {
        if (what instanceof Neg) {
            return pos == null || !pos.contains(what.unneg());
            //TODO detect reducible disjunction-in-sequence here to trigger recurse
        } else {
            return neg == null || !neg.contains(what);
        }
    }

    private boolean addParallelN(Term n) {
        assert (n instanceof Neg);
        Term nu = n.unneg();

        if (neg!=null && neg.contains(nu))
            return true;
        if (pos!=null && pos.contains(nu)) {
            terminate(False);
            return false;
        }

        if (pos != null && !(nu instanceof Bool)) {
            nu = reducePN(nu, pos, true);
            if (nu instanceof Neg)
                return addParallelP(nu.unneg()); //became positive
        }

        if (neg != null && !(nu instanceof Bool)) {
            nu = reduceNegNeg(nu, neg);
            if (nu instanceof Neg)
                return addParallelP(nu.unneg()); //became positive
        }



        if (nu == True)
            return true; //absorbed
        else if (nu == False || nu == Null) {
            terminate(nu);
            return false; /*conflict */
        } else {
            if (neg == null) neg = newSet();
            assert(nu.op()!=NEG);
            neg.add(nu);
            return true;
        }
    }

    private Set<Term> newSet() {
        return new ArrayHashSet<>(4);
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
        //return new LinkedHashSet<>(4);
        //return new UnifiedSet<>(1, 0.99f);
        //return new HashSet<>(4);
    }


    /**
     * TODO
     * ¬( a||b) || ¬( a||¬b) == ¬a	# Robbins Algebra axiom3
     * (--a && b) || ( --a && --b) == --a
     * --(--a && b) && --(--a && --b) == a
     * --(a && b) && --(a && --b) == --a
     *
     * @param nx
     * @param neg
     * @return
     */
    private Term reduceNegNeg(Term nx, Set<Term> neg) {
        assert(nx.op()!=NEG);

        int nxs = nx.structure();
        boolean xConj = nx.op() == CONJ;

        FasterList<Term> toAdd = null;
        for (Iterator<Term> nyi = neg.iterator(); nyi.hasNext(); ) {
            final Term ny = nyi.next();
            boolean yConj = ny.op() == CONJ;
            int nys = ny.structure();
            if (yConj) {
                //disj
                {
                    if (Conj.eventOf(ny, nx.neg())) {
                        //prune
                        nyi.remove();
                        if (toAdd == null) toAdd = new FasterList(1);
                        Term z = Conj.diffAll(ny, nx.neg());
                        if (z == True) {
                            //eliminated
                        } else if (z == False || z == Null)
                            return z;
                        else
                            toAdd.add(z);
                        continue;
                    } else if (Conj.eventOf(ny, nx)) {
                        //short-circuit
                        nyi.remove();
                        continue;
                    }
                }
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
            }
            if (xConj) {
                if (Conj.eventOf(nx, ny)) {
                    return True; //absorbed
                } else if (Conj.eventOf(nx, ny.neg())) {
                    nx = Conj.diffAll(nx, ny.neg());
                    if (nx instanceof Bool)
                        return nx;
                    nxs = nx.structure();
                }

            }

        }
        if (toAdd != null) {
            if (!toAdd.allSatisfy(this::addParallelNeg))
                return False;
        }
        return nx;
    }

    private Term reducePN(Term x, Set<Term> y, boolean nP_or_pN) {
        assert(x.op()!=NEG);

        if (y.contains(x))
            return False; //contradiction


        boolean xConj = x.op() == CONJ;
        if (xConj && nP_or_pN) {

            for (Term yy : y) {

                    if (Conj.eventOf(x, yy.neg())) {
                        return True;
                    } else {
                        Term z = Conj.diffAll(x, yy);
                        if (!z.equals(x)) {
                            if (z instanceof Bool)
                                return z;
                            x = z;
                        }
                    }

            }

        }


        if (!nP_or_pN) {

            Term xn = x.neg();

            FasterList<Term> add = null;
            for (Iterator<Term> iterator = y.iterator(); iterator.hasNext(); ) {
                Term yy = iterator.next();
                if (yy.op() == CONJ) {// && yy.containsRecursively(x)) {
                    if (Conj.eventOf(yy, xn)) {
                        //short-circuit
                        iterator.remove();
                    } else {
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
                                if (add == null) add = new FasterList(1);
                                add.add(z);
                            }

                        }
                    }
//                    }




                }

            }

            if (add != null) {
                if (!add.allSatisfy(this::addParallelNeg))
                    return False;
            }
        }

        return x;
    }

    Term terminate(Term y) {
        Term x = terminal;
        if (x == null || x == True || y == Null) {
            return terminal = y;
        } else if (x == False) {
            if (y == Null)
                return terminal = Null;
        }
        return terminal;
    }

    @Override
    public boolean addEvent(long at, Term x) {
        if (terminal != null)
            throw new UnsupportedOperationException();

        if (at == ETERNAL) {
            return addParallel(x);
        } else {
            return addAt(at, x);
        }
    }

    private boolean addAt(long at, Term x) {
        if (x.op() != NEG) {
            if (pos!=null && pos.contains(x))
                return true;

            if (neg != null && !(x instanceof Bool)) {
                x = reducePN(x, neg, false);
            }

        } else {
            Term _xu = x.unneg();
            Term xu = _xu;

            if (neg!=null && neg.contains(xu))
                return true;

            if (pos != null && pos.contains(xu)) {
                terminate(False); //contradict
                return false;
            }

            if (pos != null && !(xu instanceof Bool)) {
                xu = reducePN(xu, pos, true);
                if (xu.op()==NEG)
                    return addAt(at, xu.unneg()); //inverted
            }

            if (neg != null && !(xu instanceof Bool)) {
                xu = reduceNegNeg(xu, neg);
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

        if (seq == null) seq = new IntObjectHashMap<>(4);
        return seq.getIfAbsentPut(Tense.occToDT(at), () -> {
            shift = TIMELESS; //invalidate
            return new ConjTree();
        }).addParallel(x);
    }

    @Override
    public boolean remove(long at, Term t) {
        if (at == ETERNAL) {
            boolean n = t instanceof Neg;
            return n ? negRemove(t.unneg()) : posRemove(t);

        } else {
            int aat = occToDT(at);
            ConjTree s = seq.get(aat);
            if (s == null) return false;
            if (s.remove(ETERNAL, t)) {
                if (s.isEmpty()) {
                    seq.remove(aat);
                    shift = TIMELESS; //invalidate
                    if (seq.isEmpty()) {
                        seq = null;
                    }
                }
                return true;
            }
            return false;
        }
    }

    private boolean isEmpty() {
        return size() == 0;
    }

    boolean posRemove(Term t) {
        if (pos != null && pos.remove(t)) {
            if (pos.isEmpty())
                pos = null;
            return true;
        }
        return false;
    }

    boolean negRemove(Term t) {
        if (neg != null && neg.remove(t)) {
            if (neg.isEmpty())
                neg = null;
            return true;
        }
        return false;
    }

    @Override
    public int eventOccurrences() {
        return ((pos!=null || neg!=null) ? 1 : 0) + (seq != null ? seq.size() : 0);
    }

    @Override
    public int eventCount(long when) {
        if (when == ETERNAL)
            return (pos != null ? pos.size() : 0) + (neg!=null ? neg.size() : 0);
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
        if (shift == TIMELESS) {
            shift = (seq == null || seq.isEmpty()) ? 0 : seq.keysView().min();
        }
        return shift;
    }

    @Override
    public boolean removeAll(Term term) {
        boolean removed = removeParallel(term);

        if (seq != null) {
            if (!removed) {
                removed |= seq.countWith(ConjTree::removeAll, term) > 0;
            } else
                seq.forEachWith(ConjTree::removeAll, term);
        }

        return removed;
    }

    private boolean removeParallel(Term term) {
        if (terminal != null)
            throw new UnsupportedOperationException();

        boolean removed = false;
        if (term instanceof Neg) {
            removed |= negRemove(term);
        } else {
            removed |= posRemove(term);
        }
        return removed;
    }

    @Override
    public void negateEvents() {
        throw new TODO();
    }


    public int size() {
        return (pos != null ? pos.size() : 0) +
                (neg != null ? neg.size() : 0) +
                (seq != null ? (int) seq.sumOfInt(ConjTree::size) : 0);
    }

    @Override
    public Term term(TermBuilder B) {
        if (terminal != null)
            return terminal;

        int ss;
        Term s = null;
        if (seq != null && ((ss = seq.size()) > 0)) {

            if (ss == 1) {
                //special case: degenerate sequence of 1 time point (probably @ 0)
                s = seq.getOnly().term(B);
            } else {

                //actual sequence
                ConjList events = new ConjList(ss);

                for (IntObjectPair<ConjTree> wc : seq.keyValuesView()) {
                    Term w = wc.getTwo().term(B);
                    if (w == False || w == Null) {
                        return terminate(w);
                    }
                    if (!events.add((long) wc.getOne(), w)) {
                        return terminate(False);
                    }
                }

                if (!events.isEmpty()) {

                    events.preDistribute(this);
                    if (terminal != null)
                        return terminal;

                    events.condense(B);
                    if (terminal != null)
                        return terminal;

                    s = events.seq(B);
                }

            }

            shift(); //cache the shift before clearing seq
            seq = null;
            //seq.clear();

            if (s == True)
                s = null;
            if (s == False || s == Null)
                return s;

            if (pos == null && neg == null) {
                return s == null ? True : s;
            }

            if (s != null) {
                if (!addParallel(s)) {
                    return terminate(False);
                }
            }
        }


//        if (s == null || !Conj.isSeq(s)) {
        int c = eventCount(ETERNAL);
        if (c == 0)
            return True;

        DisposableTermList p = c > 1 ? new DisposableTermList(c) : null;


        if (pos != null) {
            int pp = pos.size();
            if (neg == null && pp == 1)
                return pos.iterator().next();
            else {
                p.addAll(pos);
            }
        }
        if (neg != null) {
            int nn = neg.size();
            if (pos == null && nn == 1)
                return neg.iterator().next().neg();
            else {
                p.addAllNegated(neg);
            }
        }


        Term[] q = p.sortAndDedup();
        switch (q.length) {
            case 0:
                return True;
            case 1:
                return q[0];
            default: {
                return B.newCompound(CONJ, DTERNAL, q);
            }
        }


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


}
