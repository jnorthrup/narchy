package nars.index.concept;

import com.github.benmanes.caffeine.cache.*;
import nars.Param;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;


public class CaffeineIndex extends MaplikeConceptIndex implements CacheLoader<Term, Termed>, RemovalListener<Term, Termed>, Executor {

    private final Cache<Term, Termed> concepts;
    private final boolean weightDynamic;

    public static CaffeineIndex soft() {
        return new CaffeineIndex(Caffeine.newBuilder().softValues(), false);
    }

    public static CaffeineIndex weak() {
        return new CaffeineIndex(Caffeine.newBuilder().weakValues(), false);
    }

    public CaffeineIndex(long capacity) {
        this(capacity, Termed::volume);
    }

    public CaffeineIndex(long capacity, ToIntFunction<Concept> w) {
        this(capacity, false, w);
    }
    public CaffeineIndex(long capacity, boolean weightDynamic, ToIntFunction<Concept> w) {
        this(Caffeine.newBuilder().maximumWeight(capacity).weigher((k,v)->{
            if (v instanceof PermanentConcept) return 0;
            return w.applyAsInt((Concept)v);
        }), weightDynamic);

    }

    private CaffeineIndex(Caffeine builder, boolean weightDynamic) {
        super();
        this.weightDynamic = weightDynamic;


        builder.removalListener(this);
        builder.executor(this);

        this.concepts = builder.build(this);

        
        
    }


    @Override
    public Stream<Termed> stream() {
        return concepts.asMap().values().stream().filter(Objects::nonNull)
                ;
    }


    /** caffeine may measure accesses for eviction */
    @Override protected final boolean elideConceptGets() {
        return false;
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
        Termed y;
        if (createIfMissing)
            y = concepts.get(x, nar.conceptBuilder::apply);
        else {
            y = concepts.getIfPresent(x);
        }

        if (createIfMissing && weightDynamic && y!=null)
            concepts.put(x, y); 

        return y;
    }









    @Override
    public @NotNull String summary() {
        
        String s = concepts.estimatedSize() + " concepts, ";

        if (Param.DEBUG)
            s += ' ' + concepts.stats().toString();

        return s;
        
        

    }

    /**
     * this will be called from within a worker task
     */
    @Override
    public final void onRemoval(Term key, Termed value, RemovalCause cause) {
        
        if (value != null)
            onRemove(value);
    }


    @Override
    public final void execute(Runnable command) {
        if (nar == null) {
            command.run();
            return;
        }

        
        nar.exe.execute(command);
    }


    @Override
    public Termed load(Term key) {
        return nar.conceptBuilder.apply(key, null);
    }

    @Override
    public Termed reload(Term key, Termed oldValue) {
        return nar.conceptBuilder.apply(key, oldValue);
    }
}
