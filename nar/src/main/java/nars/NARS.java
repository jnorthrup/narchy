package nars;

import jcog.Log;
import jcog.data.list.FasterList;
import nars.attention.TaskLinkWhat;
import nars.attention.What;
import nars.concept.Concept;
import nars.concept.util.ConceptAllocator;
import nars.concept.util.ConceptBuilder;
import nars.concept.util.DefaultConceptBuilder;
import nars.derive.BasicDeriver;
import nars.derive.Derivers;
import nars.exe.Exec;
import nars.exe.impl.UniExec;
import nars.memory.Memory;
import nars.memory.SimpleMemory;
import nars.op.stm.STMLinkage;
import nars.task.util.PriBuffer;
import nars.term.Term;
import nars.time.Time;
import nars.time.clock.CycleTime;
import nars.time.clock.RealTime;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import static jcog.Util.curve;

/**
 * NAR builder
 */
public class NARS {

    public final NAR get(Consumer<NAR> init) {
        NAR n = get();
        init.accept(n);
        return n;
    }

    public final NAR get() {
        NAR n = new NAR(
            index.get(),
            exec.get(),
                what,
            time,
            rng,
            conceptBuilder.get()
        );
        step.forEachWith(Consumer::accept, n);
        return n;
    }

    protected Supplier<Memory> index;

    protected Time time;

    protected Supplier<Exec> exec;

    protected Function<Term,What> what;

    protected Supplier<Random> rng;

    protected Supplier<ConceptBuilder> conceptBuilder;


    /**
     * applied in sequence as final step before returning the NAR
     */
    protected final FasterList<Consumer<NAR>> step = new FasterList<>(8);


    public NARS index( Memory concepts) {
        this.index = () -> concepts;
        return this;
    }

    public NARS time( Time time) {
        this.time = time;
        return this;
    }

    public NARS exe( Exec exe) {
        this.exec = () -> exe;
        return this;
    }

    public NARS concepts(ConceptBuilder cb) {
        this.conceptBuilder = () -> cb;
        return this;
    }

//    public NARS attention(Supplier<ActiveConcepts> sa) {
//        this.attn = sa;
//        return this;
//    }
    /**
     * adds a deriver with the standard rules for the given range (inclusive) of NAL levels
     */
    @Deprecated public NARS withNAL(int minLevel, int maxLevel) {
        return then((n)->
                new BasicDeriver(Derivers.nal(n, minLevel, maxLevel))
            );
    }

    public NARS what(Function<Term,What> what) {
        this.what = what;
        return this;
    }

    /**
     * generic defaults
     */
    @Deprecated
    public static class DefaultNAR extends NARS {


        public DefaultNAR(int nal, boolean threadSafe) {
            this(0, nal, threadSafe);
        }
        public DefaultNAR(int nalMin, int nalMax, boolean threadSafe) {

            assert(nalMin <= nalMax);

            if (threadSafe)
                index =
                        //() -> new CaffeineIndex(64 * 1024)
                        () -> new SimpleMemory(4 * 1024, true)
            ;


            if (nalMax > 0)
                withNAL(nalMin, nalMax);

            if (nalMax >= 7) {
                then((nn)->new STMLinkage(nn, 1));
            }

            then((n)->{

                n.confMin.set(0.01f);

                n.termVolMax.set(22);

                ((TaskLinkWhat) n.what()).links.linksMax.set(64);
                ((TaskLinkWhat) n.what()).links.decay.set(0.05f);


                n.beliefPriDefault.amp(0.1f);
                n.goalPriDefault.amp(0.1f);
                n.questionPriDefault.amp(0.1f);
                n.questPriDefault.amp(0.1f);

            });
        }

    }

    /**
     * defaults
     */
    public NARS() {

        index = () ->
                new SimpleMemory(8 * 1024)
                //new TemporaryConceptIndex()
        ;

        time = new CycleTime();

        exec = UniExec::new;

        what = w -> new TaskLinkWhat(w, 64,
                       new PriBuffer.DirectPriBuffer()
                       //new PriBuffer.BagTaskBuffer(128, 8f)
                       //new PriBuffer.MapTaskBuffer()
        );

        rng = ThreadLocalRandom::current;

        ToIntFunction<Concept> termVolume = c->c.term().volume();


        /** shared eternal belief and goal capacity curve */
        ToIntFunction<Concept> bgEternal = curve(termVolume, new int[] {
                1, 8,
                16, 4
        });

        /** shared temporal belief and goal capacity curve */
        ToIntFunction<Concept> bgTemporal = curve(termVolume, new int[] {
                1, 256,
                8, 64,
                16, 32,
                32, 8
        });

        /** shared question and quest capacity curve */
        ToIntFunction<Concept> q = curve(termVolume, new int[] {
                1, 7,
                12, 5,
                24, 3
        });

        conceptBuilder = ()->new DefaultConceptBuilder(

            new ConceptAllocator(
                    //beliefs ete
                    bgEternal,
                    //beliefs tmp
                    bgTemporal,
                    //goals ete
                    bgEternal,
                    //goals tmp
                    bgTemporal,
                    //questions
                    q,
                    //quests
                    q
            )
        );


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
     * @param nal adjustable NAL level. level >= 7 include STM (short-target-memory) Linkage plugin
     */
    public static NAR tmp(int nal) {
        return tmp(0, nal);
    }

    public static NAR tmp(int nalStart, int nalEnd) {
        return new NARS.DefaultNAR(nalStart, nalEnd, false).get();
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

        d.rng = ThreadLocalRandom::current;

        return d.get();
    }


    /** milliseconds realtime */
    public static NARS realtime(float durFPS) {
        return new DefaultNAR(0, true).time(new RealTime.MS(true).durFPS(durFPS));
    }

    /**
     * provides only low level functionality.
     * an empty deriver, but allows any kind of target
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
                logger.error("input: {} {}", s, e);
            }

            Runnable save = () -> {
                try {
                    n.outputBinary(f, false);
                } catch (IOException e) {
                    logger.error("output: {} {}", s, e);
                }
            };
            Runtime.getRuntime().addShutdownHook(new Thread(save));
        });
    }

    /**
     * adds a post-processing step before ready NAR is returned
     */
    public NARS then(Consumer<NAR> n) {
        step.add(n);
        return this;
    }


    static final Logger logger = Log.logger(NARS.class);

}
