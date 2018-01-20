package nars.control;

import jcog.Util;
import jcog.memoize.HijackMemoize;
import jcog.memoize.Memoize;
import jcog.pri.Pri;
import jcog.pri.PriReference;
import jcog.pri.Prioritized;
import jcog.sort.TopNUnique;
import jcog.util.FloatFloatToFloatFunction;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.derive.DeriverRoot;
import nars.derive.TrieDeriver;
import nars.derive.rule.PremiseRuleSet;
import nars.exe.Causable;
import nars.index.term.PatternIndex;
import nars.term.Term;

import java.io.PrintStream;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.time.Tense.ETERNAL;

/**
 * an individual deriver process: executes a particular Deriver model
 * specified by a set of premise rules.
 * <p>
 * runtime intensity is metered and throttled by causal feedback
 */
public class Deriver extends Causable {

    private final Consumer<Predicate<Activate>> concepts;
    private final Cause[] subCauses;


    /**
     * how many premises to generate per concept in pre-derivation
     */
    int HypotheticalPremisePerConcept = 5;

    /**
     * how many premises to keep per concept; should be <= Hypothetical count
     */
    int premisesPerConcept = 3;

    int burstMax = 8;

    /**
     * TODO move this to a 'CachingDeriver' subclass
     */
    public final Memoize<ProtoDerivation.PremiseKey, int[]> whats =
            new HijackMemoize<>(ProtoDerivation.PremiseKey::solve,
                    32 * 1024, 3, false);
    private long now;
    private int conceptsPerIteration = 1;


    public static Function<NAR, Deriver> deriver(Function<NAR, PremiseRuleSet> rules) {
        return (nar) ->
                new Deriver(TrieDeriver.the(rules.apply(nar),
                        //Param.TRACE ? DebugDerivationPredicate::new : null
                        null
                ), nar);
    }

    /**
     * loads default deriver rules, specified by a range (inclusive) of levels. this allows creation
     * of multiple deriver layers each targetting a specific range of the NAL spectrum
     */
    public static Function<NAR, Deriver> deriver(int minLevel, int maxLevel, String... extraFiles) {
        assert ((minLevel <= maxLevel && maxLevel > 0) || extraFiles.length > 0);

        return deriver(nar ->
                PremiseRuleSet.rules(nar, new PatternIndex(),
                        Derivers.standard(minLevel, maxLevel, extraFiles)
                ));
    }

    public final DeriverRoot deriver;
    private final NAR nar;

    public Deriver(DeriverRoot deriver, NAR nar) {
        this(nar.exe::fire, deriver, nar);
    }

    static volatile int serial = 0;

    public Deriver(Consumer<Predicate<Activate>> source, DeriverRoot deriver, NAR nar) {
        super(null,
                $.func("deriver", $.the(serial++)) //HACK
        );
        this.deriver = deriver;
        this.concepts = source;
        this.nar = nar;
        this.subCauses = deriver.can.causes;

        nar.on(this);
    }


