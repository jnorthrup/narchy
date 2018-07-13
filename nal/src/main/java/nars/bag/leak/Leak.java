package nars.bag.leak;

import jcog.pri.bag.Bag;
import org.jetbrains.annotations.NotNull;

/**
 * drains items from a Bag at
 */
class Leak<X, Y> {

    

    @NotNull
    final Bag<X, Y> bag;


    /**
     * rate = max successful leaks per duration
     */
    Leak(@NotNull Bag<X, Y> bag) {
        this.bag = bag;
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
