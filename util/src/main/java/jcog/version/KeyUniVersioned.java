package jcog.version;

public class KeyUniVersioned<X,Y> extends UniVersioned<Y> {

    public final X key;

    public KeyUniVersioned(X key, Versioning context) {
        super(context);
        this.key = key;
    }
}
