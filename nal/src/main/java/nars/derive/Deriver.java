package nars.derive;

import jcog.Util;
import jcog.math.Range;
import jcog.memoize.HijackMemoize;
import jcog.memoize.Memoize;
import jcog.pri.Pri;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.control.Activate;
import nars.control.Cause;
import nars.derive.rule.PremiseRuleSet;
import nars.exe.Causable;
import nars.truth.Truth;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static jcog.Util.unitize;
import static nars.time.Tense.ETERNAL;

/**
 * an individual deriver process: executes a particular Deriver model
 * specified by a set of premise rules.
 * <p>
 * runtime intensity is metered and throttled by causal feedback
 */
public class Deriver extends Causable {

    /** source of concepts supplied to this for this deriver */
    private final Consumer<Predicate<Activate>> concepts;

    /** list of conclusions that in which this deriver can result */
    private final Cause[] subCauses;


    /**
     * how many premises to generate per concept in pre-derivation
     */
    @Range(min=1, max=16)
    public int HypotheticalPremisePerConcept = 4;

    /**
     * controls the rate at which tasklinks 'spread' to interact with termlinks
     */
    @Range(min=1, max=16)
    public int termLinksPerTaskLink = 3;


    /**
     * how many premises to keep per concept; should be <= Hypothetical count
     */
    @Range(min=1, max=16)
    public int premisesPerConcept = 2;

    @Range(min=1, max=1024)
    public int burstMax = 16;


    int conceptsPerIteration = 1;


    /**
     * TODO move this to a 'CachingDeriver' subclass
     */
    final Memoize<ProtoDerivation.PremiseKey, int[]> whats =
            new HijackMemoize<>(ProtoDerivation.PremiseKey::solve,
                    32 * 1024, 4, false);



    transient private long now;


    public static Function<NAR, Deriver> deriver(Function<NAR, PremiseRuleSet> rules) {
        return (nar) ->
                new Deriver(TrieDeriver.the(rules.apply(nar),
                        //Param.TRACE ? DebugDerivationPredicate::new : null
                        null
                ), nar);
    }

    public float derivationPriority(Task t, Derivation d) {

        float discount = 1f;

        //t.volume();

        {
            //relative growth compared to parent complexity
//        int pCompl = d.parentComplexity;
//        float relGrowth =
//                unitize(((float) pCompl) / (pCompl + dCompl));
//        discount *= (relGrowth);
        }

        {
            //absolute size relative to limit
            //float p = 1f / (1f + ((float)t.complexity())/termVolumeMax.floatValue());
        }

        Truth derivedTruth = t.truth();
        {

            float dCompl = t.voluplexity();
            float simplicity = 1 - d.nar.deep.floatValue();
//            if (simplicity > Float.MIN_NORMAL) {

//                //float increase = (dCompl-d.parentComplexityMax);
//                //if (increase > Pri.EPSILON) {
//                int penalty = 1;
//                float change = penalty + Math.abs(dCompl - d.parentComplexityMax); //absolute change: penalize drastic complexification or simplification, relative to parent task(s) complexity
//
//                //relative increase in complexity
//                //calculate the increases proportion to the "headroom" remaining for term expansion
//                //ie. as the complexity progressively grows toward the limit, the discount accelerates
//                float complexityHeadroom = Math.max(1, d.termVolMax - d.parentComplexityMax);
//                float headroomConsumed = Util.unitize(change /* increase */ / complexityHeadroom);
//                float headroomRemain = 1f - headroomConsumed * simplicity;
//
//                //note: applies more severe discount for questions/quest since the truth deduction can not apply
//                discount *= (derivedTruth != null) ? headroomRemain : Util.sqr(headroomRemain);
//            }

            //absolute
            int max = d.termVolMax;
            float headroomRemain = Util.unitize( 1f - (dCompl / max) * simplicity );
            discount *= (derivedTruth != null) ? headroomRemain : Util.sqr(headroomRemain);
        }


        if (/* belief or goal */ derivedTruth != null) {

            //loss of relative confidence: prefer confidence, relative to the premise which formed it
            float parentEvi = d.single ? d.premiseEviSingle : d.premiseEviDouble;
            if (parentEvi > 0) {
                discount *= unitize(derivedTruth.evi() / parentEvi);
            }

            //optional: prefer polarized
            //c *= (1f + p * (0.5f - Math.abs(t.freq()-0.5f)));
        }

        return discount * d.pri;

        //return Util.lerp(1f-t.originality(),discount, 1) * d.premisePri; //more lenient derivation budgeting priority reduction in proportion to lack of originality
    }

    public final DeriverRoot deriver;
    private final NAR nar;

    public Deriver(DeriverRoot deriver, NAR nar) {
        this(nar.exe::fire, deriver, nar);
    }

    private static final AtomicInteger serial = new AtomicInteger();

    public Deriver(Consumer<Predicate<Activate>> source, DeriverRoot deriver, NAR nar) {
        super(
                $.func("deriver", $.the(serial.getAndIncrement())) //HACK
        );
        this.deriver = deriver;
        this.concepts = source;
        this.nar = nar;
        this.subCauses = deriver.can.causes;

        nar.on(this);
    }


    @Override
    protected int next(NAR n, final int iterations) {

        Derivation d = derivation.get().cycle(n, this, deriver);

        int matchTTL = Param.TTL_MIN() * 2;
        int deriveTTL = n.matchTTLmean.intValue();


        Derivation.TopNUniquePremises premises = d.premises;

        int ammo = iterations * conceptsPerIteration;
        final int[] fired = {0};
        while (ammo > 0) {

            int burstSize = Math.min(burstMax, ammo);
            ammo -= burstSize;

            int burst[] = new int[]{burstSize};

            int premiseLimit = burstSize * premisesPerConcept;
            premises.clear(premiseLimit, Premise[]::new);

            //fire a burst of concepts to generate hypothetical premises.  collect a fraction of these (ex: 50%), ranked by priority
            concepts.accept(a -> {
                fired[0]++;
                premises.setTTL(HypotheticalPremisePerConcept);
                a.premises(n, d.activator, premises::tryAdd, termLinksPerTaskLink);
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
                                premise.priElseZero()/totalPremiesPri, Param.TTL_MIN(), totalTTL));

                        d.derive(ttl);
                    } else {
                        n.emotion.premiseUnderivable.increment();
                    }

                    //System.err.println(derivable + " " + premise.taskLink.get() + "\t" + premise.termLink + "\t" + d.can + " ..+" + d.derivations.size());
                } else {
                    n.emotion.premiseFailMatch.increment();
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
        return (x.will = whats.apply(new ProtoDerivation.PremiseKey(x))).length > 0;
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


    static Stream<Deriver> derivers(NAR n) {
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

    public static final ThreadLocal<Derivation> derivation = ThreadLocal.withInitial(Derivation::new);



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