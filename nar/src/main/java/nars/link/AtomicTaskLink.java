package nars.link;

import jcog.pri.ScalarValue;
import jcog.pri.op.PriMerge;
import jcog.pri.op.PriReturn;
import jcog.signal.tensor.AtomicFixedPoint4x16bitVector;
import jcog.signal.tensor.WritableTensor;
import jcog.util.FloatFloatToFloatFunction;
import nars.NAL;
import nars.term.Compound;
import nars.term.Term;
import nars.term.util.Image;

import static nars.Task.i;


public class AtomicTaskLink extends AbstractTaskLink {


    public static AtomicTaskLink link(Term source, Term target) {

//        if ((target instanceof Variable || target instanceof Int) && !source.containsRecursively(target))
//            throw new WTF();

        if (NAL.TASKLINK_NORMALIZE_IMAGES) {
            source = Image.imageNormalize(source);
            if (target instanceof Compound)
                target = Image.imageNormalize(target);
        }

        source = source.concept();
        target = target == null ? source /* loop */ :
            NAL.TASKLINK_TARGET_CONCEPT && target instanceof Compound && target.op().conceptualizable ?
                    target.concept()
                    :
                    target
        ;



        return new AtomicTaskLink(source, target);
    }

    public static AtomicTaskLink link(Term source) {
        return link(source, null);
    }



    @Override
    public AtomicTaskLink clone() {
        AtomicTaskLink clone = new AtomicTaskLink(from, to, hash);
        ((AtomicFixedPoint4x16bitVector)clone.punc).data(((AtomicFixedPoint4x16bitVector)this.punc).data());
        clone.invalidate();
        return clone;
    }


    @Override protected float priSum() {
        return punc.sumValues();
    }

    @Override
    protected float apply(int ith, float pri, FloatFloatToFloatFunction componentMerge, PriReturn returning) {
        return punc.merge(ith, pri, componentMerge, returning);
    }

    @Override
    public float priIndex(byte index) {
        return punc.getAt(index);
    }

    @Override
    public void transfer(TaskLink from, float factor, float sustain, byte punc) {

        byte i = i(punc);

        float xfer = (1-sustain) * merge(i, factor * from.priIndex(i), PriMerge.plus, PriReturn.Delta);
        if (xfer >= ScalarValue.EPSILON)
            ((AtomicTaskLink)from).merge(i, -xfer, PriMerge.plus, PriReturn.Delta);

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
        Term f = from(), t = to();
        return toBudgetString() + ':' + punc + ' ' + (f.equals(t) ? f : (f + " " + t));
    }

    private AtomicTaskLink(Term source, Term target, int hash) {
        super(source, target, hash);
    }

    protected AtomicTaskLink(Term source, Term target) {
        super(source, target);
    }

    protected AtomicTaskLink(Term self) {
        super(self, self);
    }

    @Override
    public float take(float pct) {
        float taken = 0;
        for (byte i = 0; i < 4; i++) {
            taken += punc.merge(i, pct, mult, PriReturn.Delta);
        }
        invalidate();
        return -taken;
    }

    private final WritableTensor punc =
            new AtomicFixedPoint4x16bitVector();
            //new AtomicFloatArray(4);
}
