package nars.link;

import jcog.TODO;
import jcog.signal.tensor.AtomicQuad16Vector;
import jcog.signal.tensor.WritableTensor;
import jcog.util.FloatFloatToFloatFunction;
import nars.term.Term;

import static nars.time.Tense.ETERNAL;


public final class AtomicTaskLink extends AbstractTaskLink {

    private final WritableTensor punc =
            new AtomicQuad16Vector();
            //new AtomicFloatArray(4);

    AtomicTaskLink(Term source, Term target) {
        super(source, target);
    }

    public AtomicTaskLink(Term self) {
        super(self);
    }

    public AtomicTaskLink(Term source, Term target, long when, byte punc, float pri) {
        this(source, target);
        if (when != ETERNAL) throw new TODO("non-eternal tasklink not supported yet");
        if (pri > Float.MIN_NORMAL)
            priMerge(punc, pri);
    }

    @Override protected float priSum() {
        return punc.sumValues();
    }

    @Override protected float merge(int ith, float pri, FloatFloatToFloatFunction componentMerge, boolean valueOrDelta) {
        return punc.merge(ith, pri, componentMerge, valueOrDelta);
    }

    @Override
    public float priIndex(byte index) {
        return punc.getAt(index);
    }

    @Override
    protected void fill(float pri) {
        punc.fill(pri);
        invalidate();
    }

    @Override
    public float[] priGet() {
        return punc.snapshot();
    }

    @Override
    public String toString() {
        return toBudgetString() + ' ' + from() + (punc) + ':' + to();
    }


}
