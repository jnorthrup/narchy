package nars.derive;

import jcog.Util;
import jcog.data.ArrayHashSet;
import jcog.math.IntRange;
import jcog.math.Range;
import jcog.pri.PriReference;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.control.Activate;
import nars.control.Cause;
import nars.derive.rule.DeriveRuleSet;
import nars.exe.Causable;
import nars.term.Term;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * an individual deriver process: executes a particular Deriver model
 * specified by a set of premise rules.
 * <p>
 * runtime intensity is metered and throttled by causal feedback
 */
public class Deriver extends Causable {

    private static final AtomicInteger serial = new AtomicInteger();
    public static final ThreadLocal<Derivation> derivation = ThreadLocal.withInitial(Derivation::new);

    public final IntRange conceptsPerIteration = new IntRange(2, 1, 512);

    public final DeriveRules rules;

    /**
     * source of concepts supplied to this for this deriver
     */
    private final Consumer<Predicate<Activate>> source;

    private final Consumer<Collection<Task>> target;

    protected PrioritizeDerivation prioritize = new PrioritizeDerivation.DefaultPrioritizeDerivation();

    /**
     * how many premises to keep per concept; should be <= Hypothetical count
     */
    @Range(min = 1, max = 8)
    public int premisesPerConcept = 2;
    /**
     * controls the rate at which tasklinks 'spread' to interact with termlinks
     */
    @Range(min = 1, max = 8)
    public int termLinksPerTaskLink = 2;

    @Range(min = 1, max = 1024)
    public int burstMax = 64;

    public Deriver(NAR nar, String... rules) {
        this(new DeriveRuleSet(nar, rules), nar);
    }

    public Deriver(DeriveRuleSet rules, NAR nar) {
        this(rules.compile(), nar);
        if (rules.isEmpty())
            throw new RuntimeException("rules missing");
    }

    public Deriver(DeriveRules rules, NAR nar) {
        this(nar.exe::fire, nar::input, rules, nar);
    }

    public Deriver(Consumer<Predicate<Activate>> source, Consumer<Collection<Task>> target, DeriveRules rules, NAR nar) {
        super(
                $.func("deriver", $.the(serial.getAndIncrement())) //HACK
        );
        this.rules = rules;
        this.source = source;
        this.target = target;

        nar.on(this);
    }


    public static Stream<Deriver> derivers(NAR n) {
        return n.services().filter(Deriver.class::isInstance).map(Deriver.class::cast);
    }

    /** sloppy pre-sort of premises by task/task_term,
     *  to maximize sequential repeat of derived task term */
    static final Comparator<? super Premise> sortByTask =
            Comparator
                .comparingInt((Premise a) -> a.task.hashCode())
                .thenComparing((Premise a) -> a.task.term().hashCode());

    @Override
    protected int next(NAR n, final int iterations) {


        int matchTTL = Param.TTL_MIN() * 2;
        int deriveTTL = n.matchTTLmean.intValue();


        int fired = 0;

        int iterMult = premisesPerConcept * conceptsPerIteration.intValue();
        int totalPremisesRemain = iterations * iterMult;


        /** temporary buffer for storing unique premises */
        ArrayHashSet<Premise> premiseBurst =
                //new LinkedHashSet();
                new ArrayHashSet<>(Math.round(burstMax*1.5f));

        Derivation d = derivation.get().cycle(n, this);

        while (totalPremisesRemain > 0) {

            int burstSize = Math.min(burstMax, totalPremisesRemain);
            totalPremisesRemain -= burstSize;

            assert(premiseBurst.isEmpty());

            //SELECT
            selectPremises(n, burstSize, (t, termlink) -> {

                Premise premise = new Premise(t, termlink);
                if (!premiseBurst.add(premise))
                    n.emotion.premiseBurstDuplicate.increment();

                return true;
            });


            int s = premiseBurst.size();
            if (s == 0)
                break;

            if (s > 2)
                Collections.sort(premiseBurst.list, sortByTask);

            fired += s;




            //--- FIRE
            premiseBurst.forEach(premise -> {

                if (premise.match(d, matchTTL)) {

                    if (derivable(d)) {

                        d.derive(deriveTTL);

                        n.emotion.premiseFire.increment();

                    } else {
                        n.emotion.premiseUnderivable.increment();
                    }

                } else {
                    n.emotion.premiseFailMatch.increment();
                }

            });

            premiseBurst.clear();

            d.flush(target);
        }

        if (fired == 0)
            return 0;
        else
            return (int) Math.ceil(fired / ((float) iterMult)); //adjust for the workload to correspond with the demand units
    }

