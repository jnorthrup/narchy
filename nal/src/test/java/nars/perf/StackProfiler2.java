package nars.perf;

import jcog.Texts;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.tree.perfect.Trie;
import jcog.tree.perfect.TrieMatch;
import jcog.tree.perfect.Tries;
import joptsimple.OptionException;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.IntObjectPair;
import org.eclipse.collections.api.tuple.primitive.ObjectIntPair;
import org.eclipse.collections.impl.bag.mutable.HashBag;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.profile.InternalProfiler;
import org.openjdk.jmh.profile.ProfilerException;
import org.openjdk.jmh.results.*;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


/**
 * Very basic and naive stack profiler.
 */
public class StackProfiler2 implements InternalProfiler {

    /**
     * Threads to ignore (known system and harness threads)
     */
    private static final String[] IGNORED_THREADS = {
            "Finalizer",
            "Signal Dispatcher",
            "Reference Handler",
            "main",
            "Sampling Thread",
            "Attach Listener"
    };

    private final int stackLines = 12;
    private final int topStacks = 10;
    private final int periodMsec = 1;
    private final Trie<String, Boolean> excludePackageNames;

    public StackProfiler2() throws ProfilerException {




































        try {

            
            MutableSet<String> exc = Sets.mutable.of("java.", "jdk.", "javax.", "sun.",
                    "sunw.", "com.sun.", "org.openjdk.jmh.", "com.intellij.rt.");

            excludePackageNames = Tries.forStrings();
            exc.forEach(e -> excludePackageNames.put(e, true));

        } catch (OptionException e) {
            throw new ProfilerException(e.getMessage());
        }
    }

    private volatile SamplingTask samplingTask;

