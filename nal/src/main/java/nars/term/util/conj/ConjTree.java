package nars.term.util.conj;

import com.google.common.collect.Iterables;
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
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static nars.Op.CONJ;
import static nars.Op.NEG;
import static nars.term.atom.Bool.*;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;

/**
 * conj tree node
 */
public class ConjTree implements ConjBuilder {
    IntObjectHashMap<ConjTree> seq = null;
    Set<Term> pos = null;
    Set<Term> neg = null;
    Term terminal = null;

    private static boolean validate(Term x, Iterable<Term> yy) {
//        if (x.unneg().op()==CONJ)
//            throw new TODO();


        for (Term y : yy) {

//            if (y.unneg().op()==CONJ)
//                throw new TODO();

            if (x.equalsNeg(y))
                return false;

        }
        return true;
    }

    private boolean addParallelEventNegated(Term x) {
        return addParallelEvent(x.neg());
    }

    private boolean addParallelEvent(Term x) {

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
        if (pos!=null && pos.contains(p))
            return true;

        if (neg != null) {
            int w = reducePosNeg(p, neg);
            if (w == +1) return true; //absorbed
            if (w == -1) {
                terminate(False);
                return false; //conflict
            }
        }

        if (pos == null) pos = newSet();
        pos.add(p);
        return true;
    }

    private boolean addParallelN(Term n) {
        Term nu = n.unneg();

        if (pos != null && !(nu instanceof Bool))
            nu = reduceNegPos(nu, pos);

        if (neg != null && !(nu instanceof Bool))
            nu = reduceNegNeg(nu, neg);

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

    private int reducePosNeg(Term x, Set<Term> neg) {

        if (neg.contains(x))
            return -1; //conflict

        FasterList<Term> toAdd = null;

        //check for disj reductions
        for (Iterator<Term> iterator = neg.iterator(); iterator.hasNext(); ) {
            Term n = iterator.next();

            if (n.op() == CONJ && n.containsRecursively(x)) {
                //disj
                if (Conj.eventOf(n, x.neg())) {
                    iterator.remove();
                } else if (Conj.eventOf(n, x)) {
                    iterator.remove();
                    if (toAdd == null) toAdd = new FasterList(1);
                    Term z = Conj.diffAll(n, x);
                    if (z == True) {
                        //ignore
                    } else if (z == False || z == Null)
                        return -1;
                    else
                        toAdd.add(z);
                }

            }
        }

        if (toAdd != null) {
            neg.addAll(toAdd);
        }

        return 0;
    }

    private Term reduceNegNeg(Term x, Set<Term> neg) {
        if (neg.contains(x))
            return True;

        FasterList<Term> toAdd = null;
        for (Iterator<Term> iterator = neg.iterator(); iterator.hasNext(); ) {
            Term n = iterator.next();
            if (n.op() == CONJ) {
                //disj
                if (n.containsRecursively(x)) {
                    if (Conj.eventOf(n, x.neg())) {
                        //prune
                        iterator.remove();
                        if (toAdd == null) toAdd = new FasterList(1);
                        Term z = ConjDiff.diffAll(n, x.neg());
                        if (z == True) {
                            //eliminated
                        } else if (z == False || z == Null)
                            return z;
                        else
                            toAdd.add(z);
                    } else if (Conj.eventOf(n, x)) {
                        //short-circuit
                        iterator.remove();
                    }
                } else if (x.containsRecursively(n)) {
                    //TODO
                }
            }
        }
        if (toAdd != null) {
            neg.addAll(toAdd);
        }
        return x;
    }

    private Term reduceNegPos(Term nu, Set<Term> pos) {

        if (pos.contains(nu))
            return False; //contradiction

        for (Term p : pos) {

            if (nu.op() == CONJ) {
                if (Conj.eventOf(nu, p.neg())) {
                    return True; //absorb
                }

                //TODO remove from sequence

                else if (Conj.eventOf(nu, p)) {
                    nu = Conj.diffAll(nu, p);
                }
            }
        }

        return nu;
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
            return addParallelEvent(x);
        } else {
            if (x.op()!=NEG) {
                if (neg!=null && !validate(x, neg)) {
                    terminate(False);
                    return false;
                }
            } else {
                if (pos!=null && !validate(x, pos)) {
                    terminate(False);
                    return false;
                }
            }

            if (seq == null) seq = new IntObjectHashMap<>(4);
            return seq.getIfAbsentPut(Tense.occToDT(at), ConjTree::new).addParallelEvent(x);
        }
    }

    @Override
    public boolean remove(long at, Term t) {
        if (at == ETERNAL) {
            boolean n = t.op() == NEG;
            if (n) {
                if (neg != null)
                    return neg.remove(t.unneg());
            } else {
                if (pos != null)
                    return pos.remove(t);
            }
            return false;
        } else {
            ConjTree s = seq.get(Tense.occToDT(at));
            if (s == null) return false;
            return s.remove(ETERNAL, t);
        }
    }

    @Override
    public int eventOccurrences() {
        throw new TODO();
    }

