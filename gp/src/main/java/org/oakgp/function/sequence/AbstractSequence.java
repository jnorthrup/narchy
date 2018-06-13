package org.oakgp.function.sequence;

import org.oakgp.function.ImpureFn;
import org.oakgp.node.Node;

public interface AbstractSequence extends ImpureFn {

    /** tests for two mutually exclusive (ex: opposite or contradictory) actions that cancel each other */
    default boolean isMutex(Node firstArg, Node secondArg) {
        return false;
    }
}
