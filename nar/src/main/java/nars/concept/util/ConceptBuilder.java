package nars.concept.util;

import jcog.WTF;
import jcog.util.ArrayUtil;
import nars.Op;
import nars.Task;
import nars.concept.Concept;
import nars.concept.NodeConcept;
import nars.concept.TaskConcept;
import nars.subterm.Subterms;
import nars.table.BeliefTable;
import nars.table.dynamic.ImageBeliefTable;
import nars.table.eternal.EternalTable;
import nars.table.question.QuestionTable;
import nars.table.temporal.TemporalBeliefTable;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.util.Image;
import nars.truth.dynamic.AbstractDynamicTruth;
import nars.truth.dynamic.DynamicConjTruth;
import nars.truth.dynamic.DynamicImplConjTruth;
import nars.truth.dynamic.DynamicStatementTruth;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanToObjectFunction;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.truth.dynamic.AbstractDynamicTruth.table;

/**
 * TODO make this BiFunction<Term,Concept,Concept>
 */
public abstract class ConceptBuilder implements BiFunction<Term, Concept, Concept> {

    private static final Predicate<Term> validDynamicSubterm = x -> Task.validTaskTerm(x.unneg());

    private static boolean validDynamicSubterms(Subterms subterms) {
        return subterms.AND(validDynamicSubterm);
    }

    private static boolean validDynamicSubtermsAndNoSharedVars(Term conj) {
        Subterms conjSubterms = conj.subterms();
        if (validDynamicSubterms(conjSubterms)) {
            if (conjSubterms.hasAny(VAR_DEP)) {

                Map<Term, Term> varLocations = new UnifiedMap(conjSubterms.subs());

                return conj.eventsAND((when, event) ->
                                !event.hasAny(VAR_DEP) ||
                                        event.recurseTerms(x -> x.hasAny(VAR_DEP),
                                                (possiblyVar, parent) ->
                                                        (possiblyVar.op() != VAR_DEP) ||
                                                                varLocations.putIfAbsent(possiblyVar, event) == null
                                                , null)

                        , 0, true, true);
            }
            return true;
        }
        return false;
    }

    /**
     * returns the overlay tables builder for the term, or null if the target is not dynamically truthable
     */
    @Nullable
    public static ObjectBooleanToObjectFunction<Term, BeliefTable[]> dynamicModel(Compound t) {

        if (t.hasAny(Op.VAR_QUERY.bit))
            return null; //TODO maybe this can answer query questions by index query

        switch (t.op()) {

            case INH:
                return dynamicInh(t);
            case IMPL:
                return dynamicImpl(t);
            case CONJ:
                if (validDynamicSubtermsAndNoSharedVars(t))
                    return table(DynamicConjTruth.ConjIntersection);
                break;

//            case SIM:
//                //TODO NAL2 set identities?
//                break;

            case NEG:
                throw new RuntimeException("negation terms can not be conceptualized as something separate from that which they negate");
        }
        return null;
    }

    private static @Nullable ObjectBooleanToObjectFunction<Term, BeliefTable[]> dynamicImpl(Compound t) {

        //TODO allow indep var if they are involved in (contained within) either but not both subj and pred
        if (t.hasAny(Op.VAR_INDEP.bit | Op.VAR_QUERY.bit))
            return null;

        if (t.sub(0).unneg() instanceof nars.term.Variable || t.sub(1) instanceof nars.term.Variable)
            return null; //TODO this may be decomposable if the other term is && or ||

        AbstractDynamicTruth c = null;
        if (t.hasAny(Op.CONJ)) {

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
                //remember: these are reversed (NAL6)
                if (suo != NEG) {
                    c = DynamicImplConjTruth.ImplSubjConj;
                } else {
                    c = DynamicImplConjTruth.ImplSubjDisj;
                }
            } else if (predDyn) {
                c = DynamicImplConjTruth.ImplPred;
            }
        }

