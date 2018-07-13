package jcog.data.list;


/**
 * capacity limited list, doesnt allow additions beyond a certain size
 */
public class LimitedFasterList<X> extends FasterList<X> {

    final int max;

    public LimitedFasterList(int max) {
        super(0); 
        this.max = max;
    }

    final boolean ensureLimit() {
        return size() + 1 <= max;
    }

    @Override
    public boolean add(X newItem) {
        return ensureLimit() && super.add(newItem);
    }

    @Override
    public void add(int index, X element) {
        ensureLimit();
        super.add(index, element);
    }
}
