package nars.concept;

import jcog.data.list.FasterList;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.concept.util.ConceptBuilder;
import nars.control.op.Remember;
import nars.table.BeliefTable;
import nars.table.TaskTable;
import nars.table.question.QuestionTable;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class TaskConcept extends NodeConcept  {

    private final BeliefTable beliefs;
    private final BeliefTable goals;
    private final QuestionTable quests;
    private final QuestionTable questions;

    public TaskConcept(Term term, @Nullable BeliefTable beliefs, @Nullable BeliefTable goals, ConceptBuilder b) {
        this(term,
                beliefs != null ? beliefs : b.newTable(term, true),
                goals != null ? goals : b.newTable(term, false),
                b.questionTable(term, true),
                b.questionTable(term, false)
        );
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
                       QuestionTable questions, QuestionTable quests) {
        super(term);
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
    public void remember(Remember r) {
        Task t = r.input;

        assert !NAL.test.DEBUG_EXTRA || (t.term().concept().equals(term));
        table(t.punc()).remember(r);
    }



    public void forEachTask(boolean includeConceptBeliefs, boolean includeConceptQuestions, boolean includeConceptGoals, boolean includeConceptQuests, Consumer<Task> each) {
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


































































































































































