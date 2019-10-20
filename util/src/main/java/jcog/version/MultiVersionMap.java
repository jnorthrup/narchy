package jcog.version;

public class MultiVersionMap<X,Y> extends VersionMap<X,Y> {
    final int maxValuesPerItem;

    public MultiVersionMap(Versioning<Y> context, int maxValuesPerItem) {
        super(context);
        this.maxValuesPerItem = maxValuesPerItem;
    }

    @Override
    public int size() {
        long count = 0L;
        for (Versioned<Y> yVersioned : map.values()) {
            Y y = yVersioned.get();
            if (y != null) {
                count++;
            }
        }
        return (int) count;
    }

    @Override
    public Versioned<Y> apply(X x) {
        return new KeyMultiVersioned<>(x, maxValuesPerItem);
    }
}
