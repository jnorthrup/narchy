package nars.derive;

import jcog.Util;
import jcog.pri.PriReference;
import jcog.pri.bag.Bag;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.control.Cause;
import nars.control.DurService;
import nars.derive.budget.DefaultPriWeightedDeriverBudgeting;
import nars.derive.premise.DeriverRules;
import nars.derive.premise.PremiseDeriverCompiler;
import nars.derive.premise.PremiseDeriverRuleSet;
import nars.derive.premise.PremiseRuleProto;
import nars.derive.timing.NonEternalTaskOccurenceOrPresentDeriverTiming;
import nars.exe.Attention;
import nars.exe.Causable;
import nars.link.Activate;
import nars.link.ActivatedLinks;
import nars.link.TaskLink;
import nars.task.ITask;
import nars.term.Term;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

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

    public final DeriverBudgeting budgeting =
            //new DefaultDeriverBudgeting();
            new DefaultPriWeightedDeriverBudgeting();

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

    protected final DeriverRules rules;

    /**
     * source of concepts supplied to this for this deriver
     */
    protected final Consumer<Predicate<Activate>> source;

//    private Deriver(NAR nar, String... rules) {
//        this(new PremiseDeriverRuleSet(nar, rules));
//    }

    protected Deriver(Consumer<Predicate<Activate>> source, Set<PremiseRuleProto> rules, NAR nar) {
        this(source, PremiseDeriverCompiler.the(rules), nar);
        if (rules.isEmpty())
            throw new RuntimeException("rules empty");
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


    private Deriver(Consumer<Predicate<Activate>> source, DeriverRules rules, NAR nar) {
        super(
            $.func("deriver", $.the(serial.getAndIncrement())) 
        );
        this.rules = rules;
        this.source = source;
        this.timing =
                //new AdHocDeriverTiming(nar);
                //new TaskOccurenceDeriverTiming();
                new NonEternalTaskOccurenceOrPresentDeriverTiming(nar);


        nar.on(this);
    }

    public static Stream<Deriver> derivers(NAR n) {
        return n.services().filter(Deriver.class::isInstance).map(Deriver.class::cast);
    }

    @Override
    protected void starting(NAR nar) {
        super.starting(nar);
        on(DurService.on(nar, this::update));
    }



    private void update() {
        budgeting.update(this, nar);
    }

    @Override
    protected final void next(NAR n, final BooleanSupplier kontinue) {

        derive(derivation.get().next(n, this), kontinue);

        if (!linked.isEmpty()) {
            //nar.input(linked);
            ITask.run(linked, n);//inline
        }

        derived.commit(n);

    }



    abstract protected void derive(Derivation d, BooleanSupplier kontinue);


    /** returns false if no tasklinks are present */
    static protected boolean commit(NAR nar, Bag<?, TaskLink> tasklinks, Bag<Term, PriReference<Term>> termlinks) {
        float linkForgetting = nar.forgetRate.floatValue();
        tasklinks.commit(tasklinks.forget(linkForgetting));
        if (tasklinks.isEmpty())
            return false;

        termlinks.commit(termlinks.forget(linkForgetting));
        return true;

    }

    /** punctuation equalizer: value factor for the conclusion punctuation type [0..1.0] */
    public final float puncFactor(byte conclusion) {
        return budgeting.puncFactor(conclusion);
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
        float v = Util.sum(Cause::value, rules.causes());
        //System.out.println(this + " " + v);
        return v;
    }


    //public final FloatRange sustain = new FloatRange(0f, 0f, 0.99f);
    public int dur() {
        //return Math.round((nar.dur() * (1/(1- sustain.floatValue()))));
        return nar.dur();
    }
}

















































































































