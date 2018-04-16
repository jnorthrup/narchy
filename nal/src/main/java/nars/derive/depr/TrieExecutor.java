//package nars.derive;
//
//import jcog.data.graph.AdjGraph;
//import jcog.list.FasterIntArrayList;
//import jcog.list.FasterList;
//import nars.control.Derivation;
//
///**
// * stackless recursive virtual machine which
// * feedback controls for obeying AIKR.  the goal was to
// * write a more efficient TrieDeriver evaluation procedure
// * by avoiding recursive Java calls with an iterative loop
// * and custom stack behavior that includes the feedback
// * and shuffling requirements within it
// */
//public class TrieExecutor extends AbstractPred<Derivation> {
//
//
//    final static ThreadLocal<CPU<Derivation>> cpu = ThreadLocal.withInitial(CPU::new);
//
//    private final PrediTerm<Derivation> root;
//
//    public TrieExecutor(PrediTerm<Derivation> root) {
//        super(root);
//        this.root = root;
//
//    }
//
//
//    class Path {
//
//    }
//
//    final AdjGraph<PrediTerm, Path> value = new AdjGraph(true);
//
//
//    @Override
//    public boolean test(Derivation d) {
//
//        CPU c = cpu.get();
//
//        c.ready(d);
//
//        FasterList<PrediTerm<Derivation>> stack = c.stack;
//        FasterIntArrayList ver = c.ver;
//
//        PrediTerm<Derivation> cur = root;
//        //System.out.println(d.time);
//        while (true) {
//
//            //System.out.println("\t" + d.ttl + " " + stack.size());
//            PrediTerm<Derivation> next = exec(cur, d, c);
//
//            if (next == cur) {
//                break; //termination signal
//            } else if (next == null) {
//                if ((cur = stack.removeLastElseNull()) == null)
//                    break;
//                if (!d.revertAndContinue(ver.pop()))
//                    break;
//            } else {
//                cur = next;
//            }
//
//        }
//        //System.out.println(d.time + " \t" + d.ttl + " " + stack.size());
//
//        return true;
//    }
//
//    protected PrediTerm<Derivation> exec(PrediTerm<Derivation> cur, Derivation d, CPU<Derivation> c) {
////        //custom instrumentation, to be moved to subclass
////        if (cur instanceof Fork || cur instanceof UnifyTerm) {
////            int to = value.addNode(cur);
////            c.stack.forEach(cause -> {
////               int from = value.addNode(cause);
////               value.edge(from, to, ()->new Path());
////            });
////            if (Math.random() < 0.001f) {
////                try {
////                    value.writeGML(new PrintStream(new FileOutputStream("/tmp/x.gml")));
////                } catch (FileNotFoundException e) { }
////                //value.writeGML(System.out);
////            }
////        }
//
//        PrediTerm<Derivation> next = cur.exec(d, c);
//
//
//        return next;
//    }
//
//
//}
///*
//        final int[] serial = {1};
//        TrieDeriver.forEach(null, root, (from, to) -> {
//            ;
//
//            int t = value.addNode(node(to));
//
//            if (from!=null) {
//                int f = value.addNode(node(from));
//                value.setEdge(f, t, new Path());
//            }
//        });
//
////
//        try {
//            value.writeGML(new PrintStream(new FileOutputStream("/tmp/x.gml")));
//            System.out.println();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//
//    @NotNull
//    public Term node(PrediTerm from) {
//        Term f;
//        if (from instanceof AndCondition || from instanceof Fork || from instanceof OpSwitch) {
//            f = from;
//        } else {
//            f = $.p(from, $.the(System.identityHashCode(from)));
//        }
//        return f;
//    }
////    public PrediTerm<Derivation> network(PrediTerm<Derivation> x) {
////        if (x instanceof Fork) {
//////            CompoundTransform ct = (parent, subterm) -> {
//////                if (subterm instanceof Fork) {
//////
//////                }
//////                return subterm;
//////            };
//////            Choice c = new Choice(Util.map(y -> (AbstractPred)y.transform(ct),
//////                    AbstractPred[]::new, ((Fork)x).cache));
//////
//////            return c;
////            return x;
////        } else {
////            return x;
////        }
////    }
//
// */

