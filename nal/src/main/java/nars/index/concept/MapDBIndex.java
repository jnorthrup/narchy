//package nars.index;
//
//import com.github.benmanes.caffeine.cache.*;
//import nars.IO;
//import nars.NAR;
//import nars.concept.Concept;
//import nars.concept.ConceptBuilder;
//import nars.term.Compound;
//import nars.term.Term;
//import nars.term.Termed;
//import nars.term.Termlike;
//import nars.term.atom.Atomic;
//import nars.term.container.TermContainer;
//import nars.util.signal.WiredConcept;
//import org.jetbrains.annotations.NotNull;
//import org.mapdb.*;
//
//import javax.annotation.Nonnull;
//import java.io.IOException;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.Executor;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.function.Consumer;
//
//
//public class MapDBIndex extends MaplikeIndex implements MapModificationListener<Term, Termed> {
//
//    private final HTreeMap<Term, Termed> concepts;
//
//    private NAR nar;
//
//
//
//
//    /** use the soft/weak option with CAUTION you may experience unexpected data loss and other weird symptoms */
//    public MapDBIndex(ConceptBuilder conceptBuilder, long maxSize, @NotNull ScheduledExecutorService executor) {
//        super(conceptBuilder);
//
//        concepts = (HTreeMap<Term, Termed>) DBMaker
//                //.heapShardedHashMap(Runtime.getRuntime().availableProcessors())
//                .heapDB().make()
//                .hashMap(
//                        "concepts").keySerializer(
//                        new Serializer<Term>() {
//
//                            @Override
//                            public void serialize(DataOutput2 out, Term value) throws IOException {
//                                IO.writeTerm(out, value);
//                            }
//
//                            @Override
//                            public Term deserialize(DataInput2 input, int available) throws IOException {
//                                return IO.readTerm(input, nar.index);
//                            }
//                        })
//                .expireMaxSize(maxSize)
//                .expireExecutorPeriod(5000)
//                .expireCompactThreshold(0.2f)
//                .expireAfterUpdate()
//                //.expireAfterCreate()
//                //.expireAfterGet()
//                .counterEnable()
//                .expireExecutor(executor).create();
//
//
//    }
//
//
//    @Override
//    public void remove(@NotNull Termed x) {
//        concepts.remove(x);
//    }
//
//    @Override
//    public Termed get(@NotNull Termed x) {
//        return concepts.get(x);
//    }
//
//    @Override
//    public void set(@NotNull Termed src, @NotNull Termed target) {
//        concepts.put(src.term(), target);
//    }
//
//    @Override
//    public @NotNull TermContainer internSubterms(@NotNull TermContainer s) {
//        return null;
//    }
//
//
//    @Override
//    public void clear() {
//        concepts.clear();
//    }
//
//    @Override
//    public void forEach(@NotNull Consumer<? super Termed> c) {
//        concepts.forEach((k, v) -> {
//            c.accept(k);
//        });
//
//    }
//
//    @Override
//    public int size() {
//        return concepts.size();
//    }
//
//    @Override
//    public int subtermsCount() {
//        return -1; //not calculated when they share the same cache
//        //return (int) subs.estimatedSize();
//    }
//
//
//
//
//    /**
//     * default lowest common denominator impl, subclasses may reimpl for more efficiency
//     */
//    @Override
//    @NotNull
//    protected Termed getConceptCompound(@NotNull Compound x) {
//        return concepts.computeIfAbsent(x, this::buildConcept);
//    }
//
//
//
//    @NotNull
//    @Override
//    protected Termed getNewAtom(@NotNull Atomic x) {
//        return concepts.computeIfAbsent(x, this::buildConcept);
//    }
//
//    @Override
//    public @NotNull String summary() {
//        return concepts.size() + " concepts";
//    }
//
////    /** this will be called from within a worker task */
////    @Override public final void onRemoval(Object key, Object value, @Nonnull RemovalCause cause) {
////        if (value instanceof Concept) {
////            ((Concept) value).delete(nar);
////        }
////    }
//
//    @Override
//    public void start(NAR nar) {
//        this.nar = nar;
//    }
//
//
//    @Override
//    public void modify(Term key, Termed oldValue, Termed newValue, boolean triggered) {
//        System.out.println(key + " " + oldValue + " " + newValue + " "+ triggered);
//    }
//}
