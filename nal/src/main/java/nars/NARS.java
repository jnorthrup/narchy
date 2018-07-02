package nars;

import jcog.TODO;
import jcog.data.map.MRUCache;
import jcog.list.FasterList;
import jcog.math.random.XoRoShiRo128PlusRandom;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.concept.util.ConceptBuilder;
import nars.concept.util.DefaultConceptBuilder;
import nars.derive.Deriver;
import nars.derive.Derivers;
import nars.derive.deriver.MatrixDeriver;
import nars.exe.Attention;
import nars.exe.Exec;
import nars.exe.UniExec;
import nars.index.concept.CaffeineIndex;
import nars.index.concept.ConceptIndex;
import nars.index.concept.MapConceptIndex;
import nars.op.stm.STMLinkage;
import nars.term.Term;
import nars.term.Termed;
import nars.time.Time;
import nars.time.clock.CycleTime;
import nars.time.clock.RealTime;
import nars.util.TimeAware;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * NAR builder
 */
public class NARS {

    public NAR get() {
        NAR n = new NAR(index.get(), exe.get(), attn.get(), time, rng.get(), concepts.get());
        init(n);
        derivers.forEach(d -> d.apply(n));
//        n.synch();
        postInit.forEach(x -> x.accept(n));
        n.synch();
        return n;
    }

    /**
     * subclasses may override this to configure newly constructed NAR's
     */
    protected void init(NAR n) {

    }

    protected Supplier<ConceptIndex> index;

    protected Time time;

    protected Supplier<Exec> exe;

    protected Supplier<Random> rng;

    protected Supplier<Attention> attn;

    protected Supplier<ConceptBuilder> concepts;

    protected List<Function<TimeAware, Deriver>> derivers;

    /**
     * applied in sequence as final step before returning the NAR
     */
    protected final List<Consumer<NAR>> postInit = new FasterList(0);


    public NARS index(@NotNull ConceptIndex concepts) {
        this.index = () -> concepts;
        return this;
    }

    public NARS time(@NotNull Time time) {
        this.time = time;
        return this;
    }

    public NARS exe(Exec exe) {
        this.exe = () -> exe;
        return this;
    }

    public NARS concepts(ConceptBuilder cb) {
        this.concepts = () -> cb;
        return this;
    }

    public NARS attention(Supplier<Attention> sa) {
        this.attn = sa;
        return this;
    }
    /**
     * adds a deriver with the standard rules for the given range (inclusive) of NAL levels
     */
    @Deprecated public NARS withNAL(int minLevel, int maxLevel) {
        postInit.add((n)->
                new MatrixDeriver(Derivers.nal(n, minLevel, maxLevel))
        );
        return this;
    }

    /**
     * defaults
     */
    public NARS() {

        index = () ->
                
                new MapConceptIndex(
                        
                        new MRUCache<>(16*1024) {
                            @Override
                            protected void onEvict(Map.Entry<Term, Termed> entry) {
                                Termed c = entry.getValue();
                                if (c instanceof PermanentConcept) {
                                    throw new TODO("Should not evict " + c); 
                                } else {
                                    ((Concept)c).delete(null /* HACK */);
                                }
                            }
                        }
                );

        time = new CycleTime();

        exe = () -> new UniExec();


        rng = () ->
                new XoRoShiRo128PlusRandom(1);

        concepts = DefaultConceptBuilder::new;

        derivers = new FasterList<>();

        attention(()->new Attention(256));
    }

    /**
     * temporary, disposable NAR. safe for single-thread access only.
     * full NAL8 with STM Linkage
     */
    public static NAR tmp() {
        return tmp(8);
    }


    /**
     * temporary, disposable NAR. useful for unit tests or embedded components
     * safe for single-thread access only.
     *
     * @param nal adjustable NAL level. level >= 7 include STM (short-term-memory) Linkage plugin
     */
    public static NAR tmp(int nal) {
        return new DefaultNAR(nal, false).get();
    }

    /**
     * single-thread, limited to NAL6 so it should be more compact than .tmp()
     */
    public static NAR tmpEternal() {
        return new DefaultNAR(6, false).get();
    }

    /**
     * single thread but for multithread usage:
     * unbounded soft reference index
     */
    public static NAR threadSafe() {
        return threadSafe(8);
    }
    
    public static NAR threadSafe(int level) {
        NARS d = new DefaultNAR(level, true)
                .time(new RealTime.CS().durFPS(25f));

        d.rng = ()->new XoRoShiRo128PlusRandom(System.nanoTime());

         return d.get();
    }


    /** milliseconds realtime */
    public static NARS realtime(float durFPS) {
        return new DefaultNAR(0, true).time(new RealTime.MS().durFPS(durFPS));
    }

    /**
     * provides only low level functionality.
     * an empty deriver, but allows any kind of term
     */
    public static NAR shell() {
        return tmp(0);
    }

    public NARS memory(String s) {
        return then(n -> {
            File f = new File(s);

            try {
                n.inputBinary(f);
            } catch (FileNotFoundException ignored) {
                
            } catch (IOException e) {
                n.logger.error("input: {} {}", s, e);
            }

            Runnable save = () -> {
                try {
                    n.outputBinary(f, false);
                } catch (IOException e) {
                    n.logger.error("output: {} {}", s, e);
                }
            };
            Runtime.getRuntime().addShutdownHook(new Thread(save));
        });
    }

    /**
     * adds a post-processing step before ready NAR is returned
     */
    public NARS then(Consumer<NAR> n) {
        postInit.add(n);
        return this;
    }


    /**
     * generic defaults
     */
    @Deprecated
    public static class DefaultNAR extends NARS {


        public DefaultNAR(int nal, boolean threadSafe) {

            if (nal > 0)
                withNAL(1, nal);

            if (threadSafe)
                index = () -> new CaffeineIndex(64 * 1024);

            if (nal >= 7) {
                then((nn)->new STMLinkage(nn, 1, false));
            }
        }

        @Override
        protected void init(NAR nar) {

            nar.termlinkBalance.set(0.5f);
            nar.termVolumeMax.set(26);


//            nar.activateConceptRate.set(0.4f);

            nar.forgetRate.set(0.5f);

            nar.beliefPriDefault.set(0.5f);
            nar.goalPriDefault.set(0.5f);
            nar.questionPriDefault.set(0.5f);
            nar.questPriDefault.set(0.5f);


        }
    }

}
