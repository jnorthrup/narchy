package nars.index.term.map;

import com.github.benmanes.caffeine.cache.*;
import nars.Param;
import nars.concept.PermanentConcept;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;


public class CaffeineIndex extends MaplikeTermIndex implements RemovalListener<Term,Termed>, Executor {

    /** holds compounds and subterm vectors */
    @NotNull public final Cache<Term, Termed> concepts;

    final static Weigher<? super Term, ? super Termed> w = (k,v) -> {
        if (v instanceof PermanentConcept) return 0;
        else return
                //(v.complexity() + v.volume())/2;
                //v.complexity();
                v.volume();
    };

    /** use the soft/weak option with CAUTION you may experience unexpected data loss and other weird symptoms */
    public CaffeineIndex(long capacity) {
        super();


        Caffeine<Term, Termed> builder = Caffeine.newBuilder().removalListener(this);
        if (capacity > 0) {
            //builder.maximumSize(capacity); //may not protect PermanentConcept's from eviction

            builder.maximumWeight(capacity*10);
            builder.weigher(w);

        } else
            builder.softValues();

//        if (Param.DEBUG)
//            builder.recordStats();

        builder.executor(this);

        this.concepts = builder.build();

    }

    @Override
    public Stream<Termed> stream() {
        return concepts.asMap().values().stream();
    }

    //    @Override
//    public void start(NAR nar) {
//        super.start(nar);
//        //nar.onCycle(this::cleanUp);
//    }

//private static final long cleanPeriod = 16 /* cycles */;
//    protected void cleanUp() {
//        if (nar.time() % cleanPeriod == 0) {
//            concepts.cleanUp();
//            if (subterms != null)
//                subterms.cleanUp();
//        }
//    }

//    @NotNull
//    @Override
//    public final TermContainer intern(@NotNull Term[] a) {
//
//        TermContainer v = super.intern(a);
//
//        if (subterms!=null) {
//            int len = a.length;
//            if (len < 1)
//                return v; //dont intern small or empty containers
//
//            //        //HACK
//            //        if (x instanceof EllipsisTransform || y instanceof EllipsisTransform)
//            //            return new TermVector2(x, y);
//
//            //        DynByteSeq d = new DynByteSeq(4 * len /* estimate */);
//            //        try {
//            //            IO.writeTermContainer(d, a);
//            //        } catch (IOException e) {
//            //            throw new RuntimeException(e);
//            //        }
//
//            return subterms.get(v, vv -> vv);
//        } else {
//            return v;
//        }
//
//
//        //return subterms!=null ? subterms.get(s, (ss) -> ss) :s;
//    }

    @Override
    public void remove(Term x) {
        concepts.invalidate(x);
    }


    @Override
    public void set(@NotNull Term src, @NotNull Termed target) {
        concepts.asMap().merge(src, target, setOrReplaceNonPermanent);
    }


    @Override
    public void clear() {
        concepts.invalidateAll();
    }

    @Override
    public void forEach( Consumer<? super Termed> c) {
        concepts.asMap().values().forEach(c::accept);
    }

    @Override
    public int size() {
        return (int) concepts.estimatedSize();
    }


    @Override
    public Termed get(Term x, boolean createIfMissing) {
       if (!x.op().conceptualizable)
            return x;

        if (createIfMissing) {
            //return concepts.get(x, conceptBuilder);
            return concepts.asMap().compute(x, conceptBuilder);
        } else {
            return concepts.getIfPresent(x);
        }
    }

//    @Override
//    public void commit(Concept c) {
//        concepts.getIfPresent(c.term());
//    }

    //    protected Termed theCompoundCreated(@NotNull Compound x) {
//
//        if (x.hasTemporal()) {
//            return internCompoundSubterms(x.subterms(), x.op(), x.relation(), x.dt());
//        }
//
//        Termed yyy = data.get(x, xx -> {
//            Compound y = (Compound)xx;
//            Termed yy = internCompoundSubterms(y.subterms(), y.op(), y.relation(), y.dt());
//            return internCompound(yy);
//        });
//        return yyy;
//
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

    /** this will be called from within a worker task */
    @Override public final void onRemoval(Term key, Termed value,RemovalCause cause) {

        if (value!=null)
            onRemove(value);
        else {
            System.err.println(key + " removed by " + this);
        }
    }


    final AtomicBoolean cleanupPending = new AtomicBoolean(false);

    @Override
    public final void execute(Runnable command) {
        if (nar!=null) {
            if (command.getClass().getSimpleName().equals("PerformCleanupTask")) {
                    //ignore while hopefully coalescing stateless repeat tasks,
                    // there are too many it spams the worker pool
                if (cleanupPending.compareAndSet(false, true)) {
                    nar.exe.execute(()->{
                       concepts.cleanUp();
                       cleanupPending.set(false);
                    });
                } else {
                    return;
                }
            } else {
                //possibly a removal notification (its class will be an anonymous lambda :( ), execute inline immediately
                command.run();
                //nar.exe.execute(command);
            }

        } else
            command.run();
    }
}
