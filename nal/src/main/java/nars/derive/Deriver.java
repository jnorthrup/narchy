package nars.derive;

import jcog.Util;
import jcog.pri.PriReference;
import jcog.pri.bag.Bag;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.control.Cause;
import nars.derive.budget.DefaultDeriverBudgeting;
import nars.derive.premise.PremiseDeriver;
import nars.derive.premise.PremiseDeriverCompiler;
import nars.derive.premise.PremiseDeriverRuleSet;
import nars.derive.premise.PremiseRuleProto;
import nars.derive.timing.AdHocDeriverTiming;
import nars.exe.Attention;
import nars.exe.Causable;
import nars.link.Activate;
import nars.link.ActivatedLinks;
import nars.link.TaskLink;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.Op.*;

/**
 * an individual deriver process: executes a particular Deriver model
 * specified by a set of premise rules.
 * <p>
 * runtime intensity is metered and throttled by causal feedback
 *
 * this is essentially a Function<Premise, Stream<DerivedTask>> but
 * the current level of code complexity makes this non-obvious
 */
abstract public class Deriver extends Causable {

    public final DeriverBudgeting prioritize =
            new DefaultDeriverBudgeting();

    /** determines the time for beliefs to be matched during premise formation
     *    input: premise Task, premise belief term
     *    output: long[2] time interval
     **/
    public BiFunction<Task,Term,long[]> timing;


    protected final DerivedTasks derived =
            new DerivedTasks.DerivedTasksBag(Param.DerivedTaskBagCapacity);

    public final ActivatedLinks linked = new ActivatedLinks();


    @Deprecated private static final AtomicInteger serial = new AtomicInteger();

    private static final ThreadLocal<Derivation> derivation = ThreadLocal.withInitial(Derivation::new);

    protected final PremiseDeriver rules;

    /**
     * source of concepts supplied to this for this deriver
     */
    protected final Consumer<Predicate<Activate>> source;

    private Deriver(NAR nar, String... rules) {
        this(new PremiseDeriverRuleSet(nar, rules));
    }

    private Deriver(PremiseDeriverRuleSet rules) {
        this(rules.nar.attn, rules, rules.nar);
    }


    protected Deriver(Attention attn, Set<PremiseRuleProto> rules, NAR nar) {
        this(attn::fire, rules, nar);
    }

    protected Deriver(Consumer<Predicate<Activate>> source, PremiseDeriverRuleSet rules) {
        this(source, rules, rules.nar);
    }

    protected Deriver(Consumer<Predicate<Activate>> source, Set<PremiseRuleProto> rules, NAR nar) {
        this(source, PremiseDeriverCompiler.the(rules), nar);
        if (rules.isEmpty())
            throw new RuntimeException("rules empty");
    }

    private Deriver(Consumer<Predicate<Activate>> source, PremiseDeriver rules, NAR nar) {
        super(
            $.func("deriver", $.the(serial.getAndIncrement())) 
        );
        this.rules = rules;
        this.source = source;
        this.timing =
                new AdHocDeriverTiming(nar);
                //new TaskOccurenceDeriverTiming();


        nar.on(this);
    }

    public static Stream<Deriver> derivers(NAR n) {
        return n.services().filter(Deriver.class::isInstance).map(Deriver.class::cast);
    }

    @Override
    protected int next(NAR n, final int iterations) {

        derive(derivation.get().next(n, this), iterations);

        if (!linked.isEmpty())
            nar.input(linked);

        derived.commit(n);

        return iterations;
    }



    abstract protected void derive(Derivation d, int iterations);


    /** returns false if no tasklinks are present */
    static protected boolean commit(NAR nar, Bag<?, TaskLink> tasklinks, @Nullable Bag<Term, PriReference<Term>> termlinks) {
        float linkForgetting = nar.forgetRate.floatValue();
        tasklinks.commit(tasklinks.forget(linkForgetting));
        int ntasklinks = tasklinks.size();
        if (ntasklinks == 0)
            return false;


        if (termlinks!=null)
            termlinks.commit(termlinks.forget(linkForgetting));

        return true;

    }

    /** punctuation equalizer: value factor for the conclusion punctuation type [0..1.0] */
    public float puncFactor(byte conclusion) {


        switch (conclusion) {
            case BELIEF: return 1f;
            case GOAL: return 1f;
            case QUESTION: return 1f;
            case QUEST: return 1f;
            default:
                throw new UnsupportedOperationException();
        }
    }


    /** unifier TTL used for matching in premise formation */
    protected int matchTTL() {
        return nar.matchTTL.intValue();
    }


    @Override
    public boolean singleton() {
        return false;
    }

    @Override
    public float value() {
        //TODO cache this between cycles
        return Util.sum(Cause::value, rules.causes());
    }


    //public final FloatRange sustain = new FloatRange(0f, 0f, 0.99f);
    public int dur() {
        //return Math.round((nar.dur() * (1/(1- sustain.floatValue()))));
        return nar.dur();
    }
}

















































































































