package nars.derive;

import jcog.Util;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.bag.Bag;
import nars.NAR;
import nars.Op;
import nars.attention.TaskLinkWhat;
import nars.attention.What;
import nars.control.How;
import nars.control.Why;
import nars.derive.action.AdjacentLinks;
import nars.derive.action.CompoundDecompose;
import nars.derive.action.TaskResolve;
import nars.derive.adjacent.TangentIndexer;
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

    int innerLoops = 4;
    int bufferCap = 4;
    int runBatch = bufferCap/2;

    /**
     * variable types unifiable in premise formation
     */
    public static final int PremiseUnifyVars =
        //VAR_QUERY.bit
        Op.VAR_QUERY.bit | Op.VAR_DEP.bit
        //Op.Variable //all
        ;

    public DeriverProgram rules;


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


    public Deriver(PremiseRuleSet rules) {
        this(rules, new NonEternalTaskOccurenceOrPresentDeriverTiming());
    }

    public Deriver(PremiseRuleSet rules, TimeFocus timing) {
        this(rules
            //HACK adds standard derivation behaviors
            .add(new TaskResolve())
            .add(new CompoundDecompose(true))
            .add(new CompoundDecompose(false))
            .add(new AdjacentLinks(new TangentIndexer()))
            .compile()
//           .print()
        ,
        timing);
    }

    public Deriver(DeriverProgram rules, TimeFocus timing) {
        super();
        NAR nar = rules.nar;
        this.rules = rules;
        this.timing = timing;

        nar.start(this);
    }

    @Override
    public final void next(What w, final BooleanSupplier kontinue) {

        TaskLinks links = ((TaskLinkWhat) w).links;
        if (links.isEmpty())
            return;

        Derivation d = Derivation.derivation.get().next(this, w);



        Bag<Premise, PLink<Premise>> p = d.premises;
        p.clear();
        p.capacity(bufferCap);

        Consumer<PriReference<Premise>> each = (nextPremise) -> d.derive(nextPremise.get());
        Random rng = d.random;

        do {

            int cap = p.capacity();

            for (int i = 0; i < innerLoops; i++) {
                int tries = 1 + (cap - p.size()); //TODO penalize duplicates more
                do {
                    Premise x = hypothesize(links, d);
                    if (x!=null) {
                        if (d.add(x) && p.size() >= cap)
                            break;
                    }
                } while (--tries > 0);

                //System.out.println();
                //p.print();
                //System.out.println();

                p.commit(null);
                p.pop(rng, runBatch, each); //HijackBag
                //p.pop(null, runBatch, each); //ArrayBag
//                p.commit();
//                p.sample(rng, runBatch, each);
            }


        } while (kontinue.getAsBoolean());

        p.clear();
    }

    public final Premise hypothesize(TaskLinks links, Derivation d) {
        return links.sample(d.random);
    }

    @Override
    public float value() {
        //TODO cache this between cycles
        double  v = Util.sumDouble(Why::pri, rules.causes());
        //System.out.println(this + " " + v);
        return (float) v;
    }


    public final short[] what(PreDerivation d) {
        return rules.pre.apply(d);
    }


}

















































































































