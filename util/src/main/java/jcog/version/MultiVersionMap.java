package jcog.version;

public class MultiVersionMap<X,Y> extends VersionMap<X,Y> {
    final int maxValuesPerItem;

    public MultiVersionMap(Versioning<Y> context, int maxValuesPerItem) {
        super(context);
        this.maxValuesPerItem = maxValuesPerItem;
    }

    @Override
    public int size() {
        long result = 0L;
        for (Versioned<Y> e : map.values()) {
            if (e.get() != null) {
                result++;
            }
        }
        int count = (int) result;
        return count;
    }

    @Override
    public Versioned<Y> apply(X x) {
        return new KeyMultiVersioned<>(x, maxValuesPerItem);
    }
}
