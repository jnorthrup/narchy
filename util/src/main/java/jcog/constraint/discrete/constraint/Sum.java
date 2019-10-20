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

public class Sum extends Propagator {

    private final IntVar sum;
    private final IntVar[] assigned;
    private final TrailedInt nAssignedT;
    private final TrailedInt sumAssignedT;

    public Sum(IntVar[] variables, IntVar sum, int offset) {
        this.sum = sum;
        this.assigned = variables.clone();
        this.nAssignedT = new TrailedInt(sum.trail(), 0);
        this.sumAssignedT = new TrailedInt(sum.trail(), offset);
    }

    @Override
    public boolean setup() {
        sum.watchBounds(this);
        for (var i = 0; i < assigned.length; i++) {
            assigned[i].watchBounds(this);
        }
        return propagate();
    }

    @Override
    public boolean propagate() {

        var nAssigned = nAssignedT.getValue();
        var sumAssigned = sumAssignedT.getValue();


        var reduce = true;
        while (reduce) {
            reduce = false;

            var sumTermsMin = sumAssigned;
            var sumTermsMax = sumAssigned;
            var maxDiff = 0;

            
            
            for (var i = nAssigned; i < assigned.length; i++) {
                var term = assigned[i];
                var min = term.min();
                var max = term.max();
                sumTermsMin += min;
                sumTermsMax += max;
                var diff = max - min;
                if (diff == 0) {
                    sumAssigned += min;
                    
                    
                    assigned[i] = assigned[nAssigned];
                    assigned[nAssigned] = term;
                    nAssigned++;
                    continue;
                }
                maxDiff = Math.max(maxDiff, diff);
            }

            
            
            if (!sum.updateMin(sumTermsMin))
                return false;
            if (!sum.updateMax(sumTermsMax))
                return false;


            var sumMax = sum.max();
            var sumMin = sum.min();

            if (sumTermsMax - maxDiff < sumMin) {
                for (var i = nAssigned; i < assigned.length; i++) {
                    var term = assigned[i];
                    var oldMin = term.min();
                    var newMin = sumMin - sumTermsMax + term.max();
                    if (newMin > oldMin) {
                        if (!term.updateMin(newMin))
                            return false;
                        reduce |= newMin != term.min();
                    }
                }
            }

            if (sumTermsMin - maxDiff > sumMax) {
                for (var i = nAssigned; i < assigned.length; i++) {
                    var term = assigned[i];
                    var oldMax = term.max();
                    var newMax = sumMax - sumTermsMin + term.min();
                    if (newMax < oldMax) {
                        if (!term.updateMax(newMax))
                            return false;
                        reduce |= newMax != term.max();
                    }
                }
            }
        }

        
        nAssignedT.setValue(nAssigned);
        sumAssignedT.setValue(sumAssigned);
        return true;
    }
}
