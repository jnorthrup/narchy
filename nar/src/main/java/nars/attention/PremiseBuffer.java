package nars.attention;

import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.PLinkArrayBag;
import jcog.pri.bag.impl.hijack.PriLinkHijackBag;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Task;
import nars.derive.Derivation;
import nars.derive.Premise;
import nars.link.TaskLink;
import nars.link.TaskLinks;
import nars.term.Term;
import nars.time.When;
import nars.time.event.WhenTimeIs;

import java.io.Serializable;

/** premise buffer with novelty filter
 *  thread-safe; for cooperative use by multiple threads
 */
public class PremiseBuffer implements Serializable {
    private final What.TaskLinkWhat taskLinkWhat;

    public static final int premiseBufferCapacity = 128;

//    /** if < 0, removes from bag (but a copy remains in novelty bag so it can subtract from a repeat) */
//    public float premiseSelectMultiplier = 0.5f;

    /** rate that priority from the novelty bag subtracts from potential premises.
     * may need to be divided by concurrency so that threads dont step on each other */
    float notNovelCost = 0.5f;

    /** search rate */
    public float fillRate = 1f;

    /** active premises for sampling */
    public final Bag<Premise,PriReference<Premise>> premise;

    /** tracks usage, the anti-premise novelty filter */
    public final Bag<Premise,PriReference<Premise>> premiseTried;

    //TODO premise blacklist to detect pointless derivations that shouldnt even be considered, per-Deriver

    public PremiseBuffer(What.TaskLinkWhat taskLinkWhat) {
        this.taskLinkWhat = taskLinkWhat;

        this.premise =
                new PLinkArrayBag<>(PriMerge.max, premiseBufferCapacity);
                //new PriLinkHijackBag<>(PriMerge.max, premiseBufferCapacity, 3);
        this.premiseTried =
                new PriLinkHijackBag<>(PriMerge.max, premiseBufferCapacity, 2);
    }

    /**
     * samples premises
     */
    public void derive(int premisesPerIteration, int termlinksPerTaskLink, int matchTTL, int deriveTTL, TaskLinks links, Derivation d) {

        When<NAR> when = WhenTimeIs.now(d);

        taskLinkWhat.sample(d.random, (int) Math.max(1, Math.ceil(((float) premisesPerIteration) / termlinksPerTaskLink) * fillRate), tasklink -> {

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

        d.deriver.derive(P, d, matchTTL, deriveTTL);
    }

    void hypothesize(TaskLink tasklink, Task task, int termlinksPerTaskLink, TaskLinks links, Derivation d) {
        Term prevTerm = null;

        float linkPri = 0;
        for (int i = 0; i < termlinksPerTaskLink; i++) {
            Term term = links.term(tasklink, task, d);
            if (term != null && (prevTerm == null || !term.equals(prevTerm))) {
                if (i == 0)
                    linkPri = tasklink.priPunc(task.punc());

                PLink<Premise> l = new PLink<>(new Premise(task, term), linkPri);
                premise.put(l);
            }
            prevTerm = term;
        }
    }

    public void commit() {
        premise.commit();
        premiseTried.commit();
    }
}
