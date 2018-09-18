package jcog.data.list.table;

import jcog.sort.SortedArray;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
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
    
    public final SortedArray<Y> items;


    public SortedListTable(SortedArray<Y> items, Map<X, Y> map) {
        super(map);
        
        this.items = items;
    }


    
    @Override
    public final Iterator<Y> iterator() {
        return items.iterator();
    }

    @Override
    public final Y get(int i) {
        return items.get(i);
    }

    @Override
    public final int size() {
        return items.size();
    }

    @Override
    protected final boolean removeItem(Y removed) {
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





































    
    public List<Y> listCopy() {
        List<Y> l = new ArrayList(size());
        forEach((Consumer<Y>) l::add);
        return l;
    }













}
