package nars.derive.action;

import nars.NAL;
import nars.Task;
import nars.attention.What;
import nars.derive.Derivation;
import nars.derive.premise.AbstractPremise;
import nars.derive.rule.RuleCause;
import nars.link.TaskLink;
import nars.table.TaskTable;
import nars.term.Term;
import nars.time.When;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;

/** resolves a tasklink to a single-premise */
public class TaskResolve extends NativeHow {

	public static final TaskResolve the = new TaskResolve();

	private TaskResolve() {
		taskCommand();
	}

	@Override
	protected void run(RuleCause why, Derivation d) {
        TaskLink x = (TaskLink)d._task;

        Task y = get(x, d);
		if (y != null) // && !x.equals(y))
			d.add(new AbstractPremise(y, why.whyLazy(x, y)));
	}

	private final @Nullable Predicate<nars.Task> tasklinkTaskFilter =
		//t -> !t.isDeleted();
		null;

	public @Nullable Task get(TaskLink t, Derivation d) {
		return get(t, d.when, tasklinkTaskFilter);
	}

	@Nullable static Task get(TaskLink t, When<What> when, @Nullable Predicate<Task> filter) {
		return get(t.from(), t.punc(when.x.random()), when, filter);
	}


	static @Nullable Task get(Term x, byte punc, When<What> w, @Nullable Predicate<Task> filter) {

		if ((int) punc == 0)
			punc = TaskLink.randomPunc(x, w.x.random()); //flat-lined tasklink

        TaskTable table =
			//n.concept(t);
			//n.conceptualizeDynamic(x);
			//beliefOrGoal ? n.conceptualizeDynamic(x) : n.beliefDynamic(x);
			w.x.nar.tableDynamic(x, punc);

		if (table == null || table.isEmpty())
			return null;



//            boolean beliefOrGoal = punc == BELIEF || punc == GOAL;

		//TODO abstract TaskLinkResolver strategy
		Task y = ((int) punc == (int) BELIEF && NAL.TASKLINK_ANSWER_BELIEF) || ((int) punc == (int) GOAL && NAL.TASKLINK_ANSWER_GOAL) ? table.match(w, null, filter, w.dur, false) : table.sample(w, null, filter);

//            if (y == null) {
//                if (!beliefOrGoal) {
//                    //form question?
//                    float qpri = NAL.TASKLINK_GENERATED_QUESTION_PRI_RATE;
//                    if (qpri > Float.MIN_NORMAL) {
//                        Task.validTaskTerm(x.term(), punc, true);
//                    }
//                }
//
////                if (y == null)
////                    delete(punc); //TODO try another punc?
//            }

		return y;

	}

}
