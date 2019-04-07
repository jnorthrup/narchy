package nars;

import jcog.data.list.FasterList;
import nars.attention.TaskLinks;
import nars.concept.Concept;
import nars.concept.util.ConceptAllocator;
import nars.concept.util.ConceptBuilder;
import nars.concept.util.DefaultConceptBuilder;
import nars.derive.Derivers;
import nars.derive.impl.BatchDeriver;
import nars.exe.Exec;
import nars.exe.impl.UniExec;
import nars.index.concept.Memory;
import nars.index.concept.SimpleMemory;
import nars.op.stm.STMLinkage;
import nars.task.util.PriBuffer;
import nars.time.Time;
import nars.time.clock.CycleTime;
import nars.time.clock.RealTime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
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
            new TaskLinks(),
            time,
            in.get(),
            rng,
            conceptBuilder.get()
        );
        step.forEach(x -> x.accept(n));
        n.synch();
        return n;
    }

    protected Supplier<Memory> index;

    protected Time time;

    protected Supplier<Exec> exec;

    protected Supplier<PriBuffer> in;

    protected Supplier<Random> rng;

    protected Supplier<ConceptBuilder> conceptBuilder;


    /**
     * applied in sequence as final step before returning the NAR
     */
    protected final List<Consumer<NAR>> step = new FasterList<>(8);


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

    public NARS input(PriBuffer in) {
        this.in = ()->in;
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

                n.termVolumeMax.set(20);

                n.attn.linksMax.set(128);
                n.attn.decay.set(0.03f);

                n.beliefPriDefault.set(0.1f);
                n.goalPriDefault.set(0.1f);
                n.questionPriDefault.set(0.02f);
                n.questPriDefault.set(0.02f);


            });
        }

    }

    /**
     * defaults
     */
    public NARS() {

        index = () ->
                new SimpleMemory(32 * 1024)
                //new TemporaryConceptIndex()
        ;

        time = new CycleTime();

        exec = () -> new UniExec();

        in =   ()->
            //new TaskBuffer.BagTaskBuffer(256, 5f)
            //new TaskBuffer.MapTaskBuffer(64)
            new PriBuffer.DirectPriBuffer()
        ;

        rng = ThreadLocalRandom::current;

        ToIntFunction<Concept> termVolume = c->c.term().volume();

        conceptBuilder = ()->new DefaultConceptBuilder(
                new ConceptAllocator(
                        //beliefs ete
                        curve(termVolume,
                        1, 10,
                                12, 5,
                                18, 3
                        ),
                        //beliefs tmp
                        curve(termVolume,
                                1, 96,
                                16, 64,
                                32, 16
                        ),
                        //goals ete
                        curve(termVolume,
                                1, 8,
                                16, 5,
                                32, 2
                        ),
                        //goals tmp
                        curve(termVolume,
                                1, 96,
                                16, 64,
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
        step.add(n);
        return this;
    }



}
