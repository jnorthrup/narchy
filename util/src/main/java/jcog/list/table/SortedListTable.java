package jcog.list.table;

import jcog.sort.SortedArray;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


/**
 * Created by me on 1/15/16.
 */
abstract public class SortedListTable<X, Y> extends ArrayListTable<X, Y> implements SortedTable<X, Y>, FloatFunction<Y> {

    /**
     * array of lists of items, for items on different level
     */
    //protected final SortedList_1x4<L> items;
    public final @NotNull SortedArray<Y> items;


    public SortedListTable(SortedArray<Y> items, @NotNull Map<X, Y> map) {
        super(map);
        //this.items = new SortedList_1x4<>(items, this, searchType, false);
        this.items = items;
    }

    @NotNull
    @Override
    protected final Object _items() {
        return items;
    }

    @NotNull
    @Override
    public Iterator<Y> iterator() {
        return items.iterator();
    }

    @Override
    public final Y get(int i) {
        return items.list[i];//array()[i];
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    protected final boolean listRemove(Y removed) {
        return items.remove(removed, this);
    }


    @Override
    protected final void listClear() {
        items.clear();
    }


    @Override @Nullable
    public final Y top() {
        return (size()==0) ? null : get(0);
    }

    @Override @Nullable
    public final Y bottom() {
        int s = size();
        return s == 0 ? null : get(s - 1);
    }



//    /** gets the key associated with a value */
//    @Nullable @Override
//    abstract public X key(@NotNull Y l);




//    @Nullable
//    @Override
//    @Deprecated protected Y addItem(@NotNull Y i) {
//        int cap = capacity();
//        if (cap < 1) {
//            return i; //bounce
//        }
//
//        int size = size();
//
//        Y displaced = null;
//
//        if (size == cap) {
//            Y last = items.last();
//            if (Float.compare(floatValueOf(last), floatValueOf(i)) < 0) {
//                //insufficient rank, bounce
//                return i;
//            }
//
//            displaced = items.removeLast(); //remove last
//        }
//
//        items.add(i, this);
//
//        return displaced;
//    }

    @NotNull
    public List<Y> listCopy() {
        List<Y> l = new ArrayList(size());
        forEach((Consumer<Y>) l::add);
        return l;
    }


//    protected final class ArrayMapping extends CollectorMap<V, L> {
//
//        final SortedIndex<L> items;
//
//        public ArrayMapping(Map<V, L> map, SortedIndex<L> items) {
//            super(map);
//            this.items = items;
//        }
//
//
//    }
}
