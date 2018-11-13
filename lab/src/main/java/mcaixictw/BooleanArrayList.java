package mcaixictw;

import jcog.data.bit.MetalBitSet;
import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanIntToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.ObjectBooleanToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanIntProcedure;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.iterator.BooleanIterator;
import org.eclipse.collections.api.iterator.MutableBooleanIterator;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.list.primitive.BooleanList;
import org.eclipse.collections.api.list.primitive.ImmutableBooleanList;
import org.eclipse.collections.impl.factory.primitive.BooleanLists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.set.mutable.primitive.BooleanHashSet;

import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * BooleanArrayList is similar to {@link FastList}, and is memory-optimized for boolean primitives.
 *
 * @since 3.0.
 */
public class BooleanArrayList extends MetalBitSet.LongArrayBitSet {

    private int size;

    public BooleanArrayList() {
        this(0);
    }

    public BooleanArrayList(int initialCapacity) {
        super(initialCapacity);
    }

    public BooleanArrayList(boolean... array) {
        super(array.length);
        size = array.length;
        for (int i = 0; i < array.length; i++) {
            if (array[i]) {
                set(i);
            }
        }
    }

    public int size() {
        return size;
    }


    @Override
    public boolean isEmpty() {
        return size == 0;
    }


    public boolean notEmpty() {
        return size > 0;
    }


    @Override
    public void clear() {
        if (this != null) {
            super.clear();
            size = 0;
        }
    }


    public boolean contains(boolean value) {
        for (int i = 0; i < size; i++) {
            if (get(i) == value) {
                return true;
            }
        }
        return false;
    }


    public boolean containsAll(boolean... source) {
        for (boolean value : source) {
            if (!contains(value)) {
                return false;
            }
        }
        return true;
    }


    public boolean containsAll(BooleanIterable source) {
        for (BooleanIterator iterator = source.booleanIterator(); iterator.hasNext(); ) {
            if (!contains(iterator.next())) {
                return false;
            }
        }
        return true;
    }











    private IndexOutOfBoundsException newIndexOutOfBoundsException(int index) {
        return new IndexOutOfBoundsException("Index: " + index + " Size: " + size);
    }


    public boolean getFirst() {
        checkEmpty();
        return get(0);
    }


    public boolean getLast() {
        checkEmpty();
        return get(size() - 1);
    }

    private void checkEmpty() {
        if (isEmpty()) {
            throw newIndexOutOfBoundsException(0);
        }
    }


    public int indexOf(boolean object) {
        for (int i = 0; i < size; i++) {
            if (get(i) == object) {
                return i;
            }
        }
        return -1;
    }


    public int lastIndexOf(boolean object) {
        for (int i = size - 1; i >= 0; i--) {
            if (get(i) == object) {
                return i;
            }
        }
        return -1;
    }


    public boolean add(boolean newItem) {

        if (size == capacity()) {
            resize(capacity()+64);
        }

        if (newItem) {
            set(size);
        }
        size++;
        return true;
    }




    public boolean addAll(boolean... source) {
        if (source.length < 1) {
            return false;
        }

        for (boolean sourceItem : source) {
            add(sourceItem);
        }
        return true;
    }


    public void addAll(BooleanArrayList source) {
        source.forEach(this::add);
    }

    public boolean addAll(BooleanIterable source) {
        return addAll(source.toArray());
    }


    public void  addAtIndex(int index, boolean element) {
        if (index > -1 && index < size) {
            addAtIndexLessThanSize(index, element);
        } else if (index == size) {
            add(element);
        } else {
            throw newIndexOutOfBoundsException(index);
        }
    }

    private void addAtIndexLessThanSize(int index, boolean element) {
        for (int i = size + 1; i > index; i--) {
            set(i, get(i - 1));
        }
        set(index, element);
        size++;
    }


