package nars.index.term;

import com.google.common.util.concurrent.Futures;
import nars.NAR;
import nars.Op;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.concept.builder.ConceptBuilder;
import nars.concept.builder.DefaultConceptBuilder;
import nars.concept.state.ConceptState;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.transform.CompoundTransform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static nars.Op.Null;

/**
 *
 */
public abstract class TermIndex implements TermContext {


    public final ConceptBuilder conceptBuilder = new DefaultConceptBuilder();
    public NAR nar;

    /**
     * internal get procedure (synchronous)
     */
    @Nullable
    public abstract Termed get(/*@NotNull*/ Term key, boolean createIfMissing);

    public CompletableFuture<Termed> getAsync(Term key, boolean createIfMissing) {
        return CompletableFuture.completedFuture(get(key, createIfMissing));
    }

    @Override
    public final Termed apply(Term term) {
        return get(term, false);
    }

    /**
     * sets or replaces the existing value, unless the existing value is a PermanentConcept it must not
     * be replaced with a non-Permanent concept
     */
    public abstract void set(Term src, Termed target);

    public final void set(Termed t) {
        set(t.term(), t);
    }

    abstract public void clear();


//    /**
//     * called when a concept has been modified, ie. to trigger persistence
//     */
//    public void commit(Concept c) {
//        //by default does nothing
//    }

    public void start(NAR nar) {
        this.nar = nar;

        conceptBuilder.start(nar);

    }

    /**
     * # of contained terms
     */
    public abstract int size();

    /**
     * a string containing statistics of the index's current state
     */
    @NotNull
    public abstract String summary();

    public abstract void remove(@NotNull Term entry);

    public void print(@NotNull PrintStream out) {
        stream().forEach(out::println);
        out.println();
    }

    abstract public Stream<? extends Termed> stream();

    /**
     * default impl
     */
    public void forEach(Consumer<? super Termed> c) {
        stream().forEach(c);
    }

    /**
     * applies normalization and anonymization to resolve the term of the concept the input term maps t
     * term should be conceptualizable prior to calling this
     */
    @Nullable
    public final Concept concept(Termed x, boolean createIfMissing) {

        Term y;
        if (x instanceof Concept) {
            Concept ct = (Concept) x;
            if (!ct.isDeleted())
                return ct; //assumes an existing Concept index isnt a different copy than what is being passed as an argument
            //otherwise if it is deleted, continue
            y = ct.term();
        } else {
            y = x.term().conceptual();
            if (!y.op().conceptualizable)
                return null;
        }

        return (Concept)get(y, createIfMissing);
    }

    public final void conceptAsync(Termed x, boolean createIfMissing, Consumer<Concept> with) {

        Term y;
        if (x instanceof Concept) {
            Concept ct = (Concept) x;
            if (!ct.isDeleted()) {
                with.accept(ct);
                return; //assumes an existing Concept index isnt a different copy than what is being passed as an argument
            }
            //otherwise if it is deleted, continue
            y = ct.term();
        } else {
            y = x.term().conceptual();
            if (!y.op().conceptualizable)
                return; //TODO error?
        }


        getAsync(y, createIfMissing).handle((t, e) -> {
                    if (e!=null) {
                        e.printStackTrace();
                    } else {
                        with.accept((Concept) t);
                    }
                    return this;
                });
    }

    protected final void onRemove(Termed value) {
        if (value instanceof Concept) {
            if (value instanceof PermanentConcept) {
                //refuse deletion
                set(value);
            } else {

                Concept c = (Concept) value;
                onBeforeRemove(c);
                c.delete(nar);
            }
        }
    }

    protected void onBeforeRemove(Concept c) {

    }

    /** accesses the interning termcontext which recursively replaces subterms with what this index resolves them to */
    public final TermContext intern() {
        return intern;
    }

    private final TermContext intern = new InterningContext();


    private class InterningContext implements CompoundTransform {

        @Override
        public Termed apply(Term x) {
            return applyTermIfPossible(x);
        }


        @Override
        public Term applyTermIfPossible(Term x) {
            //only resolve atomics
            if (!(x instanceof PermanentConcept)) {
                Op op = x.op();
                if (op.atomic && op.conceptualizable) {
                    return TermIndex.this.applyTermIfPossible(x);
                }
            }

            return x;
        }


        @Override
        public @Nullable Term applyTermOrNull(Term x) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public @Nullable Term transform(Compound x, Op op, int dt) {
            //return TermIndex.this.applyTermIfPossible(x);
            throw new UnsupportedOperationException();
        }


        @Override
        public Term intern(Term x) {
            return applyTermIfPossible(x);
        }
    }
}
