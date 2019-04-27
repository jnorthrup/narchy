//package nars.op.stm;
//
//import nars.$;
//import nars.NAR;
//import nars.Task;
//import CauseChannel;
//import nars.task.ITask;
//import nars.task.NALTask;
//import nars.target.Term;
//import nars.target.atom.Bool;
//import nars.truth.Truth;
//import nars.truth.TruthFunctions;
//import org.eclipse.collections.api.block.function.primitive.FloatFunction;
//import org.jetbrains.annotations.NotNull;
//
//import static nars.Op.BELIEF;
//import static nars.Op.SETe;
//
//public class RelationClustering extends ChainClustering {
//
//    private final CauseChannel<ITask> in;
//
//    public RelationClustering(@NotNull NAR nar, FloatFunction<Task> accept, int centroids, int capacity) {
//        super(nar, accept, centroids, capacity);
//        in = nar.newChannel(this);
//    }
//
//    @Override
//    protected void link(Task tx, Task ty) {
//        assert (tx.isBelief() && ty.isBelief());
//
//
//
//        String relation;
//        if (tx.intersects(ty.start(), ty.end())) {
//            relation = "simul";
//        } else if (ty.isAfter(tx.end(), dur / 2)) {
//            relation = "seq";
//        } else if (tx.isAfter(ty.end(), dur / 2)) {
//            Task z = tx;
//            tx = ty;
//            ty = z;
//            relation = "seq";
//        } else {
//            relation = null;
//        }
//
//        if (relation != null) {
//            Term x = tx.target();
//            Truth truX = tx.truth();
//            if (truX.isNegative()) {
//                x = x.neg();
//                truX = truX.neg();
//            }
//            Term y = ty.target();
//            Truth truY = ty.truth();
//            if (truY.isNegative()) {
//                y = y.neg();
//                truY = truY.neg();
//            }
//
//            if (x.volume() + y.volume() < nar.termVolumeMax.intValue() - 2) {
//                Truth tru = TruthFunctions.intersection(truX, truY, nar.confMin.floatValue());
//                if (tru == null)
//                    return;
//
//
//                Term t;
//                switch (relation) {
//                    case "simul":
//                        t = $.inh(SETe.the(x, y), $.the("simul"));
//                        break;
//                    case "seq":
//                        t = $.func(relation, x, y);
//                        break;
//                    default:
//                        throw new UnsupportedOperationException();
//                }
//
//                if (t instanceof Bool)
//                    return;
//
//                t = t.normalize();
//
//                long now = nar.time();
//                Task tt = new NALTask(t, BELIEF, tru, now, Math.min(tx.start(), ty.start()),
//                        Math.max(tx.end(), ty.end()), nar.evidence());
//                tt.pri(tx.priElseZero() * ty.priElseZero());
//                in.input(tt);
//            }
//        }
//    }
//
//}
