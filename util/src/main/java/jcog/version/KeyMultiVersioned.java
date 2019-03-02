package jcog.version;

public class KeyMultiVersioned<X,Y> extends MultiVersioned<Y> {
    public final X key;

    public KeyMultiVersioned(X key, Versioning<Y> sharedContext, int initialCap) {
        super(sharedContext, initialCap);
        this.key = key;
    }
}
