package nars.derive.impl;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.math.IntRange;
import jcog.pri.bag.Bag;
import jcog.sort.TopN;
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
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Random;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;


/** buffers premises in batches*/
public class BatchDeriver extends Deriver {

    public final IntRange tasklinksPerIteration = new IntRange(2, 1, 32);


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
            Term tt = tasklink.target();

            Task task = TaskLink.task(tasklink, nar);
            if (task == null)
                return true;


            Term src = tasklink.source();

            Term b;
            if (src instanceof Compound) {
                Concept cc = nar.conceptualize(src);
                if (cc != null) {
                    TermLinker linker = cc.linker();

                    linker.link(tasklink, task, d);

                    if (cc.term().equals(tt) && ((!(linker instanceof FasterList) || rng.nextInt(((FasterList) linker).size()+1)==0)))
                        b = src;  //HACK
                    else
                        b = linker.sample(rng); //TODO for atoms

                } else {
                    b = src;
                }
            } else if (src.op().conceptualizable) {
                //scan active tasklinks for a match to the atom
                b =
                    //atomTangentRandom
                    atomTangentRanked
                            (src, tt, d.atomTangent, d, tasklinks);
            } else {
                b = src; //variable, int, etc.. ?
            }

            if (b != null) {
                Premise p = new Premise(task, b);
  //              System.out.println("  premise: " + p);
                premises.add(p);
            }

            return true;
        });

        return premises;
    }

    /** acts as a virtual tasklink bag associated with an atom concept allowing it to otherwise act as a junction between tasklinking compounds which share it */
    private static Term atomTangentRanked(Term src, Term tt, TopN<TaskLink> match, Derivation d, Bag<TaskLink,TaskLink> tasklinks) {

        //heuristic
        match.setCapacity(Util.clamp(tasklinks.size()/2, 1, match.capacityMax()));

        tasklinks.forEach((x)->{
            float xp = x.priElseZero();
            if (xp > match.minValueIfFull()) {
                Term bb = atomTangent(tt, src, x);
                if (bb != null)
                    match.add(x);
            }
        });

        if (!match.isEmpty()) {
            Term y = match.getRoulette(d.random).target();
            match.clear();
            return y;
        } else
            return null;
    }

//    private static Term atomTangentRandom(Term tt, Term src, Derivation d, Bag<TaskLink, TaskLink> tasklinks) {
//        Term b;//TODO use Rank and sample
//        ArrayHashSet<Term> atomMatches = d.atomMatches;
//        tasklinks.forEach(t -> {
//            Term bb = atomTangent(tt, src, t);
//            if (bb!=null)
//                atomMatches.add(bb);
//        });
//
//        if (!atomMatches.isEmpty()) {
//            b = atomMatches.get(d.random);
//            atomMatches.clear();
//        } else {
//            b = src;
//        }
//        return b;
//    }

    @Nullable
    static private Term atomTangent(Term tt, Term src, TaskLink t) {
        if (src.equals(t.source())) {
            Term y = t.target();
            if (!src.equals(y) && !tt.equals(y)) {
                return y;
            }
        }
        return null;
    }


}
