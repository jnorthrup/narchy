package nars.term.util.conj;

import jcog.TODO;
import jcog.data.bit.MetalBitSet;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import jcog.data.set.LongObjectArraySet;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.util.builder.TermBuilder;
import nars.time.Tense;
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

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

    //        private static boolean validate(Term x, Iterable<Term> yy) {
////        if (x.unneg().op()==CONJ)
////            throw new TODO();
//
//
//        for (Term y : yy) {
//
////            if (y.unneg().op()==CONJ)
////                throw new TODO();
//
//            if (x.equalsNeg(y))
//                return false;
//
//        }
//        return true;
//    }

    private boolean addParallelNeg(Term x) {
        return addParallel(x.neg());
    }

    private boolean addParallel(Term x) {


        if (terminal != null)
            throw new UnsupportedOperationException();

        if (x == True)
            return true;
        if (x == False || x == Null) {
            terminate(x);
            return false;
        }

        if (x.op() == NEG) {
            return addParallelN(x);
        } else {
            return addParallelP(x);
        }

    }

    private boolean addParallelP(Term p) {
        assert (p.op() != NEG);

        if (p.op() == CONJ && p.dt() != XTERNAL && !Conj.isSeq(p))
            return p.subterms().AND(this::addParallel); //decompose conj

        if (pos != null && pos.contains(p))
            return true;

        if (neg != null) {
            p = reducePN(p, neg, false);
            if (p.op() == NEG)
                return addParallelN(p); //became positive

            if (p == True)
                return true; //absorbed
            else if (p == False || p == Null) {
                terminate(p);
                return false; /*conflict */
            }
        }

        if (pos == null) pos = newSet();
        pos.add(p);
        return true;
    }

    private boolean addParallelN(Term n) {
        assert (n.op() == NEG);
        Term nu = n.unneg();

        if (neg != null && !(nu instanceof Bool)) {
            nu = reduceNegNeg(nu, neg);
            if (nu.op() == NEG)
                return addParallelP(nu.unneg()); //became positive
        }

        if (pos != null && !(nu instanceof Bool)) {
            nu = reducePN(nu, pos, true);
            if (nu.op() == NEG)
                return addParallelP(nu.unneg()); //became positive
        }


        if (nu == True)
            return true; //absorbed
        else if (nu == False || nu == Null) {
            terminate(nu);
            return false; /*conflict */
        } else {
            if (neg == null) neg = newSet();
            neg.add(nu);
            return true;
        }
    }

    private Set<Term> newSet() {
        return new ArrayHashSet<>(4);
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
        if (neg.contains(nx))
            return True;

        boolean xConj = nx.op() == CONJ;

        FasterList<Term> toAdd = null;
        for (Iterator<Term> nyi = neg.iterator(); nyi.hasNext(); ) {
            Term ny = nyi.next();
            boolean yConj = ny.op() == CONJ;
            if (yConj) {
                //disj
                if (ny.containsRecursively(nx)) {
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
                //Robbins Algebra / Huntington Reduction
                if (xConj && (nx.hasAny(NEG) || ny.hasAny(NEG)) && Term.commonStructure(nx.subterms().structure(), ny.subterms().structure())) {
                    ConjLazy nxe = ConjLazy.events(nx);
                    ConjLazy nye = ConjLazy.events(ny);
                    //symmetric difference
                    if (nxe.removeIf((LongObjectPredicate<Term>) nye::removeNeg)) {
                        if (nxe.isEmpty()) {
                            //XOR ??? TODO check that this shouldnt be eliminated
                        } else {
                            nyi.remove();

                            nx = nxe.term(); //the intersection
                            if (nx == False || nx == Null)
                                return nx; //shouldnt happen

                            long nxs = nxe.shift();
                            if (nxs == ETERNAL || nxs == 0) {
                                //continue, adding at present time
                            } else {
                                //add at shifted time
                                if (!add(nxs, nx))
                                    return False;
                                break;
                            }
                        }
                    }
                }

            }
            if (xConj && nx.containsRecursively(ny)) {
                if (Conj.eventOf(nx, ny)) {
                    return ny;
                } else if (Conj.eventOf(nx, ny.neg())) {
                    nx = Conj.diffAll(nx, ny.neg());
                    if (nx instanceof Bool)
                        return nx;
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

        if (y.contains(x))
            return False; //contradiction

        boolean xConj = x.op() == CONJ;
        if (xConj) {

            for (Term yy : y) {

                if (nP_or_pN) {
                    if (Conj.eventOf(x, yy.neg()))
                        return True; //absorb

                    if (Conj.eventOf(x, yy)) {
                        x = Conj.diffAll(x, yy);
                        if (x instanceof Bool) return x;
                    }

                } else {
                    if (Conj.eventOf(x, yy)) {
                        return False; //contradict
                    } else if (Conj.eventOf(x, yy.neg())) {
                        x = Conj.diffAll(yy, x);
                        if (x instanceof Bool) return x;
                    }

                }
            }

        }


        if (!nP_or_pN) {
            FasterList<Term> toAdd = null;
            for (Iterator<Term> iterator = y.iterator(); iterator.hasNext(); ) {
                Term yy = iterator.next();
                if (yy.op() == CONJ && yy.containsRecursively(x)) {

                    if (Conj.eventOf(yy, x.neg())) {
                        iterator.remove();
                    } else if (Conj.eventOf(yy, x)) {
                        iterator.remove();
                        if (toAdd == null) toAdd = new FasterList(1);
                        Term z = Conj.diffAll(yy, x);
                        if (z == True) {
                            //ignore
                        } else if (z == False || z == Null)
                            return z;
                        else
                            toAdd.add(z);
                    }

                }

            }

            if (toAdd != null) {
                if (!toAdd.allSatisfy(nP_or_pN ? this::addParallel : this::addParallelNeg))
                    return False;
            }
        }

        return x;
    }

    private void terminate(Term result) {
//        if (pos != null) pos.clear();
//        if (neg != null) neg.clear();
//        if (seq != null) seq.clear();
        terminal = result;
    }

    @Override
    public boolean addEvent(long at, Term x) {
        if (terminal != null)
            throw new UnsupportedOperationException();

        if (at == ETERNAL) {
            return addParallel(x);
        } else {
            return addSequential(at, x);
        }
    }

    private boolean addSequential(long at, Term x) {
        if (x.op() != NEG) {
            if (neg != null && neg.contains(x)) {
                terminate(False); //contradict
                return false;
            }
            if (neg != null && !(x instanceof Bool)) {
                x = reducePN(x, neg, false);
                if (x.op() == NEG)
                    return addEvent(at, x); //became positive
            }
        } else {
            Term nu = x.unneg();
            Term _nu = nu;
            if (pos != null && pos.contains(nu)) {
                terminate(False); //contradict
                return false;
            }

            if (neg != null && !(nu instanceof Bool)) {
                nu = reduceNegNeg(nu, neg);
                if (nu.op() == NEG)
                    return addEvent(at, nu.unneg()); //became positive
            }

            if (pos != null && !(nu instanceof Bool)) {
                nu = reducePN(nu, pos, true);
                if (nu.op() == NEG)
                    return addEvent(at, nu.unneg()); //became positive
            }


            if (nu == True)
                return true; //absorbed
            else if (nu == False || nu == Null) {
                terminate(nu);
                return false; /*conflict */

            }
            if (_nu != nu)
                x = nu.neg(); //HACK
        }

        if (seq == null) seq = new IntObjectHashMap<>(4);
        return seq.getIfAbsentPut(Tense.occToDT(at), ConjTree::new).addParallel(x);
    }

    @Override
    public boolean remove(long at, Term t) {
        if (at == ETERNAL) {
            boolean n = t.op() == NEG;
            return n ? negRemove(t.unneg()) : posRemove(t);

        } else {
            ConjTree s = seq.get(Tense.occToDT(at));
            if (s == null) return false;
            if (s.remove(ETERNAL, t)) {
                if (seq.isEmpty())
                    seq = null;
                return true;
            }
            return false;
        }
    }

    private boolean posRemove(Term t) {
        if (pos != null && pos.remove(t)) {
            if (pos.isEmpty())
                pos = null;
            return true;
        }
        return false;
    }

    private boolean negRemove(Term t) {
        if (neg != null && neg.remove(t)) {
            if (neg.isEmpty())
                neg = null;
            return true;
        }
        return false;
    }

    @Override
    public int eventOccurrences() {
        throw new TODO();
    }

    @Override
    public boolean removeAll(Term term) {
        boolean removed = removeParallel(term);

        if (seq != null) {
            if (!removed)
                removed |= seq.anySatisfyWith(ConjTree::removeAll, term);
            else
                seq.forEachWith(ConjTree::removeAll, term);
        }

        return removed;
    }

    private boolean removeParallel(Term term) {
        if (terminal != null)
            throw new UnsupportedOperationException();

        boolean removed = false;
        if (term.op() == NEG) {
            removed |= negRemove(term);
        } else {
            removed |= posRemove(term);
        }
        return removed;
    }

    @Override
    public int eventCount(long when) {
        throw new TODO();
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
        if (seq != null && ((ss = seq.size()) > 0)) {
            Term s = null;

            if (ss == 1) {
                //special case: degenerate sequence of 1 time point (probably @ 0)
                s = seq.getOnly().term(B);
            } else {
                //actual sequence
                LongObjectArraySet<Term> events = new LongObjectArraySet<>(ss);

                for (IntObjectPair<ConjTree> wc : seq.keyValuesView()) {
                    Term w = wc.getTwo().term(B);
                    if (w == True) continue;
                    if (w == False || w == Null) return w;
                    events.add((long) wc.getOne(), w);
                }
                if (!events.isEmpty()) {

                    factor(events);
                    if (terminal != null)
                        return terminal;

                    s = ConjSeq.conjSeq(B, events);
                }

            }

            seq.clear();

            if (s == True)
                s = null;
            if (s == False || s == Null)
                return s;

            if (pos == null && neg == null) {
                return s == null ? True : s;
            }

            if (s != null) {
                //add seq as if parallel component avoiding decompose what we just constructed

                if ((pos != null || neg != null) && Conj.isSeq(s)) {
                    //check for sub-event conflicts that ordinarily wouldnt be tested on addParallel insert

                    if (!s.eventsWhile((when, what) -> {
                        if (what.op() == NEG) {
                            if (pos != null)
                                if (pos.contains(what.unneg())) {
                                    return false; //contradiction
                                }
                            //TODO detect reducible disjunction-in-sequence here to trigger recurse
                        } else {
                            if (neg != null)
                                if (neg.contains(what)) {
                                    return false; //contradiction
                                }
                        }
                        return true;
                    }, 0, true, true)) {
                        return False; //contradiction
                    }

                }

                if (!addParallel(s))
                    return terminal;
            }
        }


//        if (s == null || !Conj.isSeq(s)) {
        TermList p = new TermList();


        if (pos != null) {
            int pp = pos.size();
            if (neg == null && pp == 1)
                return pos.iterator().next();
            else {
                p.ensureCapacityForAdditional(pp);
                p.addAll(pos);
            }
        }
        if (neg != null) {
            int nn = neg.size();
            if (pos == null && nn == 1)
                return neg.iterator().next().neg();
            else {
                p.ensureCapacityForAdditional(nn);
                p.addAllNegated(neg);
            }
        }


        if (p.size() > 1)
            return ConjCommutive.conjDirect(B, DTERNAL, p.sortAndDedup());
        else
            return p.get(0);
//        } else {
//            //distribute
//            ConjTree c = new ConjTree();
//            c.add(ETERNAL, s);
//
//
//            if (pos != null) {
//                if (!c.seq.allSatisfy(cc -> pos.allSatisfy(cc::addParallelEvent))) {
//                    return False;
//                }
//            }
//            if (neg != null) {
//                if (!c.seq.allSatisfy(cc -> neg.allSatisfy(cc::addParallelEventNegated))) {
//                    return False;
//                }
//            }
//            return c.term(B);
//        }


    }

    private void factor(LongObjectArraySet<Term> events) {
        int n = events.size();
        if (n < 2)
            return;


        //HACK pure repeating sequence - quick test to see whether all terms are equal in which case factorization will be impossible
        if (pos == null && neg == null) {
            boolean someDifference = false;
            for (int i = 1, eventsSize = n; i < eventsSize; i++) {
                Term t = events.get(i);
                if (!t.equals(events.get(i - 1))) {
                    someDifference = true;
                    break;
                }
            }
            if (!someDifference)
                return;
        }


        ObjectIntHashMap<Term> count = new ObjectIntHashMap(n); //estimate
        for (int i = 0, eventsSize = n; i < eventsSize; i++) {
            Term t = events.get(i);
            if (t.op() == CONJ && t.dt() == DTERNAL) {
                //assert(!Conj.isSeq(t)); //?
                for (Term tt : t.subterms()) {
                    if (tt.op() != CONJ)
                        count.addToValue(tt, 1);
                }
            } else
                count.addToValue(t, 1);
        }
        if (count.isEmpty())
            return;

        TermList newFactors = new TermList(), partialFactors = new TermList();
        Set<Term> factorable = count.keyValuesView().select((xc) -> {
            Term x = xc.getOne();
            int c = xc.getTwo();
            if (c < n) {
                if (x.op() != NEG) {
                    if (pos != null && posRemove(x)) {
                        partialFactors.add(x);
                        return true;
                    }
                } else {
                    if (neg != null && negRemove(x.unneg())) {
                        partialFactors.add(x);
                        return true;
                    }
                }
                return false;
            } else {
                //new factor component
                newFactors.add(x);
                return true;
            }
        }).collect(ObjectIntPair::getOne).toSet();

        if (terminal != null)
            return; //TODO find why if this happens as a reuslt of addParallel

        if (!factorable.isEmpty()) {
            Predicate<Term> isFactored = factorable::contains;

            List<MetalBitSet> removals = new FasterList(n);
            for (Term t : events) {
                Subterms ts = t.subterms();
                MetalBitSet m = ts.indicesOfBits(isFactored);
                int mc = m.cardinality();
                //TODO remove inner event that is fully factored. but not if it's a start or stop event?
                if (mc > 0 && mc < ts.subs()) {
                    removals.add(m);
                }
            }

            if (removals.size() == n) {
                for (int i = 0; i < n; i++) {
                    Term x = events.get(i);
                    MetalBitSet m = removals.get(i);
                    Term[] removing = x.subterms().removing(m);
                    Term y = removing.length == 1 ? removing[0] : CONJ.the(x.dt(), removing);
                    events.set(i, y);
                }
                newFactors.forEach(this::addParallel);
            } else {
                newFactors.forEach(this::removeParallel);
            }


            int pf = partialFactors.size();
            if (pf > 0) {
                //distribute partial factors
                for (int i = 0; i < n; i++) {
                    Term xf = events.get(i);
                    if (pf == 1 && partialFactors.sub(0).equals(xf))
                        continue;

                    Term[] t = new Term[pf + 1];
                    partialFactors.arrayClone(t);
                    t[t.length - 1] = xf;
                    Term xd = CONJ.the(t);
                    if (xd == False || xd == Null) {
                        terminate(xd);
                        return;
                    }
                    events.set(i, xd);
                }
            }


        } else {
            return;
        }

    }

    private boolean addTo(TermList t, Term term) {
        if (term == True)
            return true;
        else if (term == False || term == Null) {
            terminate(term);
            return false;
        } else {

            if (false) {
                terminate(False);
                return false;
            } else {
                t.add(term);
                return true;
            }
        }
    }

    @Override
    public LongIterator eventOccIterator() {
        throw new TODO();
    }


}
