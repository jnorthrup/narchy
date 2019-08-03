package nars.derive.op;

import nars.$;
import nars.Op;
import nars.derive.PreDerivation;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import org.eclipse.collections.api.block.function.primitive.ByteToByteFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;

import static nars.Op.*;

public final class PuncMap extends AbstractPred<PreDerivation> {
    //private final BytePredicate taskPunc;

    private static final Atom U = (Atom) Atomic.the(PuncMap.class.getSimpleName());

    byte belief, goal, question, quest;


    public static PuncMap get(BytePredicate enable, ByteToByteFunction p) {
        return new PuncMap(
                p(enable, p, BELIEF),
                p(enable, p, GOAL),
                p(enable, p, QUESTION),
                p(enable, p, QUEST));
    }

    private static byte p(BytePredicate enable, ByteToByteFunction p, byte b) {
        return enable.accept(b) ? p.valueOf(b) : 0;
    }

    private PuncMap(byte belief, byte goal, byte question, byte quest) {
        super($.func(U, $.p(Op.puncAtom(belief), Op.puncAtom(goal), Op.puncAtom(question), Op.puncAtom(quest))));
        this.belief = belief; this.goal = goal; this.question = question; this.quest = quest;
    }

    public boolean all() {
        return belief!=0 && goal!=0 && question!=0 && quest!=0;
    }

    @Override
    public float cost() {
        return 0.003f;
    }

    @Override
    public boolean test(PreDerivation d) {
        return get(d.taskPunc)!=0;
    }

    public byte get(byte in) {
        byte y = 0;
        switch(in) {
            case BELIEF: y = belief; break;
            case GOAL: y = goal; break;
            case QUESTION: y = question; break;
            case QUEST: y = quest; break;
        }
        return y;
    }
}
