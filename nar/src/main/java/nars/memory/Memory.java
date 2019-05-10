package nars.memory;

import nars.NAR;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.util.TermException;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 *
 */
public abstract class Memory {


    protected NAR nar;

    /**
     * internal get procedure (synchronous)
     */
    @Nullable
    public abstract Termed get(/*@NotNull*/ Term key, boolean createIfMissing);

    private CompletableFuture<Termed> getAsync(Term key, boolean createIfMissing) {
        return CompletableFuture.completedFuture(get(key, createIfMissing));
    }


    /**
     * sets or replaces the existing value, unless the existing value is a PermanentConcept it must not
     * be replaced with a non-Permanent concept
     */
    public abstract void set(Term src, Concept target);

    public final void set(Concept t) {
        set(t.term(), t);
    }

    abstract public void clear();

    public void start(NAR nar) {
        this.nar = nar;
    }

    /**
     * # of contained terms
     */
    public abstract int size();

    /**
     * a string containing statistics of the index's current state
     */
    public abstract String summary();

    public abstract @Nullable Concept remove(Term entry);

    public void print(PrintStream out) {
        stream().forEach(out::println);
        out.println();
    }

    abstract public Stream<Concept> stream();


    /**
     * default impl
     * @param c
     */
    public void forEach(Consumer<? super Concept> c) {
        stream().forEach(c);
    }

    /**
     * applies normalization and anonymization to resolve the target of the concept the input target maps t
     * target should be conceptualizable prior to calling this
     */
    @Nullable
    public final Concept concept(Termed _x, boolean createIfMissing) {
        if (_x instanceof Concept) {
            if (elideConceptGets() && !(((Concept) _x).isDeleted())) {
                return ((Concept) _x);
            }
        }

        Term xt = _x.term();
        if (!xt.op().conceptualizable) {
//            if (NAL.DEBUG)
                throw new TermException("not conceptualizable", xt);
//            else
//                return null;
        }

//        if (NAL.DEBUG) { if (!xt.the()) throw new TermException("not immutable", xt); }

        if (xt instanceof Compound && !xt.isNormalized()) { //pre-test
            Term xtn = xt.normalize();
            if (xt.op()!=xtn.op()) //if (!xt.equals(xtn))
                throw new TermException("concept term not normalized", xt);
            xt = xtn;
        }

        Term x = xt.concept();
        if (!x.op().conceptualizable) {
            throw new TermException("not conceptualizable", _x.term());
        }

//        if (NAL.DEBUG) { if (!x.the()) throw new TermException("not immutable", x); }

        return (Concept) get(x, createIfMissing);
    }

    /**
     * for performance, if lookup of a Concept instance is performed using
     * a supplied non-deleted Concept instance, return that Concept directly.
     * ie. it assumes that the known Concept is the active one.
     * <p>
     * this can be undesirable if the concept index has an eviction mechanism
     * which counts lookup frequency, which would be skewed if elision is enabled.
     */
    boolean elideConceptGets() {
        return true;
    }

    public final void conceptAsync(Termed x, boolean createIfMissing, Consumer<Concept> with) {

        Term y;
        if (x instanceof Concept) {
            Concept ct = (Concept) x;
            if (!ct.isDeleted()) {
                with.accept(ct);
                return;
            }

            y = ct.term();
        } else {
            y = x.term().concept();
            if (!y.op().conceptualizable)
                return;
        }


        getAsync(y, createIfMissing).handle((t, e) -> {
            if (e != null) {
                e.printStackTrace();
            } else {
                with.accept((Concept) t);
            }
            return this;
        });
    }

    /** call this after each removal */
    protected final void onRemove(Concept value) {
//        if (value instanceof Concept) {
            if (value instanceof PermanentConcept) {

//                if (nar.exe.concurrent()) {
//                    nar.runLater(() -> set(value));
//                } else
                    set(value);

            } else {
                value.delete(nar);
            }
//        }
    }

    /** useful for map-like impl */
    static final BiFunction<? super Concept, ? super Concept, ? extends Concept> setOrReplaceNonPermanent = (prev, next) -> {
        if (prev instanceof PermanentConcept && !(next instanceof PermanentConcept))
            return prev;
        return next;
    };


}
