package nars.concept;

import jcog.data.list.FasterList;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.util.ConceptBuilder;
import nars.control.MetaGoal;
import nars.control.op.Remember;
import nars.link.TermLinker;
import nars.table.BeliefTable;
import nars.table.TaskTable;
import nars.table.question.QuestionTable;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;

public class TaskConcept extends NodeConcept  {

    private final BeliefTable beliefs, goals;
    private final QuestionTable quests, questions;

    public TaskConcept(Term term, @Nullable BeliefTable beliefs, @Nullable BeliefTable goals, ConceptBuilder b) {
        this(term, beliefs, goals, b.termlinker(term), b);
    }

    public TaskConcept(Term term, @Nullable BeliefTable beliefs, @Nullable BeliefTable goals, TermLinker linker, ConceptBuilder b) {
        this(term,
                beliefs != null ? beliefs : b.newTable(term, true),
                goals != null ? goals : b.newTable(term, false),
                b.questionTable(term, true),
                b.questionTable(term, false),
                linker);
    }


    /**
     * Constructor, called in Memory.getConcept only
     *
     * @param term      A target corresponding to the concept
     * @param termLinks
     * @param taskLinks
     */
    public TaskConcept(Term term,
                       BeliefTable beliefs, BeliefTable goals,
                       QuestionTable questions, QuestionTable quests,
                       TermLinker linker) {
        super(term, linker);
        this.beliefs = beliefs;
        this.goals = goals;
        this.questions = questions;
        this.quests = quests;

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
     * Judgments directly made about the target Use ArrayList because of access
     * and insertion in the middle
     */
    @Override
    public BeliefTable beliefs() {
        return beliefs;
    }

    /**
     * Desire values on the target, similar to the above one
     */
    @Override
    public BeliefTable goals() {
        return goals;
    }


    /**
     * Directly process a new task, if belief tables agree to store it.
     * Called exactly once on each task.
     */
    public void add(Remember t, NAR n) {
        Task ti = t.input;
        //if (ti!=null) {
            assert !Param.DEBUG_EXTRA || (ti.term().concept().equals(term));
            table(ti.punc()).remember(t);
        //}
    }

    public void value(Task t, NAR n) {

        n.feel.perceive(t);

        byte punc = t.punc();
        if (punc == BELIEF || punc == GOAL) {
            (punc == BELIEF ? MetaGoal.Believe : MetaGoal.Desire)
                    .learn(t.cause(), t.priElseZero(), n.control.causes);
        }
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
        int c = 0;
        if (includeBeliefs) c++;
        if (includeGoals) c++;
        if (includeQuestions) c++;
        if (includeQuests) c++;
        assert (c > 0);

        List<TaskTable> tables = new FasterList(c);

        if (includeBeliefs) tables.add(beliefs());
        if (includeGoals) tables.add(goals());
        if (includeQuestions) tables.add(questions());
        if (includeQuests) tables.add(quests());

        return tables.stream().flatMap(TaskTable::taskStream)
                //.filter(Objects::nonNull)
                ;
    }


    @Override
    public boolean delete(NAR nar) {
        if (super.delete(nar)) {
            if (beliefs != null) beliefs.delete();
            if (goals != null) goals.delete();
            if (questions != null) questions.delete();
            if (quests != null) quests.delete();
            return true;
        }
        return false;
    }

}


































































































































































