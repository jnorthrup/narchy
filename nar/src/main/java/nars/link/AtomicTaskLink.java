package nars.link;

import jcog.pri.op.PriMerge;
import jcog.pri.op.PriReturn;
import jcog.signal.tensor.AtomicFixedPoint4x16bitVector;
import jcog.signal.tensor.WritableTensor;
import jcog.util.FloatFloatToFloatFunction;
import nars.term.Term;


public final class AtomicTaskLink extends AbstractTaskLink {

    private final WritableTensor punc =
            new AtomicFixedPoint4x16bitVector();
            //new AtomicFloatArray(4);

    AtomicTaskLink(Term source, Term target) {
        super(source, target);
    }

    public AtomicTaskLink(Term self) {
        super(self);
    }

    public AtomicTaskLink(Term self, byte punc, float puncPri) {
        super(self);
        if (puncPri==puncPri && puncPri > Float.MIN_NORMAL)
            priMerge(punc, puncPri, PriMerge.replace);
    }

    public AtomicTaskLink(Term source, Term target, byte punc, float puncPri) {
        this(source, target);
        if (puncPri > Float.MIN_NORMAL)
            priMerge(punc, puncPri, PriMerge.replace);
    }

    @Override protected float priSum() {
        return punc.sumValues();
    }

    @Override
    protected float merge(int ith, float pri, FloatFloatToFloatFunction componentMerge, PriReturn returning) {
        return punc.merge(ith, pri, componentMerge, returning);
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
