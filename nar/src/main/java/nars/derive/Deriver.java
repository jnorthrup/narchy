package nars.derive;

import jcog.Util;
import jcog.pri.PriReference;
import jcog.pri.bag.impl.PLinkArrayBag;
import nars.NAR;
import nars.attention.TaskLinkWhat;
import nars.attention.What;
import nars.control.How;
import nars.control.Why;
import nars.derive.hypothesis.AbstractHypothesizer;
import nars.derive.hypothesis.Hypothesizer;
import nars.derive.hypothesis.TangentIndexer;
import nars.derive.premise.Premise;
import nars.derive.rule.DeriverProgram;
import nars.derive.rule.PremiseRuleSet;
import nars.derive.time.NonEternalTaskOccurenceOrPresentDeriverTiming;
import nars.derive.util.TimeFocus;
import nars.link.TaskLinks;

import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

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

    public Hypothesizer hypo;

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
        this.hypo = premises;
        return this;
    }

    public Deriver time(TimeFocus timing) {
        this.timing = timing;
        return this;
    }


    public Deriver(PremiseRuleSet rules, TimeFocus timing, Hypothesizer hypo) {
        this(rules.compile(), hypo, timing);
    }

    public Deriver(PremiseRuleSet rules, Hypothesizer hypo) {
        this(rules, new NonEternalTaskOccurenceOrPresentDeriverTiming(), hypo);
    }

    public Deriver(PremiseRuleSet rules) {
        this(rules
            //HACK add standard derivation behaviors
            .add(new AbstractHypothesizer.TaskResolve())
            .add(new AbstractHypothesizer.CompoundDecompose())
            .add(new AbstractHypothesizer.ReverseLink())
            .compile()
//            .print()
        );
    }

    public Deriver(DeriverProgram rules) {
        this(rules, new TangentIndexer(),
                //new TaskOrPresentTiming(nar);
                //new AdHocDeriverTiming(nar);
                //new TaskOccurenceDeriverTiming();
                new NonEternalTaskOccurenceOrPresentDeriverTiming()
        );
    }

    public Deriver(DeriverProgram rules, Hypothesizer hypo, TimeFocus timing) {
        super();
        NAR nar = rules.nar;
        this.rules = rules;
        this.hypo = hypo;
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


        int innerLoops = 4;


        int bufferCap = 4;
        int runBatch = 4;

        PLinkArrayBag<Premise> p = d.premises;
        p.clear();
        p.capacity(bufferCap);

        Consumer<PriReference<Premise>> each = (nextPremise) -> d.derive(nextPremise.get());
        Random rng = d.random;

        do {

            int cap = p.capacity();

            for (int i = 0; i < innerLoops; i++) {
                int tries = 1 + (cap - p.size()); //TODO penalize duplicates more
                do {
                    Premise x = hypo.hypothesize(links, d);
                    if (x!=null) {
                        if (d.add(x) && p.size() >= cap)
                            break;
                    }
                } while (--tries > 0);

                //System.out.println();
                //p.print();
                //System.out.println();

                p.commit(null);
                p.pop(null, runBatch, each);
//                p.commit();
//                p.sample(rng, runBatch, each);
            }


        } while (kontinue.getAsBoolean());

        p.clear();
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

















































































































