package nars.concept;

import jcog.math.FloatParam;
import nars.NAR;
import nars.Param;
import nars.table.BeliefTable;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

/**
 * base class for concepts which are more or less programmatically "hard-wired" into
 * external systems and transducers that populate certain segments of its
 * belief tables and other components.
 *
 * this usually requires some specific management of
 * beliefs to prevent influence from derivations that the reasoner may form
 * in contradiction with provided values.
 *
 * warning: using action and sensor concepts with a term that can be structurally transformed
 * culd have unpredictable results because their belief management policies
 * may not be consistent with the SensorConcept.  one solution may be to
 * create dummy placeholders for all possible transforms of a sensorconcept term
 * to make them directly reflect the sensor concept as the authority.
 *
 * */
public class WiredConcept extends TaskConcept implements PermanentConcept {


    public FloatParam resolution = new FloatParam(Param.TRUTH_EPSILON, 0f, 1f);

    protected WiredConcept(@NotNull Term term, BeliefTable beliefs, BeliefTable goals, @NotNull NAR n) {
        super(term, beliefs, goals, n.terms.conceptBuilder);
    }



//    @Override
//    public void process(@NotNull Task t, @NotNull NAR n) {
//        if (t.isEternal())
//            return;
//        super.process(t, n);
//    }

    // THIS DITHERING AND CLONING IS DANGEROUS
//    public void process(@NotNull Task t, @NotNull NAR n) {
//        //dither the task frequency
//        if (!t.isInput() && /*t.isBeliefOrGoal()*/ t.isBelief()) {
//            float f = t.freq();
//            float r = resolution.floatValue();
//            float ff = Util.round(f, r);
//            if (f!=ff) {
//                Truth tr = t.truth().ditherFreqAdjustConf(r);
//                if (tr == null)
//                    return;
//
//                Task tt = new NALTask(t.term(), t.punc(),
//                        tr,
//                        t.creation(), t.start(), t.end(), t.stamp()
//                );
//                tt.pri(t.pri());
//                t = tt;
//            }
//        }
//
//        super.process(t, n);
//    }


//    @Override
//    public TemporalBeliefTable newTemporalTable(int tCap, NAR nar) {
//        return
//            //new MyListTemporalBeliefTable(tCap * 2, tCap * 4);
//            new HijackTemporalExtendedBeliefTable2(tCap, tCap*4) {
//                @Override
//                protected boolean include(Task t) {
//                    return true;
//                    //return t instanceof SignalTask || t.isGoal();
//                }
//            };
//    }

//   @Override
//    public TemporalBeliefTable newTemporalTable(int tCap, NAR nar) {
//        return
//
//            new HijackTemporalBeliefTable(tCap) {
//            //new MyListTemporalBeliefTable(tCap * 2, tCap * 4);
//            //new HijackTemporalExtendedBeliefTable2(tCap, tCap*4) {
////                @Override
////                protected boolean include(Task t) {
////                    return true;
////                    //return t instanceof SignalTask || t.isGoal();
////                }
//
//                @Override
//                public Truth truth(long when, int dur, @Nullable EternalTable eternal) {
//                    Truth tt = super.truth(when, dur, eternal);
//                    return tt!=null ? tt.ditherFreq(resolution.floatValue()) : null;
//                }
//            };
//    }

    //    @Override
//    protected TermContainer buildTemplates(Compound term, NAR nar) {
//        if (term.volume()==2 && term.op() == Op.PROD) {
//            //special case. these are atom-like products of 1 term
//            return Terms.NoSubterms;
//        }
//        return super.buildTemplates(term, nar);
//    }
//
//    @Nullable protected Task filter(@NotNull Task t, @NotNull BeliefTable table, @Nullable BiPredicate<Task,NAR> valid, @NotNull NAR nar, @NotNull List<Task> displaced) {
//
//        if (valid!=null) {
//            if (!table.isEmpty() /*&& ((DefaultBeliefTable)beliefs()).temporal.isFull()*/) {
//                //try to remove at least one past belief which did not originate from this sensor
//                //this should clear space for future predictions
//                TemporalBeliefTable tb = ((DefaultBeliefTable) table).temporal;
//                tb.removeIf(x -> !valid.test(x, nar), displaced);
//            }
//
//            if (!valid.test(t, nar)) {
//
//                //TODO delete its non-input parent tasks?
//                onConflict(t);
//
//                //TaskTable.removeTask(t, "Ignored Speculation", displaced); //will be displaced normally by returning null
//                return null;
//            }
//        }
//
//        return t;
//    }

//    @Override
//    public TruthDelta processBelief(@NotNull Task t, @NotNull NAR nar, @NotNull List<Task> displaced) {
//        t = filterBeliefs(t, nar, displaced);
//        if (t != null) {
//            TruthDelta td = super.processBelief(t, nar, displaced);
//            if (td != null) {
//                //executeLater(t, nar);
//                return td;
//            }
//        }
//        return null;
//    }
//
//    @Override
//    public TruthDelta processGoal(@NotNull Task t, @NotNull NAR nar, @NotNull List<Task> displaced) {
//        t = filterGoals(t, nar, displaced);
//        if (t != null) {
//            TruthDelta td = super.processGoal(t, nar, displaced);
//            if (td != null) {
//                //executeLater(t, nar);
//                return td;
//            }
//        }
//        return null;
//    }

//    /** NOTE: if validBelief always returns true, then this can be bypassed by overriding with blank method */
//    public @Nullable Task filterBeliefs(@NotNull Task t, @NotNull NAR nar, @NotNull List<Task> displaced) {
//        t = filter(t, beliefs(), null, nar, displaced);
//        return t;
//    }
//
//    /** NOTE: if validGoal always returns true, then this can be bypassed by overriding with blank method */
//    public @Nullable Task filterGoals(@NotNull Task t, @NotNull NAR nar, @NotNull List<Task> displaced) {
//        t = filter(t, goals(), null, nar, displaced);
//        return t;
//    }


//    @Nullable
//    private Task executeLater(@Nullable Task t, @NotNull NAR nar) {
//        if (t != null && pendingRun.compareAndSet(false, true)) {
//            nar.runLater(this);
//        }
//
//        return t;
//    }

//    /** called at most once per frame */
//    @Override public final void accept(NAR n) {
//        pendingRun.set(false); //this needs to happen first in case update re-triggers a change in this concept
//        update();
//    }
//
//
//    protected void update() {
//        //override in subclasses when used in combination with runLater(t,n)
//    }

//    /** when true, update(nar) will be called before the next frame */
//    protected boolean runLater(@NotNull Task t, @NotNull NAR nar) {
//        return false;
//    }


//    public abstract boolean validBelief(@NotNull Task belief, @NotNull NAR nar);
//    public abstract boolean validGoal(@NotNull Task goal, @NotNull NAR nar);

//    /** called when a conflicting belief has attempted to be processed */
//    protected void onConflict(@NotNull Task belief) {
//        //logger.error("Sensor concept rejected derivation:\n {}\npredicted={} derived={}", belief.explanation(), belief(belief.occurrence()), belief.truth());
//
//    }

//    @Override
//    protected @NotNull BeliefTable newBeliefTable() {
//        return newBeliefTable(nar, 0,beliefCapacity);
//    }
//
//    @Override
//    protected @NotNull BeliefTable newGoalTable() {
//        return newGoalTable(0,goalCapacity);
//    }
}