    @Override
    protected int next(NAR n, final int iterations) {

        Derivation d = derivation.get().cycle(n, deriver);

        int matchTTL = Param.TTL_PREMISE_MIN * 4;
//        int ttlMin = nar1.matchTTLmin.intValue();
        int deriveTTL = n.matchTTLmean.intValue();


        TopNUniquePremises premises = new TopNUniquePremises();

        int ammo = iterations * conceptsPerIteration;
        final int[] fired = {0};
        while (ammo > 0) {

            int burstSize = Math.min(burstMax, ammo);
            ammo -= burstSize;

            int burst[] = new int[]{burstSize};

            int premiseLimit = burstSize * premisesPerConcept;
            premises.clear(premiseLimit, Premise[]::new);

            //fire a burst of concepts to generate hypothetica premises.  collect a fraction of these (ex: 50%), ranked by priority
            concepts.accept(a -> {
                fired[0]++;
                premises.setTTL(HypotheticalPremisePerConcept);
                a.premises(n, d.activator, premises::tryAdd);
                return (--burst[0]) > 0;
            });

            int ps = premises.size();

            int totalTTL = deriveTTL * ps;
            Premise[] pp = premises.list;
            float totalPremiesPri = Math.max(Pri.EPSILON, Util.sum(ps, (p)->pp[p].pri()));

            this.now = nar.time();

            //derive the premises
            for (Premise premise : pp) {

                if (premise == null) break; //end

                if (premise.match(d, this::matchTime, matchTTL) != null) {
                    n.emotion.premiseFire.increment();

                    boolean derivable = proto(d);
                    if (derivable) {
                        //specific ttl as fraction of the total TTL allocated to the burst, proportional to its priority contribution
                        int ttl = Math.round(Util.lerp(
                                premise.priElseZero()/totalPremiesPri, Param.TTL_PREMISE_MIN, totalTTL));

                        derive(d, ttl);
                    }

                    //System.err.println(derivable + " " + premise.taskLink.get() + "\t" + premise.termLink + "\t" + d.can + " ..+" + d.derivations.size());
                } else {
                    n.emotion.premiseFail.increment();
                }
            }

        }

        int derived = d.commit(n::input);

        return fired[0];
    }

    /**
     * 1. CAN (proto) stage
     */
    private boolean proto(Derivation x) {
        int[] trys = x.will = whats.apply(new ProtoDerivation.PremiseKey(x));
        return trys.length > 0;
    }

    /**
     * 2. TRY stage
     */
    private void derive(Derivation x, int ttl) {
        if (x.derive()) {
            x.setTTL(ttl);
            deriver.can.test(x);
        }
    }

    @Override
    public boolean singleton() {
        return false;
    }

    @Override
    public float value() {
        return Util.sum(Cause::value, subCauses);
    }

    protected long matchTime(Task task) {
        assert (now != ETERNAL);

        if (task.isEternal()) {
            return
                    //ETERNAL;
                    now;
        } else {

            //return now;

            //return task.nearestTimeTo(now);

            return task.myNearestTimeTo(now);

//            return nar.random().nextBoolean() ?
//                    now : task.myNearestTimeTo(now);

            //        return nar.random().nextBoolean() ?
            //                task.nearestTimeTo(now) :
            //                now + Math.round((-0.5f + nar.random().nextFloat()) * 2f * (Math.abs(now - task.mid())));
        }

        //return now + dur;

//        if (task.isEternal()) {
//            return ETERNAL;
//        } else //if (task.isInput()) {
//            return task.nearestTimeTo(now);

//        } else {
//            if (task.isBelief()) {
//                return now +
//                        nar.dur() *
//                            nar.random().nextInt(2*Param.PREDICTION_HORIZON)-Param.PREDICTION_HORIZON; //predictive belief
//            } else {
//                return Math.max(now, task.start()); //the corresponding belief for a goal or question task
//            }
//        }

        //now;
        //now + dur;

    }


//    private int premises(Activate a) {
//        return Math.round(Util.lerp(a.priElseZero(), minHypoPremisesPerConceptFire, maxHypoPremisesPerConceptFire));
//    }


//    public static final Function<NAR, PrediTerm<Derivation>> NullDeriver = (n) -> new AbstractPred<Derivation>(Op.Null) {
//        @Override
//        public boolean test(Derivation derivation) {
//            return true;
//        }
//    };


    public static Stream<Deriver> derivers(NAR n) {
        return n.services().filter(Deriver.class::isInstance).map(Deriver.class::cast);
    }

    public static void print(NAR n, PrintStream p) {
        derivers(n).forEach(d -> {
            p.println(d);
            TrieDeriver.print(d.deriver.what, p);
            TrieDeriver.print(d.deriver.can, p);
            p.println();
        });
    }

    //    public final IterableThreadLocal<Derivation> derivation =
//            new IterableThreadLocal<>(() -> new Derivation(this));
    public static final ThreadLocal<Derivation> derivation =
            ThreadLocal.withInitial(Derivation::new);

    static final FloatFloatToFloatFunction merge = Param.taskTermLinksToPremise;

