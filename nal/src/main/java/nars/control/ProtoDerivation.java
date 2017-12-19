package nars.control;

import nars.Op;
import nars.Task;
import nars.term.Term;
import nars.term.subst.Unify;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

import static nars.time.Tense.ETERNAL;

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

    public Truth taskTruth, beliefTruth;

    /**
     * whether either the task or belief are events and thus need to be considered with respect to time
     */
    public boolean temporal;
    public boolean eternal;

    public ProtoDerivation(@Nullable Op type, Random random, int stackMax, int initialTTL) {
        super(type, random, stackMax, initialTTL);
    }

    public ProtoDerivation reset() {

        termutes.clear();

        this.task = this.belief = null;
        this.beliefTerm = null;
        this.beliefTruth = this.taskTruth = null;

        this.size = 0; //HACK instant revert to zero
        this.xy.map.clear(); //must also happen to be consistent



        return this;
    }
}
