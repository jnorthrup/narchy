package jcog.data.list;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Extends ArrayList to expose its private elementData array for fast read/write
 * access
 */
public class DirectArrayList<E> extends ArrayList<E> {

    public Object[] data;
    boolean autoupdateData = true;
    private Field f;

    public DirectArrayList() {
        this(1);
    }

    public DirectArrayList(int size) {
        super(size);
        updateData();
    }
    
    public DirectArrayList(E[] data) {
        this.data = data;
    }

    public void updateData() {
        try {
            if (f == null) {
                f = ArrayList.class.getDeclaredField("elementData");
                f.setAccessible(true);
            }
            data = (Object[]) f.get(this);
        } catch (Exception ex) {
            System.err.println(ex);
            System.exit(1);
        }
    }

    
    public boolean fastAdd(E e) {
        return super.add(e);
    }

    @Override
    public boolean add(E e) {

        boolean b = super.add(e);
        if (autoupdateData) {
            updateData();
        }
        return b;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean b = super.addAll(c);
        if (autoupdateData) {
            updateData();
        }
        return b;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        boolean b = super.addAll(index, c);
        if (autoupdateData) {
            updateData();
        }
        return b;
    }









}
