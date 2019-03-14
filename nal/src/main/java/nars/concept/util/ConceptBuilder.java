package nars.concept.util;

import jcog.WTF;
import nars.Op;
import nars.Task;
import nars.concept.Concept;
import nars.concept.NodeConcept;
import nars.concept.TaskConcept;
import nars.link.TermLinker;
import nars.subterm.Subterms;
import nars.table.BeliefTable;
import nars.table.eternal.EternalTable;
import nars.table.question.QuestionTable;
import nars.table.temporal.TemporalBeliefTable;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Bool;
import nars.term.util.Image;
import nars.truth.dynamic.AbstractDynamicTruth;
import nars.truth.dynamic.DynamicConjTruth;
import nars.truth.dynamic.DynamicImageTruth;
import nars.truth.dynamic.DynamicStatementTruth;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import static nars.Op.*;

/**
 * TODO make this BiFunction<Term,Concept,Concept>
 */
public abstract class ConceptBuilder implements BiFunction<Term, Termed, Termed> {

//    private final Map<Term, Conceptor> conceptors = new ConcurrentHashMap<>();

    public abstract QuestionTable questionTable(Term term, boolean questionOrQuest);

    public abstract BeliefTable newTable(Term t, boolean beliefOrGoal);

    public abstract EternalTable newEternalTable(Term c);

    public abstract TemporalBeliefTable newTemporalTable(Term c, boolean beliefOrGoal);

    private Concept taskConcept(final Term t) {

        BeliefTable B, G;


        AbstractDynamicTruth dmt = ConceptBuilder.dynamicModel(t);

        if (dmt != null) {

            //2. handle dynamic truth tables
            B = dmt.newTable(t, true, this);
            G = !t.hasAny(IMPL) ?
                    dmt.newTable(t, false, this) :
                    BeliefTable.Empty;
        } else {
//                //3. handle dynamic conceptualizers (experimental)
//                Term conceptor = Functor.func(t);
//                if (conceptor != Bool.Null) {
//                    @Nullable Conceptor cc = conceptors.get(conceptor);
//                    if (cc instanceof Conceptor) {
//
//                        Concept x = cc.apply(conceptor, Operator.args(t));
//                        if (x != null)
//                            return x;
//                    }
//                }

            //4. default task concept
            B = this.newTable(t, true);
            G = !t.hasAny(IMPL) ? this.newTable(t, false) : BeliefTable.Empty;
        }



        return new TaskConcept(t, B, G,
                this.questionTable(t, true), this.questionTable(t, false),
                this.termlinker(t));
    }


    protected abstract Concept nodeConcept(final Term t);

//    public void on(Conceptor c) {
//        conceptors.put(c.target, c);
//    }


    private static final Predicate<Term> validDynamicSubterm = x -> Task.validTaskTerm(x.unneg());


    private static boolean validDynamicSubterms(Subterms subterms) {
        return subterms.AND(validDynamicSubterm);
    }


    private static boolean validDynamicSubtermsAndNoSharedVars(Term conj) {
        Subterms conjSubterms = conj.subterms();
        if (validDynamicSubterms(conjSubterms)) {
            if (conjSubterms.hasAny(VAR_DEP)) {

                Map<Term, Term> varLocations = new UnifiedMap(conjSubterms.subs());

                return conj.eventsWhile((when, event) ->
                                !event.hasAny(VAR_DEP) ||
                                        event.recurseTerms(x -> x.hasAny(VAR_DEP),
                                                (possiblyVar, parent) ->
                                                        (possiblyVar.op() != VAR_DEP) ||
                                                                varLocations.putIfAbsent(possiblyVar, event) == null
                                                , null)

                        , 0, true, true, true);
            }
            return true;
        }
        return false;
    }

