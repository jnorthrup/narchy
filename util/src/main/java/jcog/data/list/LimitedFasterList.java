package jcog.data.list;


import jcog.TODO;

import java.util.Collection;

/**
 * capacity limited list, doesnt allow additions beyond a certain size
 * TODO needs more methods safeguarded
 */
public class LimitedFasterList<X> extends FasterList<X> {

    public static final LimitedFasterList Empty = new LimitedFasterList(0);

    final int max;

    public LimitedFasterList(int max) {
        super(0); 
        this.max = max;
    }

    final boolean ensureLimit() {
        return size() + 1 <= max;
    }

    @Override
    public boolean add(X x) {
        return ensureLimit() && super.add(x);
    }

    @Override
    public void add(int index, X element) {
        if (ensureLimit())
            super.add(index, element);
    }

    @Override
    public boolean addAll(Collection<? extends X> source) {
        throw new TODO();
    }

    @Override
    public boolean addAll(int index, Collection<? extends X> source) {
        throw new TODO();
    }

}