    public boolean addAllAtIndex(int index, boolean... source) {
        if (index > size || index < 0) {
            throw newIndexOutOfBoundsException(index);
        }
        if (source.length == 0) {
            return false;
        }
        int sourceSize = source.length;
        int newSize = size + sourceSize;

        for (int i = newSize; i > index; i--) {
            set(i, get(i - sourceSize));
        }

        for (int i = 0; i < sourceSize; i++) {
            set(i + index, source[i]);
        }

        size = newSize;
        return true;
    }


    public boolean addAllAtIndex(int index, BooleanIterable source) {
        return addAllAtIndex(index, source.toArray());
    }


    public boolean remove(boolean value) {
        int index = indexOf(value);
        if (index >= 0) {
            removeAtIndexFast(index);
            return true;
        }
        return false;
    }


    public boolean removeAll(BooleanIterable source) {
        boolean modified = false;
        for (int i = 0; i < size; i++) {
            if (source.contains(get(i))) {
                removeAtIndexFast(i);
                i--;
                modified = true;
            }
        }
        return modified;
    }


    public boolean removeAll(boolean... source) {
        if (isEmpty() || source.length == 0) {
            return false;
        }
        BooleanHashSet set = BooleanHashSet.newSetWith(source);
        if (set.size() == 2) {
            size = 0;
            return true;
        }
        int oldSize = size;
        int trueCount = getTrueCount();
        if (set.contains(true)) {
            size -= trueCount;
            set(0, size, false);
        } else {
            size = trueCount;
            set(0, size, true);
        }
        return oldSize != size;
    }


    public boolean retainAll(BooleanIterable source) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(boolean... source) {
        return retainAll(BooleanHashSet.newSetWith(source));
    }

    /** TODO optimize */
    private int getTrueCount() {
        int count = 0;
        for (int i = 0; i < size; i++) {
            if (get(i)) {
                count++;
            }
        }
        return count;
    }


    public boolean removeAtIndex(int index) {
        boolean previous = get(index);
        if (index < size) {
            for (int i = index; i < size-1; i++) {
                set(i, get(i + 1));
            }
            clear(--size);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }

        return previous;
    }

    public void removeAtIndexFast(int index) {

        if (size - index > 1) {
            for (int i = index; i < size-1; i++) {
                set(i, get(i + 1));
            }
            --size;
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }

    }



    public MutableBooleanIterator booleanIterator() {
        return new InternalBooleanIterator();
    }


    public void forEach(BooleanProcedure procedure) {
        each(procedure);
    }

    /**
     * @since 7.0.
     */

    public void each(BooleanProcedure procedure) {
        for (int i = 0; i < size; i++) {
            procedure.value(get(i));
        }
    }


    public void forEachWithIndex(BooleanIntProcedure procedure) {
        for (int i = 0; i < size; i++) {
            procedure.value(get(i), i);
        }
    }


    public <T> T injectInto(T injectedValue, ObjectBooleanToObjectFunction<? super T, ? extends T> function) {
        T result = injectedValue;
        for (int i = 0; i < size; i++) {
            result = function.valueOf(result, get(i));
        }
        return result;
    }


    public <T> T injectIntoWithIndex(T injectedValue, ObjectBooleanIntToObjectFunction<? super T, ? extends T> function) {
        T result = injectedValue;
        for (int i = 0; i < size; i++) {
            result = function.valueOf(result, get(i), i);
        }
        return result;
    }


    public int count(BooleanPredicate predicate) {
        int count = 0;
        for (int i = 0; i < size; i++) {
            if (predicate.accept(get(i))) {
                count++;
            }
        }
        return count;
    }


    public boolean anySatisfy(BooleanPredicate predicate) {
        for (int i = 0; i < size; i++) {
            if (predicate.accept(get(i))) {
                return true;
            }
        }
        return false;
    }


    public boolean allSatisfy(BooleanPredicate predicate) {
        for (int i = 0; i < size; i++) {
            if (!predicate.accept(get(i))) {
                return false;
            }
        }
        return true;
    }


    public boolean noneSatisfy(BooleanPredicate predicate) {
        for (int i = 0; i < size; i++) {
            if (predicate.accept(get(i))) {
                return false;
            }
        }
        return true;
    }


