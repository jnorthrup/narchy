//package nars.derive.pri;
//
//import jcog.Util;
//import jcog.data.list.FasterList;
//import jcog.pri.Prioritized;
//import jcog.pri.ScalarValue;
//import nars.Task;
//import nars.derive.Derivation;
//
///** wraps another DeriverBudgeting implementation and ensures that
// * the sum of the priorities of all a premise's derivation
// * dont exceed some function of the parent task priorities,
// * by normalizing the results in a batch
// */
//public class NormalizingDerivePri implements DerivePri {
//
//    /** delegates basic budgeting operations to this impl */
//    final DerivePri base;
//
//
//    protected static class Batch extends FasterList<Task> {
//        float premisePri = Float.NaN;
//
//        protected Batch() {
//            super(1024);
//        }
//
//        public void update(Derivation d) {
//            if (!isEmpty()) {
//                float pp = Util.numOr(premisePri, 0);
//                double totalPri = sumOfFloat(Prioritized::priElseZero);
//                float normalizeFactor = (totalPri > ScalarValue.EPSILON) ? (float) (pp / totalPri) : 0 /* zero */;
//                forEach(b -> b.priMult(normalizeFactor));
//                clear();
//            }
//
//            premisePri = d.parentPri();
//        }
//    }
//
//    final static ThreadLocal<Batch> batch = ThreadLocal.withInitial(Batch::new);
//
//    public NormalizingDerivePri(DerivePri base) {
//        this.base = base;
//    }
//
//    @Override
//    public void premise(Derivation d) {
//        batch.get().update(d);
//    }
//
//    @Override
//    public float pri(Task t, Derivation d) {
//        batch.get().add(t);
//        return base.pri(t, d);
//    }
//
//    @Override
//    public float prePri(Derivation d) {
//        return 1;
//    }
//}
