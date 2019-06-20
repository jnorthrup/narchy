package nars.attention;

import jcog.Skill;
import jcog.Util;
import jcog.pri.PriMap;
import jcog.pri.Prioritizable;
import jcog.pri.bag.impl.ArrayBag;
import jcog.pri.op.PriMerge;
import nars.term.Term;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** useful for schedulers.  fast sampling access as tradeoff for expectation of rare updates that
 * might cause re-indexing.
 *
 * antistatic = keep system active
 *
 *
 * */
@Skill("Antistatic_bag") abstract public class AntistaticBag<X extends Prioritizable> extends ArrayBag<Term, X> {

    final AtomicBoolean invalid = new AtomicBoolean(false);

    public AntistaticBag(int capacity) {
        super(PriMerge.replace, capacity, PriMap.newMap(false));
    }

    public <Y> void forEachWith(BiConsumer<? super X, Y> action, Y param) {
        if (invalid.compareAndSet(true,false))
            commit();

        int s = size();
        Object[] a = items();
        for (int i = 0; i < s; i++) {
            Object aa = a[i];
            if (aa!=null)
                action.accept(((X) aa), param);
        }
    }

    @Override
    public void forEach(Consumer<? super X> action) {
        if (invalid.compareAndSet(true,false))
            commit();
        super.forEach(action);
    }

    public void pri(X x, float p) {
        if (!Util.equals(x.priGetAndSet(p), p) || invalid.get()) {
            invalid.set(true);
        }
    }
}
