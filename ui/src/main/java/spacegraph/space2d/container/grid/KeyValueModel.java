package spacegraph.space2d.container.grid;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

/** simple 2-column/2-row key->value table */
public class KeyValueModel implements GridModel {

    private final Map map;

    /** cached keys as an array for fast access */
    private final transient Object[] keys;

    public KeyValueModel(Map map) {
        this.map = map;
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

    @Nullable
    @Override
    public Object get(int x, int y) {
        switch (x) {
            case 0: //key
                return keys[y];
            case 1: //value
                return map.get(keys[y]);
        }
        return null;
    }
}
