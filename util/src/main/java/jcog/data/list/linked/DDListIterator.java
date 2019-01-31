package jcog.data.list.linked;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
    note: assumes that no calls to DDList.addAt() will be made during iteration,
    so it is probably only safe in non-concurrent cases
*/
public class DDListIterator<E> implements Iterator<E> {

    private DD<E> current;  
    
    
    private int index;
    private int size;

    public void init(DDList<E> d) {
        current = d.pre.next;
        
        index = 0;
        size = d.size;
    }
    @Override
    public boolean hasNext() {
        return index < size;
    }

    public boolean hasPrevious() {
        return index > 0;
    }

    public int previousIndex() {
        return index - 1;
    }

    public int nextIndex() {
        return index;
    }

    @Override
    public E next() {
        if (!hasNext()) throw new NoSuchElementException();
        
        E item = current.item;
        current = current.next;
        index++;
        return item;
    }

    public E previous() {
        if (!hasPrevious()) throw new NoSuchElementException();
        current = current.prev;
        index--;
        
        return current.item;
    }



















































}
