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
package org.oakgp.terminate;

import org.oakgp.rank.Ranking;

import java.util.function.Predicate;

/**
 * A predicate that will return {@code true} when any of it component predicates return {@code true}.
 * <p>
 * A composite of multiple termination criteria.
 */
public final class CompositeTerminator implements Predicate<Ranking> {
    private final Predicate<Ranking>[] terminators;

    /**
     * Constructs a new {@code Predicate} consisting of the specified component predicates.
     */
    @SafeVarargs
    public CompositeTerminator(Predicate<Ranking>... terminators) {
        this.terminators = terminators;
    }

    @Override
    public boolean test(Ranking candidates) {
        for (Predicate<Ranking> t : terminators) {
            if (t.test(candidates)) {
                return true;
            }
        }
        return false;
    }
}
