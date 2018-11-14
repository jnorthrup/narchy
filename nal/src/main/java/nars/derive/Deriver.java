package nars.derive;

import jcog.Util;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.control.Cause;
import nars.derive.premise.DeriverRules;
import nars.derive.premise.PremiseDeriverCompiler;
import nars.derive.premise.PremiseDeriverRuleSet;
import nars.derive.premise.PremiseRuleProto;
import nars.derive.timing.NonEternalTaskOccurenceOrPresentDeriverTiming;
import nars.attention.ActiveConcepts;
import nars.exe.Causable;
import nars.link.Activate;
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
 * <p>
 * this is essentially a Function<Premise, Stream<DerivedTask>> but
 * the current level of code complexity makes this non-obvious
 */
abstract public class Deriver extends Causable {


    /**
     * determines the time for beliefs to be matched during premise formation
     * input: premise Task, premise belief term
     * output: long[2] time interval
     **/
    public BiFunction<Task, Term, long[]> timing;




    protected final DeriverRules rules;

    /**
     * source of concepts supplied to this for this deriver
     */
    protected final Consumer<Predicate<Activate>> source;


    protected Deriver(Consumer<Predicate<Activate>> source, Set<PremiseRuleProto> rules, NAR nar) {
        this(source, PremiseDeriverCompiler.the(rules), nar);
        if (rules.isEmpty())
            throw new RuntimeException("rules empty");
    }


    protected Deriver(ActiveConcepts attn, Set<PremiseRuleProto> rules, NAR nar) {
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
    protected final void next(NAR n, final BooleanSupplier kontinue) {

        derive(Derivation.derivation.get().next(n, this), kontinue);

    }


    abstract protected void derive(Derivation d, BooleanSupplier kontinue);




    /**
     * punctuation equalizer: value factor for the conclusion punctuation type [0..1.0]
     */
    public final float preAmp(byte concPunc) {
        return nar.attn.deriving.preAmp(concPunc);
    }


    /**
     * unifier TTL used for matching in premise formation
     */
    protected int matchTTL() {
        return nar.matchTTL.intValue();
    }


//    @Override
//    public boolean singleton() {
//        return false;
//    }

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

    @Deprecated
    private static final AtomicInteger serial = new AtomicInteger();

}

















































































































