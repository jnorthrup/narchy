package nars.concept.builder;

import jcog.bag.Bag;
import jcog.bag.impl.CurveBag;
import jcog.list.FasterList;
import jcog.pri.PriReference;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.concept.BaseConcept;
import nars.concept.Concept;
import nars.concept.dynamic.DynamicBeliefTable;
import nars.concept.dynamic.DynamicConcept;
import nars.concept.dynamic.DynamicTruthModel;
import nars.concept.state.ConceptState;
import nars.concept.state.DefaultConceptState;
import nars.table.*;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.container.Subterms;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

import static nars.Op.*;

//import org.eclipse.collections.impl.map.mutable.ConcurrentHashMapUnsafe;

/**
 * Created by me on 2/24/16.
 */
public class DefaultConceptBuilder implements ConceptBuilder {

    public DefaultConceptBuilder() {
        this(
                new DefaultConceptState("sleep", 64, 64, 8),
                new DefaultConceptState("awake", 64, 64, 8)
        );
    }

    public DefaultConceptBuilder(ConceptState sleep, ConceptState awake) {
        this.sleep = sleep;
        this.init = awake;
        this.awake = awake;
    }


    @NotNull
    private final ConceptState init;
    @NotNull
    private final ConceptState awake;
    @NotNull
    private final ConceptState sleep;
    private NAR nar;

    @Override
    public Bag[] newLinkBags(Term t) {
        int v = t.volume();
        //if (/*v > 3 && */v < 16) {
//        Map sharedMap = newBagMap(v);
        Random rng = nar.random();
        Bag<Term, PriReference<Term>> termbag =
                new CurveBag<>(Param.termlinkMerge, newBagMap(v), rng, 0);
        CurveBag<PriReference<Task>> taskbag =
                new TaskLinkCurveBag(newBagMap(v), rng);

        return new Bag[] {  termbag, taskbag }

                ;
//        } else {
//            return new Bag[]{
//                    new MyDefaultHijackBag(Param.termlinkMerge),
//                    new MyDefaultHijackBag(Param.tasklinkMerge)
//            };
//        }

    }