    public BooleanArrayList select(BooleanPredicate predicate) {
        BooleanArrayList result = new BooleanArrayList();
        for (int i = 0; i < size; i++) {
            boolean item = get(i);
            if (predicate.accept(item)) {
                result.add(item);
            }
        }
        return result;
    }


    public BooleanArrayList reject(BooleanPredicate predicate) {
        BooleanArrayList result = new BooleanArrayList();
        for (int i = 0; i < size; i++) {
            boolean item = get(i);
            if (!predicate.accept(item)) {
                result.add(item);
            }
        }
        return result;
    }


    public BooleanArrayList reverseThis() {
        int endIndex = size - 1;
        for (int i = 0; i < size / 2; i++) {
            boolean tempSwapValue = get(i);
            set(i, get(endIndex - i));
            set(endIndex - i, tempSwapValue);
        }
        return this;
    }


    public ImmutableBooleanList toImmutable() {
        if (size == 0) {
            return BooleanLists.immutable.empty();
        }
        if (size == 1) {
            return BooleanLists.immutable.with(get(0));
        }
        return BooleanLists.immutable.with(toArray());
    }


    public boolean detectIfNone(BooleanPredicate predicate, boolean ifNone) {
        for (int i = 0; i < size; i++) {
            boolean item = get(i);
            if (predicate.accept(item)) {
                return item;
            }
        }
        return ifNone;
    }


    public <V> MutableList<V> collect(BooleanToObjectFunction<? extends V> function) {
        FastList<V> target = FastList.newList(size);
        for (int i = 0; i < size; i++) {
            target.add(function.valueOf(get(i)));
        }
        return target;
    }


    public boolean[] toArray() {
        boolean[] newItems = new boolean[size];
        for (int i = 0; i < size; i++) {
            newItems[i] = get(i);
        }
        return newItems;
    }


    public boolean equals(Object otherList) {
        if (otherList == this) {
            return true;
        }
        if (!(otherList instanceof BooleanList)) {
            return false;
        }
        BooleanList list = (BooleanList) otherList;
        if (size != list.size()) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (get(i) != list.get(i)) {
                return false;
            }
        }
        return true;
    }


    public int hashCode() {
        int hashCode = 1;
        for (int i = 0; i < size; i++) {
            boolean item = get(i);
            hashCode = 31 * hashCode + (item ? 1231 : 1237);
        }
        return hashCode;
    }


    public String toString() {
        return makeString("[", ", ", "]");
    }


    public String makeString() {
        return makeString(", ");
    }


    public String makeString(String separator) {
        return makeString("", separator, "");
    }


    public String makeString(String start, String separator, String end) {
        Appendable stringBuilder = new StringBuilder();
        appendString(stringBuilder, start, separator, end);
        return stringBuilder.toString();
    }


    public void appendString(Appendable appendable) {
        appendString(appendable, ", ");
    }


    public void appendString(Appendable appendable, String separator) {
        appendString(appendable, "", separator, "");
    }


    public void appendString(
            Appendable appendable,
            String start,
            String separator,
            String end) {
        try {
            appendable.append(start);
            for (int i = 0; i < size; i++) {
                if (i > 0) {
                    appendable.append(separator);
                }
                boolean value = get(i);
                appendable.append(String.valueOf(value));
            }
            appendable.append(end);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean pop() {
        return removeAtIndex(size-1);
    }

    public void popFast(int toRemove) {
        size -= toRemove;
        assert(size >= 0);
    }


    private class InternalBooleanIterator implements MutableBooleanIterator {
        /**
         * Index of element to be returned by subsequent call to next.
         */
        private int currentIndex;
        private int lastIndex = -1;


        @Override
        public boolean hasNext() {
            return currentIndex != size();
        }


        @Override
        public boolean next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            boolean next = get(currentIndex);
            lastIndex = currentIndex++;
            return next;
        }


        @Override
        public void remove() {
            if (lastIndex == -1) {
                throw new IllegalStateException();
            }
            removeAtIndexFast(lastIndex);
            currentIndex--;
            lastIndex = -1;
        }
    }
}
