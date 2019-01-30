//package nars.derive;
//
//import jcog.data.set.ArrayHashSet;
//import jcog.pri.bag.Bag;
//import nars.NAR;
//import nars.Task;
//import nars.concept.Concept;
//import nars.derive.impl.ZipperDeriver;
//import nars.derive.premise.PremiseDeriverRuleSet;
//import nars.link.Activate;
//import nars.link.TaskLink;
//import nars.task.Tasklike;
//import nars.term.Term;
//import nars.term.Termed;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.List;
//import java.util.Random;
//import java.util.function.BiFunction;
//import java.util.function.Consumer;
//import java.util.function.Predicate;
//import java.util.function.Supplier;
//
///** sources of beliefterms for premise formation */
//public class BeliefSource {
//
//    /**
//     * termlinks sampled from the derived concept
//     */
//    public static final BiFunction<Concept, Derivation, LinkModel> ConceptTermLinker = (c, d) -> {
//
//        final Bag<Tasklike, TaskLink> tl = c.tasklinks();
//        if (tl.isEmpty())
//            return null;
//
//        return new LinkModel() {
//
//            private final Supplier<Term> terms;
//            private final Random rng = d.random;
//
//            {
//                terms = (c.term().op().atomic) ?
//                    () -> tl.sample(rng).term()
//                :
//                    () -> c.linker().sample(rng);
//
//            }
//            @Override
//            public ArrayHashSet<TaskLink> tasklinks(int max, ArrayHashSet<TaskLink> buffer) {
//
//                tl.sample(rng, max /*Math.min(max, tl.size())*/, x -> {
//                    if (x != null) buffer.add(x);
//                });
//
//                return buffer;
//
//            }
//
//            @Override
//            public Supplier<Term> beliefTerms() {
//    //            Sampler<PriReference<Term>> ct = c.termlinks();
//    //            return () -> {
//    //                @Nullable PriReference<Term> t = ct.sample(rng);
//    //                return t != null ? t.get() : null;
//    //            };
//               return terms;
//            }
//        };
//    };
//    /**
//     * virtual termlinks sampled from concept index
//     */
//    public static final BiFunction<Concept, Derivation, LinkModel> GlobalTermLinker = (c, d) -> new LinkModel() {
//
//        final NAR n = d.nar;
//        final Random rng = d.random;
//
//        @Override
//        public ArrayHashSet<TaskLink> tasklinks(int max, ArrayHashSet<TaskLink> buffer) {
//            c.tasklinks().sample(rng, max, x -> {
//                if (x!=null) buffer.add(x);
//            });
//            return buffer;
//        }
//
//        @Override
//        public Supplier<Term> beliefTerms() {
//            return () -> {
//                @Nullable Activate a = n.concepts.sample(rng);
//                return a != null ? a.term() : null;
//            };
//        }
//    };
//
//    /** randomly samples from list of concepts */
//    public static ZipperDeriver forConcepts(NAR n, PremiseDeriverRuleSet rules, List<Concept> concepts) {
//
//        return forConcepts(n, rules, concepts, GlobalTermLinker);
//    }
//
//
//    public static ZipperDeriver forConcepts(NAR n, PremiseDeriverRuleSet rules, List<? extends Termed> concepts, BiFunction<Concept, Derivation, LinkModel> linker) {
//        int cc = concepts.size();
//        assert(cc>0);
//        Random rng = n.random();
//        Consumer<Predicate<Activate>> forEach = x -> {
//            Concept c = n.concept(concepts.get(rng.nextInt(cc)));
//            if (c==null)
//                return;
//            while (x.test(new Activate(c, 1f))) ;
//        };
//
//
//        return new ZipperDeriver(forEach, rules, linker);
//    }
//
//    public static ZipperDeriver forTasks(NAR n, List<Task> tasks) {
//        int tt = tasks.size();
//        assert(tt>0);
//        Random rng = n.random();
//        Consumer<Predicate<Activate>> forEach = x -> {
//            while (x.test(new Activate(n.conceptualize(tasks.get(rng.nextInt(tt))), 1f))) ;
//        };
//        PremiseDeriverRuleSet rules = Derivers.nal(n, 1, 8);
//
//        return new ZipperDeriver(forEach, rules, GlobalTermLinker);
//    }
//
//    public static BiFunction<Concept, Derivation, LinkModel> ListTermLinker(List<? extends Termed> terms) {
//        return (c, d) -> new LinkModel() {
//
//            final Random rng = d.random;
//
//            @Override
//            public ArrayHashSet<TaskLink> tasklinks(int max, ArrayHashSet<TaskLink> buffer) {
//                c.tasklinks().sample(rng, max, x -> {
//                    if (x != null) buffer.add(x);
//                });
//                return buffer;
//            }
//
//            @Override
//            public Supplier<Term> beliefTerms() {
//                return () -> terms.get(rng.nextInt(terms.size())).term();
//            }
//        };
//    }
//
//    public interface LinkModel {
//        /** buffer is not automatically cleared here, do that first if neceessary */
//        ArrayHashSet<TaskLink> tasklinks(int max, ArrayHashSet<TaskLink> buffer);
//
//        Supplier<Term> beliefTerms();
//    }
//}
