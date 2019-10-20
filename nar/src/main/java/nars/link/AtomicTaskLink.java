package nars.link;

import jcog.pri.op.PriReturn;
import jcog.signal.tensor.AtomicFixedPoint4x16bitVector;
import jcog.signal.tensor.WritableTensor;
import jcog.util.FloatFloatToFloatFunction;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;


public class AtomicTaskLink extends AbstractTaskLink {


    /**
     * tasklink endpoints are usualy the concept() form of a term
     * (which could differ from the input if temporal or not normalized)
     * */
    public static AtomicTaskLink link(Term source, Term target) {

        source = source.concept();
        target = target == null ? source :
            target instanceof Compound && target.op().conceptualizable ?
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
        clone.why = why;
        clone.invalidate();
        return clone;
    }

    @Override protected float priSum() {
        return punc.sumValues();
    }

    @Override
    protected float apply(int ith, float pri, FloatFloatToFloatFunction componentMerge, @Nullable PriReturn returning) {
        return punc.merge(ith, pri, componentMerge, returning);
    }

    @Override
    public final float priIndex(byte index) {
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
        Term f = from(), t = to();
        return toBudgetString() + ':' + punc + ' ' + (f.equals(t) ? f : (f + " " + t));
    }

    private AtomicTaskLink(Term source, Term target, int hash) {
        super(source, target, hash);
    }

    protected AtomicTaskLink(Term source, Term target) {
        super(source, target);
    }

    private final WritableTensor punc = new AtomicFixedPoint4x16bitVector(); //OR: new AtomicFloatArray(4);

    @Override
    public final Term term() {
        return from;
    }

}
