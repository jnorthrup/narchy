package nars.concept.util;

import jcog.bag.Bag;
import nars.Op;
import nars.Task;
import nars.concept.Concept;
import nars.concept.dynamic.DynamicTruthModel;
import nars.subterm.Subterms;
import nars.table.BeliefTable;
import nars.table.QuestionTable;
import nars.table.TemporalBeliefTable;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Predicate;

import static nars.Op.DIFFi;
import static nars.Op.INH;

/**
 * Created by me on 3/23/16.
 */
public interface ConceptBuilder extends BiFunction<Term, Termed, Termed> {

    Predicate<Term> validDynamicSubterm = x -> Task.validTaskTerm(x.unneg());

    static boolean validDynamicSubterms(Subterms subterms) {
        return subterms.AND(validDynamicSubterm);
    }

    @Nullable
    static DynamicTruthModel unroll(Term t) {
        DynamicTruthModel dmt = null;

        final Subterms ts = t.subterms();
        switch (t.op()) {

            case INH:

                Term subj = ts.sub(0);
                Term pred = ts.sub(1);

                Op so = subj.op();
                Op po = pred.op();

                if (dmt == null /*&& (so.atomic || so == PROD || so.isSet())*/) {
                    if (((po == Op.SECTi) || (po == Op.SECTe) || (po == DIFFi))) {
                        
                        
                        
                        Compound cpred = (Compound) pred;
                        int s = cpred.subs();
                        Term[] x = new Term[s];
                        for (int i = 0; i < s; i++) {
                            Term y;
                            if (!validDynamicSubterm.test(y = INH.the(subj, cpred.sub(i))))
                                return null;
                            x[i] = y;
                        }

                        switch (po) {
                            case SECTi:
                                dmt = new DynamicTruthModel.Union(x);
                                break;
                            case SECTe:
                                dmt = new DynamicTruthModel.SectIntersection(x);
                                break;
                            case DIFFi:
                                dmt = new DynamicTruthModel.Difference(x[0], x[1]);
                                break;
                        }
                    } /*else if (so.image) {
                        Compound img = (Compound) subj;
                        Term[] ee = new Term[img.size()];

                        int relation = img.dt();
                        int s = ee.length;
                        for (int j = 1, i = 0; i < s; ) {
                            if (j == relation)
                                ee[i++] = pred;
                            if (i < s)
                                ee[i++] = img.sub(j++);
                        }
                        Compound b = compoundOrNull(INH.the(DTERNAL, img.sub(0), $.p(ee)));
                        if (b != null)
                            dmt = new DynamicTruthModel.Identity(t, b);
                    }*/

                }

                if (dmt == null /* && (po.atomic || po == PROD || po.isSet()) */) {
                    if ((so == Op.SECTi) || (so == Op.SECTe) || (so == Op.DIFFe)

                            ) {
                        
                        
                        


                        Subterms subjsubs = subj.subterms();
                        int s = subjsubs.subs();
                        Term[] x = new Term[s];
                        for (int i = 0; i < s; i++) {
                            Term y;
                            
                            
                            
                            
                            
                            
                            if (!validDynamicSubterm.test(y = INH.the(subjsubs.sub(i), pred)))
                                return null;
                            x[i] = y;
                        }








                        /*if (so != PROD)*/


                        switch (so) {


                            case SECTi:
                                dmt = new DynamicTruthModel.SectIntersection(x);
                                break;
                            case SECTe:
                                dmt = new DynamicTruthModel.Union(x);
                                break;
                            case DIFFe:
                                dmt = new DynamicTruthModel.Difference(x[0], x[1]);
                                break;
                        }


                    }

                }


                break;

            case CONJ:
                
                if (validDynamicSubterms(ts)) {
                    dmt = DynamicTruthModel.Intersection.ConjIntersection.the;
                }
                break;

            case DIFFe:
                
                if (validDynamicSubterms(ts))
                    dmt = new DynamicTruthModel.Difference(ts.arrayShared());
                break;

            case NEG:
                throw new RuntimeException("negation terms can not be conceptualized as something separate from that which they negate");
        }
        return dmt;
    }

    ConceptState init();
    ConceptState awake();
    ConceptState sleep();

    QuestionTable questionTable(Term term, boolean questionOrQuest);
    BeliefTable newTable(Term t, boolean beliefOrGoal);
    TemporalBeliefTable newTemporalTable(Term c);



    /** passes through terms without creating any concept anything */
    ConceptBuilder Null = new ConceptBuilder() {

        @Override
        public Concept build(Term term) {
            return null;
        }


        @Override
        public ConceptState init() {
            return ConceptState.Deleted;
        }

        @Override
        public ConceptState awake() {
            return ConceptState.Deleted;
        }

        @Override
        public ConceptState sleep() {
            return ConceptState.Deleted;
        }

        @Override
        public TemporalBeliefTable newTemporalTable(Term c) {
            return TemporalBeliefTable.Empty;
        }

        @Override
        public BeliefTable newTable(Term t, boolean beliefOrGoal) {
            return BeliefTable.Empty;
        }

        @Override
        public QuestionTable questionTable(Term term, boolean questionOrQuest) {
            return QuestionTable.Empty;
        }

        @Override
        public Bag[] newLinkBags(Term term) {
            return new Bag[] { Bag.EMPTY, Bag.EMPTY };
        }
    };


    Bag[] newLinkBags(Term term);

    Concept build(Term term);

    @Override
    default Termed apply(Term x, Termed prev) {
        if (prev != null) {
            
                Concept c = ((Concept) prev);
                if (!c.isDeleted())
                    return c;
            
        }

        return apply(x);
    }

    @Nullable
    default Termed apply(Term x) {

        x = x.the();

        Concept c = build(x);
        if (c == null) {
            return null;
        }

        ConceptState s = c.state();

        if (s == ConceptState.New || s == ConceptState.Deleted) {
            c.state(init());
        }

        return c;
    }

}
