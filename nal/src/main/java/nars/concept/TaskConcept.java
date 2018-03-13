package nars.concept;

import jcog.bag.Bag;
import jcog.list.FasterList;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.util.ConceptBuilder;
import nars.control.MetaGoal;
import nars.table.BeliefTable;
import nars.table.QuestionTable;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;

public class TaskConcept extends NodeConcept implements Concept {


    protected final BeliefTable beliefs;
    protected final BeliefTable goals;
    protected final QuestionTable quests;
    protected final QuestionTable questions;

    public TaskConcept(Term term, @Nullable BeliefTable beliefs, @Nullable BeliefTable goals, ConceptBuilder conceptBuilder) {
        this(term,
                beliefs != null ? beliefs : conceptBuilder.beliefTable(term, true),
                goals != null ? goals : conceptBuilder.beliefTable(term, false),
                conceptBuilder.questionTable(term, true),
                conceptBuilder.questionTable(term, false),
                conceptBuilder.newLinkBags(term));
    }

    /**
     * Constructor, called in Memory.getConcept only
     *
     * @param term      A term corresponding to the concept
     * @param termLinks
     * @param taskLinks
     */
    public TaskConcept(Term term,
                       BeliefTable beliefs, BeliefTable goals,
                       QuestionTable questions, QuestionTable quests,
                       Bag[] bags) {
        super(term, bags);
        this.beliefs = beliefs;
        this.goals = goals;
        this.questions = questions;
        this.quests = quests;

    }


    /**
     * used for setting an explicit OperationConcept instance via java; activates it on initialization
     */
    public TaskConcept(Term term, NAR n) {
        this(term, n.conceptBuilder);
    }


    public TaskConcept(Term term, ConceptBuilder b) {
        this(term, b.beliefTable(term, true), b.beliefTable(term, false),
                b.questionTable(term, true), b.questionTable(term, false),
                b.newLinkBags(term));
    }


    @Override
    public QuestionTable quests() {
        return quests;
    }

    @Override
    public QuestionTable questions() {
        return questions;
    }

    /**
     * Judgments directly made about the term Use ArrayList because of access
     * and insertion in the middle
     */
    @Override
    public BeliefTable beliefs() {
        return beliefs;
    }

    /**
     * Desire values on the term, similar to the above one
     */
    @Override
    public BeliefTable goals() {
        return goals;
    }

    protected void beliefCapacity(int be, int bt, int ge, int gt) {

        beliefs.setCapacity(be, bt);
        goals.setCapacity(ge, gt);

    }


    @Override
    protected void stateChanged() {
        super.stateChanged();
        int be = state.beliefCap(this, true, true);
        int bt = state.beliefCap(this, true, false);

        int ge = state.beliefCap(this, false, true);
        int gt = state.beliefCap(this, false, false);

        beliefCapacity(be, bt, ge, gt);

        if (questions != null)
            questions.capacity(state.questionCap(this, true));
        if (quests != null)
            quests.capacity(state.questionCap(this, false));

    }

    /**
     * Directly process a new task, if belief tables agree to store it.
     * Called exactly once on each task.
     */
    public boolean add(Task t, NAR n) {
        return table(t.punc()).add(t, this, n);
    }

    public void value(Task t, float activation, NAR n) {

        byte punc = t.punc();
        if (punc == BELIEF || punc == GOAL) {
            MetaGoal p = punc == BELIEF ? MetaGoal.Believe : MetaGoal.Desire;
            p.learn(t.cause(), Param.beliefValue(t) * activation, n.causes);
        }


        //return Emotivation.preferConfidentAndRelevant(t, activation, when, n);
        //positive value based on the conf but also multiplied by the activation in case it already was known
        //return valueIfProcessedAt(t, activation, n.time(), n);

//            @Override
//    public float value(@NotNull Task t, NAR n) {
//        byte p = t.punc();
//        if (p == BELIEF || p == GOAL) {// isGoal()) {
//            //example value function
//            long s = t.end();
//
//            if (s!=ETERNAL) {
//                long now = n.time();
//                long relevantTime = p == GOAL ?
//                        now - n.dur() : //present or future goal
//                        now; //future belief prediction
//
//                if (s > relevantTime) //present or future TODO irrelevance discount for far future
//                    return (float) (0.1f + Math.pow(t.conf(), 0.25f));
//            }
//        }
//
//        //return super.value(t, activation, n);
//        return 0;
//    }
    }


    public void forEachTask(boolean includeConceptBeliefs, boolean includeConceptQuestions, boolean includeConceptGoals, boolean includeConceptQuests, @NotNull Consumer<Task> each) {
        if (includeConceptBeliefs && beliefs != null) beliefs.forEachTask(each);
        if (includeConceptQuestions && questions != null) questions.forEachTask(each);
        if (includeConceptGoals && goals != null) goals.forEachTask(each);
        if (includeConceptQuests && quests != null) quests.forEachTask(each);
    }

    public void forEachTask(Consumer<Task> each) {
        if (beliefs != null) beliefs.forEachTask(each);
        if (questions != null) questions.forEachTask(each);
        if (goals != null) goals.forEachTask(each);
        if (quests != null) quests.forEachTask(each);
    }

    @Override
    public Stream<Task> tasks(boolean includeBeliefs, boolean includeQuestions, boolean includeGoals, boolean includeQuests) {
        List<Stream<Task>> s = new FasterList<>();
        if (includeBeliefs) s.add(beliefs.streamTasks());
        if (includeGoals) s.add(goals.streamTasks());
        if (includeQuestions) s.add(questions.streamTasks());
        if (includeQuests) s.add(quests.streamTasks());
        return s.stream().flatMap(x -> x);
    }


