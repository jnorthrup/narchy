//package nars.op;
//
//import jcog.event.Off;
//import jcog.pri.ScalarValue;
//import nars.NAR;
//import nars.Op;
//import nars.Task;
//import nars.control.NARPart;
//import nars.link.TaskLink;
//import nars.subterm.Subterms;
//import nars.term.Term;
//
//import java.util.function.Consumer;
//
//import static nars.Op.BELIEF;
//
///** TODO adapt into a How */
//public class StatementLinker extends NARPart implements Consumer<Task> {
//    private Off off;
//
//    public StatementLinker(NAR n) {
//        super(n);
//    }
//
//
//    public boolean include(Op o) {
//        switch (o) {
//            case IMPL:
//            case SIM:
//                return true;
//        }
//        return false;
//    }
//
//    @Override
//    public void accept(Task task) {
//        Term t = task.term();
//        if (include(t.op())) {
//            float pri = task.pri() * task.polarity()
//                    //    * task.conf()
//                    ;
//            if (pri > ScalarValue.EPSILON) {
//                Subterms tt = t.subterms();
//                Term a = tt.sub(0);
//                if (a.op().taskable) {
//                    Term b = tt.sub(1);
//                    if (b.op().taskable) {
//                        if (!a.equals(b)) {
//                            Term subj = a.concept(), pred = b.concept();
//                            if ((a == subj && b == pred) || !subj.equals(pred)) {
//
//                                nar.attn.link(
//                                    TaskLink.tasklink(subj, pred, BELIEF, pri / 2),
//                                    TaskLink.tasklink(pred, subj, BELIEF, pri / 2)
//                                );
//
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    @Override
//    protected void starting(NAR nar) {
//        super.starting(nar);
//        off = nar.onTask(this, BELIEF);
//    }
//
//    @Override
//    protected void stopping(NAR nar) {
//        off.pause();
//        off = null;
//    }
//
//}
