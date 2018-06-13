package org.oakgp.function.sequence;

import org.oakgp.function.ImpureFunction;
import org.oakgp.node.Node;

public interface AbstractSequence extends ImpureFunction {

    /** tests for two mutually exclusive (ex: opposite or contradictory) actions that cancel each other */
    default boolean isMutex(Node firstArg, Node secondArg) {
        return false;
    }
}
