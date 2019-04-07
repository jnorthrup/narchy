package nars.attention;

import jcog.pri.PriMap;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.op.PriMerge;
import nars.term.Term;

/** bag composing a weighted set of Attention's that can be reprioritized and sampled */
public class WhatBag extends ArrayBag<Term, What> {
    public WhatBag() {
        super(PriMerge.replace, 64, PriMap.newMap(false));
    }

    @Override
    public float pri(What value) {
        return value.pri();
    }

    @Override
    public Term key(What value) {
        return value.id;
    }
}
