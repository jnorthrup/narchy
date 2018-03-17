package nars.link;

import jcog.bag.Bag;
import jcog.list.FasterList;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import nars.NAR;
import nars.concept.Concept;
import nars.term.Term;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Nullable;

public class TermlinkTemplates extends FasterList<Term> {

    static final TermlinkTemplates EMPTY = new TermlinkTemplates(Term.EmptyArray) {
        {
            concepts = 0;
        }
    };

    /** index of the last concept template; any others beyond this index are non-conceptualizable */
    byte concepts = -1;

    public TermlinkTemplates(Term[] terms) {
        super(terms.length, terms);

        if (size > 1)
            sortThisByBoolean((t) -> !conceptualizable(t));

        //scan backwards to find the index where the first non-conceptualizable occurrs
        int lastConcept = size-1;
        for (; lastConcept >= 0; lastConcept--) {
            if (conceptualizable(get(lastConcept)))
                break;
        }
        assert(lastConcept < 127);
        concepts = (byte)(lastConcept+1);
    }

    /** creates a sub-array of the conceptualizable terms and shuffles them */
    public Concept[] conceptsShuffled(NAR nar, boolean conceptualize) {
        int concepts = this.concepts;
        if (concepts == 0)
            return Concept.EmptyArray;

        Concept[] x = new Concept[concepts];
        int nulls = 0;
        for (int i = 0; i < concepts; i++) {
            if ((x[i] = nar.concept(items[i], conceptualize)) == null)
                nulls++;
        }
        if (nulls == concepts)
            return Concept.EmptyArray; //none conceptualizable
        else if (nulls > 0) {
            x = ArrayUtils.removeNulls(x, Concept[]::new);
        }

        if (x.length > 1)
            ArrayUtils.shuffle(x, nar.random());

        return x;
    }

    public static boolean conceptualizable(Term x) {
        return x.op().conceptualizable;
    }

    public void activate(Concept src, float budgeted, NAR nar) {
        int n = this.size();
        if (n == 0)
            return;

        float budgetedToEach = budgeted / n;
//        if (budgetedToEach < Pri.EPSILON)
//            return;

        MutableFloat refund = new MutableFloat(0);

//        int nextTarget = nar.random().nextInt(n);
        Term srcTerm = src.term();
        Bag<Term, PriReference<Term>> srcTermLinks = src.termlinks();
        float balance = nar.termlinkBalance.floatValue();


        for (int i = 0; i < n; i++) {

            Term t = this.get(i);
//            if (nextTarget == n) nextTarget = 0; //wrap around

            Term tgtTerm = null;
            boolean reverseLinked = false;

            boolean conceptualizable = i < concepts;
            if (conceptualizable) {
                @Nullable Concept tgt = nar.conceptualize(t);
                if (tgt != null) {
                    tgt.termlinks().put(
                        new PLink<>(srcTerm, budgetedToEach * (1f - balance)), refund
                    );

                    nar.activate(tgt, budgetedToEach);

                    reverseLinked = true;
                    tgtTerm = tgt.term(); //use the concept's id
                } else {
                    tgtTerm = t;
                }
            } else {
                tgtTerm = t;
            }

            if (!reverseLinked)
                refund.add(budgetedToEach * (1f - balance));

            ((Bag) srcTermLinks).put( new PLink(tgtTerm, budgetedToEach * balance), refund );

        }

//        float r = refund.floatValue();
//        float cost = budgeted - r;
//        return cost;

    }

}
