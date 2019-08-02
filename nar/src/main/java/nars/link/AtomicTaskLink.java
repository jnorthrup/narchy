package nars.link;

import jcog.pri.ScalarValue;
import jcog.pri.op.PriReturn;
import jcog.signal.tensor.AtomicFixedPoint4x16bitVector;
import jcog.signal.tensor.WritableTensor;
import jcog.util.FloatFloatToFloatFunction;
import nars.NAL;
import nars.Op;
import nars.task.util.TaskException;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.util.Image;
import nars.term.util.TermException;


public class AtomicTaskLink extends AbstractTaskLink {


    public static AtomicTaskLink link(Term source, Term target) {

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

        if (source instanceof Bool)
            throw new TermException("source bool", source);
//        if (target instanceof Bool)
//            throw new TermException("source bool", target);

        Op so = source.op();
        if (!so.taskable)
            throw new TaskException(source, "source term not taskable");
        if (!so.conceptualizable)
            throw new TaskException(source, "source term not conceptualizable");
//        if (NAL.DEBUG) {
//            if (!source.isNormalized())
//                throw new TaskException(source, "source term not normalized and can not name a task");
//        }

        return new AtomicTaskLink(source, target);
    }

    public static AtomicTaskLink link(Term source) {
        return link(source, null);
    }


    @Override
    @Deprecated public AtomicTaskLink clone(float priNormalize) {
        AtomicTaskLink l = new AtomicTaskLink(src, tgt, hash);
        ((AtomicFixedPoint4x16bitVector)l.punc).data(((AtomicFixedPoint4x16bitVector)this.punc).data());
        l.invalidate();
        float p = l.priElseZero();
        if (p - priNormalize > ScalarValue.EPSILON)
            l.priMult(priNormalize /p); //normalize
        return l;
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
        return toBudgetString() + ':' + punc + ' ' + from() + ' ' + to();
    }


    private AtomicTaskLink(Term source, Term target, int hash) {
        super(source, target, hash);
    }

    private AtomicTaskLink(Term source, Term target) {
        super(source, target);
    }

    protected AtomicTaskLink(Term self) {
        super(self, self);
    }


    private final WritableTensor punc =
            new AtomicFixedPoint4x16bitVector();
            //new AtomicFloatArray(4);
}