//package nars.derive;
//
//import jcog.Util;
//import jcog.list.FasterIntArrayList;
//import jcog.list.FasterList;
//import jcog.map.CustomConcurrentHashMap;
//import jcog.pri.Pri;
//import nars.control.Derivation;
//import nars.derive.op.UnifySubtermThenConclude;
//
//import java.util.Arrays;
//import java.util.Map;
//import java.util.concurrent.atomic.AtomicLong;
//
//import static jcog.map.CustomConcurrentHashMap.IDENTITY;
//import static jcog.map.CustomConcurrentHashMap.STRONG;
//import static nars.time.Tense.ETERNAL;
//
///**
// * deriver virtual machine state
// */
//final class CPU<D> {
//
//    static final int stackLimit = 64;
//    final FasterIntArrayList ver = new FasterIntArrayList(stackLimit);
//    final FasterList<PrediTerm> stack = new FasterList<>(stackLimit);
//
////        /**
////         * call to lazily revert the derivation versioning right before it's necessary
////         */
////        void sync(Derivation d) {
////            int ss = stack.size();
////            int vv = ver.size();
////            if (ss == vv)
////                return;
////
////
////            assert (vv > ss);
////            d.revert(ver.pop(vv - ss)); //reduce the version stack to meet the instruction stack
////        }
//
//    void fork(Derivation d, PrediTerm<Derivation>[] branch) {
//
////            sync(d);
//        //assert (ver.size() == stack.size());
//
//        int branches = branch.length;
//        int stackStart = stack.size();
//        int stackEnd = stackStart + branches;
//        if (stackEnd < stackLimit) {
//            int before = d.now();
//
//            Object[] stackData = stack.array();
//            System.arraycopy(branch, 0, stackData, stackStart, branches);
//            stack.setSize(stackEnd);
//            //assert (stack.size() == ver.size() + branches);
//
//            loaded(d, stackStart, stackEnd);
//
//            d.shuffler.shuffle(d.random, stackData, stackStart, stackEnd);
//
//            int[] va = ver.array();
//            Arrays.fill(va, stackStart, stackEnd, before);
//            ver.setSize(stackEnd);
//            //assert (ver.size() == stack.size());
//
//        }
//    }
//
//    public void ready(Derivation d) {
//
//        stack.clearFast();
//        ver.clearFast();
//
//        long now = d.time;
//        final int UPDATE_DURATIONS = 2;
//        long lastRefresh = CPU.lastRefresh.get();
//        if (lastRefresh == ETERNAL || now - lastRefresh >= d.dur * UPDATE_DURATIONS) {
//            if (CPU.lastRefresh.compareAndSet(lastRefresh, now)) {
////                if (!valueCache.isEmpty()) {
////                    System.out.println("valueCache cleared of " + valueCache.size() + " entries");
////                }
//                valueCache.clear();
//            }
//        }
//
//    }
//
//    /**
//     * filter method for annotating a predicate being pushed on the stack.
//     * it can pass the input value through or wrap it in some method
//     */
//    protected void loaded(Derivation d, int start, int end) {
//
//
//        //<custom implemenation which wraps the new items in equally budgeted Budgeted instances
//        float baseRate = 0.2f;
//        float totalTTLConsumption = 2.5f;
//
//        final int input = Math.round(d.ttl * totalTTLConsumption);
//        int num = end - start;
//
//        float v[] = new float[num];
//        float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
//        int j = 0;
//        for (int i = start; i < end; i++) {
//            float a = v[j++] = value(stack.get(i), d);
//            min = Math.min(a, min);
//            max = Math.max(a, max);
//        }
//        float range = max - min; //>=0
//
//        //how much to allocate by default
//        float ttlEach = ((float) input) / num;
//        if (Util.equals(range, 0f, Pri.EPSILON)) {
//            Arrays.fill(v, ttlEach); //flat, there is no significant variation
//        } else {
//            //normalize to 0..1.0 which will directly represent the additional proportion allocated in addition to the base rate
//            float sum = 0;
//            for (int i = 0; i < num; i++) {
//                sum += (v[i] = (v[i] - min) / range);
//            }
//
//            float baseTTLforEach = baseRate * ttlEach;
//            float sharedTTL = (1f - baseRate) * input;
//            for (int i = 0; i < num; i++) {
//                v[i] = baseTTLforEach + sharedTTL * (v[i] / sum) /* v[i] after the above normalization */;
//            }
//        }
//
//        int k = 0;
//        for (int i = start; i < end; i++) {
//            stack.set(i, new Budgeted(stack.get(i), (int) v[k++]));
//        }
//
//        //assert (Util.equals(Math.abs(Util.sum(v) - input), 0, 0.01f + input/100.0f)): "alloc=" + Util.sum(v) + " vs input=" + input;
//        //</custom implemenation>
//    }
//
//    final static Map<PrediTerm, Float> valueCache = new CustomConcurrentHashMap(
//            STRONG, IDENTITY, STRONG, IDENTITY, 512
//    );
//    static final AtomicLong lastRefresh = new AtomicLong(ETERNAL);
//    //TODO final static Map<PrediTerm,Function<Derivation,Float>> valueCalculationCache = new ConcurrentHashMap<>();
//
//    float value(PrediTerm<Derivation> x, Derivation d) {
//
//
//        Float v = valueCache.get(x);
//        if (v == null) {
//            float v2 = _value(x, d); //cant do the one call because its recursive
//            valueCache.put(x, v2);
//            return v2;
//        } else {
//            return v;
//        }
//
//    }
//
//    /**
//     * estimates the value of investing in the given branch
//     * ie. find any predicates resulting in something which can backprop causal value feedback
//     * the returned value may be positive or negative. by default returns 0 (neutral)
//     */
//    float _value(PrediTerm<Derivation> x, Derivation d) {
//        if (x instanceof AndCondition) {
//            PrediTerm[] p = ((AndCondition) x).cache;
//            return _value(p[p.length - 1], d);
//        } else if (x instanceof UnifySubtermThenConclude) {
//            return _value(((UnifySubtermThenConclude) x).eachMatch, d);
//        } else if (x instanceof Conclusion) {
//            return ((Conclusion) x).channel.value();
//        } else if (x instanceof Fork) {
//            return valueAggregate(((Fork) x).cache, d);
//        } //else if (x instanceof OpSwitch)
//        //TODO else if (x instanceof Fork) {
//        //used cached graph mapping the downstream causal structure, using Fork's as intermediate nodes
//
//        return 0f;
//    }
//
//    /**
//     * hack, expensive
//     */
//    private float valueAggregate(PrediTerm[] cache, Derivation d) {
//        float p = Util.sum(x -> value(x, d), cache);
////        if (p > 0)
////            System.out.println("valAgg: " + p);
//        return p;
//    }
//
//    static class Budgeted extends AbstractPred<Derivation> {
//        private final PrediTerm<Derivation> rel;
//        int ttl;
//
//        Budgeted(PrediTerm<Derivation> rel, int ttl) {
//            super(rel);
//            this.ttl = ttl;
//            this.rel = rel;
//        }
//
//        @Override
//        public boolean test(Derivation d) {
//            return rel.test(d);
//        }
//
//        @Override
//        public PrediTerm<Derivation> exec(Derivation d, CPU cpu) {
//            int fund = Math.min(ttl, d.ttl);
//            this.ttl = d.getAndSetTTL(fund) - fund;
//            PrediTerm<Derivation> y = rel.exec(d, cpu);
//            d.addTTL(ttl); //refund
//            return y;
//        }
//    }
//
////        void push(int before, @NotNull PrediTerm x) {
////            ver.add(before);
////            stack.add(x);
////        }
//
//}
