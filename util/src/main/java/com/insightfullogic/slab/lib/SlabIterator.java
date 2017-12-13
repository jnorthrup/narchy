package com.insightfullogic.slab.lib;

import com.insightfullogic.slab.Cursor;

import java.util.Iterator;

public class SlabIterator<T extends Cursor> implements Iterator<T> {

    public static <T extends Cursor> SlabIterator<T> of(T cursor) {
        return new SlabIterator<>(cursor);
    }

    private final T cursor;

    private int index;

    public SlabIterator(T cursor) {
        this.cursor = cursor;
        index = cursor.getIndex();
    }

    @Override
    public boolean hasNext() {
        return index < cursor.size();
    }

    @Override
    public T next() {
        cursor.move(++index);
        return cursor;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
