package nars.index.term.map;

import com.github.benmanes.caffeine.cache.*;
import nars.Param;
import nars.concept.PermanentConcept;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;


public class CaffeineIndex extends MaplikeTermIndex implements CacheLoader<Term, Termed>, RemovalListener<Term, Termed>, Executor {

    /**
     * holds compounds and subterm vectors
     */
    @NotNull
    public final Cache<Term, Termed> concepts;

    final static Weigher<? super Term, ? super Termed> w = (k, v) -> {
        if (v instanceof PermanentConcept) return 0;
        else return
                //(v.complexity() + v.volume())/2;
                //v.complexity();
                v.volume();
    };
    //private final AsyncLoadingCache<Term, Termed> conceptsAsync;

    /**
     * use the soft/weak option with CAUTION you may experience unexpected data loss and other weird symptoms
     */
    public CaffeineIndex(long capacity) {
        super();


        Caffeine<Term, Termed> builder = Caffeine.newBuilder().removalListener(this);
        if (capacity > 0) {
            //builder.maximumSize(capacity); //may not protect PermanentConcept's from eviction

            builder.maximumWeight(capacity * 10);
            builder.weigher(w);

        } else
            builder.softValues();

//        if (Param.DEBUG)
//            builder.recordStats();

        builder.executor(this);

        //this.conceptsAsync = builder.buildAsync(this);
        this.concepts = builder.build(this);
        //this.concepts = conceptsAsync.synchronous();
    }


    @Override
    public Stream<Termed> stream() {
        return concepts.asMap().values().stream();
    }


    @Override
    public void remove(Term x) {
        concepts.invalidate(x);
    }


    @Override
    public void set(Term src, Termed target) {
        concepts.asMap().merge(src, target, setOrReplaceNonPermanent);
    }


    @Override
    public void clear() {
        concepts.invalidateAll();
    }

    @Override
    public void forEach(Consumer<? super Termed> c) {
        concepts.asMap().values().forEach(c::accept);
    }

    @Override
    public int size() {
        return (int) concepts.estimatedSize();
    }


    @Override
    public Termed get(Term x, boolean createIfMissing) {
        if (createIfMissing)
            return concepts.get(x, conceptBuilder::apply);
        else
            return concepts.getIfPresent(x);
    }

//    @Override
//    public CompletableFuture<Termed> getAsync(Term x, boolean createIfMissing) {
//        if (createIfMissing)
//            return conceptsAsync.get(x, (Function<? super Term, ? extends Termed>) conceptBuilder::apply);
//        else
//            return conceptsAsync.getIfPresent(x);
//    }

    @Override
    public @NotNull String summary() {
        //CacheStats s = cache.stats();
        String s = concepts.estimatedSize() + " concepts, ";

        if (Param.DEBUG)
            s += ' ' + concepts.stats().toString();

        return s;
        //(" + n2(s.hitRate()) + " hitrate, " +
        //s.requestCount() + " reqs)";

    }

    /**
     * this will be called from within a worker task
     */
    @Override
    public final void onRemoval(Term key, Termed value, RemovalCause cause) {
        //value will be null if collected (Weak/Soft modes)
        if (value != null)
            onRemove(value);
    }


    @Override
    public final void execute(Runnable command) {
        if (nar == null) {
            command.run();
            return;
        }

        //possibly a removal notification (its class will be an anonymous lambda :( ), execute inline immediately
        nar.exe.execute(command);
    }


    @Override
    public Termed load(Term key) {
        return conceptBuilder.apply(key, null);
    }

    @Override
    public Termed reload(Term key, Termed oldValue) {
        return conceptBuilder.apply(key, oldValue);
    }
}
