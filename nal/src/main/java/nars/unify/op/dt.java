//package nars.nal.meta.pre;
//
//import nars.concept.ConceptProcess;
//import nars.nal.Tense;
//import nars.nal.meta.AtomicBooleanCondition;
//import nars.nal.meta.PremiseMatch;
//import nars.task.Task;
//import nars.term.Compound;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import static nars.nal.Tense.ITERNAL;
//
///**
// * applies dt to the derived term according to premise terms
// */
//public abstract class dt extends AtomicBooleanCondition<PremiseMatch> {
//
//
//    public static final dt avg = new dt() {
//        @Override
//        protected boolean computeDT(Compound t, int tt, Compound b, int bt, @NotNull PremiseMatch m) {
//            if (tt != ITERNAL && bt != ITERNAL) {
//                int avg = (tt + bt) / 2;
//                //float diff = Math.abs(at - bt)/avg;
//                m.tDelta.set(avg);
//            }
//            return true;
//        }
//
//        @Override
//        protected boolean allowMixedTemporality() {
//            return true;
//        }
//
//        @Override
//        public String toString() {
//            return "dt(avg)";
//        }
//    };
//    public static final dt sum = new dt() {
//        @Override
//        protected boolean computeDT(Compound t, int tt, Compound b, int bt, @NotNull PremiseMatch m) {
//            if (tt != ITERNAL && bt != ITERNAL) {
//                int sum = (tt + bt);
//                m.tDelta.set(sum);
//            }
//            return true;
//        }
//
//        @Override
//        protected boolean allowMixedTemporality() {
//            return true;
//        }
//
//        @Override
//        public String toString() {
//            return "dt(sum)";
//        }
//    };
//
//    public static final dt sumNeg = new dt() {
//        @Override
//        protected boolean computeDT(Compound t, int tt, Compound b, int bt, @NotNull PremiseMatch m) {
//            if (tt != ITERNAL && bt != ITERNAL) {
//                int sum = (tt + bt);
//                m.tDelta.set(-sum);
//            }
//            return true;
//        }
//
//        @Override
//        protected boolean allowMixedTemporality() {
//            return true;
//        }
//
//        @Override
//        public String toString() {
//            return "dt(sumNeg)";
//        }
//    };
//    public static final dt task = new dt() {
//        @Override
//        protected boolean computeDT(Compound t, int tt, Compound b, int bt, @NotNull PremiseMatch m) {
//            m.tDelta.set(tt);
//            return true;
//        }
//
//        @Override
//        protected boolean allowMixedTemporality() {
//            return true;
//        }
//
//        @Override
//        public String toString() {
//            return "dt(task)";
//        }
//    };
//    public static final dt belief = new dt() {
//        @Override
//        protected boolean computeDT(Compound t, int tt, Compound b, int bt, @NotNull PremiseMatch m) {
//            m.tDelta.set(bt);
//            return true;
//        }
//
//        @Override
//        protected boolean allowMixedTemporality() {
//            return true;
//        }
//
//        @Override
//        public String toString() {
//            return "dt(belief)";
//        }
//    };
//    public static final dt exact = new dt() {
//        @Override
//        protected boolean computeDT(Compound t, int tt, Compound b, int bt, @NotNull PremiseMatch m) {
//            if (tt == bt) {
//                m.tDelta.set(tt);
//                return true;
//            }
//            return false;
//        }
//
//        @Override
//        public String toString() {
//            return "dt(exact)";
//        }
//    };
//
//    /** translates difference in task occurrence to temporal relation */
//    @Nullable
//    public static final dt occ = new dt() {
//        @Override
//        public boolean booleanValueOf(@NotNull PremiseMatch m) {
//            long at = m.premise.task().occurrence();
//            boolean ate = at == Tense.ETERNAL;
//
//            if (!ate) {
//                Task bb = m.premise.belief();
//                if (bb != null) {
//                    //return ate; //proceed only if task is eternal
//                    long bt = bb.occurrence();
//                    boolean bte = bt == Tense.ETERNAL;
//                    if (!bte) {
//                        int d = (int) (at - bt);
//                        m.tDelta.set(d);
//                        m.occDelta.set(-d);
//                    }
//                }
//            }
//
//            return true;
//        }
//
//        @Override
//        protected boolean computeDT(Compound t, int tt, Compound b, int bt, PremiseMatch m) {
//            throw new RuntimeException("N/A");
//        }
//
//        @Override
//        public String toString() {
//            return "dt(occ)";
//        }
//    };
//
//
////    public static final dt opposite = new dt() {
////        @Override protected boolean computeDT(Compound t, int tt, Compound b, int bt, @NotNull PremiseMatch m) {
////            if (tt == -bt) {
////                m.tDelta.set(tt);
////                return true;
////            }
////            return false;
////        }
////
////        @Override public String toString() {
////            return "dt(opposite)";
////        }
////    };
//
//    /**
//     * belief minus task
//     */
//    public static final dt bmint = new dt() {
//        @Override
//        protected boolean computeDT(Compound t, int tt, Compound b, int bt, @NotNull PremiseMatch m) {
//            if (tt != ITERNAL) {
//                m.tDelta.set(bt - tt);
//            }
//            return true;
//        }
//
//        @Override
//        public String toString() {
//            return "dt(bmint)";
//        }
//    };
//    /**
//     * task minus belief
//     */
//    public static final dt tminb = new dt() {
//        @Override
//        protected boolean computeDT(Compound t, int tt, Compound b, int bt, @NotNull PremiseMatch m) {
//            if (tt != ITERNAL) {
//                m.tDelta.set(tt - bt);
//            }
//            return true;
//        }
//
//        @Override
//        public String toString() {
//            return "dt(tminb)";
//        }
//    };
//
//
//    protected dt() {
//
//    }
//
//    @NotNull
//    @Override
//    abstract public String toString();
//
//    @Override
//    public boolean booleanValueOf(@NotNull PremiseMatch m) {
//        ConceptProcess p = m.premise;
//
//        Compound a = p.getTaskTerm();
//        int at = a.t();
//
//        Compound b = p.getBeliefCompound();
//        int bt = b != null ? b.t() : ITERNAL;
//
//        if (!allowMixedTemporality()) {
//            boolean aternal = (at == ITERNAL);
//            boolean bternal = (bt == ITERNAL);
//            if (aternal ^ bternal)
//                return false;
//        }
//
////        } else {
//        return computeDT(a, at, b, bt, m);
////        }
//    }
//
//    protected boolean allowMixedTemporality() {
//        return false;
//    }
//
//    protected abstract boolean computeDT(Compound t, int tt, Compound b, int bt, PremiseMatch m);
// }
