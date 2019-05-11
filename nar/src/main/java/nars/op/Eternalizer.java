package nars.op;

import jcog.math.FloatRange;
import jcog.sort.FloatRank;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.attention.What;
import nars.control.channel.CauseChannel;
import nars.link.TaskLink;
import nars.table.BeliefTable;
import nars.term.Term;
import nars.time.When;

import java.util.Random;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;

public class Eternalizer extends LinkProcessor<Task> {

    private static final int cap = 16; //TODO IntRange
    public final FloatRange eviFactor = new FloatRange(1f, 0, 1);
    public final FloatRange priFactor = new FloatRange(0.5f, 0, 1);
    public final FloatRange noise = new FloatRange(0f, 0, 1);
    private final CauseChannel<Task> in;

    public Eternalizer(NAR nar) {
        super();
        in = nar.newChannel(this);
    }


    @Override
    public float value() {
        return in.value();
    }

    public boolean filter(Op op) {
        switch (op) {
            case IMPL:
            case SIM:
//            case INH:
            case CONJ:
                return true;
        }
        return false;
    }

    public boolean filter(Term t) {
        return t.hasAny(Op.ATOM); //prevent variable-only compounds
    }


    /**
     * TODO weight according to punct components
     */
    protected byte punc() {
        return
                //BELIEF;
                nar.random().nextBoolean() ? BELIEF : GOAL;
    }

    @Override
    protected Task apply(TaskLink x, When when) {
        Term xs = x.from();
        if (filter(xs.op()) && filter(xs.term())) {
            Task t = x.get(punc(), when, (tt) -> !tt.isEternal());

            if (t == null || t.isInput())
                return null;

            assert !t.isEternal();

            return t;
        } else
            return null;
    }

    @Override
    public FloatRank<Task> score() {
        Random rng = nar.random();
        return (t, min) -> {
            float base = (float) (BeliefTable.eternalTaskValueWithOriginality(t) * ((1 / (1f + Math.sqrt(t.complexity())))));
            //* (t.originality()) //polarity()
            float noise = this.noise.floatValue();
            return base + (noise > 0 ? noise * ((rng.nextFloat() - 0.5f) * 2) : 0);
        };
    }

    @Override
    public int cap() {
        return cap;
    }

    @Override
    protected void run(Task t, What w) {
        //best.forEach(t->{
//            try {
        Task u = Task.eternalized(t, eviFactor.floatValue(), nar.confMin.floatValue(), nar);
        if (u != null) {
            u.priMult(priFactor.floatValue());
            //System.out.println(u);
            in.accept(u, w);
        }
//            } catch (TermException e) {
//                if (Param.DEBUG)
//                    e.printStackTrace();
//                //ex: nars.term.util.TermException: TermException: invalid conjunction factorization {null, dt=-2147483648, args=[(((--,left)&|right) &&+44 (--,rotate)), ((--,left)&|(--,right))]}
//            }
        //});
    }

    @Override
    protected Task[] newArray(int size) {
        return new Task[size];
    }

}
