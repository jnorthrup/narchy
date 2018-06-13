/*
 * Copyright 2015 S. Webber
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
package org.oakgp.select;

import java.util.Random;

/**
 * The <i>relative</i> fitness of candidates is used to determine the probability that they will be selected.
 */
public final class RankSelector extends WeightSelector {

    /**
     * Creates a {@code RankSelection} that uses the given {@code Random} to select from the given {@code RankedCandidates}.
     */
    public RankSelector(Random random) {
        super(random);
    }

    @Override
    public float valueOf(int i) {
        return 1f - ((float)i+1)/(n+1);
    }

}
