package jcog.bloom.hash;

import jcog.data.byt.DynBytes;

public final class DynBytesHashProvider implements HashProvider<DynBytes> {

    public final static DynBytesHashProvider the = new DynBytesHashProvider();

    private DynBytesHashProvider() {

    }

    @Override
    public int hash1(DynBytes d) {
        return d.hashJava();
    }

    @Override
    public int hash2(DynBytes d) {
        return d.hashFNV();
    }
}
