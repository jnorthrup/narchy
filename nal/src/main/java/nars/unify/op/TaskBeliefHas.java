//package nars.unify.op;
//
//import nars.$;
//import nars.Op;
//import nars.derive.Derivation;
//import nars.derive.premise.PreDerivation;
//import nars.term.atom.Atomic;
//import nars.term.control.AbstractPred;
//import nars.term.control.PrediTerm;
//
//import java.util.Collections;
//import java.util.List;
//
//import static nars.Op.VAR_PATTERN;
//
///**
// * requires a specific subterm to have minimum bit structure
// */
//public final class TaskBeliefHas extends AbstractPred<PreDerivation> {
//
//    /** higher number means a stucture with more enabled bits will be decomposed to its components */
//    public static final int SPLIT_THRESHOLD = 3;
//
//    public final boolean taskOrBelief;
//    public final int struct;
//    private final boolean inclOrExclude;
//
//    public static List<TaskBeliefHas> get(boolean taskOrBelief, int struct, boolean inclOrExcl) {
//
//
//        struct &= ~VAR_PATTERN.bit;
//
//        int numBits = Integer.bitCount(struct);
//        assert (numBits > 0);
//        if ((numBits == 1) || (numBits > SPLIT_THRESHOLD)) {
//            return Collections.singletonList(new TaskBeliefHas(taskOrBelief, struct, inclOrExcl));
//        } else {
//            List<TaskBeliefHas> components = $.newArrayList(numBits);
//            for (Op o : Op.values()) {
//
//
//                int b = o.bit;
//                if ((struct & b) != 0) {
//                    components.add(new TaskBeliefHas(taskOrBelief, b, inclOrExcl));
//                }
//            }
//            return components;
//        }
//    }
//
//    final static Atomic has = Atomic.the("has");
//
//    private TaskBeliefHas(boolean taskOrBelief, int struct, boolean includeOrExclude) {
//        super($.func(has, Op.strucTerm(struct),
//                taskOrBelief ? Derivation.Task : Derivation.Belief
//                ).negIf(!includeOrExclude)
//        );
//
//        this.inclOrExclude = includeOrExclude;
//        this.struct = struct;
//        this.taskOrBelief = taskOrBelief;
//
//        assert(this.struct > 0): "no filter effected";
//    }
//
//    @Override
//    public boolean remainInAND(PrediTerm[] p) {
//        boolean subsumed = false;
//        for (PrediTerm x : p) {
//            if (x==this) continue;
//            if (x instanceof SubtermMatch) {
//                //the path subterm op test is more specific, so remove this if it covers this
//                SubtermMatch t = (SubtermMatch) x;
//                byte[] target = taskOrBelief ? t.pathInTask : t.pathInBelief;
//                if (target != null) {
//                    subsumed |= testSubsumption(t.trueOrFalse, t.struct);
//                }
//            }
//
//            if (x instanceof TaskBeliefMatch) {
//                //is is more specific than has, so remove this if it covers this
//
//                TaskBeliefMatch t = (TaskBeliefMatch)x;
//                if (((taskOrBelief && t.task) || (!taskOrBelief && t.belief))) {
//                    subsumed |= testSubsumption(t.trueOrFalse, t.struct);
//                }
//            }
//        }
//        return !subsumed;
//    }
//
//    public boolean testSubsumption(boolean isOrIsnt, int otherStruct) {
//        boolean subsumed = false;
//        if (isOrIsnt == inclOrExclude) {
//            if ((otherStruct | struct) == otherStruct) {
//                subsumed = true;
//            }
//        } else {
//            if ((otherStruct & struct) != 0)
//                throw new RuntimeException("conflict");
//        }
//        return subsumed;
//    }
//
//    @Override
//    public final boolean test(PreDerivation d) {
//        return inclOrExclude == Op.hasAll(
//                taskOrBelief ? d._taskStruct : d._beliefStruct,
//                struct);
//    }
//
//
//    @Override
//    public float cost() {
//        return 0.25f;
//    }
//}