        return c == null ? table(DynamicStatementTruth.Impl) : table(DynamicStatementTruth.Impl, c);
    }

    private static @Nullable ObjectBooleanToObjectFunction<Term, BeliefTable[]> dynamicInh(Term i) {

        Subterms ii = i.subterms();

        @Nullable AbstractDynamicTruth m = dynamicInhSect(ii);
        @Nullable ObjectBooleanToObjectFunction<Term, BeliefTable[]> s = m != null ? table(m) : null;

        if (!ii.hasAny(Temporal | VAR_INDEP.bit)) {
            //HACK the temporal restriction is until ImageDynamicTruthModel can support dynamic transformation and untransformation of temporal-containing INH, like:
            //  ((believe("-ÈÛWédxÚñB",(y-->$1)) ==>+- (y-->$1))-->(believe,"-ÈÛWédxÚñB",/))
            Term it = Image.imageNormalize(i);
            if (!(it instanceof Bool) && !i.equals(it)) {
                if (s == null)
                    return (term, bOrG) -> new BeliefTable[]{new ImageBeliefTable(term, bOrG)};
                else
                    return (term, bOrG) -> {
                        return ArrayUtil.add(s.valueOf(term, bOrG),
                                             new ImageBeliefTable(term, bOrG));
                    };
            }
        }

        return null;
    }

//    public void on(Conceptor c) {
//        conceptors.put(c.target, c);
//    }

    @Nullable
    private static AbstractDynamicTruth dynamicInhSect(Subterms ii) {
        if (ii.hasAny(Op.CONJ /*| Op.PROD.bit*/)) {

            Term s = ii.sub(0), p = ii.sub(1);

            Op so = s.op();
            Term su = s.unneg();
            if (so == Op.CONJ || (so == NEG && su.op() == CONJ)) {
                if (su.subterms().AND(z -> validDynamicSubterm.test(INH.the(z, p)))) {
                    switch (so) {
                        case CONJ:
                            return DynamicStatementTruth.SubjInter;
                        case NEG:
                            return DynamicStatementTruth.SubjUnion;
                    }
                }
            }

            Op po = p.op();
            Term pu = p.unneg();
            if (po == Op.CONJ || (po == NEG && pu.op() == CONJ)) {
                if (pu.subterms().AND(z -> validDynamicSubterm.test(INH.the(s, z)))) {
                    switch (po) {
                        case CONJ:
                            return DynamicStatementTruth.PredInter;
                        case NEG:
                            return DynamicStatementTruth.PredUnion;
                    }
                }
            }
        }
        return null;
    }

    public abstract QuestionTable questionTable(Term term, boolean questionOrQuest);

    public abstract BeliefTable newTable(Term t, boolean beliefOrGoal, BeliefTable... overlay);

    public abstract EternalTable newEternalTable(Term c);

    public abstract TemporalBeliefTable newTemporalTable(Term c, boolean beliefOrGoal);

    private Concept taskConcept(final Term t) {

        BeliefTable B, G;
        ObjectBooleanToObjectFunction<Term, BeliefTable[]> dmt = t instanceof Compound ?
                ConceptBuilder.dynamicModel((Compound) t) : null;

        if (dmt != null) {

            //2. handle dynamic truth tables
            B = this.newTable(t, true, dmt.valueOf(t, true));
            G = !t.hasAny(IMPL) ?
                    newTable(t, false, dmt.valueOf(t, false)) :
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
                this.questionTable(t, true), this.questionTable(t, false)
        );
    }

    protected abstract Concept nodeConcept(final Term t);

    @Override
    public final Concept apply(Term x, Concept prev) {
        if (prev != null) {
            Concept c = prev;
            if (!c.isDeleted())
                return c;
        }

        return apply(x);
    }


    public final Concept apply(Term x) {

        Concept c = construct(x);

        start(c);

        return c;
    }

    /**
     * constructs a concept but does no capacity allocation (result will have zero capacity, except dynamic abilities)
     */
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
        ((NodeConcept) c).meta.clear(); //HACK remove any deleted state
    }


//    /**
//     * passes through terms without creating any concept anything
//     */
//    public static final ConceptBuilder NullConceptBuilder = new ConceptBuilder() {
//
////        @Override
////        public void on(Conceptor c) {
////            throw new UnsupportedOperationException();
////        }
//
//        @Override
//        public Concept nodeConcept(Term t) {
//            throw new UnsupportedOperationException();
//        }
//
//
//        @Override
//        public TermLinker termlinker(Term term) {
//            return TermLinker.NullLinker;
//        }
//
//        @Override
//        public TemporalBeliefTable newTemporalTable(Term c, boolean beliefOrGoal) {
//            return null;
//        }
//
//        @Override
//        public BeliefTable newTable(Term t, boolean beliefOrGoal) {
//            return BeliefTable.Empty;
//        }
//
//        @Override
//        public EternalTable newEternalTable(Term c) {
//            return null;
//        }
//
//        @Override
//        public QuestionTable questionTable(Term term, boolean questionOrQuest) {
//            return QuestionTable.Empty;
//        }
//
//    };

}
