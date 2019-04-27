package nars.op.in;

import nars.NAR;
import nars.Narsese;
import nars.Task;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * TextInput subclass that only inputs when the next input value changes from
 * previous
 */
public class ChangedTextInput {

	private final NAR nar;
	private @NotNull List<Task> last = Collections.emptyList();
	private boolean allowRepeats;

	public ChangedTextInput(NAR n) {
		nar = n;
	}

	public boolean set(@NotNull String s) throws Narsese.NarseseException {
        return enable() && set(Narsese.tasks(s, nar));
	}

	public boolean set(@NotNull List<Task> s) {
		if ((enable() && allowRepeats()) || (!last.equals(s))) {
			nar.input(s);
			last = s;
			return true;
		}
		
		return false;
	}

	public boolean allowRepeats() {
		return allowRepeats;
	}

	public boolean enable() {
		return true;
	}

	public void setAllowRepeatInputs(boolean b) {
		allowRepeats = b;
	}
}