    @Override
    public void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) {
        samplingTask = new SamplingTask();
        samplingTask.start();
    }

    @Override
    public Collection<? extends Result> afterIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams, IterationResult result) {
        samplingTask.stop();
        return Collections.singleton(new StackResult(samplingTask.stacks, new HashBag() /* TODO */, topStacks));
    }

    @Override
    public String getDescription() {
        return "Simple and naive Java stack profiler++";
    }

    class SamplingTask implements Runnable {

        private final Thread thread;
        private final Map<Thread.State, HashBag<StackRecord>> stacks;

        SamplingTask() {
            stacks = new EnumMap<>(Thread.State.class);
            for (Thread.State s : Thread.State.values()) {
                stacks.put(s, new HashBag<>());
            }
            thread = new Thread(this);
            thread.setName("Sampling Thread");
            thread.setPriority(Thread.MAX_PRIORITY);
        }

        @Override
        public void run() {
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            while (!Thread.interrupted()) {
                measure(threadBean.dumpAllThreads(false, false));
                /*if (!*/Util.sleepMS(periodMsec);//)
                    //break;
            }
        }

        void measure(ThreadInfo[] infos) {


            info:
            for (ThreadInfo info : infos) {

                
                switch (info.getThreadName()) {
                    case "Finalizer":
                    case "Signal Dispatcher":
                    case "Reference Handler":
                    case "main":
                    case "Sampling Thread":
                    case "Attach Listener":
                        continue info; 

                }


                
                


                StackRecord lines = new StackRecord(stackLines);
                Stream.of(info.getStackTrace())
                        .filter(f -> !exclude(f.getClassName()))
                        .limit(stackLines)
                        .forEach(lines::add)
                ;






















                if (!lines.isEmpty()) {
                    lines.commit();
                    stacks.get(info.getThreadState()).add(lines);
                }
            }
        }

        void start() {
            thread.start();
        }

        void stop() {
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }

    private boolean exclude(String className) {
        return excludePackageNames.has(className, TrieMatch.STARTS_WITH);
    }


    public static boolean pause(int periodMsec) {
        try {
            TimeUnit.MILLISECONDS.sleep(periodMsec);
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }

    static class StackRecord extends FasterList<Pair<String, IntObjectPair<String>>> {

        private int hash;

        public StackRecord() {
            this(16);
        }

        StackRecord(int cap) {
            super(cap);
        }

        void commit() {
            compact();
            this.hash = super.hashCode();
        }

        @Override
        public boolean equals(Object o) {

            if (this == o) return true;
            StackRecord r = (StackRecord)o;
            return hash == r.hash && super.equals(r);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        void add(StackTraceElement l) {
            add(
                    Tuples.pair(
                            l.getClassName(),
                            PrimitiveTuples.pair(l.getLineNumber(), l.getMethodName())
                    )
            );
        }

        public void add(StackWalker.StackFrame l) {
            add(
                    Tuples.pair(
                            l.getClassName(),
                            PrimitiveTuples.pair(l.getLineNumber(), l.getMethodName())
                    )
            );
        }
    }

    public static class StackResult extends Result<StackResult> {

        private final Map<Thread.State, HashBag<StackRecord>> calleeSum;
        private final int topStacks, topCallees;
        private final HashBag<Pair<String, IntObjectPair<String>>> calledSum;

        StackResult(Map<Thread.State, HashBag<StackRecord>> calleeSum, HashBag<Pair<String, IntObjectPair<String>>> calledSum, int topStacks) {
            super(ResultRole.SECONDARY, Defaults.PREFIX + "stack", of(Double.NaN), "---", AggregationPolicy.AVG);
            this.calleeSum = calleeSum;
            this.calledSum = calledSum;
            this.topStacks = topStacks;
            this.topCallees = topStacks * 4;
        }

        @Override
        protected Aggregator<StackResult> getThreadAggregator() {
            return new StackResultAggregator();
        }

        @Override
        protected Aggregator<StackResult> getIterationAggregator() {
            return new StackResultAggregator();
        }

        @Override
        public String toString() {
            return "<delayed till summary>";
        }

        @Override
        public String extendedInfo() {
            return toString(calleeSum) + "\n" + toString(calledSum);
        }



        String toString(final Map<Thread.State, HashBag<StackRecord>> stacks) {

            int top = 32;

            StringBuilder sb = new StringBuilder(16*1024);

            stacks.entrySet().forEach(e -> {
                HashBag<StackRecord> cc = e.getValue();
                MutableList<ObjectIntPair<StackRecord>> dd = cc.topOccurrences(top);

                Thread.State state = e.getKey();
                float totalHundredths = cc.size()/100f;
                sb.append(state).append(" (").append(totalHundredths + " recorded)\n");
                dd.forEach(x -> {
                    sb.append('\t').append( Texts.n4(x.getTwo()/totalHundredths) ).append("%\t").append(x.getOne()).append('\n');
                });

                sb.append("\n");
            });

            return sb.toString();






























































        }

        private String toString(HashBag<Pair<String, IntObjectPair<String>>> calleeSum) {
            StringBuilder sb = new StringBuilder(16*1024).append("CALlEES\n");

            float totalHundredths = calleeSum.size()/100f;
            calleeSum.topOccurrences(topCallees).forEach((x) -> {
                sb.append('\t').append( Texts.n4(x.getTwo()/totalHundredths) ).append("%\t").append(x.getOne()).append('\n');
            });

            return sb.toString();

        }
    }

    static class StackResultAggregator implements Aggregator<StackResult> {
        @Override
        public StackResult aggregate(Collection<StackResult> results) {
            int topStacks = 0;
            Map<Thread.State, HashBag<StackRecord>> calleeSum = new EnumMap<>(Thread.State.class);
            HashBag<Pair<String, IntObjectPair<String>>> calledSum = new HashBag();
            for (StackResult r : results) {
                r.calleeSum.forEach((key, value) -> {
                    HashBag<StackRecord> sumSet = calleeSum.computeIfAbsent(key, (x) -> new HashBag<>());
                    value.forEachWithOccurrences((x,o)->{
                        sumSet.addOccurrences(x, o);
                        calledSum.addOccurrences(x.getFirst(), o);
                    });

                });
                topStacks = r.topStacks;
            }
            return new StackResult(calleeSum, calledSum, topStacks);
        }
    }

}
