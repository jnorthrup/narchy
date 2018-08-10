package nars.concept.action;

import nars.NAR;
import nars.link.TermLinker;
import nars.table.dynamic.SignalBeliefTable;
import nars.task.ITask;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;


/**
 * ActionConcept which is driven by Goals that are interpreted into feedback Beliefs
 */
public class AsyncActionConcept extends ActionConcept {



    public AsyncActionConcept(Term c, TermLinker linker, NAR nar) {
        super(c, linker, nar);


//        Param.DEBUG  = true;
    }


//    @Override
//    public void add(Remember r, NAR n) {
//        Task t = r.input;
//        if (t.isGoal()) {
//
//            long range = t.range();
//            System.out.println(t + "\t" + n.time.timeString(range));
//            System.out.println();
//        }
//        super.add(r, n);
//    }

    @Override
    public float dexterity(long start, long end, NAR n) {
        Truth t = goals().truth(start, end, null, n);
        return t!=null ? t.conf() : 0;
    }

    @Override
    public void update(long prev, long now, long next, NAR nar) {
        //do nothing. managed elsewhere.  feedback will be invoked externally as well
    }


    @Nullable public ITask feedback(@Nullable Truth f, @Nullable Truth g, long now, long next, NAR nar) {

//        Task fg;
//        if (g != null) {
//            fg = null;
//        } else
//            fg = null;


        return ((SignalBeliefTable) beliefs()).add(f, now, next, this, nar);

    }


}
