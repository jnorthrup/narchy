package spacegraph.space2d.container.grid;



import org.eclipse.collections.api.multimap.Multimap;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;

/** simple 2-column/2-row key->value table */
public class KeyValueGrid implements GridModel {

    private final Function map;

    /** cached keys as an array for fast access */
    private final transient Object[] keys;

    public KeyValueGrid(Map map) {
        this.map = map::get;
        this.keys = map.keySet().toArray();
    }
    public KeyValueGrid(com.google.common.collect.Multimap map) {
        this.map = map::get;
        this.keys = map.keySet().toArray();
    }
    public KeyValueGrid(Multimap map) {
        this.map = map::get;
        this.keys = map.keySet().toArray();
    }

    @Override
    public int cellsX() {
        return 2;
    }

    @Override
    public int cellsY() {
        return keys.length;
    }

    @Override
    public @Nullable Object get(int x, int y) {
        var k = this.keys;
        if ((y < 0) || y >= keys.length)
            return null; //OOB

        switch (x) {
            case 0: 
                return k[y];
            case 1: 
                return map.apply(k[y]);
        }
        return null;
    }
}