    @Override
    public void delete(NAR nar) {
        beliefs.clear();
        goals.clear();
        questions.clear();
        quests.clear();
        super.delete(nar);
    }

}


//    /**
//     * apply derivation feedback and update NAR emotion state
//     */
//    protected void feedback(@NotNull Task input, @NotNull TruthDelta delta, @NotNull CompoundConcept concept, @NotNull NAR nar) {
//
//        //update emotion happy/sad
//        Truth before = delta.before;
//        Truth after = delta.after;
//
//        float deltaSatisfaction, deltaConf, deltaFreq;
//
//
//        if (before != null && after != null) {
//
//            deltaFreq = after.freq() - before.freq();
//            deltaConf = after.conf() - before.conf();
//
//        } else {
//            if (before == null && after != null) {
//                deltaConf = after.conf();
//                deltaFreq = after.freq();
//            } else if (before!=null) {
//                deltaConf = -before.conf();
//                deltaFreq = -before.freq();
//            } else {
//                deltaConf = 0;
//                deltaFreq = 0;
//            }
//        }
//
//        Truth other;
//        int polarity = 0;
//
//        Time time = nar.time;
//        int dur = time.dur();
//        long now = time.time();
//        if (input.isBelief()) {
//            //compare against the current goal state
//            other = concept.goals().truth(now, dur);
//            if (other != null)
//                polarity = +1;
//        } else if (input.isGoal()) {
//            //compare against the current belief state
//            other = concept.beliefs().truth(now, dur);
//            if (other != null)
//                polarity = -1;
//        } else {
//            other = null;
//        }
//
//
//        if (other != null) {
//
//            float otherFreq = other.freq();
//
//            if (polarity==0) {
//
//                //ambivalence: no change
//                deltaSatisfaction = 0;
//
//            } else {
//
////                if (otherFreq > 0.5f) {
////                    //measure how much the freq increased since goal is positive
////                    deltaSatisfaction = +polarity * deltaFreq / (2f * (otherFreq - 0.5f));
////                } else {
////                    //measure how much the freq decreased since goal is negative
////                    deltaSatisfaction = -polarity * deltaFreq / (2f * (0.5f - otherFreq));
////                }
//
//                if (after!=null) {
//                    deltaSatisfaction = /*Math.abs(deltaFreq) * */ (2f * (1f - Math.abs(after.freq() - otherFreq)) - 1f);
//
//                    deltaSatisfaction *= (after.conf() * other.conf());
//
//                    nar.emotion.happy(deltaSatisfaction);
//                } else {
//                    deltaSatisfaction = 0;
//                }
//            }
//
//
//        } else {
//            deltaSatisfaction = 0;
//        }
//
//        feedback(input, delta, nar, deltaSatisfaction, deltaConf);
//
//    }
//
//    protected void feedback(@NotNull Task input, @NotNull TruthDelta delta, @NotNull NAR nar, float deltaSatisfaction, float deltaConf) {
//        if (!Util.equals(deltaConf, 0f, TRUTH_EPSILON))
//            nar.emotion.confident(deltaConf, input.term());
//
//        input.feedback(delta, deltaConf, deltaSatisfaction, nar);
//    }

//    private void checkConsistency() {
//        synchronized (tasks) {
//            int mapSize = tasks.size();
//            int tableSize = beliefs().size() + goals().size() + questions().size() + quests().size();
//
//            int THRESHOLD = 50; //to catch when the table explodes and not just an off-by-one inconsistency that will correct itself in the next cycle
//            if (Math.abs(mapSize - tableSize) > THRESHOLD) {
//                //List<Task> mapTasks = new ArrayList(tasks.keySet());
//                Set<Task> mapTasks = tasks.keySet();
//                ArrayList<Task> tableTasks = Lists.newArrayList(
//                        Iterables.concat(beliefs(), goals(), questions(), quests())
//                );
//                //Collections.sort(mapTasks);
//                //Collections.sort(tableTasks);
//
//                System.err.println(mapSize + " vs " + tableSize + "\t\t" + mapTasks.size() + " vs " + tableTasks.size());
//                System.err.println(Joiner.on('\n').join(mapTasks));
//                System.err.println("----");
//                System.err.println(Joiner.on('\n').join(tableTasks));
//                System.err.println("----");
//            }
//        }
//    }

//    public long minTime() {
//        ageFactor();
//        return min;
//    }
//
//    public long maxTime() {
//        ageFactor();
//        return max;
//    }
//
//    public float ageFactor() {
//
//        if (min == ETERNAL) {
//            //invalidated, recalc:
//            long t[] = new long[] { Long.MAX_VALUE, Long.MIN_VALUE };
//
//            beliefs.range(t);
//            goals.range(t);
//
//            if (t[0] == Long.MAX_VALUE) {
//                min = max= 0;
//            } else {
//                min = t[0];
//                max = t[1];
//            }
//
//        }
//
//        //return 1f;
//        long range = max - min;
//        /* history factor:
//           higher means it is easier to hold beliefs further away from current time at the expense of accuracy
//           lower means more accuracy at the expense of shorter memory span
//     */
//        float historyFactor = Param.TEMPORAL_DURATION;
//        return (range == 0) ? 1 :
//                ((1f) / (range * historyFactor));
//    }

