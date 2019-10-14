package nars.derive.action;

import jcog.Util;
import nars.Task;
import nars.attention.What;
import nars.derive.Derivation;
import nars.derive.premise.AbstractPremise;
import nars.derive.rule.RuleCause;
import nars.table.BeliefTable;
import org.jetbrains.annotations.Nullable;

public class QuestionAnswering extends NativeHow {

	public static final QuestionAnswering the = new QuestionAnswering();

	private QuestionAnswering() {
		single();
		taskPunc(false, false, true, true);
	}

	@Override
	protected void run(RuleCause why, Derivation d) {

		Task q = d._task;
		//assert(q.isQuestionOrQuest());

		BeliefTable answerTable = d.nar.tableDynamic(q, !q.isQuest());
		if (answerTable != null && !answerTable.isEmpty()) {
			Task a = answer(q, answerTable, d);
			if (a!=null) {
				d.add(new AbstractPremise(a, why.why(q)));
			}
		}

	}

	@Nullable
	private static Task answer(Task question, BeliefTable answerTable, Derivation d) {

		//assert (task.isQuest() || match.punc() == BELIEF) : "quest answered with a belief but should be a goal";

		Task answer = MatchBelief.task(answerTable, question.term(),
			d.deriver.timing.premise(d.what, question), null, d);

		if (answer == null)
			return null;

		answer = question.onAnswered(answer);
		if (answer == null)
			return null;

		answered(question, answer, d);
		return answer;
	}

	private static void answered(Task q, Task a, Derivation d) {
////        if (x.conf() > d.confMin) {
////            if (x.isGoal())
////                d.what.accept(x);
////            else
		float qPri = q.priElseZero(), aPri = a.priElseZero();
		float pri =
			//qPri * aPri;
			Util.or(qPri, aPri);
//
		What w = d.what;
////		((AbstractTask)a).why(q.why()); //merge question reason into answer
		w.link(a, pri);
		w.emit(a);
	}


}