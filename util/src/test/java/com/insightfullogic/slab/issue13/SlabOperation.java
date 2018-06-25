package com.insightfullogic.slab.issue13;

import com.insightfullogic.slab.Cursor;


public interface SlabOperation extends Cursor {

    byte getMagic();

    void setMagic(byte magic);

    byte getOpCode();

    void setOpCode(byte opCode);

    short getKeyLength();

    void setKeyLength(short keyLength);

    byte getExtraLength();

    void setExtraLength(byte extraLength);

    byte getDataType();

    void setDataType(byte dataType);

    short getReserved();

    void setReserved(short reserved);

    int getBodySize();

    void setBodySize(int bodySize);

    int getOpaque();

    void setOpaque(int opaque);

    long getCas();

    void setCas(long cas);
}
