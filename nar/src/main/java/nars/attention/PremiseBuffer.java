package nars.attention;

import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.hijack.PriLinkHijackBag;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Task;
import nars.derive.Derivation;
import nars.derive.Premise;
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

    public static final int premiseBufferCapacity = 64;

    /** if < 0, removes from bag (but a copy remains in novelty bag so it can subtract from a repeat) */
    public float premiseSelectMultiplier = 0.5f;

    /** rate that priority from the novelty bag subtracts from potential premises */
    float notNovelCost = 0.5f;

    /** search rate */
    public float fillRate = 1.5f;

    public final Bag<Premise,PLink<Premise>> premise;

    /** tracks usage, the anti-premise novelty filter */
    public final Bag<Premise,PLink<Premise>> premiseTried;

    public PremiseBuffer(What.TaskLinkWhat taskLinkWhat) {
        this.taskLinkWhat = taskLinkWhat;

        this.premise =
                //new PLinkArrayBag<Premise>(PriMerge.max, premiseBufferCapacity);
                new PriLinkHijackBag<>(PriMerge.max, premiseBufferCapacity, 3);
        this.premiseTried =
                new PriLinkHijackBag<>(PriMerge.max, premiseBufferCapacity, 2);
    }

    /**
     * samples premises
     */
    public void hypothesize(int premisesPerIteration, int termlinksPerTaskLink, int matchTTL, int deriveTTL, TaskLinks links, Derivation d) {

        When<NAR> when = WhenTimeIs.now(d);


        taskLinkWhat.sample(d.random, (int) Math.max(1, Math.ceil(((float) premisesPerIteration) / termlinksPerTaskLink) * fillRate), tasklink -> {

            Task task = tasklink.get(when);
            if (task != null && !task.isDeleted()) {
                Term prevTerm = null;
                for (int i = 0; i < termlinksPerTaskLink; i++) {
                    Term term = links.term(tasklink, task, d);
                    if (term != null && (prevTerm == null || !term.equals(prevTerm))) {
                        PLink<Premise> l = new PLink<>(new Premise(task, term), tasklink.priPunc(task.punc()));
                        PLink<Premise> existing = premiseTried.put(l);
                        if (existing!=null && existing!=l)
                            l.priSub(existing.pri() * notNovelCost);
                        premise.put(l);
                    }
                    prevTerm = term;
                }
            }

            return true;

        });

        for (int i = 0; i < premisesPerIteration; i++) {
            PriReference<Premise> pp = premise.sample(d.random);
            if (pp == null)
                continue;

            Premise P = pp.get();

            if (premiseSelectMultiplier < 0)
                premise.remove(P);
            else
                pp.priMult(premiseSelectMultiplier);

            d.deriver.derive(P, d, matchTTL, deriveTTL);
        }

    }

    public void commit() {
        premise.commit();
        premiseTried.commit();
    }
}
