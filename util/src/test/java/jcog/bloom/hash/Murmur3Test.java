package jcog.bloom.hash;

import jcog.bloom.Murmur3Hash;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by jeff on 16/05/16.
 */
class Murmur3Test {

    @Test
    void whenInvoked_returnsCorrectHash() {
        byte[] data = "hello world".getBytes();

        int hash = Murmur3Hash.hash(data);

        assertEquals(hash, 0x5E928F0F);
    }

}
