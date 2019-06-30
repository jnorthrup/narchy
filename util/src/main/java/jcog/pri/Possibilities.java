package jcog.pri;


import jcog.data.set.ArrayHashSet;

import java.util.ListIterator;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

public class Possibilities<X,Y> extends ArrayHashSet<Possibilities.Possibility<X,Y>> {

    final X ctx;

    public Possibilities(X ctx) {
        this.ctx = ctx;
    }

    abstract public static class Possibility<X,Y> implements Function<X,Y> {

        float _value = Float.NaN;

        /** value estimate; NaN if invalid */
        abstract public float value();

    }


    public boolean commit(boolean requireAll, boolean sort) {
        boolean removed = removeIf(p -> {
            float pp = p.value();
            p._value = pp;
            if (pp!=pp)
                return true;
            return false;
        });
        if (removed && requireAll) {
            return false;
        }

        if(sort && list.size() > 1)
            list.sortThisByFloat(p->-p._value);

        return true;
    }

    public void execute(BooleanSupplier kontinue, float exploration) {
        ListIterator<Possibility<X,Y>> p = listIterator();
        while (kontinue.getAsBoolean() && p.hasNext()) {
            Object q = p.next().apply(ctx);
            p.remove();
            if (q == null) {
                //done
            } else {
                if (q instanceof Possibility) {
                    p.add((Possibility)q);
                    p.next();
                } else
                    accept((Y)q);
            }
        }
    }

    protected void accept(Y q) {

    }
}
