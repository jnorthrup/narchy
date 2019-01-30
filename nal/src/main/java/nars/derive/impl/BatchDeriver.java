package nars.derive.impl;

import jcog.data.list.FasterList;
import jcog.data.set.ArrayHashSet;
import jcog.math.IntRange;
import jcog.pri.bag.Bag;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.derive.Derivation;
import nars.derive.Deriver;
import nars.derive.Premise;
import nars.derive.premise.PremiseDeriverRuleSet;
import nars.derive.premise.PremiseRuleProto;
import nars.index.concept.AbstractConceptIndex;
import nars.link.Activate;
import nars.link.TaskLink;
import nars.link.TermLinker;
import nars.term.Compound;
import nars.term.Term;

import java.util.Collection;
import java.util.Random;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;


/** buffers premises in batches*/
public class BatchDeriver extends Deriver {

    public final IntRange tasklinksPerIteration = new IntRange(3, 1, 32);


//    /**
//     * how many premises to keep per concept; should be <= Hypothetical count
//     */
//    public final IntRange premisesPerLink = new IntRange(1, 1, 8);

//    /** what % premises to actually try deriving */
//    public final FloatRange premiseElitism = new FloatRange(0.5f, 0, 1f);



    public BatchDeriver(PremiseDeriverRuleSet rules) {
        this(rules, rules.nar);
    }

    public BatchDeriver(Set<PremiseRuleProto> rules, NAR nar) {
        super(fire(nar), rules, nar);
    }


    public BatchDeriver(Consumer<Predicate<Activate>> source, PremiseDeriverRuleSet rules) {
        super(source, rules, rules.nar);
    }

    public BatchDeriver(Consumer<Predicate<Activate>> source, Set<PremiseRuleProto> rules, NAR nar) {
        super(source, rules, nar);
    }

    @Override
    protected final void derive(Derivation d, BooleanSupplier kontinue) {

        int matchTTL = matchTTL(), deriveTTL = d.nar.deriveBranchTTL.intValue();

        do {

            Collection<Premise> pp = hypothesize(d);
            if (!pp.isEmpty()) {
                for (Premise p : pp)
                    p.derive(d, matchTTL, deriveTTL);
                pp.clear();
            }

        } while (kontinue.getAsBoolean());

    }


    /**
     * forms premises
     */
    private Collection<Premise> hypothesize(Derivation d) {

        Collection<Premise> premises = d.premiseBuffer;
        premises.clear();

        Random rng = d.random;
//
//        Supplier<Term> beliefSrc;
//        if (concept.term().op().atomic) {
//            Bag<?, TaskLink> src = tasklinks;
//            beliefSrc = ()->src.sample(rng).term();
//        } else {
//            Sampler<Term> src = concept.linker();
//            beliefSrc = ()->src.sample(rng);
//        }

        Bag<TaskLink, TaskLink> tasklinks = ((AbstractConceptIndex) nar.concepts).active;

//        tasklinks.print(); System.out.println();

        tasklinks.sample(rng, tasklinksPerIteration.intValue(), tasklink->{
            Term tt = tasklink.term();

            Task task = TaskLink.task(tasklink, nar);
            if (task == null)
                return true;


            Term src = tasklink.source();

            Term b;
            if (src instanceof Compound) {
                Concept cc = nar.conceptualize(src);
                if (cc != null) {
                    TermLinker linker = cc.linker();

                    linker.link(task, d);

                    if (cc.term().equals(tt) && ((!(linker instanceof FasterList) || rng.nextInt(((FasterList) linker).size()+1)==0)))
                        b = src;  //HACK
                    else
                        b = linker.sample(rng); //TODO for atoms

                } else {
                    b = src;
                }
            } else if (src.op().conceptualizable) {
                //scan active tasklinks for a match to the atom
                //TODO use Rank and sample
                ArrayHashSet<Term> atomMatches = d.atomMatches;
                tasklinks.forEach(t -> {
                    if (t!=null && t.source().equals(src)) {
                        Term bb = t.term();
                        if(!bb.equals(src) && !bb.equals(tt))
                            atomMatches.add(bb);
                    }
                });
                if (!atomMatches.isEmpty()) {
                    b = atomMatches.get(rng);
                    atomMatches.clear();
                } else {
                    b = src;
                }
            } else {
                b = src; //variable, int, etc..
            }

            if (b != null) {
                premises.add(new Premise(task, b));
            }

            return true;
        });

        return premises;
    }


}
