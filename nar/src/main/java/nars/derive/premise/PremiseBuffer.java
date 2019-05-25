package nars.derive.premise;

import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.PLinkArrayBag;
import jcog.pri.bag.impl.hijack.PriLinkHijackBag;
import jcog.pri.op.PriMerge;
import nars.Task;
import nars.derive.model.Derivation;
import nars.link.TaskLink;
import nars.link.TaskLinks;
import nars.op.mental.Abbreviation;
import nars.term.Term;
import nars.time.When;

import java.io.Serializable;

/** premise buffer with novelty filter
 *  thread-safe; for cooperative use by multiple threads
 */
public class PremiseBuffer implements Serializable {

    static final int premiseBufferCapacity = 64;

    /** rate that priority from the novelty bag subtracts from potential premises.
     * may need to be divided by concurrency so that threads dont step on each other */
    float notNovelCost = 0.5f;

    /** search rate */
    public float fillRate = 1.5f;

    /** active premises for sampling */
    public final Bag<Premise,PriReference<Premise>> premise;

    /** tracks usage, the anti-premise novelty filter */
    public final Bag<Premise,PriReference<Premise>> premiseTried;

    //TODO premise blacklist to detect pointless derivations that shouldnt even be considered, per-Deriver

    public PremiseBuffer() {

        this.premise =
                new PLinkArrayBag<>(PriMerge.max, premiseBufferCapacity);
                //new PriLinkHijackBag<>(PriMerge.max, premiseBufferCapacity, 3);
        this.premiseTried =
                new PriLinkHijackBag<>(PriMerge.max, premiseBufferCapacity, 2);
    }

    /**
     * samples premises
     */
    public void derive(When when, int premisesPerIteration, int termlinksPerTaskLink, int matchTTL, int deriveTTL, TaskLinks links, Derivation d) {

        d.what.sample(d.random, (int) Math.max(1, Math.ceil((fillRate * premisesPerIteration) / termlinksPerTaskLink)), tasklink -> {

            Task task = tasklink.get(when);
            if (task != null && !task.isDeleted())
                hypothesize(tasklink, task, termlinksPerTaskLink, links, d);

            return true;

        });

        premise.sample(d.random, premisesPerIteration, pp->{
            fire(pp, matchTTL, deriveTTL, d);
        });

    }

    public void fire(PriReference<Premise> pp, int matchTTL, int deriveTTL, Derivation d) {
        Premise P = pp.get();
        PriReference<Premise> existing = premiseTried.put(pp);
        if (existing!=null && existing!=pp)
            pp.priSub(existing.pri() * notNovelCost);

        P.derive(d, matchTTL, deriveTTL);
    }

    void hypothesize(TaskLink tasklink, Task task, int termlinksPerTaskLink, TaskLinks links, Derivation d) {
        Term prevTerm = null;

        Task task2 = Abbreviation.unabbreviate(task, d.nar);
        if (task!=task2 && task2!=null) {
            if (task2.term().volume() <= ((float)(d.termVolMax/2))) {
                //System.out.println(task + " " + task2);
                task = task2; //use decompressed form if small enough
            } else {
                //remain compressed
                //System.out.println(task + " " + task2);
            }
        }

        float linkPri = 0;
        for (int i = 0; i < termlinksPerTaskLink; i++) {
            Term term = links.term(tasklink, task, d);
            if (term != null && (prevTerm == null || !term.equals(prevTerm))) {

//                term = Abbreviation.unabbreviate(term, d.nar);
//                if (term == null || term instanceof Bool)
//                    continue;

                if (i == 0)
                    linkPri = tasklink.priPunc(task.punc());

                Premise p = new Premise(task, term);

                this.premise.put(new PLink<>(p, linkPri));
            }
            prevTerm = term;
        }
    }

    public void commit() {
        premise.commit();
        premiseTried.commit();
    }
}
