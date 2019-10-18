package jcog.version;

import java.util.Objects;

public class MultiVersionMap<X,Y> extends VersionMap<X,Y> {
    final int maxValuesPerItem;

    public MultiVersionMap(Versioning<Y> context, int maxValuesPerItem) {
        super(context);
        this.maxValuesPerItem = maxValuesPerItem;
    }

    @Override
    public int size() {
        return (int) map.values().stream().map(Versioned::get).filter(Objects::nonNull).count();
    }

    @Override
    public Versioned<Y> apply(X x) {
        return new KeyMultiVersioned<>(x, maxValuesPerItem);
    }
}