    protected boolean derivable(Derivation d) {
        return rules.derivable(d);
    }

    private void selectPremises(NAR nar, int premisesMax, BiPredicate<Task, PriReference<Term>> each) {

        int premisesRemain[] = new int[]{premisesMax};
        int perConceptRemain[] = new int[1];

        int tasklinks = (int) Math.ceil(premisesMax / ((float) termLinksPerTaskLink));

        //return false to stop the current concept but not the entire chain
        BiPredicate<Task, PriReference<Term>> kontinue = (tasklink, termlink) ->
                (perConceptRemain[0]-- > 0) && each.test(tasklink, termlink) && (--premisesRemain[0] > 0);

        //for safety in case nothing is generated, this will limit the max # of concepts tried
        int[] conceptsRemain = new int[]{2 * (int) Math.ceil(premisesMax / ((float) (termLinksPerTaskLink * termLinksPerTaskLink)))};

        this.source.accept(a -> {

            perConceptRemain[0] = premisesPerConcept;

            a.premiseMatrix(
                    nar, kontinue,
                    tasklinks, termLinksPerTaskLink);

            return premisesRemain[0] > 0 && conceptsRemain[0]-- > 0;
        });


    }

//    protected long[] matchTime(Task task) {
//        assert (now != ETERNAL);
//
//        return
//        if (task.isEternal()) {
//            return
//                    //task.punc()!=GOAL ? ETERNAL : now;
//                    //ETERNAL
//                    now;
//        } else {
//
//            //return now;
//
//            //return task.myNearestTimeTo(now);
//
//            switch (task.punc()) {
//                case GOAL:
//                case QUEST:
//                    return now;
//                case BELIEF:
//                case QUESTION:
//                    return task.nearestPointInternal(now);
//                default:
//                    throw new UnsupportedOperationException();
//            }
//
//
////            return nar.random().nextBoolean() ?
////                    now : task.myNearestTimeTo(now);
//
//            //        return nar.random().nextBoolean() ?
//            //                task.nearestTimeTo(now) :
//            //                now + Math.round((-0.5f + nar.random().nextFloat()) * 2f * (Math.abs(now - task.mid())));
//        }
//
//        //return now + dur;
//
////        if (task.isEternal()) {
////            return ETERNAL;
////        } else //if (task.isInput()) {
////            return task.nearestTimeTo(now);
//
////        } else {
////            if (task.isBelief()) {
////                return now +
////                        nar.dur() *
////                            nar.random().nextInt(2*Param.PREDICTION_HORIZON)-Param.PREDICTION_HORIZON; //predictive belief
////            } else {
////                return Math.max(now, task.start()); //the corresponding belief for a goal or question task
////            }
////        }
//
//        //now;
//        //now + dur;
//
//    }


//    private int premises(Activate a) {
//        return Math.round(Util.lerp(a.priElseZero(), minHypoPremisesPerConceptFire, maxHypoPremisesPerConceptFire));
//    }


//    public static final Function<NAR, PrediTerm<Derivation>> NullDeriver = (n) -> new AbstractPred<Derivation>(Op.Null) {
//        @Override
//        public boolean test(Derivation derivation) {
//            return true;
//        }
//    };



    @Override
    public boolean singleton() {
        return false;
    }

    @Override
    public float value() {
        return Util.sum(Cause::value, rules.causes());
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

//    protected void input(Collection<Task> x) {
////        //experimental normalization
////        final float[] priSum = {0};
////        derivations.values().forEach(dd -> priSum[0] = dd.priElseZero());
////        if (priSum[0] > 1f) {
////            float factor = 1f/priSum[0];
////            derivations.values().forEach(dd -> dd.priMult(factor));
////        }
//
////        int limit = Math.max(8, premises * 2);
////        if (x.size() > limit) {
////            x = x.stream().sorted(Comparators.byFloatFunction(Task::priElseZero).reversed()).limit(limit).collect(toList());
////        }
//
//        target.accept(x);
//    }
