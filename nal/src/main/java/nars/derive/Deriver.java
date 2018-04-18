package nars.derive;

import jcog.Util;
import jcog.bag.Bag;
import jcog.pri.PriReference;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.control.Activate;
import nars.control.Cause;
import nars.derive.premise.PremiseDeriver;
import nars.derive.premise.PremiseDeriverCompiler;
import nars.derive.premise.PremiseDeriverProto;
import nars.derive.premise.PremiseDeriverRuleSet;
import nars.exe.Causable;
import nars.link.TaskLink;
import nars.link.Tasklinks;
import nars.term.Term;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * an individual deriver process: executes a particular Deriver model
 * specified by a set of premise rules.
 * <p>
 * runtime intensity is metered and throttled by causal feedback
 */
abstract public class Deriver extends Causable {

    @Deprecated private static final AtomicInteger serial = new AtomicInteger();

    public static final ThreadLocal<Derivation> derivation = ThreadLocal.withInitial(Derivation::new);

    public final PremiseDeriver rules;

    /**
     * source of concepts supplied to this for this deriver
     */
    protected final Consumer<Predicate<Activate>> source;

    private final Consumer<Collection<Task>> target;

    public DeriverBudgeting prioritize = new DeriverBudgeting.DefaultDeriverBudgeting();

    public Deriver(NAR nar, String... rules) {
        this(new PremiseDeriverRuleSet(nar, rules));
    }

    public Deriver(Set<PremiseDeriverProto> rules, NAR nar) {
        this(nar.exe::fire, nar::input, rules, nar);
    }
    public Deriver(PremiseDeriverRuleSet rules) {
        this(rules, rules.nar);
    }

    public Deriver(Consumer<Predicate<Activate>> source, Consumer<Collection<Task>> target, Set<PremiseDeriverProto> rules, NAR nar) {
        this(source, target, PremiseDeriverCompiler.the(rules, null), nar);
        if (rules.isEmpty())
            throw new RuntimeException("rules empty");
    }

    public Deriver(Consumer<Predicate<Activate>> source, Consumer<Collection<Task>> target, PremiseDeriver rules, NAR nar) {
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

    @Override
    protected final int next(NAR n, final int iterations) {

        Derivation d = derivation.get().cycle(n, this);

        derive(n, iterations, d);

        int derived = d.flush(target);
        return iterations; //HACK
    }

    abstract protected void derive(NAR n, int iterations, Derivation d);


    protected void activate(Concept concept, TaskLink tasklink, NAR nar) {
        Tasklinks.linkTaskTemplates(concept, tasklink, tasklink.priElseZero(), nar);
    }

    protected boolean update(NAR nar, Bag<?, TaskLink> tasklinks, Bag<Term, PriReference<Term>> termlinks) {
        float linkForgetting = nar.forgetRate.floatValue();
        tasklinks.commit(tasklinks.forget(linkForgetting));
        int ntasklinks = tasklinks.size();
        if (ntasklinks == 0)
            return false;

        termlinks.commit(termlinks.forget(linkForgetting));
        int ntermlinks = termlinks.size();
        if (ntermlinks == 0)
            return false;

        return true;
    }


    protected boolean derivable(Derivation d) {
        return rules.derivable(d);
    }

    @Override
    public final boolean singleton() {
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
