package nars.derive.util;

import nars.$;
import nars.Op;
import nars.Task;
import nars.derive.PreDerivation;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import org.eclipse.collections.api.block.function.primitive.ByteToByteFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import static nars.Op.*;

/** tests premise Task punctuation */
public final class PuncMap extends AbstractPred<PreDerivation> {
    //private final BytePredicate taskPunc;

    public static final byte FALSE = (byte) 0;
    public static final byte TRUE = (byte) 1;
    public static final PuncMap All = new PuncMap(TRUE, TRUE, TRUE, TRUE, TRUE);

    final byte belief;
    final byte goal;
    final byte question;
    final byte quest;
    final byte command;

    public static byte p(BytePredicate enable, ByteToByteFunction p, byte b) {
        return enable.accept(b) ? p.valueOf(b) : (byte) 0;
    }

    public PuncMap(boolean belief, boolean goal, boolean question, boolean quest) {
        this((byte)(belief ? 1 : 0), (byte)(goal ? 1: 0), (byte)(question ? 1 : 0), (byte)(quest ? 1 : 0), (byte)0);
    }

    public PuncMap(byte belief, byte goal, byte question, byte quest, byte command) {
        super(id(belief, goal, question, quest, command));
        this.belief = belief;
        this.goal = goal;
        this.question = question;
        this.quest = quest;
        this.command = command;
    }



    private static Term id(byte belief, byte goal, byte question, byte quest, byte command) {
        Atom PUNC = Atomic.atom("punc");
        if ((int) belief != 0 && (int) goal != 0&& (int) question != 0 && (int) quest != 0 && (int) command != 0) {
            return PUNC;
        } else {
            java.util.Set<Term> s = new UnifiedSet(5);
            if ((int) belief !=0) s.add(idTerm(Task.BeliefAtom, belief));
            if ((int) goal !=0) s.add(idTerm(Task.GoalAtom, goal));
            if ((int) question !=0) s.add(idTerm(Task.QuestionAtom, question));
            if ((int) quest !=0) s.add(idTerm(Task.QuestAtom, quest));
            if ((int) command !=0) s.add(idTerm(Task.CommandAtom, command));
            //HACK
            Term tt = s.size() == 1 ? s.iterator().next() : SETe.the(s);

            return $.INSTANCE.func(PUNC, tt);
        }
    }

    private static Term idTerm(Atom puncAtom, byte value) {
        switch (value) {
            case 0:
                return puncAtom.neg();
            case 1:
                return puncAtom;
            default:
                return $.INSTANCE.p(puncAtom, Op.puncAtom(value));
        }
    }

    public boolean all() {
        return (int) belief !=0 && (int) goal !=0 && (int) question !=0 && (int) quest !=0 && (int) command !=0;
    }

    @Override
    public float cost() {
        return 0.003f;
    }

    @Override
    public boolean test(PreDerivation d) {
        return (int) get(d.taskPunc) !=0;
    }

    public final byte get(byte in) {
        byte y;
        switch(in) {
            case BELIEF: y = belief; break;
            case GOAL: y = goal; break;
            case QUESTION: y = question; break;
            case QUEST: y = quest; break;
            case COMMAND: y = command; break;
            default: y = (byte) 0; break;
        }
        return y;
    }

    /** any output punctuation match */
    final boolean outAny(byte p) {
        return (int) belief == (int) p || (int) goal == (int) p || (int) question == (int) p || (int) quest == (int) p || (int) command == (int) p;
    }
}
