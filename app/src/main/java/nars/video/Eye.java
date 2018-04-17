//package nars.video;
//
//import jcog.pri.Prioritized;
//import jcog.signal.Bitmap2D;
//import jcog.sort.CachedTopN;
//import nars.*;
//import nars.control.channel.CauseChannel;
//import nars.exe.UniExec;
//import nars.index.concept.HijackConceptIndex;
//import nars.op.stm.ConjClustering;
//import nars.task.ITask;
//import nars.task.TaskProxy;
//import nars.task.signal.SignalTask;
//import nars.term.Term;
//import nars.util.signal.Bitmap2DSensor;
//
//import static nars.Op.BELIEF;
//
///** prototype active peripheral for 2d scalar spaces */
//public class Eye extends SubNAR {
//
//    public Eye(NAR superNAR, Bitmap2D source) {
//        super(superNAR, (superr)->{
//
//
//            NAR sub =
//                new NARS()
//                    .exe(new UniExec(64))
//                    .index(new HijackConceptIndex(32*1024,4))
//                    .time(superNAR.time)
//                    .deriverAdd(1, 8)
//                    .get();
//
//
//            sub.termVolumeMax.set(30);
//            sub.freqResolution.set(0.04f);
//            sub.confResolution.set(0.04f);
//
//
//            {
//                int centroids = 8;
//                int cap = 64;
//                new ConjClustering(sub, BELIEF, (x) -> true, centroids, cap);
//            }
//
//            final CachedTopN<Task> top = new CachedTopN<>(new Task[64], Prioritized::priElseNeg1);
//
//            sub.onTask(t->{
//               if (t.isBelief() && !(t instanceof SignalTask))
//                   top.add(t);
//            });
//
//            final Term ID = $.the("eye");
//            CauseChannel<ITask> superIn = superNAR.newChannel(sub);
//            final short[] cause = new short[] { superIn.id };
//
//
//            Bitmap2DSensor retina = new Bitmap2DSensor(
//                    (Term)null, source, sub);
//
//
//            sub.onOp("see", (t, n)->{
//                retina.input();
//            });
//
//            sub.onOp("blink", (t, n)->{
//                superIn.input(top.drain(top.size()).stream().map(x ->
//                        new TaskProxy.WithTerm($.inh(x.term(),ID), x) {
//                            @Override public short[] cause() {
//                                return cause;
//                            }
//                        }
//                ));
//                //top.capacity..
//            });
//
//            return sub;
//        });
//
//        onDur(this::update);
//    }
//
//
//
//    private void update(NAR superNAR, NAR subNAR) {
//
//        try {
//            subNAR.input("see();");
//            subNAR.run(4);
//            subNAR.input("blink();");
//            subNAR.run(1);
//        } catch (Narsese.NarseseException e) {
//            e.printStackTrace();
//        }
//    }
//}
