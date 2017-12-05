package nars.control;

import jcog.Util;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.derive.*;
import nars.derive.instrument.DebugDerivationPredicate;
import nars.derive.rule.PremiseRuleSet;
import nars.index.term.PatternIndex;

import java.io.PrintStream;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * an individual deriver process: executes a particular Deriver model
 * specified by a set of premise rules.
 * <p>
 * runtime intensity is metered and throttled by causal feedback
 */
public class Deriver extends NARService {

    private final Consumer<Predicate<Activate>> source;
    private float minPremisesPerConcept = 1;
    private float maxPremisesPerConcept = 3;

    public static Function<NAR, Deriver> deriver(Function<NAR, PremiseRuleSet> rules) {
        return (nar) ->
                new Deriver(PrediTrie.the(rules.apply(nar),
                        Param.TRACE ? DebugDerivationPredicate::new : null
                ), nar);
    }

    /** loads default deriver rules, specified by a range (inclusive) of levels. this allows creation
     * of multiple deriver layers each targetting a specific range of the NAL spectrum
     */
    public static Function<NAR, Deriver> deriver(int minLevel, int maxLevel, String... additional) {
        assert ((minLevel <= maxLevel && maxLevel > 0) || additional.length > 0);

        return deriver(nar ->
                PremiseRuleSet.rules(nar, new PatternIndex(nar),
                        Derivers.standard(minLevel, maxLevel, additional)
                ));
    }

    public final PrediTerm<Derivation> deriver;
    private final NAR nar;
    private final Causable can;

    public Deriver(PrediTerm<Derivation> deriver, NAR nar) {
        this(nar.exe::fire, deriver, nar);
    }

    public Deriver(Consumer<Predicate<Activate>> source, PrediTerm<Derivation> deriver, NAR nar) {
        super(nar);
        this.deriver = deriver;
        this.source = source;
        this.nar = nar;

        Try t = (Try) ((AndCondition) (deriver)).cache[((AndCondition) (deriver)).cache.length - 1]; //HACK

        //this.cause = nar.newCauseChannel(this);
        this.can = new Causable(nar) {
            @Override
            protected int next(NAR n, int iterations) {
                return Deriver.this.run(iterations);
            }

            @Override
            public boolean singleton() {
                return false;
            }

            @Override
            public float value() {
                return Util.sum(Cause::value, t.causes);
            }
        };
        //this.can.can.update(1,1,0.0);
    }


    protected int run(int work) {


        NAR nar = this.nar;
        Derivation d = derivation.get().cycle(nar, deriver);

        int matchTTL = Param.TTL_PREMISE_MIN * 4;
        int ttlMin = nar.matchTTLmin.intValue();
        int ttlMax = nar.matchTTLmax.intValue();


        int premisesRemain[] = new int[]{work};

        //hard limit on # of concepts processed. since usually there will be >1 premises per concept, this will normally not be exhausted
        int conceptsRemain[] = new int[]{work};

        source.accept(a -> {

            for (Premise p : a.premises(nar, d.activator, premises(a))) {

                if (p.match(d, matchTTL) != null) {

                    int deriveTTL = Util.lerp(
                            //p.task.priElseZero() / nar.priDefault(p.task.punc()), //relative
                            p.task.priElseZero(),                                   //absolute
                            ttlMin, ttlMax);

                    d.derive(deriveTTL);

                    if (--premisesRemain[0] <= 0)
                        return false; //done
                }
            }

            return (--conceptsRemain[0] > 0);
        });

        int derived = d.commit(nar::input);

        return work - premisesRemain[0];
    }


    private int premises(Activate a) {
        return Math.round(Util.lerp(a.priElseZero(), minPremisesPerConcept, maxPremisesPerConcept));
    }


    public static final Function<NAR, PrediTerm<Derivation>> NullDeriver = (n) -> new AbstractPred<Derivation>(Op.Null) {
        @Override
        public boolean test(Derivation derivation) {
            return true;
        }
    };


    public static Stream<Deriver> derivers(NAR n) {
        return n.services().filter(Deriver.class::isInstance).map(Deriver.class::cast);
    }

    public static void print(NAR n, PrintStream p) {
        derivers(n).forEach(d -> {
            p.println(d.toString());
            TrieDeriver.print(d.deriver, p);
            p.println();
        });
    }

    //    public final IterableThreadLocal<Derivation> derivation =
//            new IterableThreadLocal<>(() -> new Derivation(this));
    private static final ThreadLocal<Derivation> derivation =
            ThreadLocal.withInitial(Derivation::new);

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
