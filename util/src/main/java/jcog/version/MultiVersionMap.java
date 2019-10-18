package jcog.version;

public class MultiVersionMap<X,Y> extends VersionMap<X,Y> {
    final int maxValuesPerItem;

    public MultiVersionMap(Versioning<Y> context, int maxValuesPerItem) {
        super(context);
        this.maxValuesPerItem = maxValuesPerItem;
    }

    @Override
    public int size() {
        long result = map.values().stream().filter(e -> e.get() != null).count();
        int count = (int) result;
        return count;
    }

    @Override
    public Versioned<Y> apply(X x) {
        return new KeyMultiVersioned<>(x, maxValuesPerItem);
    }
}
