/*
 * Copyright 2016, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jcog.constraint.discrete.constraint;

import jcog.constraint.discrete.IntVar;
import jcog.constraint.discrete.propagation.Propagator;
import jcog.constraint.discrete.trail.TrailedInt;

public class AllDifferent extends Propagator {

    private final IntVar[] unassigned;
    private final TrailedInt nUnassignedT;

    public AllDifferent(IntVar[] variables) {
        unassigned = variables.clone();
        nUnassignedT = new TrailedInt(unassigned[0].trail(), variables.length);
    }

    @Override
    public boolean setup() {
        for (var i = 0; i < unassigned.length; i++) {
            unassigned[i].watchAssign(this);
        }
        return propagate();
    }

    @Override
    public boolean propagate() {
        var nUnassigned = nUnassignedT.getValue();
        if (nUnassigned == 1)
            return true;
        var reduce = true;
        while (reduce) {
            reduce = false;
            for (var i = nUnassigned - 1; i >= 0; i--) {
                var variable = unassigned[i];
                if (variable.isAssigned()) {
                    
                    nUnassigned--;
                    unassigned[i] = unassigned[nUnassigned];
                    unassigned[nUnassigned] = variable;

                    var value = variable.min();
                    for (var j = 0; j < nUnassigned; j++) {
                        var var = unassigned[j];
                        if (var.contains(value)) {
                            if (!var.remove(value))
                                return false;
                            reduce |= var.isAssigned();
                        }
                    }
                }
            }
        }
        nUnassignedT.setValue(nUnassigned);
        return true;
    }
}
