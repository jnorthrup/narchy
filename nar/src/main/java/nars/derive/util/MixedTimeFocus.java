package nars.derive.util;

import nars.Task;
import nars.attention.What;
import nars.term.Term;

/** TODO weighted version */
public class MixedTimeFocus implements TimeFocus {

	final TimeFocus[] choices;

	public MixedTimeFocus(TimeFocus... choices) {
		this.choices = choices;
		assert(choices.length > 1);
	}

	@Override
	public long[] premise(What what, Task task, Term beliefTerm) {
		return choices[what.random().nextInt(choices.length)].premise(what, task, beliefTerm);
	}
}
