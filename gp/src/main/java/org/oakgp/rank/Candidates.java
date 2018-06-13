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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * A sorted immutable collection of {@code RankedCandidate} objects.
 */
public final class Candidates implements Iterable<RankedCandidate> {
    private static final Comparator<RankedCandidate> COMPARE_TO = RankedCandidate::compareTo;

    private final RankedCandidate[] sortedCandidates;

    /**
     * Constructs a new collection of candidates sorted by their natural ordering.
     *
     * @see RankedCandidate#compareTo(RankedCandidate)
     */
    @Deprecated public Candidates(RankedCandidate[] candidates) {
        this(Stream.of(candidates), COMPARE_TO);
    }
    public Candidates(Stream<RankedCandidate> candidates) {
        this(candidates, COMPARE_TO);
    }


    /**
     * Constructs a new collection of candidates sorted according to the given comparator.
     */
    public Candidates(Stream<RankedCandidate> candidates, Comparator<RankedCandidate> comparator) {
        this.sortedCandidates =
                candidates.sorted(comparator).toArray(RankedCandidate[]::new);
                //Arrays.copyOf(candidates, candidates.length);
        Arrays.sort(sortedCandidates, comparator);
    }

    /**
     * Returns the candidate at the specified position in this collection.
     *
     * @param index index of the candidate to return
     * @return the candidate at the specified position in this collection
     * @throws ArrayIndexOutOfBoundsException if the index is out of range (<tt>index &lt; 0 || index &gt;= size()</tt>)
     */
    public RankedCandidate get(int index) {
        return sortedCandidates[index];
    }

    /**
     * Returns the best candidate in this collection.
     */
    public RankedCandidate best() {
        return sortedCandidates[0];
    }

    /**
     * Returns the number of candidates in this collection.
     */
    public int size() {
        return sortedCandidates.length;
    }

    @Override
    public String toString() {
        return Arrays.toString(sortedCandidates);
    }

    /**
     * Returns a sequential {@code Stream} with this collection as its source.
     */
    public Stream<RankedCandidate> stream() {
        return Arrays.stream(sortedCandidates);
    }

    @Override
    public Iterator<RankedCandidate> iterator() {
        return new RankedCandidatesIterator();
    }

    private class RankedCandidatesIterator implements Iterator<RankedCandidate> {
        private final AtomicInteger ctr = new AtomicInteger();

        @Override
        public boolean hasNext() {
            return ctr.get() < sortedCandidates.length;
        }

        @Override
        public RankedCandidate next() {
            int next = ctr.getAndIncrement();
            if (next < sortedCandidates.length) {
                return sortedCandidates[next];
            } else {
                throw new NoSuchElementException();
            }
        }
    }
}
