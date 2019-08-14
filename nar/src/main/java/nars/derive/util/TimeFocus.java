package nars.derive.util;

import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.attention.What;
import nars.term.Term;
import nars.time.When;
import nars.time.event.WhenTimeIs;

/** the deriver uses TimeFocus implementations to determine TWO specific time intervals,
 * the second depending indirectly on the first:
 *   1) at the start of a derivation process, TimeFocus determines when in time that
 *      a TaskLink should resolve Tasks in
 *   2) during premise formation, in which a premise containing a Task resulting from (1),
 *      the TimeFocus determines when to match belief tasks and answer questions.
 * */
public interface TimeFocus {


	/**
	 *  default temporal focus to be used throughout multiple successive derivations.
	 *  constructs a time interval surrounding the present moment, with a diameter of
	 *  1 duration.
	 * */
	default When<NAR> task(What what) {
		return WhenTimeIs.now(what, NAL.premise.ANSWER_HONEST_DUR ? 0 : what.dur());
	}

	/** premise match focus */
	long[] premise(What what, Task task, Term belifTerm);


}
