package nars.task.util;

import jcog.data.map.ConcurrentFastIteratingHashSet;
import jcog.data.set.ArrayHashSet;
import jcog.event.Off;
import jcog.util.ArrayUtil;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.control.NARPart;
import nars.derive.Deriver;
import nars.term.Term;
import nars.term.util.Image;
import nars.unify.SubUnify;

import java.util.Set;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;

public class DialogTask extends NARPart {

    final ConcurrentFastIteratingHashSet<Task> tasks = new ConcurrentFastIteratingHashSet<>(Task.EmptyArray);
    private final Deriver deriver;
    private final Off monitor;
    private final NAR nar;
    final Set<Term> unifyWith = new ArrayHashSet();

    public boolean add(Task t) {
        return tasks.add(t);
    }

    public DialogTask(NAR n, Task... input) {
        this.nar = n;

        assert (input.length > 0);
        boolean questions = false, quests = false;
        for (Task i : input) {
            if (add(i)) {
                unifyWith.add(i.term());
            }
            questions |= i.isQuestion();
            quests |= i.isQuest();
        }


        deriver = null; //TODO
//        deriver = BeliefSource.forConcepts(n, Derivers.nal(n, 1, 8),
//                tasks.asList().stream().map(t -> {
//
//                    nar.input(t);
//
//                    return nar.concept(t.term(), true);
//
//                }).collect(Collectors.toList()));

        byte[] listenPuncs;
        if (questions && quests)
            listenPuncs = new byte[] { BELIEF, GOAL };
        else if (questions && !quests)
            listenPuncs = new byte[] { BELIEF };
        else if (!questions && quests)
            listenPuncs = new byte[] { GOAL };
        else
            listenPuncs = ArrayUtil.EMPTY_BYTE_ARRAY;

        monitor = n.onTask(this::onTask, listenPuncs);

        n.start(this);

        add(deriver);
        finallyRun(monitor);
    }



//    protected void onTask(Collection<Task> x) {
//        x.removeIf(t -> !this.onTask(t));
//        nar.input(x);
//    }

    /**
     * return false to filter this task from input
     */
    protected boolean onTask(Task x) {

        Term xx = Image.imageNormalize( x.term() );
        Op xo = xx.op();

        SubUnify uu = new SubUnify(nar.random());
        for (Term u : unifyWith) {
            if (u.op()==xo) { //prefilter
                uu.clear();
                if (u.unify(xx, uu)) {
                    if (!onTask(x, u))
                        return false;
                }
            }
        }
        return true;
    }

    protected boolean onTask(Task x, Term unifiedWith) {
        return true;
    }

}