    protected class TopNUniquePremises extends TopNUnique<Premise> {
        private int premisesRemain;



        public TopNUniquePremises() {
            super(Prioritized::pri);
        }

        @Override
        protected void merge(Premise existing, Premise next) {
            existing.priMax(next.pri());
        }

        /**
         * call before generating a concept's premises
         */
        public void setTTL(int hypotheticalPremisePerConcept) {
            this.premisesRemain = hypotheticalPremisePerConcept;
        }

        /**
         * returns whether to continue
         */
        public boolean tryAdd(PriReference<Task> tasklink, PriReference<Term> termlink) {
            float pri = tasklink.pri();
            if (pri == pri) {

                Task t = tasklink.get();
                if (t != null) {

                    float p = merge.apply(pri, termlink.priElseZero()) * nar.amp(t);
                    if (p > minAdmission()) {
                        add( new Premise(t, termlink.get(), p) );
                    } else {
                        //System.out.println("rejected early");
                    }
                }

            }
            return --premisesRemain > 0;
        }
    }
}


//    /**
//     * for now it seems there is a leak so its better if each NAR gets its own copy. adds some overhead but we'll fix this later
//     * not working yet probably due to unsupported ellipsis IO codec. will fix soon
//     */
//    static PremiseRuleSet DEFAULT_RULES_cached() {
//
//
//        return new PremiseRuleSet(
//                Stream.of(
//                        "nal1.nal",
//                        //"nal4.nal",
//                        "nal6.nal",
//                        "misc.nal",
//                        "induction.nal",
//                        "nal2.nal",
//                        "nal3.nal"
//                ).flatMap(x -> {
//                    try {
//                        return rulesParsed(x);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                    return Stream.empty();
//                }), new PatternTermIndex(), true);
//    }


//    PremiseRuleSet DEFAULT_RULES = PremiseRuleSet.rules(true,
//                "nal1.nal",
//                //"nal4.nal",
//                "nal6.nal",
//                "misc.nal",
//                "induction.nal",
//                "nal2.nal",
//                "nal3.nal"
//        );


//    Cache<String, Deriver> derivers = Caffeine.newBuilder().builder();
//    Function<String,Deriver> loader = (s) -> new TrieDeriver(PremiseRuleSet.rules(s));

//    @NotNull
//    static Deriver get(String... path) {
//        PremiseRuleSet rules = PremiseRuleSet.rules(true, path);
//        return TrieDeriver.get(rules);
//    }


//    Logger logger = LoggerFactory.getLogger(Deriver.class);
//
//    BiConsumer<Stream<Compound>, DataOutput> encoder = (x, o) -> {
//        try {
//            IO.writeTerm(x, o);
//            //o.writeUTF(x.getTwo());
//        } catch (IOException e) {
//            throw new RuntimeException(e); //e.printStackTrace();
//        }
//    };
//
//
//    @NotNull
//    static Stream<Pair<PremiseRule, String>> rulesParsed(String ruleSet) throws IOException, URISyntaxException {
//
//        PatternTermIndex p = new PatternTermIndex();
//
//        Function<DataInput, PremiseRule> decoder = (i) -> {
//            try {
//                return //Tuples.pair(
//                        (PremiseRule) readTerm(i, p);
//                //,i.readUTF()
//                //);
//            } catch (IOException e) {
//                throw new RuntimeException(e); //e.printStackTrace();
//                //return null;
//            }
//        };
//
//
//        URL path = NAR.class.getResource("nal/" + ruleSet);
//
//        Stream<PremiseRule> parsed =
//                FileCache.fileCache(path, PremiseRuleSet.class.getSimpleName(),
//                        () -> load(ruleSet),
//                        encoder,
//                        decoder,
//                        logger
//                );
//
//        return parsed.map(x -> Tuples.pair(x, "."));
//    }
//
//    static Stream<PremiseRule> load(String ruleFile) {
//        return parsedRules(new PatternTermIndex(), ruleFile).map(Pair::getOne /* HACK */);
//    }
