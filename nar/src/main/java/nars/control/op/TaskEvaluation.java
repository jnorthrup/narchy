package nars.control.op;

import jcog.data.list.FasterList;
import nars.$;
import nars.NAL;
import nars.Task;
import nars.attention.What;
import nars.eval.Evaluation;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Bool;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.term.atom.Bool.False;
import static nars.term.atom.Bool.True;

final class TaskEvaluation extends Evaluation implements Predicate<Term> {

	final Term tt;
	private final What what;
	private final Task t;
	private int tried = 0;
	Collection result = null;

	TaskEvaluation(Task t, What w) {
		super();

		this.t = t;
		this.tt = t.term();
		this.what = w;

		evalTry((Compound) (t.term()), w.nar.evaluator.get(), false);

		if (result!=null) {
			if (result.size()>=2 && result.contains(True) && result.contains(False)) {
				//explicit contradiction
				result = null;
			} else {

				FasterList f = new FasterList<>(result);
				result = f;
				f.replaceAll(yTerm -> perceiveable(this.t, (Term) yTerm, what));
				f.removeNulls();

				if (result.isEmpty())
					result = null;
			}
		}

	}
	/**
	 * returns true if the task is acceptable
	 */
	@Nullable
	private static Task perceiveable(Task x, Term y, What w) {

		if (x.term().equals(y))
			return null;

		return y instanceof Bool ?
			perceiveBooleanAnswer(x, y, w) :
			rememberTransformed(x, y);
	}

	@Override
	public boolean test(Term y) {
		tried++;

		if (y != Bool.Null && !y.equals(tt)) {
			if (result(y)) {
				if (result.size() >= NAL.TASK_EVAL_FORK_SUCCESS_LIMIT)
					return false; //done, enough forks
			}
		}

		return tried < NAL.TASK_EVAL_FORK_ATTEMPT_LIMIT;
	}

	protected boolean result(Term y) {
		if (!(y instanceof Bool /* allow Bool for answering */) && !Task.validTaskTerm(y.unneg()))
			return false;

		if (result == null)
			result = new UnifiedSet(1);

		return result.add(y);
	}

	@Override
	protected Term bool(Term x, Bool b) {
//                    //filter non-true
		return b;
//                    if (b == True && x.equals(x))
//                        return True; //y;
//                    else if (b == False && x.equals(x))
//                        return False; //y.neg();
//                    else
//                        return Bool.Null; //TODO
	}

	@Nullable
	private static Task perceiveBooleanAnswer(Task x, Term y, What w) {

		byte punc = x.punc();
		if (punc == QUESTION || punc == QUEST) {
			//conver to an answering belief/goal now that the absolute truth has been determined
			//TODO decide if this makes sense for QUEST

			byte answerPunc;
			if (punc == QUESTION) answerPunc = BELIEF;
			else if (punc == QUEST) {
				answerPunc = GOAL;
			} else
				throw new UnsupportedOperationException();

//                    if (it.hasXternal())
//                        it = Retemporalize.retemporalizeXTERNALToDTERNAL.apply(it);

			return Task.clone(x,
				x.term(),
				$.t(y == True ? 1 : 0, w.nar.confDefault(answerPunc)),
				answerPunc,
				x.start(), x.end());

		} else {
			//throw new WTF();
			return null;
		}
	}
	@Nullable private static Task rememberTransformed(Task input, Term y) {
		Task u = Task.clone(input, y);
		assert(u!=null);
		return u; //recurse
	}

}

