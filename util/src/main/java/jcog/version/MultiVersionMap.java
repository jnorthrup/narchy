package jcog.version;

public class MultiVersionMap<X,Y> extends VersionMap<X,Y> {
    final int maxValuesPerItem;

    public MultiVersionMap(int maxValuesPerItem, Versioning<Y> context) {
        super(context);
        this.maxValuesPerItem = maxValuesPerItem;
    }

    @Override
    protected Versioned<Y> newEntry(X x) {
        return new KeyMultiVersioned<>(x, maxValuesPerItem);
    }
}
