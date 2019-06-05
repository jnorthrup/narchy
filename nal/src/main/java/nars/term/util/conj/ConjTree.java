package nars.term.util.conj;

import com.google.common.collect.Iterables;
import jcog.TODO;
import jcog.data.set.LongObjectArraySet;
import nars.subterm.TermList;
import nars.term.Term;
import nars.term.util.builder.TermBuilder;
import nars.time.Tense;
import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Collections;

import static nars.Op.CONJ;
import static nars.Op.NEG;
import static nars.term.atom.Bool.*;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;

/** conj tree node */
public class ConjTree implements ConjBuilder {
    IntObjectHashMap<ConjTree> seq = new IntObjectHashMap(0); //TODO lazy
    MutableSet<Term> pos = null;
    MutableSet<Term> neg = null;

    private boolean addParallelEvent(Term x) {
        if (x.op()==NEG) {
            Term xu = x.unneg();
            if (pos!=null && pos.contains(xu))
                return false; //conflict

            if (neg == null) neg = new UnifiedSet(1);
            neg.add(xu);
        } else {
            if (neg!=null && neg.contains(x))
                return false; //conflict

            if (pos == null) pos = new UnifiedSet(1);
            pos.add(x);
        }
        return true;
    }

    @Override
    public boolean addEvent(long at, Term x) {
        if (at == ETERNAL) {
            return addParallelEvent(x);
        } else {
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
        boolean removed = false;
        if (term.op()==NEG) {
            if (neg!=null) { removed |= neg.remove(term); }
        } else {
            if (pos!=null) { removed |= pos.remove(term); }
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

    private <E> Term term(TermBuilder B, Iterable<Term> superConditions) {
        TermList t = new TermList(0); //parallel component
        if (pos != null) {
            if (superConditions!=Collections.EMPTY_LIST && !pos.allSatisfy(p -> validate(p,true,superConditions)))
                return False;
            t.ensureCapacityForAdditional(pos.size());
            t.addAll(pos);
        }
        if (neg!=null) {
            if (superConditions!=Collections.EMPTY_LIST && !neg.allSatisfy(p -> validate(p,false,superConditions)))
                return False;
            t.ensureCapacityForAdditional(neg.size());
            t.addAllNegated(neg);
        }

        if (!seq.isEmpty()) {

            Iterable<Term> sc = superConditions != Collections.EMPTY_LIST ? Iterables.concat(superConditions, t) : t;

            int ss = seq.size();
            if (ss == 1) {
                //special case: degenerate sequence of 1 time point (probably @ 0)
                t.add(seq.getOnly().term(B, sc));
            } else {
                //actual sequence
                LongObjectArraySet<Term> events = new LongObjectArraySet<>(ss);

                for (IntObjectPair<ConjTree> wc : seq.keyValuesView()) {
                    ConjTree what = wc.getTwo();
                    Term w = what.term(B, sc);
                    if (w==True) continue;
                    if (w==False || w == Null) return w;
                    events.add((long)wc.getOne(), w);
                }
                if (!events.isEmpty())
                    t.add(ConjSeq.conjSeq(B, events));
            }
        }

        if (t.size() > 1)
            return ConjCommutive.conjDirect(B, DTERNAL, t.sort());
        else
            return t.get(0);
    }

    private static boolean validate(Term _x, boolean positive, Iterable<Term> yy) {
        if (_x.op()==CONJ)
            throw new TODO();

        Term x = null;
        for (Term y : yy) {

            if (y.unneg().op()==CONJ)
                throw new TODO();

            if (x == null)
                x = _x.negIf(!positive);
            if (x.equalsNeg(y))
                return false;

        }
        return true;
    }

    @Override
    public LongIterator eventOccIterator() {
        throw new TODO();
    }
}
