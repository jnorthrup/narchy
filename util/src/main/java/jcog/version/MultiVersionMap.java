package jcog.version;

public class MultiVersionMap<X,Y> extends VersionMap<X,Y> {
    final int maxValuesPerItem;

    public MultiVersionMap(Versioning<Y> context, int maxValuesPerItem) {
        super(context);
        this.maxValuesPerItem = maxValuesPerItem;
    }

    @Override
    public int size() {
        int count = 0;
        for (Versioned<Y> e : map.values()) {
            if (e.get()!=null)
                count++;
        }
        return count;
    }

    @Override
    protected Versioned<Y> newEntry(X x) {
        return new KeyMultiVersioned<>(x, maxValuesPerItem);
    }
}
