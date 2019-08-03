package nars.derive;

import jcog.Util;
import nars.NAR;
import nars.Op;
import nars.attention.What;
import nars.control.How;
import nars.control.Why;
import nars.derive.model.Derivation;
import nars.derive.model.PreDerivation;
import nars.derive.premise.PremiseSource;
import nars.derive.rule.DeriverRules;
import nars.derive.rule.PremiseRuleCompiler;
import nars.derive.rule.PremiseRuleProto;
import nars.derive.rule.PremiseRuleSet;
import nars.derive.timing.NonEternalTaskOccurenceOrPresentDeriverTiming;
import nars.term.Term;
import nars.term.anon.AnonWithVarShift;

import java.util.Random;
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

    public final PremiseSource premises;

    //shift heuristic condition probabalities TODO refine
    float PREMISE_SHIFT_EQUALS_ROOT = 0.02f;
    float PREMISE_SHIFT_OTHER = 0.9f;
    float PREMISE_SHIFT_RANDOM = 0.75f;

    /**
     * determines the temporal focus of (TODO tasklink and ) belief resolution to be matched during premise formation
     * input: premise Task, premise belief target
     * output: long[2] time interval
     **/
    public TimeFocus timing;

    protected Deriver(Set<PremiseRuleProto> rules, NAR nar) {
        this(PremiseRuleCompiler.the(rules), nar);
        if (rules.isEmpty())
            throw new RuntimeException("rules empty");
    }

    protected Deriver(PremiseRuleSet rules) {
        this(rules, rules.nar);
    }

    protected Deriver(PremiseRuleSet rules, TimeFocus timing, PremiseSource premises) {
        this(PremiseRuleCompiler.the(rules), premises, timing, rules.nar);
    }

    @Deprecated protected Deriver(DeriverRules rules, NAR nar) {
        this(rules, new PremiseSource.TangentConceptCaching(),
                //new TaskOrPresentTiming(nar);
                //new AdHocDeriverTiming(nar);
                //new TaskOccurenceDeriverTiming();
                new NonEternalTaskOccurenceOrPresentDeriverTiming(),
                nar);
    }

    protected Deriver(DeriverRules rules, PremiseSource premises, TimeFocus timing, NAR nar) {
        super();
        this.rules = rules;
        this.premises = premises;
        this.timing = timing;

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
        double  v = Util.sumDouble(Why::value, rules.causes());
        //System.out.println(this + " " + v);
        return (float) v;
    }


    public final short[] what(PreDerivation d) {
        return rules.pre.apply(d);
    }

    public Term loadBelief(Term b, AnonWithVarShift anon, Term taskTerm, Term beliefTerm, Random random) {


        boolean shift, shiftRandomOrCompletely;
        if (!b.hasAny(Op.Variable & taskTerm.structure())) {
            shift = shiftRandomOrCompletely = false; //unnecessary
        } else {
            if (beliefTerm.equalsRoot(taskTerm)) {
                shift = random.nextFloat() < PREMISE_SHIFT_EQUALS_ROOT; //structural identity
            } /*else if (beliefTerm.containsRecursively(taskTerm) || taskTerm.containsRecursively(beliefTerm)) {
                shift = random.nextFloat() < PREMISE_SHIFT_CONTAINS_RECURSIVELY;
            } */ else {
                shift = random.nextFloat() < PREMISE_SHIFT_OTHER;
            }
            shiftRandomOrCompletely =  random.nextFloat() < PREMISE_SHIFT_RANDOM;
        }

        return shift ?
            anon.putShift(b, taskTerm, shiftRandomOrCompletely ? random : null) :
            anon.put(b);
    }
}

















































































































