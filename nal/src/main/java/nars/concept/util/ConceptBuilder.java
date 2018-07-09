package nars.concept.util;

import jcog.bag.Bag;
import nars.Op;
import nars.Task;
import nars.concept.Concept;
import nars.concept.dynamic.DynamicTruthModel;
import nars.link.TemplateTermLinker;
import nars.link.TermLinker;
import nars.subterm.Subterms;
import nars.table.BeliefTable;
import nars.table.QuestionTable;
import nars.table.TemporalBeliefTable;
import nars.term.Compound;
import nars.term.Conceptor;
import nars.term.Term;
import nars.term.Termed;
import nars.term.compound.util.Image;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Predicate;

import static nars.Op.*;

/**
 * Created by me on 3/23/16.
 */
public interface ConceptBuilder extends BiFunction<Term, Termed, Termed> {

    Predicate<Term> validDynamicSubterm = x -> Task.validTaskTerm(x.unneg());
    /**
     * passes through terms without creating any concept anything
     */
    ConceptBuilder NullConceptBuilder = new ConceptBuilder() {

        @Override
        public void on(Conceptor c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Concept build(Term term) {
            return null;
        }

        @Override
        public TermLinker termlinker(Term term) {
            return TermLinker.Empty;
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
            return new Bag[]{Bag.EMPTY, Bag.EMPTY};
        }
    };

    static boolean validDynamicSubterms(Subterms subterms) {
        return subterms.AND(validDynamicSubterm);
    }

    /**
     * returns the builder for the term, or null if the term is not dynamically truthable
     */
    @Nullable
    static DynamicTruthModel dynamicModel(Term t) {

        if (t.hasAny(Op.VAR_QUERY.bit))
            return null; //TODO maybe this can answer query questions by index query

        switch (t.op()) {

            case INH:
                return dynamicInh(t);

            case SIM:
                //TODO NAL2 set identities?
                break;

                //TODO not done yet
            case IMPL:
                Term subj = t.sub(0);
                /* TODO:
                    ((&&,x,y,z,...) ==> z) from (x ==> z) and (y ==> z) //intersect pre
                    (--(--x && --y) ==> z) from (x ==> z) and (y ==> z) //union pre
                    (z ==> (x && y))  //intersect conc
                    (z ==> --(--x && --y))  //union conc
                 */
                Term su = subj;
                if (su.op() == CONJ && validDynamicSubterms(su.subterms())) {
                    return DynamicTruthModel.IsectSubj;
                }
                //TODO if subj is negated
                break;

            case CONJ:
                if (validDynamicSubterms(t.subterms()))
                    return DynamicTruthModel.ConjIntersection;
                break;

            case DIFFe:
                if (validDynamicSubterms(t.subterms()))
                    return DynamicTruthModel.DiffRoot;
                break;

            case NEG:
                throw new RuntimeException("negation terms can not be conceptualized as something separate from that which they negate");
        }
        return null;
    }

    static DynamicTruthModel dynamicInh(Term t) {

        //quick pre-test
        Subterms tt = t.subterms();
        if (!tt.hasAny(Op.SectBits | Op.DiffBits | Op.PROD.bit))
            return null;

        if ((tt.OR(s -> s.isAny(Op.SectBits | Op.DiffBits)))) {


            DynamicTruthModel dmt = null;
            Term subj = tt.sub(0);
            Term pred = tt.sub(1);

            Op so = subj.op();
            Op po = pred.op();


            if ((so == Op.SECTi) || (so == Op.SECTe) || (so == Op.DIFFe)

            ) {

                //TODO move this to impl-specific test function
                Subterms subjsubs = subj.subterms();
                int s = subjsubs.subs();
//                Term[] x = new Term[s];
                for (int i = 0; i < s; i++) {
                    Term y;
                    if (!validDynamicSubterm.test(y = INH.the(subjsubs.sub(i), pred)))
                        return null;
//                    x[i] = y;
                }

                switch (so) {
                    case SECTi:
                        return DynamicTruthModel.IsectSubj;
                    case SECTe:
                        return DynamicTruthModel.UnionSubj;
                    case DIFFe:
                        return DynamicTruthModel.DiffSubj;
                }


            }


            if (((po == Op.SECTi) || (po == Op.SECTe) || (po == DIFFi))) {


                Compound cpred = (Compound) pred;
                int s = cpred.subs();
//                Term[] x = new Term[s];
                for (int i = 0; i < s; i++) {
                    Term y;
                    if (!validDynamicSubterm.test(y = INH.the(subj, cpred.sub(i))))
                        return null;
//                    x[i] = y;
                }

                switch (po) {
                    case SECTi:
                        return DynamicTruthModel.UnionPred;
                    case SECTe:
                        return DynamicTruthModel.IsectPred;
                    case DIFFi:
                        return DynamicTruthModel.DiffPred;
                }
            }
        }
        Term iNorm = Image.imageNormalize(t);
        if (!iNorm.equals(t)) {
            return DynamicTruthModel.ImageIdentity;
        }
        return null;
    }

    ConceptState init();

    ConceptState awake();

    ConceptState sleep();

    QuestionTable questionTable(Term term, boolean questionOrQuest);

    BeliefTable newTable(Term t, boolean beliefOrGoal);

    TemporalBeliefTable newTemporalTable(Term c);

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
        if (x == null)
            return null;

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

    /** register a Conceptor */
    void on(Conceptor c);

    default TermLinker termlinker(Term term) {
        return TemplateTermLinker.of(term);
    }
}
