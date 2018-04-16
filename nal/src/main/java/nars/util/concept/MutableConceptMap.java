//package nars.util.data;
//
//
//import nars.$;
//import nars.NAR;
//import nars.concept.Concept;
//import nars.term.Term;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.Set;
//
//public abstract class MutableConceptMap<T extends Term> extends ConceptMap implements Iterable<T> {
//
//    public final Set<T> inclusions = $.newHashSet(16);
//
//    protected MutableConceptMap(@NotNull NAR n) {
//        super(n);
//    }
//
//    public boolean contains(T t) {
//        return inclusions.contains(t);
//    }
//
//    public void include(T t) {
//        inclusions.add(t);
//    }
//
//    public abstract boolean include(Concept c);
//    public abstract boolean exclude(Concept c);
//
//    @Override
//    protected boolean onConceptActive(Concept c) {
//        return include(c);
//    }
//
//    @Override
//    protected boolean onConceptForget(@NotNull Concept c) {
//        if (inclusions.contains(c.term())) return false;
//        return exclude(c);
//    }
//
//}