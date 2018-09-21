package jcog.data.map;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * most recently used cache based on (non-thread_safe) LinkedHashMap
 */
public class MRUMap<K, V> extends LinkedHashMap<K, V> {

    protected int capacity;

    public MRUMap(int capacity) {
        super(capacity, 0.99f, true);
        this.capacity = capacity;
    }


    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> entry) {
        if (this.size() >= capacity) {
            onEvict(entry);
            return true;
        }
        return false;
    }

    protected void onEvict(Map.Entry<K, V> entry) {

    }
}
