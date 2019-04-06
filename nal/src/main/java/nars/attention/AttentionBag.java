package nars.attention;

import jcog.pri.PriBuffer;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.op.PriMerge;
import nars.term.Term;

/** bag composing a weighted set of Attention's that can be reprioritized and sampled */
public class AttentionBag extends ArrayBag<Term, Attention> {
    public AttentionBag() {
        super(PriMerge.replace, 64, PriBuffer.newMap(false));
    }

    @Override
    public float pri(Attention value) {
        return value.pri();
    }

    @Override
    public Term key(Attention value) {
        return value.id;
    }
}
