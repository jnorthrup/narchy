package nars.index.term;

import nars.NAR;
import nars.Op;
import nars.Param;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.concept.TaskConcept;
import nars.control.MetaGoal;
import nars.term.Functor;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static nars.Op.*;

/**
 *
 */
public abstract class ConceptIndex {


    public NAR nar;

    public final TermContext resolveFunctors = new Functor.FunctorResolver() {
        @Override
        public final Termed apply(Term term) {
            return get(term, false);
        }
    };

    /**
     * internal get procedure (synchronous)
     */
    @Nullable
    public abstract Termed get(/*@NotNull*/ Term key, boolean createIfMissing);

    public CompletableFuture<Termed> getAsync(Term key, boolean createIfMissing) {
        return CompletableFuture.completedFuture(get(key, createIfMissing));
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

    public void init(NAR nar) {
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

        x = x.unneg();

        Term y;
        if (x instanceof Concept) {
            Concept ct = (Concept) x;
            if (!ct.isDeleted())
                return ct; //assumes an existing Concept index isnt a different copy than what is being passed as an argument
            //otherwise if it is deleted, continue
            y = ct.term();
        } else {
            Term xx = x.term();
            if (xx.op().conceptualizable) {
                y = xx.concept();
                Op yop = y.op();
                if (!yop.conceptualizable || yop != x.op()) {
                    //x.term().conceptual(); //HACK for debugging
                    //throw new RuntimeException("conceptualization fault: " + x + " -> " + y);
                    return null;
                }
            } else {
                return null;
            }
        }

        return (Concept) get(y, createIfMissing);
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
            y = x.term().concept();
            if (!y.op().conceptualizable)
                return; //TODO error?
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

    protected final void onRemove(Termed value) {
        if (value instanceof Concept) {
            if (value instanceof PermanentConcept) {
                //refuse deletion
                nar.runLater(() -> {
                    set(value);
                });

            } else {

                Concept c = (Concept) value;
                if (c instanceof TaskConcept)
                    forget((TaskConcept) c);
                c.delete(nar);
            }
        }
    }

    protected void forget(TaskConcept tc) {
        tc.tasks().forEach(t -> {
            short[] c = t.cause();
            if (c.length > 0) {
                switch (t.punc()) {
                    case BELIEF:
                        MetaGoal.Believe.learn(c, -Param.beliefValue(t), nar.causes);
                        break;
                    case GOAL:
                        MetaGoal.Desire.learn(c, -Param.beliefValue(t), nar.causes);
                        break;
                    case QUESTION:
                    case QUEST:
                        //TODO
                        break;
                }
            }

            //TODO harvest anything else important from the tasks before deletion?
            t.delete();
        });
    }


}
