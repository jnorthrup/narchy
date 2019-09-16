package nars.derive.op;

import nars.$;
import nars.Op;
import nars.derive.Derivation;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;

/**
 *
 */
abstract public class Premisify extends AbstractPred<Derivation> {


    public final Term taskPat, beliefPat;
    final boolean fwd;

    private static final Atomic FWD = Atomic.the("fwd");
    private static final Atomic REV = Atomic.the("rev");
    public final Taskify taskify;


    public Premisify(Term taskPat, Term beliefPat, Taskify taskify) {
        this(taskPat, beliefPat, isFwd(taskPat, beliefPat), taskify);
    }

    public Premisify(Term taskPat, Term beliefPat, boolean fwd, Taskify taskify) {
        super($.func(UNIFY, $.p(taskPat, beliefPat), fwd ? FWD : REV));
        this.taskPat = taskPat;
        this.beliefPat = beliefPat;
        this.fwd = fwd;
        this.taskify = taskify;
    }


    protected final boolean unify(Derivation d, boolean dir, boolean finish) {

        if (finish) {
            UnifyMatchFork mf = d.termifier;
            d.forEachMatch = mf;
            mf.reset(taskify);
        }

        return d.unify(dir ? taskPat : beliefPat, dir ? d.taskTerm : d.beliefTerm, finish);
    }


    private static final Atomic UNIFY = $.the("unify");

    /** task,belief or belief,task ordering heuristic */
    protected static boolean isFwd(Term T, Term B) {

        int dir = 0; //-1 !fwd, 0=undecided yet, +1 = fwd

        if (T.equals(B)) {
            dir = +1; //equal, so use convention
        }

        if (dir == 0) {
            //match ellipsis first. everything else will basically depend on this
            boolean te = Terms.hasEllipsisRecurse(T), be = Terms.hasEllipsisRecurse(B);
            if (te || be) {
                if (te && !be) dir = +1;
                else if (!te && be) dir = -1;
            }
        }

        if (dir == 0) {
            //first if one is contained recursively by the other
            boolean Tb = T.containsRecursively(B);
            boolean Bt = B.containsRecursively(T);
            if (Tb && !Bt) dir = -1; //belief first as it is a part of Task
            if (Bt && !Tb) dir = +1; //task first as it is a part of Belief
        }


        if (dir == 0) {
            //first which has fewer variables
            if (T.varPattern() > B.varPattern()) dir = -1;
            if (B.varPattern() > T.varPattern()) dir = +1;
        }

        if (dir == 0) {
            // first which is more specific in its constant structure
            int taskBits = Integer.bitCount(T.structure() & ~Op.Variable);
            int belfBits = Integer.bitCount(B.structure() & ~Op.Variable);
            if (belfBits > taskBits) dir = -1;
            if (taskBits > belfBits) dir = +1;
        }


        if (dir == 0) {
            //first which is smaller
            if (T.volume() > B.volume()) dir = -1;
            if (B.volume() > T.volume()) dir = +1;
        }

        return dir >= 0; //0 or +1 = fwd (task first), else !fwd (belief first)
    }

}