    private BaseConcept newTaskConcept(final Term t) {
        DynamicTruthModel dmt = null;

        final Subterms ts = t.subterms();
        switch (t.op()) {

            case INH:

                Term subj = ts.sub(0);
                Term pred = ts.sub(1);

                Op so = subj.op();
                Op po = pred.op();

                if (dmt == null && (po.atomic || po == PROD || po.isSet())) {
                    if ((so == Op.SECTi) || (so == Op.SECTe) || (so == Op.DIFFi) || (subj instanceof Int.IntRange) || (so == PROD && subj.OR(Int.IntRange.class::isInstance))) {
                        //(P --> M), (S --> M), notSet(S), notSet(P), neqCom(S,P) |- ((S | P) --> M), (Belief:Intersection)
                        //(P --> M), (S --> M), notSet(S), notSet(P), neqCom(S,P) |- ((S & P) --> M), (Belief:Union)
                        //(P --> M), (S --> M), notSet(S), notSet(P), neqCom(S,P) |- ((P ~ S) --> M), (Belief:Difference)


                        Subterms subjsubs = subj.subterms();
                        if (validUnwrappableSubterms(subjsubs)) {
                            int s = subjsubs.subs();
                            FasterList<Term> lx = new FasterList(0, new Term[s]);
                            if (subj instanceof Int.IntRange || so == PROD && subj.hasAny(INT)) {
                                Int.unroll(subj).forEachRemaining(dsi -> lx.add(INH.the(dsi, pred)));
                            }
                            if (so != PROD) {
                                for (int i = 0; i < s; i++) {
                                    Term csi = subjsubs.sub(i);
                                    //                                if (csi instanceof Int.IntRange) {
                                    //                                    //TODO??
                                    ////                                    lx.add(
                                    ////
                                    ////                                            Int.unroll(subj).forEachRemaining(dsi -> lx.add(INH.the(dsi, pred)));
                                    //                                } else {
                                    Term x = INH.the(csi, pred);
                                    assert (!(x instanceof Bool) && !(x instanceof Variable)): "(" + csi + " --> " + pred + ") produced invalid term as part of " + t;
                                    lx.add(x);
                                    //                                }
                                }
                            }


                            if (lx.size() > 1 && validUnwrappableSubterms(lx)) {
                                Term[] x = lx.toArrayRecycled(Term[]::new);
                                switch (so) {
                                    case INT:
                                    case PROD:
                                    case SECTi:
                                        dmt = new DynamicTruthModel.Intersection(x);
                                        break;
                                    case SECTe:
                                        dmt = new DynamicTruthModel.Union(x);
                                        break;
                                    case DIFFi:
                                        dmt = new DynamicTruthModel.Difference(x[0], x[1]);
                                        break;
                                }
                            }
                        }


                    } /*else if (po.image) {
                        Compound img = (Compound) pred;
                        Term[] ee;

                        int relation = img.dt();
                        if (relation != DTERNAL) {
                            int s = img.size();
                            ee = new Term[s];

                            for (int j = 1, i = 0; i < s; ) {
                                if (j == relation)
                                    ee[i++] = subj;
                                if (i < s)
                                    ee[i++] = img.sub(j++);
                            }
                        } else {
                            ee = t.toArray();
                        }
                        Compound b = compoundOrNull(INH.the(DTERNAL, $.p(ee), img.sub(0)));
                        if (b != null)
                            dmt = new DynamicTruthModel.Identity(t, b);
                    }*/

                }

                if (dmt == null && (so.atomic || so == PROD || so.isSet())) {
                    if ((po == Op.SECTi) || (po == Op.SECTe) || (po == DIFFe)) {
                        //(M --> P), (M --> S), notSet(S), notSet(P), neqCom(S,P) |- (M --> (P & S)), (Belief:Intersection)
                        //(M --> P), (M --> S), notSet(S), notSet(P), neqCom(S,P) |- (M --> (P | S)), (Belief:Union)
                        //(M --> P), (M --> S), notSet(S), notSet(P), neqCom(S,P) |- (M --> (P - S)), (Belief:Difference)
                        Compound cpred = (Compound) pred;
                        if (validUnwrappableSubterms(cpred.subterms())) {
                            int s = cpred.subs();
                            Term[] x = new Term[s];
                            boolean valid = true;
                            for (int i = 0; i < s; i++) {
                                Term y;
                                if (!validUnwrappableSubterm.test(y = INH.the(subj, cpred.sub(i)))) {
                                    valid = false;
                                    break;
                                }
                                x[i] = y;
                            }

                            if (valid) {
                                switch (po) {
                                    case SECTi:
                                        dmt = new DynamicTruthModel.Union(x);
                                        break;
                                    case SECTe:
                                        dmt = new DynamicTruthModel.Intersection(x);
                                        break;
                                    case DIFFe:
                                        dmt = new DynamicTruthModel.Difference(x[0], x[1]);
                                        break;
                                }
                            }
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

                break;

            case CONJ:
                //allow variables onlyif they are not themselves direct subterms of this
                if (validUnwrappableSubterms(ts)) {
                    dmt = DynamicTruthModel.Intersection.conj;
                }
                break;

            case DIFFe:
                if (validUnwrappableSubterms(ts))
                    dmt = new DynamicTruthModel.Difference(ts.arrayShared());
                break;

            case NEG:
                throw new RuntimeException("negation terms can not be conceptualized as something separate from that which they negate");
        }

        if (dmt != null) {

            BeliefTable beliefs = dmt != null ?
                    new DynamicBeliefTable(t, newTemporalBeliefTable(t), dmt, true) :
                    beliefTable(t, true);

            BeliefTable goals;
            if (goalable(t)) {
                goals = dmt != null ?
                        new DynamicBeliefTable(t, newTemporalBeliefTable(t), dmt, true) :
                        beliefTable(t, false);
            } else {
                goals = BeliefTable.Empty;
            }

            return new DynamicConcept(t, beliefs, goals, nar);
        } else {
            return new BaseConcept(t, this);
        }
    }

    @Override
    public BeliefTable beliefTable(Term c, boolean beliefOrGoal) {
        //TemporalBeliefTable newTemporalTable(final int tCap, NAR nar) {
        //return new HijackTemporalBeliefTable(tCap);
        //return new RTreeBeliefTable(tCap);
        if (!c.hasAny(Op.VAR_QUERY) && (beliefOrGoal ? c.op().beliefable : goalable(c))) {
            return new DefaultBeliefTable(newTemporalBeliefTable(c));
        } else {
            return BeliefTable.Empty;
        }
    }

    @Override
    public TemporalBeliefTable newTemporalBeliefTable(Term c) {
//        if (c.complexity() < 12) {
        return new RTreeBeliefTable();
        //c.complexity() < 6 ? new DisruptorBlockingQueue() : new LinkedBlockingQueue<>()/
//        } else {
//            return new HijackTemporalBeliefTable();
//        }
    }


    @Override
    public QuestionTable questionTable(Term term, boolean questionOrQuest) {
        Op o = term.op();
        if (questionOrQuest ? o.beliefable : o.goalable) {
            //return new HijackQuestionTable(0, 4);
            return new QuestionTable.DefaultQuestionTable();
        } else {
            return QuestionTable.Empty;
        }
    }


    final static Predicate<Term> validUnwrappableSubterm = x -> !(x instanceof Bool) && !(x.unneg() instanceof Variable);

    private static boolean validUnwrappableSubterms(@NotNull Subterms subterms) {
        return subterms.AND(validUnwrappableSubterm);
    }

    private static boolean validUnwrappableSubterms(@NotNull List<Term> subterms) {
        for (Term t : subterms)
            if (!validUnwrappableSubterm.test(t))
                return false;
        return true;
    }

    @Override
    public void start(@NotNull NAR nar) {
        this.nar = nar;
    }


    @Override
    @Nullable
    public Termed apply(Term t, Termed prev) {
        if (prev != null) {
            if (prev instanceof Concept) {
                Concept c = ((Concept) prev);
                if (!c.isDeleted())
                    return c;
            }
        }

        //already a concept, or non-conceptualizable:  assume it is from here
        if (!t.op().conceptualizable) {
            return t;
        }


        if (t.volume() > nar.termVolumeMax.intValue()) {
//            if (Param.DEBUG)
//                throw new UnsupportedOperationException("tried to conceptualize concept too large");
            return null;
        }

        boolean validForTask = Task.validTaskTerm(t, (byte) 0,  /*nar -- checked above */ true);
        Concept c;
        if (validForTask) {
            c = newTaskConcept(t);
        } else {
            //empty belief tables
            c = new BaseConcept(t,
                    BeliefTable.Empty, BeliefTable.Empty, QuestionTable.Empty, QuestionTable.Empty,
                    newLinkBags(t));
        }

        c.state(awake);
        return c;
    }

    @NotNull
    @Override
    public ConceptState init() {
        return init;
    }

    @NotNull
    @Override
    public ConceptState awake() {
        return awake;
    }

    @NotNull
    @Override
    public ConceptState sleep() {
        return sleep;
    }

    @NotNull
    public static Map newBagMap(int volume) {
        //int defaultInitialCap = 0;
        float loadFactor = 0.75f;

//        if (concurrent()) {
////            //return new ConcurrentHashMap(defaultInitialCap, 1f);
////            //return new NonBlockingHashMap(cap);
////            return new org.eclipse.collections.impl.map.mutable.ConcurrentHashMapUnsafe<>();
////            //ConcurrentHashMapUnsafe(cap);
////        } else {
////            return new HashMap(defaultInitialCap, 1f);
//            //   if (volume < 16) {
//            return new ConcurrentHashMap(0, loadFactor);
//
////            } else if (volume < 32) {
////                return new SynchronizedHashMap(0, loadFactor);
////                //return new TrieMap();
////            } else {
////                return new SynchronizedUnifiedMap(0, loadFactor);
////            }
//        } else {
            //return new UnifiedMap(0, loadFactor);
            return new HashMap(0, loadFactor);
//        }

    }

    public boolean concurrent() {
        return nar.exe.concurrent();
    }

//    private class MyDefaultHijackBag extends DefaultHijackBag {
//        public MyDefaultHijackBag(PriMerge merge) {
//            super(merge, 0, 5);
//        }
//
//        @Override
//        protected Random random() {
//            return nar.random();
//        }
//    }
}
