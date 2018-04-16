package nars.index.concept;

import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** additionally caches subterm vectors */
public class MapConceptIndex extends MaplikeConceptIndex {

    protected final Map<Term,Termed> concepts;

    public MapConceptIndex(Map<Term, Termed> map) {
        super();
        this.concepts = map;
    }

    @Override public Stream<Termed> stream() {
        return concepts.values().stream();
    }


    @Nullable
    @Override
    public Termed get(Term x, boolean createIfMissing) {
        if (createIfMissing) {
            return concepts.compute(x, nar.conceptBuilder);
        } else {
            return concepts.get(x);
        }
    }

    @Override
    public String summary() {
        return concepts.size() + " concepts";
    }


    @Override
    public void remove(Term entry) {
        concepts.remove(entry);
    }

    @Override
    public void set(@NotNull Term src, @NotNull Termed target) {
        concepts.merge(src, target, setOrReplaceNonPermanent);
    }

    @Override
    public void clear() {
        concepts.clear();
    }

    @Override
    public void forEach(@NotNull Consumer<? super Termed> c) {
        concepts.forEach((k, v)-> c.accept(v));
    }

    @Override
    public int size() {
        return concepts.size() /* + atoms.size? */;
    }

//    @Override
//    public void clear() {
//        super.clear();
//        subterms.clear();
//    }

//    @Override
//    @NotNull public TermContainer internSubterms(@NotNull TermContainer x) {
//        TermContainer prev = subterms.putIfAbsent(x, x);
//        return (prev == null) ? x : prev;
//    }
}

//package nars.index;
//
//import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
//import nars.concept.Concept;
//import nars.term.*;
//import nars.term.container.TermContainer;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.Map;
//import java.util.function.Consumer;
//import java.util.function.Function;
//
///**
// * Created by me on 1/2/16.
// */
//public class GroupedMapIndex extends AbstractMapIndex {
//
//
//    public static class SubtermNode extends IntObjectHashMap<Termed> {
//        public final TermContainer vector;
//
//        public SubtermNode(TermContainer normalized) {
//            super(1);
//            this.vector = normalized;
//        }
//
//    }
//
//    /** uses an array for fast lookup and write access to basic term types where relation isnt used */
//    public static final class SubtermNodeWithArray extends SubtermNode {
//
//        final static int NUM_FAST = 16;
//        @NotNull
//        private final Termed[] fast;
//
//        public SubtermNodeWithArray(TermContainer normalized) {
//            super(normalized);
//            this.fast = new Termed[NUM_FAST];
//        }
//
//        @Override
//        public Termed get(int key) {
//            if (Terms.relComponent(key) == 0xffff) {
//                int o = Terms.opComponent(key);
//                if (o < NUM_FAST) {
//                    return fast[o];
//                }
//            }
//            return super.get(key);
//        }
//
//        @Override
//        public Termed put(int key, Termed value) {
//            if (Terms.relComponent(key) == 0xffff) {
//                int o = Terms.opComponent(key);
//                if (o < NUM_FAST) {
//                    Termed p = fast[o];
//                    fast[o] = value;
//                    return p;
//                }
//            }
//            return super.put(key, value);
//        }
//
//        //        @Override
////        public Termed put(int key, Termed value) {
////            return super.put(key, value);
////        }
//
//        /*
//            g.compact();
//        */
//
//    }
//
//
//    public final Map<TermContainer, SubtermNode> data;
//    int count;
//
//    public GroupedMapIndex(Map<TermContainer, SubtermNode> data, Concept.ConceptBuilder conceptBuilder) {
//        this(data, Terms.terms, conceptBuilder);
//    }
//
//    public GroupedMapIndex(SymbolMap symbolMap, Map<TermContainer, SubtermNode> data, TermBuilder termBuilder, Concept.ConceptBuilder conceptBuilder) {
//        super(symbolMap, termBuilder, conceptBuilder);
//        this.data = data;
//    }
//
//    public GroupedMapIndex(Map<TermContainer, SubtermNode> data, TermBuilder termBuilder, Concept.ConceptBuilder conceptBuilder) {
//        this(new HashSymbolMap(), data, termBuilder, conceptBuilder);
//    }
//
//
//
//
//    @Nullable
//    transient final Function<TermContainer, SubtermNode> termContainerSubtermNodeFunction =
//            k ->
//                new SubtermNode(normalize(k));
//                //new SubtermNodeWithArray(normalize(k));
//
//
//    @Nullable
//    @Override protected Termed theCompound(@NotNull Compound t, boolean create) {
//
//        if (t.hasTemporal())
//            return t;
//
//        TermContainer subsBefore = t.subterms();
//
//        SubtermNode node = create ?
//                getOrAddNode(subsBefore) :
//                getNode(subsBefore);
//
//        return node!=null ? theCompound(t, node, subsBefore, create) : null;
//    }
//
//    @Nullable private Termed theCompound(@NotNull Compound t, @NotNull SubtermNode node, @NotNull TermContainer subsBefore, boolean create) {
//
//        int oprel = t.opRel();
//
//        Termed interned = node.get(oprel);
//        if (interned!=null)
//            return interned;
//
//        if (create) {
//
//            TermContainer subsAfter = node.vector;
//            if (subsAfter!=subsBefore) { //rebuild if necessary
//                if ((interned = internCompoundSubterms(subsAfter, t.op(), t.relation(), t.dt())) == null)
//                    throw new InvalidTerm(t);
//                    //return null;
//            } else {
//                interned = t; //use original parameter itself; for more isolation, this could be replaced with a clone creator
//            }
//
//            interned = internCompound(interned);
//
//            //insert into node
//            return node.getIfAbsentPut(t.opRel(), interned);
//
//        } else {
//            return null;
//        }
//    }
//
//
//
//
//
//    @NotNull
//    @Override public TermContainer theSubterms(TermContainer s) {
//        return getOrAddNode(s).vector;
//    }
//
//    @NotNull public SubtermNode getOrAddNode(TermContainer s) {
//        //return data.computeIfAbsent(s, termContainerSubtermNodeFunction);
//        SubtermNode d = data.get(s);
//        if (d == null) {
//            data.put(s, d = termContainerSubtermNodeFunction.apply(s));
//        }
//        return d;
//    }
//    @Nullable public SubtermNode getNode(TermContainer s) {
//        return data.get(s);
//    }
//
//
//    @Nullable
//    @Override
//    public Termed set(@NotNull Termed src, Termed target) {
//        //TODO support Atomic insertion
//
//        SubtermNode node = getOrAddNode(((Compound) src.term()).subterms());
//        Termed existing = node.put(src.opRel(), target);
//        if (existing!=null && existing!=target)
//            throw new RuntimeException(target + " can not be set because " + existing + " already exists");
//
//        return target;
//    }
//
//    @Override
//    public void clear() {
//        count = 0;
//        data.clear();
//        //TODO atoms.clear();
//    }
//
//    @Override
//    public int subtermsCount() {
//        return data.size();
//    }
//
//    @Override
//    public int size() {
//        /** WARNING: not accurate */
//        return data.size();// + atoms.size();
//    }
//
//
//
//    @Override
//    public void forEach(@NotNull Consumer<? super Termed> c) {
//        atoms.forEach(c);
//        data.values().forEach(v->v.forEach(c));
//    }
//
//    @NotNull
//    @Override
//    public String summary() {
//        return data.size() + " termVectors, " + ((HashSymbolMap)atoms).map.size() + " atoms";
//    }
//
//}
