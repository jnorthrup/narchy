package nars.derive;

import jcog.Util;
import nars.NAR;
import nars.attention.TaskLinkWhat;
import nars.attention.What;
import nars.control.How;
import nars.control.Why;
import nars.derive.hypothesis.Hypothesizer;
import nars.derive.hypothesis.TangentIndexer;
import nars.derive.rule.DeriverProgram;
import nars.derive.rule.PremiseRuleSet;
import nars.derive.time.NonEternalTaskOccurenceOrPresentDeriverTiming;
import nars.derive.util.TimeFocus;
import nars.link.TaskLinks;
import nars.time.When;

import java.util.function.BooleanSupplier;

/**
 * an individual deriver process: executes a particular Deriver model
 * specified by a set of premise rules.
 * <p>
 * runtime intensity is metered and throttled by causal feedback
 * <p>
 * this is essentially a Function<Premise, Stream<DerivedTask>> but
 * the current level of code complexity makes this non-obvious
 */
public class Deriver extends How {

    public DeriverProgram rules;

    public Hypothesizer hypothesize;

    /**
     * determines the temporal focus of (TODO tasklink and ) belief resolution to be matched during premise formation
     * input: premise Task, premise belief target
     * output: long[2] time interval
     **/
    public TimeFocus timing;

    //shift heuristic condition probabalities TODO refine
    float PREMISE_SHIFT_EQUALS_ROOT = 0.02f;
    float PREMISE_SHIFT_CONTAINS_RECURSIVELY = 0.05f;
    float PREMISE_SHIFT_OTHER = 0.9f;
    float PREMISE_SHIFT_RANDOM = 0.75f;

    public Deriver rules(DeriverProgram rules) {
        this.rules = rules;
        return this;
    }

    public Deriver hypothesize(Hypothesizer premises) {
        this.hypothesize = premises;
        return this;
    }

    public Deriver time(TimeFocus timing) {
        this.timing = timing;
        return this;
    }


    public Deriver(PremiseRuleSet rules, TimeFocus timing, Hypothesizer hypothesize) {
        this(rules.compile(), hypothesize, timing);
    }

    public Deriver(PremiseRuleSet rules, Hypothesizer hypothesize) {
        this(rules, new NonEternalTaskOccurenceOrPresentDeriverTiming(), hypothesize);
    }

    public Deriver(PremiseRuleSet rules) {
        this(rules.compile());
    }

    public Deriver(DeriverProgram rules) {
        this(rules, new TangentIndexer(),
                //new TaskOrPresentTiming(nar);
                //new AdHocDeriverTiming(nar);
                //new TaskOccurenceDeriverTiming();
                new NonEternalTaskOccurenceOrPresentDeriverTiming()
        );
    }

    public Deriver(DeriverProgram rules, Hypothesizer hypothesize, TimeFocus timing) {
        super();
        NAR nar = rules.nar;
        this.rules = rules;
        this.hypothesize = hypothesize;
        this.timing = timing;

        nar.start(this);
    }

//    public static Stream<Deriver> derivers(NAR n) {
//        return n.partStream().filter(Deriver.class::isInstance).map(Deriver.class::cast);
//    }



    @Override
    public final void next(What w, final BooleanSupplier kontinue) {

        TaskLinks links = ((TaskLinkWhat) w).links;
        if (links.isEmpty())
            return;

        Derivation d = Derivation.derivation.get().next(this, w);

        When<NAR> now = d.deriver.timing.task(w);



        do {

            hypothesize.premises(
                now, //maybe update if these are long cycles
                links, d);

        } while (kontinue.getAsBoolean());

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


}

















































































































