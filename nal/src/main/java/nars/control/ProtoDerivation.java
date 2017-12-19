package nars.control;

import nars.Op;
import nars.Task;
import nars.term.Term;
import nars.term.subst.Unify;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/** contains only information which depends on the premise itself (Task, Belief, BeliefTerm) */
public abstract class ProtoDerivation extends Unify {
    /**
     * op ordinals: 0=task, 1=belief
     */
    public byte taskOp;
    public byte beliefOp;
    /**
     * structs, 0=task, 1=belief
     */
    public int termSub0Struct;
    public int termSub1Struct;
    public Term taskTerm;
    public Term beliefTerm;
    public Task task;
    public Task belief;
    public byte taskPunct;
    /**
     * whether either the task or belief are events and thus need to be considered with respect to time
     */
    public boolean temporal;
    public boolean eternal;

    public ProtoDerivation(@Nullable Op type, Random random, int stackMax, int initialTTL) {
        super(type, random, stackMax, initialTTL);
    }
}
