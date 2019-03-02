package jcog.version;

public class KeyValueVersioned<X,Y> extends MultiVersioned<Y> {
    public final X key;

    public KeyValueVersioned(X key, Versioning<Y> sharedContext, int initialCap) {
        super(sharedContext, initialCap);
        this.key = key;
    }
}
