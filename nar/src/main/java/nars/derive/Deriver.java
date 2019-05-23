package nars.derive;

import jcog.Util;
import jcog.func.TriFunction;
import nars.NAR;
import nars.Task;
import nars.attention.What;
import nars.control.How;
import nars.control.Why;
import nars.derive.model.Derivation;
import nars.derive.model.PreDerivation;
import nars.derive.rule.DeriverRules;
import nars.derive.rule.PremiseRuleCompiler;
import nars.derive.rule.PremiseRuleProto;
import nars.derive.rule.PremiseRuleSet;
import nars.derive.timing.NonEternalTaskOccurenceOrPresentDeriverTiming;
import nars.link.DynamicTermLinker;
import nars.link.TermLinker;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
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
abstract public class Deriver extends How {

    public final DeriverRules rules;
    /**
     * determines the temporal focus of (TODO tasklink and ) belief resolution to be matched during premise formation
     * input: premise Task, premise belief target
     * output: long[2] time interval
     **/
    public TriFunction<What, Task, Term, long[]> timing;

    protected Deriver(Set<PremiseRuleProto> rules, NAR nar) {
        this(PremiseRuleCompiler.the(rules), nar);
        if (rules.isEmpty())
            throw new RuntimeException("rules empty");
    }

    protected Deriver(PremiseRuleSet rules) {
        this(rules, rules.nar);
    }


    protected Deriver(DeriverRules rules, NAR nar) {
        super();
        this.rules = rules;
//        this.source = source;
        this.timing =
                //new TaskOrPresentTiming(nar);
                //new AdHocDeriverTiming(nar);
                //new TaskOccurenceDeriverTiming();
                new NonEternalTaskOccurenceOrPresentDeriverTiming();


        nar.start(this);
    }

    public static Stream<Deriver> derivers(NAR n) {
        return n.partStream().filter(Deriver.class::isInstance).map(Deriver.class::cast);
    }


    @Override
    public final void next(What w, final BooleanSupplier kontinue) {


        derive(Derivation.derivation.get().next(this, w), kontinue);

    }


    abstract protected void derive(Derivation d, BooleanSupplier kontinue);


    /**
     * unifier TTL used for matching in premise formation
     */
    protected int matchTTL() {
        return nar.premiseUnifyTTL.intValue();
    }


    @Override
    public boolean singleton() {
        return false;
    }

    @Override
    public float value() {
        //TODO cache this between cycles
        float v = Util.sum(Why::value, rules.causes());
        //System.out.println(this + " " + v);
        return v;
    }


    public final short[] what(PreDerivation d) {
        return rules.pre.apply(d);
    }



    @Nullable
    public TermLinker linker(Term t) {
        return t instanceof Atomic ? null : DynamicTermLinker.Weighted;
    }
}

















































































































