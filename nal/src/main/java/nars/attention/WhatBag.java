package nars.attention;

import jcog.pri.PriMap;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.op.PriMerge;
import nars.term.Term;

import java.util.function.Consumer;

/** bag composing a weighted set of Attention's that can be reprioritized and sampled */
public class WhatBag extends ArrayBag<Term, What> {

    public WhatBag(int capacity) {
        super(PriMerge.replace, capacity, PriMap.newMap(false));
    }

    public final void forEachActive(Consumer<What> c) {
        //TODO improve
        forEach(w -> {
              if (w.isOn())
                  c.accept(w);
        });
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
