package nars.derive.premise;

import nars.Task;
import nars.control.Caused;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

/**
 * Defines the conditions used in an instance of a derivation
 * Contains the information necessary for generating derivation Tasks via reasoning rules.
 * <p>
 * It is meant to be disposable and should not be kept referenced longer than necessary
 * to avoid GC loops, so it may need to be weakly referenced.
 * <p>
 * note: Comparable as implemented here is not 100% consistent with Task.equals and Term.equals.  it is
 * sloppily consistent for its purpose in collating Premises in optimal sorts during hypothesizing
 */
public interface Premise extends Caused {

	Term taskTerm();

	Term beliefTerm();

	@Nullable Task task();

	@Nullable Task belief();

}
