package nars.derive;

import nars.Task;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

/** stateless, storing any state information in the Derivation instance */
public interface DeriverBudgeting {

    /** called on new premise.  useful if an implementation wants to cache values that are common to all derivations of a premise  */
    default void premise(Derivation d) {

    }

    /**
     *
     * just returns the priority.  should not set the priority of the task
     *
     * @param t the derived task
     * @param derivedTruth - the undithered truth calculation is provided for accuracy.  use this instead of task.truth
     * @param d the derivation context
     * @return priority, or NaN to filter (cancel) this derived result
     */
    float pri(Task t, float derivedFreq, float derivedEvi, Derivation d);



}
