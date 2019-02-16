package nars.op;

import jcog.event.Off;
import jcog.pri.ScalarValue;
import nars.NAR;
import nars.Task;
import nars.control.NARService;
import nars.link.TaskLink;
import nars.subterm.Subterms;
import nars.term.Term;

import java.util.function.Consumer;

import static nars.Op.BELIEF;
import static nars.Op.IMPL;

public class ImpLinker extends NARService implements Consumer<Task> {
    private Off off;

    public ImpLinker(NAR n) {
        super(n);
    }

    @Override
    protected void starting(NAR nar) {
        super.starting(nar);
        off = nar.onTask(this, BELIEF);
    }

    @Override
    protected void stopping(NAR nar) {
        off.off();
        off = null;
    }

    @Override
    public void accept(Task task) {
        Term t = task.term();
        if (t.op()==IMPL) {
            float pri = task.pri() * task.polarity()
                //    * task.conf()
            ;
            if (pri > ScalarValue.EPSILON) {
                Subterms tt = t.subterms();
                Term a = tt.sub(0);
                Term b = tt.sub(1);
                if (!a.equals(b)) {
                    Term subj = a.concept();
                    Term pred = b.concept();
                    if (!subj.equals(pred)) {
                        TaskLink.link(
                                TaskLink.tasklink(subj, pred, BELIEF, pri/2),
                                nar
                        );
                        TaskLink.link(
                                TaskLink.tasklink(pred, subj, BELIEF, pri/2),
                                nar
                        );
                    }
                }
            }
        }
    }
}
