package jcog.data.byt;

/**
 * caches the hashcode when compact() is called
 */
public class HashCachedBytes extends DynBytes {

    private int hash;

    public HashCachedBytes(int bufferSize) {
        super(bufferSize);
    }

    /** warning dont use this value until after compact()'ing */
    @Override public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DynBytes)) return false;
        if (hash != obj.hashCode()) return false;
        return super.equals(obj);
    }

    /** must be called after construction */
    @Override public byte[] compact() {
        var b = super.compact();
        hash = super.hashCode();
        return b;
    }

}
