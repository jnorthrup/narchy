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
package org.oakgp.rank;

import jcog.pri.AtomicPri;
import jcog.pri.Pri;
import jcog.sort.TopN;

/**
 * A ranking of evolved objects (sorted immutable collection)
 */
public final class Ranking extends TopN<Evolved> {

    public Ranking(int capacity) {
        this(capacity, false);
    }

    public Ranking(int capacity, boolean reverse) {

        super(new Evolved[capacity], reverse ? Pri::priNeg : AtomicPri::pri);
    }

    public Ranking(Evolved... initial) {
        this(initial.length);
        for (Evolved e : initial)
            add(e);
    }


}
