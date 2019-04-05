package nars.derive;

import jcog.Util;
import nars.NAR;
import nars.Task;
import nars.attention.DerivePri;
import nars.control.Causable;
import nars.control.Cause;
import nars.derive.premise.DeriverRules;
import nars.derive.premise.PremiseDeriverCompiler;
import nars.derive.premise.PremiseDeriverRuleSet;
import nars.derive.premise.PremiseRuleProto;
import nars.derive.timing.NonEternalTaskOccurenceOrPresentDeriverTiming;
import nars.term.Term;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
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
     * determines the temporal focus of (TODO tasklink and ) belief resolution to be matched during premise formation
     * input: premise Task, premise belief target
     * output: long[2] time interval
     **/
    public BiFunction<Task, Term, long[]> timing;

    public final DeriverRules rules;

    public DerivePri pri;

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
        super();
        this.pri = pri;
        this.rules = rules;
//        this.source = source;
        this.timing =
                //new TaskOrPresentTiming(nar);
                //new AdHocDeriverTiming(nar);
                //new TaskOccurenceDeriverTiming();
                new NonEternalTaskOccurenceOrPresentDeriverTiming(nar);


        nar.start(this);
    }

    public static Stream<Deriver> derivers(NAR n) {
        return n.plugins().filter(Deriver.class::isInstance).map(Deriver.class::cast);
    }



    @Override
    public final void next(NAR n, final BooleanSupplier kontinue) {


        derive(Derivation.derivation.get().next(this, n), kontinue);

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


    public final short[] what(PreDerivation d) {
        return rules.planner.apply(d);
    }


}

















































































































