package nars.memory;

import com.github.benmanes.caffeine.cache.*;
import nars.NAL;
import nars.NAR;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.term.Term;
import nars.time.part.DurLoop;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;


public class CaffeineMemory extends Memory implements CacheLoader<Term, Concept>, RemovalListener<Term, Concept>, Executor {

    private final Cache<Term, Concept> concepts;
    private final boolean weightDynamic;
    private DurLoop cleanup;

    public static CaffeineMemory soft() {
        return new CaffeineMemory(Caffeine.newBuilder().softValues(), false);
    }

    public static CaffeineMemory weak() {
        return new CaffeineMemory(Caffeine.newBuilder().weakValues(), false);
    }

    public CaffeineMemory(long capacity) {
        this(capacity, false, c->1);
    }

    public CaffeineMemory(long capacity, ToIntFunction<Concept> w) {
        this(capacity, false, w);
    }
    public CaffeineMemory(long capacity, boolean weightDynamic, ToIntFunction<Concept> w) {
        this(Caffeine.newBuilder().maximumWeight(capacity).weigher((k,v)->{
            if (v instanceof PermanentConcept) return 0;
            return w.applyAsInt((Concept)v);
        }), weightDynamic);
    }

    private CaffeineMemory(Caffeine builder, boolean weightDynamic) {
        super();
        this.weightDynamic = weightDynamic;


        builder.removalListener(this);
        builder.executor(this);

        this.concepts = builder.build(this);

        
        
    }


    @Override
    public Stream<Concept> stream() {
        return concepts.asMap().values().stream().filter(Objects::nonNull);
    }


    /** caffeine may measure accesses for eviction */
    @Override
	public final boolean elideConceptGets() {
        return false;
    }

    @Override
    public @Nullable Concept remove(Term x) {
        return concepts.asMap().remove(x);
    }


    @Override
    public void set(Term src, Concept target) {
        concepts.asMap().merge(src, target, setOrReplaceNonPermanent);
    }


    @Override
    public void clear() {
        concepts.invalidateAll();
    }

    @Override
    public void forEach(Consumer<? super Concept> c) {
        concepts.asMap().values().forEach(c);
    }

    @Override
    public int size() {
        return (int) concepts.estimatedSize();
    }


    @Override
    public Concept get(Term x, boolean createIfMissing) {
        Concept y;
        if (createIfMissing)
            y = concepts.get(x, nar.conceptBuilder::apply);
        else
            y = concepts.getIfPresent(x);

        if (createIfMissing && weightDynamic && y!=null)
            concepts.put(x, y); 

        return y;
    }


    @Override
    public void start(NAR nar) {
        super.start(nar);

        cleanup = nar.onDur(concepts::cleanUp);
    }




    @Override
    public String summary() {
        
        String s = concepts.estimatedSize() + " concepts, ";

        if (NAL.DEBUG)
            s += ' ' + concepts.stats().toString();

        return s;
        
        

    }

    /**
     * this will be called from within a worker task
     */
    @Override
    public final void onRemoval(Term key, Concept value, RemovalCause cause) {
        
        if (value != null && cause.wasEvicted())
            onRemove(value);
    }


    @Override
    public final void execute(Runnable command) {
//        if (nar == null) {
            command.run();
//            return;
//        }
//
//
//        nar.exe.execute(command);
//        Exe.invoke(command);
    }


    @Override
    public Concept load(Term key) {
        return nar.conceptBuilder.apply(key);
    }

    @Override
    public Concept reload(Term key, Concept oldValue) {
        return nar.conceptBuilder.apply(key, oldValue);
    }
}
