package nars.derive.condition;

import nars.$;
import nars.derive.model.PreDerivation;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;

public final class TaskPunc extends AbstractPred<PreDerivation> {
    //private final BytePredicate taskPunc;

    private static final Atom TASKPUNC = (Atom) Atomic.the(TaskPunc.class.getSimpleName());

    boolean belief, goal, question, quest;

    @Nullable
    public static TaskPunc get(BytePredicate p) {
        boolean belief = p.accept(BELIEF), goal = p.accept(GOAL), question = p.accept(QUESTION), quest = p.accept(QUEST);
        if (belief && goal && question && quest)
            return null;
        else
            return new TaskPunc(belief, goal, question, quest);
    }

    private TaskPunc(boolean belief, boolean goal, boolean question, boolean quest) {
        super($.func(TASKPUNC, $.p(belief, goal, question, quest)));
        this.belief = belief; this.goal = goal; this.question = question; this.quest = quest;
    }

    @Override
    public float cost() {
        return 0.006f;
    }

    @Override
    public boolean test(PreDerivation preDerivation) {
        switch(preDerivation.taskPunc) {
            case BELIEF: return belief;
            case GOAL: return goal;
            case QUESTION: return question;
            case QUEST: return quest;
        }
        return true;
    }
}
