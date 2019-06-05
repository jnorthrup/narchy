package nars.term.util.conj;

import jcog.TODO;
import jcog.WTF;
import jcog.data.bit.MetalBitSet;
import jcog.data.list.FasterList;
import jcog.data.set.LongObjectArraySet;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.util.builder.TermBuilder;
import nars.time.Tense;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import static nars.Op.CONJ;
import static nars.Op.NEG;
import static nars.term.atom.Bool.*;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;

/**
 * conj tree node
 */
public class ConjTree implements ConjBuilder {
    IntObjectHashMap<ConjTree> seq = new IntObjectHashMap(0); //TODO lazy
    MutableSet<Term> pos = null;
    MutableSet<Term> neg = null;
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
            Term xu = x.unneg();
            if (pos != null) {
                xu = reduceNegPos(xu, pos);
                if (xu == True) return true; //absorbed
                else if (xu == False || xu == Null) {
                    terminate(xu);
                    return false; /*conflict */
                }
            }

            if (neg == null) neg =
                    //new UnifiedSet(1);
                    new BoolSafeUnifiedSet();
            else {
                xu = reduceNegNeg(xu, neg);
                if (xu == True) return true; //absorbed
                else if (xu == False || xu == Null) {
                    terminate(xu);
                    return false; /*conflict */
                }
            }
            neg.add(xu);
        } else {
            if (neg != null) {
                int w = reducePos(x, neg);
                if (w == +1) return true; //absorbed
                if (w == -1) {
                    terminate(False);
                    return false; //conflict
                }
            }

            if (pos == null) pos =
                    //new UnifiedSet(1);
                    new BoolSafeUnifiedSet();
            pos.add(x);
        }
        return true;
    }

    private int reducePos(Term x, MutableSet<Term> neg) {

        FasterList<Term> toAdd = null;

        //check for disj reductions
        for (Iterator<Term> iterator = neg.iterator(); iterator.hasNext(); ) {
            Term n = iterator.next();
            if (x.equals(n))
                return -1; //conflict

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

    private Term reduceNegNeg(Term x, MutableSet<Term> neg) {
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

    private Term reduceNegPos(Term nu, MutableSet<Term> pos) {

        for (Term p : pos) {
            if (p.equals(nu))
                return False; //contradiction

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
        if (pos != null) pos.clear();
        if (neg != null) neg.clear();
        seq.clear();
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
            ConjTree s = seq.getIfAbsentPut(Tense.occToDT(at), ConjTree::new);
            return s.addParallelEvent(x);
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
        if (!removed)
            removed |= seq.anySatisfyWith(ConjTree::removeAll, term);
        else
            seq.forEachWith(ConjTree::removeAll, term);

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
        if (!seq.isEmpty()) {

            int ss = seq.size();
            if (ss == 1) {
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

                    s = ConjSeq.conjSeq(B, events);
                }

            }


            seq.clear();

            if (s == True)
                s = null;
            if (s == False || s == Null) {
                return s;
            }
        }

        if (pos == null && neg == null) {
            return s == null ? True : s;
        }

        if (s != null) {
            if (!addParallelEvent(s))
                return terminal;
        }

//        if (s == null || !Conj.isSeq(s)) {
        TermList p = new TermList(s != null ? 1 : 0);


        if (pos != null) {
            if (superConditions != Collections.EMPTY_LIST && !pos.allSatisfy(z -> validate(z, superConditions)))
                return False;
            p.ensureCapacityForAdditional(pos.size());
            p.addAll(pos);
        }
        if (neg != null) {
            if (superConditions != Collections.EMPTY_LIST && !neg.allSatisfy(z -> validate(z.neg(), superConditions)))
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

        ObjectIntHashMap<Term> count = new ObjectIntHashMap(events.size()); //estimate
        for (Term t : events) {
            if (t.op()==CONJ && t.dt()==DTERNAL) {
                //assert(!Conj.isSeq(t)); //?
                for (Term tt : t.subterms()) {
                    count.addToValue(tt, 1);
                }
            }
        }
        if (count.isEmpty())
            return;

        Set<Term> factored = count.keyValuesView().select((xc)->{
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
                addParallelEvent(x);
                return true;
            }
        }).collect(ObjectIntPair::getOne).toSet();

        if (terminal!=null)
            return; //TODO find why if this happens as a reuslt of addParallel

        if (factored.isEmpty())
            return;

        events.replaceAll(t->{
            Subterms ts = t.subterms();
            MetalBitSet m = ts.indicesOfBits(factored::contains);
            int mc = m.cardinality();
            //TODO remove inner event that is fully factored. but not if it's a start or stop event?
            if (mc >0 && mc < ts.subs()) {
                Term[] removing = ts.removing(m);
                if (removing.length == 1)
                    return removing[0];
                else
                    return CONJ.the(t.dt(), removing);
            }
            return t; //no change
        });


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

    private static class BoolSafeUnifiedSet extends UnifiedSet {
        public BoolSafeUnifiedSet() {
            super(1);
        }

        @Override
        public boolean add(Object key) {
            if (key instanceof Bool)
                throw new WTF();
            return super.add(key);
        }
    }
}
