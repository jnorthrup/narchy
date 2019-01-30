package nars.derive.impl;

import jcog.data.list.FasterList;
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
import nars.term.Term;
import nars.term.atom.Atom;

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

//            if (!d.nar.exe.concurrent()) {
//                hypothesize(d).asParallel(ForkJoinPool.commonPool(), 2).forEach(p -> {
//                    p.derive(/* HACK */ Deriver.derivation.get().next(nar, this), matchTTL, deriveTTL);
//                });
//            } else {
            for (Premise p : hypothesize(d))
                p.derive(d, matchTTL, deriveTTL);
//            }

        } while (kontinue.getAsBoolean());

    }


    /**
     * forms premises
     */
    private Collection<Premise> hypothesize(Derivation d) {

        Collection<Premise> premises = d.premiseBuffer;
        premises.clear();

        int links = tasklinksPerIteration.intValue();

        nar.emotion.conceptFire.increment();


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

        tasklinks.sample(rng, links, tasklink->{
            Term tt = tasklink.term();

            Task task = TaskLink.task(tasklink, nar);
            if (task == null)
                return true;


            Term src = tasklink.source();

            Term b;
            if (!(src instanceof Atom)) {
                Concept cc = nar.conceptualize(src);
                if (cc != null) {
                    TermLinker linker = cc.linker();

                    d.tasksFired.add(task);
                    linker.link(d);
                    d.tasksFired.clear();


                    if (!(linker instanceof FasterList) || rng.nextInt(((FasterList) linker).size()+1)==0) //HACK
                        b = src;
                    else
                        b = linker.sample(rng); //TODO for atoms
//                    }
                } else {
                    b = src;
                }
            } else {
                //scan active tasklinks for a match to the atom
                b = src;
                for (TaskLink t : tasklinks) {

                    if (t!=null && t.source().equals(src)) {
                        b = t.term();
                        if(!b.equals(tt))
                            break;
                    }

                }
            }

//                do {


                    if (b != null && premises.add(new Premise(task, b))) {
//                        p++;
                    }
//                } while (--premisesPerTaskLinkTried > 0);





            return true;
        });

        return premises;
    }


}