    /**
     * returns the builder for the target, or null if the target is not dynamically truthable
     */
    @Nullable
    public static AbstractDynamicTruth dynamicModel(Term t) {

        if (t.hasAny(Op.VAR_QUERY.bit))
            return null; //TODO maybe this can answer query questions by index query

        switch (t.op()) {

            case INH:
                return dynamicInh(t);

            case SIM:
                //TODO NAL2 set identities?
                break;

//            //TODO not done yet
            case IMPL: {
                //TODO allow indep var if they are involved in (contained within) either but not both subj and pred
                if (t.hasAny(Op.VAR_INDEP))
                    return null;
                Subterms tt = t.subterms();
                Term su = tt.sub(0);
//                if (su.hasAny(Op.VAR_INDEP))
//                    return null;
                Term pu = tt.sub(1);
//                if (pu.hasAny(Op.VAR_INDEP))
//                    return null;

                Op suo = su.op();
                //subject has special negation union case
                boolean subjDyn = (
                        suo == CONJ && validDynamicSubtermsAndNoSharedVars(su)
                                ||
                                suo == NEG && (su.unneg().op() == CONJ && validDynamicSubtermsAndNoSharedVars(su.unneg()))
                );
                boolean predDyn = (pu.op() == CONJ && validDynamicSubtermsAndNoSharedVars(pu));


                if (subjDyn && predDyn) {
                    //choose the simpler to dynamically calculate for
                    if (su.volume() <= pu.volume()) {
                        predDyn = false; //dyn subj
                    } else {
                        subjDyn = false; //dyn pred
                    }
                }

                if (subjDyn) {
                    if (suo == NEG) {
                        return DynamicStatementTruth.UnionImplSubj;
//                        return DynamicTruthModel.DynamicSectTruth.SectImplSubjNeg;
                    } else {
                        return DynamicStatementTruth.SectImplSubj;
                    }
                } else if (predDyn) {
                    //TODO infer union case if the subterms of the pred's conj are all negative
                    return DynamicStatementTruth.SectImplPred;
                }

                break;
            }

            case CONJ:
                if (validDynamicSubtermsAndNoSharedVars(t))
                    return DynamicConjTruth.ConjIntersection;
                break;

//            case SECTe:
//                if (validDynamicSubterms(t.subterms()))
//                    return DynamicTruthModel.DynamicSectTruth.SectRoot;
//                break;

            case NEG:
                throw new RuntimeException("negation terms can not be conceptualized as something separate from that which they negate");
        }
        return null;
    }

    private static AbstractDynamicTruth dynamicInh(Term t) {

        //quick pre-test
        Subterms tt = t.subterms();
        if (tt.hasAny(Op.Sect | Op.PROD.bit)) {


            if ((tt.OR(s -> s.isAny(Op.Sect)))) {


                Term subj = tt.sub(0), pred = tt.sub(1);

                Op so = subj.op(), po = pred.op();


                if ((so == Op.SECTi) || (so == Op.SECTe)) {

                    //TODO move this to impl-specific test function
                    Subterms subjsubs = subj.subterms();
                    int s = subjsubs.subs();
                    for (int i = 0; i < s; i++) {
                        if (!validDynamicSubterm.test(INH.the(subjsubs.sub(i), pred)))
                            return null;
                    }

                    switch (so) {
                        case SECTi:
                            return DynamicStatementTruth.SectSubj;
                        case SECTe:
                            return DynamicStatementTruth.UnionSubj;
                    }


                }


                if (((po == Op.SECTi) || (po == Op.SECTe))) {


                    Compound cpred = (Compound) pred;
                    int s = cpred.subs();
                    for (int i = 0; i < s; i++) {
                        if (!validDynamicSubterm.test(INH.the(subj, cpred.sub(i))))
                            return null;
                    }

                    switch (po) {
                        case SECTi:
                            return DynamicStatementTruth.UnionPred;
                        case SECTe:
                            return DynamicStatementTruth.SectPred;
                    }
                }
            }
        }

        Term it = Image.imageNormalize(t);
        if (it != t && !(it instanceof Bool))
            return DynamicImageTruth.ImageDynamicTruthModel;

        return null;
    }

    @Override
    public final Termed apply(Term x, Termed prev) {
        if (prev != null) {
            Concept c = ((Concept) prev);
            if (!c.isDeleted())
                return c;
        }

        return apply(x);
    }



    public final Termed apply(Term x) {

        Concept c = construct(x);

        start(c);

        return c;
    }

    /** constructs a concept but does no capacity allocation (result will have zero capacity, except dynamic abilities) */
    public final Concept construct(Term x) {
        Concept c = Task.validTaskTerm(x) ? taskConcept(x) : nodeConcept(x);
        if (c == null)
            throw new WTF(x + " unconceptualizable");
        return c;
    }

    /**
     * called after constructing a new concept, or after a permanent concept has been installed
     */
    public void start(Concept c) {
        ((NodeConcept)c).meta.clear(); //HACK remove deleted state
    }

    abstract public TermLinker termlinker(Term term);

    /**
     * passes through terms without creating any concept anything
     */
    public static final ConceptBuilder NullConceptBuilder = new ConceptBuilder() {

//        @Override
//        public void on(Conceptor c) {
//            throw new UnsupportedOperationException();
//        }

        @Override
        public Concept nodeConcept(Term t) {
            throw new UnsupportedOperationException();
        }


        @Override
        public TermLinker termlinker(Term term) {
            return TermLinker.NullLinker;
        }

        @Override
        public TemporalBeliefTable newTemporalTable(Term c, boolean beliefOrGoal) {
            return null;
        }

        @Override
        public BeliefTable newTable(Term t, boolean beliefOrGoal) {
            return BeliefTable.Empty;
        }

        @Override
        public EternalTable newEternalTable(Term c) {
            return null;
        }

        @Override
        public QuestionTable questionTable(Term term, boolean questionOrQuest) {
            return QuestionTable.Empty;
        }

    };

}
