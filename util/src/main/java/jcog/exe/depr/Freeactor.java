//package jcog.exe;
//
//import jcog.list.FasterList;
//import org.eclipse.collections.impl.bag.mutable.primitive.ShortHashBag;
//import org.eclipse.collections.impl.map.mutable.primitive.ShortObjectHashMap;
//import org.eclipse.collections.impl.set.mutable.primitive.ShortHashSet;
//
//import java.util.List;
//import java.util.function.Consumer;
//import java.util.function.Predicate;
//
///** free (re-)actor = FreeActor
// *      dynamic non-deterministic prioritized rule executor
// */
//public class Freeactor<C> {
//
//    private final List<Rule> rules = new FasterList();
//    final ShortObjectHashMap<Predicate<C>> states = new ShortObjectHashMap<>();
//
//    final ShortObjectHashMap<List<Rule>> pre = new ShortObjectHashMap<>();
//
//    short[] preRanked = null;
//
//
//    public class Rule {
//
//        /** precondition, guards, && */
//        public final ShortHashSet pre;
//
//        /** if preconditions met, then this sequence of predi-forms are applied (in order, but foldable with other rules in common) */
//        public final ShortHashSet trans = new ShortHashSet();
//
//        /** finally the action can be run */
//        public final Consumer<C> action;
//
//        public Rule(ShortHashSet pre, Consumer<C> action) {
//            this.pre = pre;
//            this.action = action;
//        }
//
////        //returns an instrumented version of
////        public Rule instrument() {
////            return new Rule() {}
////        }
//    }
//
//    public class RuleBuilder1 {
//
//        final ShortHashSet conditions;
//
//        public RuleBuilder1(ShortHashSet conditions) {
//            this.conditions = conditions;
//        }
//
//        public Rule then(Consumer<C> c) {
//            return add(new Rule(conditions, intern(c)));
//        }
//    }
//
//    private Consumer<C> intern(Consumer<C> c) {
//        return c; //TODO
//    }
//
//    private Rule add(Rule rule) {
//        rules.add(rule);
//        rule.pre.forEach(c -> {
//            pre.getIfAbsent(c, FasterList::new).add(rule);
//        });
//        return rule;
//    }
//
//    public RuleBuilder1 when(Predicate<C> bool) {
//        short ifTrue = addState(bool);
//        return new RuleBuilder1(ShortHashSet.newSetWith(ifTrue));
//    }
//
//
//    private short addState(Predicate<C> bool) {
//        short id = (short) (1 + states.size());
//        states.put(id, bool);
//        return id;
//    }
//
//
//    public void run(C d) {
//        ShortHashSet tru = new ShortHashSet();
//        states.forEachKeyValue((k,v)->{
//            boolean t = (v.test(d));
//            tru.add(t ? k : ((short)-k));
//        });
//    }
//
//    static class DummyState {
//        public String i = null;
//        public int k = 0;
//    }
//
//    public static void main(String[] args) {
//        Freeactor<DummyState> f = new Freeactor<DummyState>();
//
//        StringBuilder log = new StringBuilder();
//
//        f.when((d)->d.k == 0).then((d)->log.append("k is zero\n"));
//        f.compile();
//        //---
//
//        DummyState d = new DummyState();
//        f.run(d);
//
//        d.k = 1;
//        f.run(d);
//
//        System.out.println(log);
//    }
//
//    private void compile() {
//        ShortHashBag rank = new ShortHashBag();
//
//        //TODO factor in relative cost of evaluating each
//        pre.forEachKeyValue((s,r)->{
//            rank.addOccurrences((short)(Math.abs(s)), r.size());
//        });
//
//        preRanked = rank.toSortedArray();
//    }
//
//
//}
