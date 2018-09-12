package nars.bag.leak;

import jcog.pri.bag.Bag;
import org.jetbrains.annotations.NotNull;

/**
 * drains items from a Bag at
 */
class Leak<X, Y> {

    


    final Bag<X, Y> bag;


    /**
     * rate = max successful leaks per duration
     */
    Leak(@NotNull Bag<X, Y> bag) {
        this.bag = bag;
    }

    public void put(Y x) {
        bag.putAsync(x);
    }

    public void setCapacity(int capacity) {
        bag.setCapacity(capacity);
    }

    public void clear() {
        bag.clear();
    }

    public boolean isEmpty() {
        return bag.isEmpty();
    }
}
