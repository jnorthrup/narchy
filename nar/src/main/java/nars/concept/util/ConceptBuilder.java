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
import nars.table.EmptyBeliefTable;
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
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

import static nars.Op.*;
import static nars.truth.dynamic.AbstractDynamicTruth.table;

/**
 * TODO make this BiFunction<Term,Concept,Concept>
 */
public abstract class ConceptBuilder implements BiFunction<Term, Concept, Concept> {


    /**
     * returns the overlay tables builder for the term, or null if the target is not dynamically truthable
     */
    public static @Nullable ObjectBooleanToObjectFunction<Term, BeliefTable[]> dynamicModel(Compound t) {

        if (!dynamicPreFilter(t))
            return null; //TODO maybe this can answer query questions by index query

        switch (t.op()) {

            case INH:
                return dynamicInh(t);
            case IMPL:
                return dynamicImpl(t);
            case CONJ:
                if (DynamicConjTruth.decomposeableConjEvents(t))
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

    /**  TODO maybe some Indep cases can work if they are isolated to a sub-condition */
    private static boolean dynamicPreFilter(Compound t) {
        return !t.hasAny(Op.VAR_QUERY.bit | Op.VAR_INDEP.bit);
    }

    private static @Nullable ObjectBooleanToObjectFunction<Term, BeliefTable[]> dynamicImpl(Compound t) {

        //TODO allow indep var if they are involved in (contained within) either but not both subj and pred
        if (!t.hasAny(AtomicConstant))
            return null;

        Subterms tt = t.subterms();

        AbstractDynamicTruth c = null;
        if (tt.hasAny(Op.CONJ)) {

            boolean predDyn = DynamicConjTruth.decomposeableConjEvents(tt.sub(1));
            if (predDyn)
                c = DynamicImplConjTruth.ImplPred;

//            if (subjDyn) {
//                if (suo == NEG) {
//                    //c = DynamicImplConjTruth.ImplSubjDisj;
//                } else {
//                    //c = DynamicImplConjTruth.ImplSubjConj;
//                }
//                //c = DynamicImplConjTruth.ImplSubjConj;
//            } else
        }

        AbstractDynamicTruth i = (!(tt.sub(0).unneg() instanceof nars.term.Variable || tt.sub(1) instanceof nars.term.Variable)) ?
            DynamicStatementTruth.Impl //TODO this may be decomposable if the other term is && or ||
            :
            null;

        return i != null ? c != null ? table(i, c) : table(i) : c != null ? table(c) : null;
    }

    private static @Nullable ObjectBooleanToObjectFunction<Term, BeliefTable[]> dynamicInh(Term i) {

        Subterms ii = i.subterms();

        @Nullable AbstractDynamicTruth[] m = dynamicInhSect(ii);
        @Nullable ObjectBooleanToObjectFunction<Term, BeliefTable[]> s = m != null ? table(m) : null;

        //HACK the temporal restriction is until ImageDynamicTruthModel can support dynamic transformation and untransformation of temporal-containing INH, like:
        //  ((believe("-ÈÛWédxÚñB",(y-->$1)) ==>+- (y-->$1))-->(believe,"-ÈÛWédxÚñB",/))
        if ((ii.sub(0).op()!=IMPL) && (ii.sub(1).op()!=IMPL)) {

            Term n = Image.imageNormalize(i);
            if (!i.equals(n) && !(n instanceof Bool)) {
                return s == null ?
                    ((t, bOrG) -> new BeliefTable[]{new ImageBeliefTable(t, bOrG)}) :
                    ((t, bOrG) -> ArrayUtil.add(s.valueOf(t, bOrG), new ImageBeliefTable(t, bOrG)));
            }
        }

        return s;
    }

//    public void on(Conceptor c) {
//        conceptors.put(c.target, c);
//    }

    private static @Nullable AbstractDynamicTruth[] dynamicInhSect(Subterms ii) {
        if (ii.hasAny(Op.CONJ /*| Op.PROD.bit*/)) {

            boolean sc = DynamicConjTruth.decomposeableConj(ii.sub(0)), pc = DynamicConjTruth.decomposeableConj(ii.sub(1));

            if (sc && pc)
                return new AbstractDynamicTruth[] { DynamicStatementTruth.SubjInter, DynamicStatementTruth.PredInter };
            else if (sc)
                return new AbstractDynamicTruth[] { DynamicStatementTruth.SubjInter };
            else if (pc)
                return new AbstractDynamicTruth[] { DynamicStatementTruth.PredInter };
        }
        return null;
    }

    public abstract QuestionTable questionTable(Term term, boolean questionOrQuest);

    public abstract BeliefTable newTable(Term t, boolean beliefOrGoal, BeliefTable... overlay);

    public abstract EternalTable newEternalTable(Term c);

    public abstract TemporalBeliefTable newTemporalTable(Term c, boolean beliefOrGoal);

    private Concept taskConcept(Term t) {

        BeliefTable B, G;
        ObjectBooleanToObjectFunction<Term, BeliefTable[]> dmt = t instanceof Compound ?
                ConceptBuilder.dynamicModel((Compound) t) : null;

        if (dmt != null) {

            //2. handle dynamic truth tables
            B = this.newTable(t, true, dmt.valueOf(t, true));
            G = !t.hasAny(IMPL) ?
                    newTable(t, false, dmt.valueOf(t, false)) :
                    EmptyBeliefTable.Empty;
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
            G = !t.hasAny(IMPL) ? this.newTable(t, false) : EmptyBeliefTable.Empty;
        }


        return new TaskConcept(t, B, G,
                this.questionTable(t, true), this.questionTable(t, false)
        );
    }

    protected abstract Concept nodeConcept(Term t);

    @Override
    public final Concept apply(Term x, Concept prev) {
        return ((prev != null) && !prev.isDeleted()) ? prev : apply(x);
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
