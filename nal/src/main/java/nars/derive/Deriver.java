package nars.derive;

import jcog.Util;
import jcog.event.Offs;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.attention.DerivePri;
import nars.control.Cause;
import nars.control.DurService;
import nars.control.channel.ConsumerX;
import nars.derive.premise.DeriverRules;
import nars.derive.premise.PremiseDeriverCompiler;
import nars.derive.premise.PremiseDeriverRuleSet;
import nars.derive.premise.PremiseRuleProto;
import nars.derive.timing.NonEternalTaskOccurenceOrPresentDeriverTiming;
import nars.exe.Causable;
import nars.task.ITask;
import nars.task.util.TaskBuffer;
import nars.term.Term;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * an individual deriver process: executes a particular Deriver model
 * specified by a set of premise rules.
 * <p>
 * runtime intensity is metered and throttled by causal feedback
 * <p>
 * this is essentially a Function<Premise, Stream<DerivedTask>> but
 * the current level of code complexity makes this non-obvious
 */
abstract public class Deriver extends Causable {


    /**
     * determines the time for beliefs to be matched during premise formation
     * input: premise Task, premise belief target
     * output: long[2] time interval
     **/
    public BiFunction<Task, Term, long[]> timing;




    public final DeriverRules rules;

//    /**
//     * source of concepts supplied to this for this deriver
//     */
//    protected final Consumer<Predicate<Activate>> source;

    public DerivePri pri;
    public TaskBuffer out;
    private Offs outputOffs;

//    public final IntRange tasklinkSpread =  new IntRange(Param.TaskLinkSpreadDefault, 1, 32);


    protected Deriver(Set<PremiseRuleProto> rules, NAR nar) {
        this(PremiseDeriverCompiler.the(rules), nar);
        if (rules.isEmpty())
            throw new RuntimeException("rules empty");
    }



    protected Deriver(PremiseDeriverRuleSet rules) {
        this(rules, rules.nar);
    }

    protected Deriver(DeriverRules rules, NAR nar) {
        this(rules, nar.attn.derivePri, nar);
    }

    protected Deriver(DeriverRules rules, DerivePri pri, NAR nar) {
        super(
                $.func("deriver", $.the(serial.getAndIncrement()))
        );
        this.pri = pri;
        this.rules = rules;
//        this.source = source;
        this.timing =
                //new TaskOrPresentTiming(nar);
                //new AdHocDeriverTiming(nar);
                //new TaskOccurenceDeriverTiming();
                new NonEternalTaskOccurenceOrPresentDeriverTiming(nar);


        nar.on(this);
    }

    public static Stream<Deriver> derivers(NAR n) {
        return n.services().filter(Deriver.class::isInstance).map(Deriver.class::cast);
    }



    @Override
    protected final void next(NAR n, final BooleanSupplier kontinue) {


        derive(Derivation.derivation.get().next(this), kontinue);

    }


    abstract protected void derive(Derivation d, BooleanSupplier kontinue);




    /**
     * punctuation equalizer: value factor for the conclusion punctuation type [0..1.0]
     */
    public final float preAmp(byte concPunc) {
        return pri.preAmp(concPunc);
    }


    /**
     * unifier TTL used for matching in premise formation
     */
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



    @Deprecated
    private static final AtomicInteger serial = new AtomicInteger();

    public final short[] what(PreDerivation d) {
        return rules.planner.apply(d);
    }

    public synchronized Deriver output(ConsumerX<ITask> target, TaskBuffer out, boolean cycleOrDur) {
        TaskBuffer prev = this.out;
        if (prev!=null) {
            if (prev == out) return this; //same instance
        }
        if (this.outputOffs!=null)
            this.outputOffs.clear();

        this.out = out;

        if (out!=null && out.synchronous()) {
            Consumer<NAR> p = nn -> out.commit(nn.time(), target);
            this.outputOffs = new Offs((cycleOrDur) ?
                    nar.onCycle(p)
                    :
                    DurService.on(nar, p), nar.eventClear.on(this::clear)
            );
        }

        return this;
    }



}

















































































