    @Override
    public boolean removeAll(Term term) {
        if (terminal != null)
            throw new UnsupportedOperationException();

        boolean removed = false;
        if (term.op() == NEG) {
            if (neg != null) {
                removed |= neg.remove(term);
            }
        } else {
            if (pos != null) {
                removed |= pos.remove(term);
            }
        }

        if (seq!=null) {
            if (!removed)
                removed |= seq.anySatisfyWith(ConjTree::removeAll, term);
            else
                seq.forEachWith(ConjTree::removeAll, term);
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

    @Override
    public Term term(TermBuilder B /* Iterable<Term> superconditionsToValidateAgainst */) {
        return term(B, Collections.EMPTY_LIST);
    }


    private Term term(TermBuilder B, Iterable<Term> superConditions) {
        if (terminal != null)
            return terminal;


        Term s = null;
        if (seq!=null) {

            int ss = seq.size();
            if (ss == 0) {
                //?
            } else if (ss == 1) {
                //special case: degenerate sequence of 1 time point (probably @ 0)
                s = seq.getOnly().term(B, superConditions);
            } else {
                //actual sequence
                LongObjectArraySet<Term> events = new LongObjectArraySet<>(ss);

                for (IntObjectPair<ConjTree> wc : seq.keyValuesView()) {
                    Term w = wc.getTwo().term(B, superConditions);
                    if (w == True) continue;
                    if (w == False || w == Null) return w;
                    events.add((long) wc.getOne(), w);
                }
                if (!events.isEmpty()) {

                    factor(events);
                    if (terminal!=null)
                        return terminal;

                    s = ConjSeq.conjSeq(B, events);
                }

            }

            if (s == True)
                s = null;
            if (s == False || s == Null)
                return s;
        }

        if (pos == null && neg == null) {
            return s == null ? True : s;
        }

        if (s != null) {
            if (s.op()!=CONJ || Conj.isSeq(s)) {
                //add seq as if parallel component avoiding decompose what we just constructed
                if (!addParallelEvent(s))
                    return terminal;
            } else {
                //otherwise, flatten
                if (!add(ETERNAL, s))
                    return terminal;
            }
        }

//        if (s == null || !Conj.isSeq(s)) {
        TermList p = new TermList();


        if (pos != null) {
            if (superConditions != Collections.EMPTY_LIST && !Iterables.all(pos, z -> validate(z, superConditions)))
                return False;
            p.ensureCapacityForAdditional(pos.size());
            p.addAll(pos);
        }
        if (neg != null) {
            if (superConditions != Collections.EMPTY_LIST && !Iterables.all(neg, z -> validate(z.neg(), superConditions)))
                return False;
            p.ensureCapacityForAdditional(neg.size());
            p.addAllNegated(neg);
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


        //HACK quick test to see whether all terms are equal in which case factorization will be impossible
        boolean someDifference = false;
        for (int i = 1, eventsSize = events.size(); i < eventsSize; i++) {
            Term t = events.get(i);
            if (!t.equals(events.get(i-1))) {
                someDifference = true;
                break;
            }
        }
        if (!someDifference)
            return;


        ObjectIntHashMap<Term> count = new ObjectIntHashMap(events.size()); //estimate
        for (int i = 0, eventsSize = events.size(); i < eventsSize; i++) {
            Term t = events.get(i);
            if (t.op() == CONJ && t.dt() == DTERNAL) {
                //assert(!Conj.isSeq(t)); //?
                for (Term tt : t.subterms()) {
                    if (tt.op() != CONJ)
                        count.addToValue(tt, 1);
                }
            }
        }
        if (count.isEmpty())
            return;

        List<Term> newFactors = new FasterList();
        Set<Term> factorable = count.keyValuesView().select((xc)->{
            Term x = xc.getOne();
            int c = xc.getTwo();
            if (c < n) {
                if (x.op() != NEG) {
                    if (pos != null && pos.contains(x))
                        return true;
                } else {
                    if (neg != null && neg.contains(x.unneg()))
                        return true;
                }
                return false;
            } else {
                //new factor component
                newFactors.add(x);
                return true;
            }
        }).collect(ObjectIntPair::getOne).toSet();

        if (terminal!=null)
            return; //TODO find why if this happens as a reuslt of addParallel

        if (factorable.isEmpty())
            return;

        Predicate<Term> isFactored = factorable::contains;

        List<MetalBitSet> removals = new FasterList(events.size());
        for (Term t : events) {
            Subterms ts = t.subterms();
            MetalBitSet m = ts.indicesOfBits(isFactored);
            int mc = m.cardinality();
            //TODO remove inner event that is fully factored. but not if it's a start or stop event?
            if (mc >0 && mc < ts.subs()) {
                removals.add(m);
            } else
                return; //give up
        }

        for (int i = 0, eventsSize = events.size(); i < eventsSize; i++) {
            Term x = events.get(i);
            MetalBitSet m = removals.get(i);
            Term[] removing = x.subterms().removing(m);
            Term y;
            if (removing.length == 1)
                y =  removing[0];
            else
                y =  CONJ.the(x.dt(), removing);
            events.set(i, y);
        }


        newFactors.forEach(this::addParallelEvent);

    }

    private boolean addTo(TermList t, Term term) {
        if (term == True)
            return true;
        else if (term == False || term == Null) {
            terminate(term);
            return false;
        } else {

            if (!validate(term, t)) {
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
