package nars.derive.action.op;

import nars.$;
import nars.Op;
import nars.derive.Derivation;
import nars.term.Term;
import nars.term.Terms;
import nars.term.Variable;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;

/**
 * premise unification step
 */
abstract public class PremiseUnify extends AbstractPred<Derivation> {


    public final Term taskPat, beliefPat;

    /** +1 task first, -1 belief first, 0 unimportant (can decide dynamically from premise) */
    final int order;

    public final Taskify taskify;

    public PremiseUnify(Term taskPat, Term beliefPat, Taskify taskify) {
        super($.func(UNIFY, $.p(taskPat, beliefPat), taskify.ref));
        this.taskPat = taskPat;
        this.beliefPat = beliefPat;
        this.order = fwd(taskPat, beliefPat);
        this.taskify = taskify;
    }

    protected final boolean unify(Derivation d, boolean dir, boolean finish) {

        if (finish) {
            d.termifier.set(taskify);
        }

        return d.unify(dir ? taskPat : beliefPat, dir ? d.taskTerm : d.beliefTerm, finish);
    }


    private static final Atomic UNIFY = $.the("unify");

    /** task,belief or belief,task ordering heuristic
     *  +1 = task first, -1 = belief first, 0 = doesnt matter
     **/
    protected static int fwd(Term T, Term B) {

        if (T.equals(B))
            return 0;

        //if one is a variable, match the other since it will be more specific and fail faster
        if (T instanceof Variable && B instanceof Variable) return 0;
        if (B instanceof Variable) return +1;
        if (T instanceof Variable) return -1;

        //match ellipsis-containing term last
        boolean te = Terms.hasEllipsisRecurse(T), be = Terms.hasEllipsisRecurse(B);
        if (te || be) {
            if (te && !be) return +1;
            else if (!te && be) return -1;
        }


        //first if one is contained recursively by the other
        boolean Tb = T.containsRecursively(B);
        boolean Bt = B.containsRecursively(T);
        if (Tb && !Bt) return -1; //belief first as it is a part of Task
        if (Bt && !Tb) return +1; //task first as it is a part of Belief

        // first which is more specific in its constant structure
        int taskBits = Integer.bitCount(T.structure() & ~Op.Variable);
        int belfBits = Integer.bitCount(B.structure() & ~Op.Variable);
        if (belfBits > taskBits) return  -1;
        if (taskBits > belfBits) return +1;

        //first which has fewer variables
        if (T.varPattern() > B.varPattern()) return -1;
        if (B.varPattern() > T.varPattern()) return +1;

        //first which is smaller
        if (T.volume() > B.volume()) return -1;
        if (B.volume() > T.volume()) return +1;

        return 0;
    }

    /** true: task first, false: belief first */
    protected boolean fwd(Derivation d) {
        switch (order) {
            case +1: return true;
            case -1: return false;
            default:
                /* decide dynamically according to heuristic function of the premise values */

                return true;

//                int taskVol = d.taskTerm.volume();
//                int beliefVol = d.beliefTerm.volume();
//                if (taskVol > beliefVol)
//                    return false;
//                if (taskVol < beliefVol)
//                    return true;

                //return d.random.nextBoolean();
        }
    }
}
