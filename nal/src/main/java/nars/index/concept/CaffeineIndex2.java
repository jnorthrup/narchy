//package nars.index.concept;
//
//import com.github.benmanes.caffeine.cache.*;
//import nars.NAR;
//import nars.Op;
//import nars.Param;
//import nars.subterm.Subterms;
//import nars.target.Term;
//import nars.target.Termed;
//import nars.target.Variable;
//import nars.util.target.TermContainerToOpMap;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.Objects;
//import java.util.stream.IntStream;
//import java.util.stream.Stream;
//
//import static nars.Op.True;
//
//
///**
// * TODO
// *      --stored Atoms do not need a TermContainerToOpMap any larger than 1
// *      --rearrange the ordering of Ops so that variables are at the end just before the virtual operators,
// *          so that the lower subset starting at 0 are the kinds of operators being stored. this will
// *          slightly reduce the size that TermContainerToOpMap's need to be.
// */
//public class CaffeineIndex2 extends MaplikeConceptIndex implements RemovalListener<Subterms, TermContainerToOpMap<Termed>> {
//    private final long capacity;
//
//
//
//
//
//
//
//
//    /**
//     * holds compounds and subterm vectors
//     */
//    @NotNull
//    public Cache<Subterms, TermContainerToOpMap<Termed>> vectors;
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//    final static Weigher<Subterms, TermContainerToOpMap> w = (k, v) -> (k.complexity() + k.volume())/2;
//
//
//    /**
//     * use the soft/weak option with CAUTION you may experience unexpected data loss and other weird symptoms
//     */
//    public CaffeineIndex2(long capacity) {
//        super();
//
//
//
//        this.capacity = capacity;
//
//    }
//
//    @Override
//    public void init(NAR nar) {
//        Caffeine builder = Caffeine.newBuilder().removalListener(this);
//        if (capacity > 0) {
//
//            builder.maximumWeight(capacity * 4);
//            builder.weigher(w);
//        } else
//            builder.softValues();
//
//        if (Param.DEBUG)
//            builder.recordStats();
//
//        builder.executor(nar.exe);
//
//        this.vectors = builder.build();
//
//        super.init(nar);
//    }
//
//
//    @Override
//    public Stream<Termed> stream() {
//        return vectors.asMap().values().stream().flatMap(x -> IntStream.range(0, x.length()).mapToObj(x::get).filter(Objects::nonNull));
//    }
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//    @Override
//    public Termed remove(@NotNull Term x) {
//
//        TermContainerToOpMap<Termed> v = vectors.getIfPresent(vector(x));
//        if (v != null) {
//            v.setAt(x.op().id, null);
//
//
//
//
//
//
//        }
//
//        return null;
//    }
//
//    static Subterms vector(Term x) {
//        Subterms xs = x.subterms();
//        if (xs.subs() == 0) {
//
//            return Op.terms.subterms(x, True);
//        } else {
//            return xs;
//        }
//
//    }
//
//    @Override
//    public void setAt(@NotNull Term src, @NotNull Termed target) {
//        vectorOrCreate(src).setAt(src.op().id, target);
//    }
//
//    private TermContainerToOpMap<Termed> vectorOrCreate(@NotNull Term x) {
//        return vectors.get(vector(x), TermContainerToOpMap::new);
//    }
//
//
//    @Override
//    public void clear() {
//        vectors.invalidateAll();
//    }
//
//
//    @Override
//    public int size() {
//        return (int) vectors.estimatedSize(); /* warning: underestimate */
//    }
//
//
//    @Override
//    public Termed get(Term x, boolean createIfMissing) {
//
//        if (x.volume() > nar.termVolumeMax.intValue())
//            return null;
//
//        assert (!(x instanceof Variable)) : "variables should not be stored in index";
//
//        Op op = x.op();
//        TermContainerToOpMap<Termed> v;
//        if (createIfMissing) {
//            v = vectors.get(vector(x), k -> {
//
//                TermContainerToOpMap<Termed> t = new TermContainerToOpMap<>(k);
//
//                Termed p = nar.conceptBuilder.apply(x, null /* HACK */);
//
//                if (p != null)
//                    t.compareAndSet(op.id, null, p);
//
//
//                return t;
//            });
//        } else {
//            v = vectors.getIfPresent(vector(x));
//        }
//
//        return v != null ? v.get(op.id) : null;
//
//    }
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//    @Override
//    public @NotNull String summary() {
//
//        return (vectors.estimatedSize() + " TermVectors, ") + ' ' +
//                (Param.DEBUG ? (" " + vectors.stats()) : "");
//
//
//
//    }
//
//
//    @Override
//    public void onRemoval(Subterms key, TermContainerToOpMap<Termed> value, RemovalCause cause) {
//        if (value!=null)
//            value.forEach(this::onRemove);
//    }
//}
