package nars;

import jcog.TODO;
import jcog.data.list.FasterList;
import jcog.data.map.MRUCache;
import jcog.random.XoRoShiRo128PlusRandom;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.concept.util.ConceptAllocator;
import nars.concept.util.ConceptBuilder;
import nars.concept.util.DefaultConceptBuilder;
import nars.derive.Derivers;
import nars.derive.impl.MatrixDeriver;
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
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static jcog.Util.curve;

/**
 * NAR builder
 */
public class NARS {

    public final NAR get() {
        NAR n = new NAR(index.get(), exec.get(), attn.get(), time, rng.get(), conceptBuilder.get());
        step.forEach(x -> x.accept(n));
        n.synch();
        return n;
    }

    protected Supplier<ConceptIndex> index;

    protected Time time;

    protected Supplier<Exec> exec;

    protected Supplier<Random> rng;

    protected Supplier<Attention> attn;

    protected Supplier<ConceptBuilder> conceptBuilder;


    /**
     * applied in sequence as final step before returning the NAR
     */
    protected final List<Consumer<NAR>> step = new FasterList<>(8);


    public NARS index(@NotNull ConceptIndex concepts) {
        this.index = () -> concepts;
        return this;
    }

    public NARS time(@NotNull Time time) {
        this.time = time;
        return this;
    }

    public NARS exe(@NotNull Exec exe) {
        this.exec = () -> exe;
        return this;
    }

    public NARS concepts(ConceptBuilder cb) {
        this.conceptBuilder = () -> cb;
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
        return then((n)->
                new MatrixDeriver(Derivers.nal(n, minLevel, maxLevel))
        );
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
                then((nn)->new STMLinkage(nn, 1));
            }

            then((n)->{

                n.freqResolution.set(0.01f);
                n.confResolution.set(0.01f);

                n.termlinkBalance.set(0.5f);
                n.termVolumeMax.set(26);

                n.activateConceptRate.set(0.1f);

                //nar.forgetRate.set(0.5f);

                float basePri = 1f; /* warning: changing this for now will affect many tests that have hardcoded priority values.  TODO fix that */
                n.beliefPriDefault.set(basePri * 0.5f);
                n.goalPriDefault.set(basePri * 0.5f);
                n.questionPriDefault.set(basePri * 0.1f);
                n.questPriDefault.set(basePri * 0.1f);

//                n.emotion.want(MetaGoal.Perceive, -0.01f);
//                n.emotion.want(MetaGoal.Believe, 0.1f);
//                n.emotion.want(MetaGoal.Desire, 0.1f);

            });
        }

    }

    /**
     * defaults
     */
    public NARS() {

        index = () ->

//                new CaffeineIndex(16*1024);
                
                new MapConceptIndex(

                        new MRUCache<>(8*1024) {
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

        exec = () -> new UniExec();

        rng = () ->
                new XoRoShiRo128PlusRandom(1);

        attention(()->new Attention(128));

        conceptBuilder = ()->new DefaultConceptBuilder(
                new ConceptAllocator(
                        //beliefs ete
                        curve(Concept::volume,
                        1, 8,
                                16, 6,
                                32, 3
                        ),
                        //beliefs tmp
                        curve(Concept::volume,
                                1, 72,
                                8,48,
                                16, 16
                        ),
                        //goals ete
                        curve(Concept::volume,
                                1, 4,
                                16, 3,
                                32, 2
                        ),
                        //goals tmp
                        curve(Concept::volume,
                                1, 72,
                                8,48,
                                16, 16
                        ),
                        //questions
                        curve(Concept::volume,
                          1, 8,
                                12, 6,
                                24, 4
                        ),
                        //quests
                        curve(Concept::volume,
                                1, 8,
                                12, 6,
                                24, 4
                        ),
                        //termlinks
                        curve(Concept::volume,
                                1, 48,
                                8,32,
                                24,24,
                                48,16
                        ),
                        //tasklinks
                        curve(Concept::volume,
                                1, 48,
                                8,32,
                                24,24,
                                48,16
                        ))
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
        step.add(n);
        return this;
    }



}
