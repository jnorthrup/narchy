package nars.derive.action;

import nars.NAL;
import nars.Task;
import nars.attention.What;
import nars.derive.Derivation;
import nars.derive.premise.AbstractPremise;
import nars.link.TaskLink;
import nars.table.TaskTable;
import nars.term.Termed;
import nars.time.When;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;

public class TaskResolve extends NativePremiseAction {

	{
		taskPunc(false,false,false,false,true); //commands only
	}

	@Override
	protected void run(Derivation d) {
		Task x = d._task;

		Task y = get((TaskLink)x, d.when, d.tasklinkTaskFilter);
		if (y != null) // && !x.equals(y))
			d.add(new AbstractPremise(y));
	}

	@Nullable Task get(TaskLink t, When<What> when, @Nullable Predicate<Task> filter) {
		return get(t, t.punc(when.x.random()), when, filter);
	}


	@Nullable Task get(TaskLink t, byte punc, When<What> w, @Nullable Predicate<Task> filter) {

		Termed x = t.from();

		if (punc == 0)
			punc = TaskLink.randomPunc(x.term(), w.x.random()); //flat-lined tasklink

		TaskTable table =
			//n.concept(t);
			//n.conceptualizeDynamic(x);
			//beliefOrGoal ? n.conceptualizeDynamic(x) : n.beliefDynamic(x);
			w.x.nar.tableDynamic(x, punc);

		if (table == null || table.isEmpty())
			return null;



//            boolean beliefOrGoal = punc == BELIEF || punc == GOAL;

		//TODO abstract TaskLinkResolver strategy
		Task y;
		if ((punc==BELIEF && NAL.TASKLINK_ANSWER_BELIEF) || (punc==GOAL && NAL.TASKLINK_ANSWER_GOAL))
			y = table.match(w, null, filter, w.dur, false);
		else {
			y = table.sample(w, null, filter);
		}

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

	@Override
	protected float pri(Derivation d) {
		return 1;
	}
}
