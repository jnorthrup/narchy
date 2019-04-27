package nars;

import jcog.Log;
import jcog.data.list.FasterList;
import nars.attention.What;
import nars.concept.Concept;
import nars.concept.util.ConceptAllocator;
import nars.concept.util.ConceptBuilder;
import nars.concept.util.DefaultConceptBuilder;
import nars.derive.Derivers;
import nars.derive.impl.BatchDeriver;
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
    protected final FasterList<Consumer<NAL<NAL<NAR>>>> step = new FasterList<>(8);


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
                new BatchDeriver(Derivers.nal(n, minLevel, maxLevel))
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

            if (threadSafe)
                index =
                        //() -> new CaffeineIndex(64 * 1024)
                        () -> new SimpleMemory(64 * 1024, true)
            ;

            if (nal > 0)
                withNAL(1, nal);

            if (nal >= 7) {
                then((nn)->new STMLinkage(nn, 1));
            }

            then((n)->{

                n.termVolumeMax.set(22);

                ((What.TaskLinkWhat) n.what()).links.decay.set(0.05f);

                n.beliefPriDefault.pri(0.1f);
                n.goalPriDefault.pri(0.1f);
                n.questionPriDefault.set(0.01f);
                n.questPriDefault.set(0.01f);

            });
        }

    }

    /**
     * defaults
     */
    public NARS() {

        index = () ->
                new SimpleMemory(16 * 1024)
                //new TemporaryConceptIndex()
        ;

        time = new CycleTime();

        exec = UniExec::new;

        what = w -> new What.TaskLinkWhat(w, 64,
                       new PriBuffer.DirectPriBuffer()
                       //new PriBuffer.BagTaskBuffer(128, 8f)
                       //new PriBuffer.MapTaskBuffer()
        );

        rng = ThreadLocalRandom::current;

        ToIntFunction<Concept> termVolume = c->c.term().volume();

        conceptBuilder = ()->new DefaultConceptBuilder(
                new ConceptAllocator(
                        //beliefs ete
                        curve(termVolume,
                        1, 9,
                                16, 5,
                                18, 3
                        ),
                        //beliefs tmp
                        curve(termVolume,
                                1, 64,
                                16, 32,
                                32, 16
                        ),
                        //goals ete
                        curve(termVolume,
                                1, 9,
                                16, 5,
                                32, 3
                        ),
                        //goals tmp
                        curve(termVolume,
                                1, 64,
                                16, 32,
                                32, 16
                        ),
                        //questions
                        curve(termVolume,
                          1, 8,
                                12, 6,
                                24, 4
                        ),
                        //quests
                        curve(termVolume,
                                1, 8,
                                12, 6,
                                24, 4
                        )
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
        return new DefaultNAR(nal, false).get();
    }

    public static NAR tmp(int nalStart, int nalEnd) {
        return new NARS.DefaultNAR(0, false).withNAL(nalStart,nalEnd).get();
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
    public NARS then(Consumer<NAL<NAL<NAR>>> n) {
        step.add(n);
        return this;
    }


    static final Logger logger = Log.logger(NARS.class);

}
