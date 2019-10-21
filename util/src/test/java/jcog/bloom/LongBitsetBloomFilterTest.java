package jcog.bloom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Created by me on 8/6/15.
 */
class LongBitsetBloomFilterTest {
    private static final int COUNT = 100;
    private Random rand = new Random(123);

    @Test
    void testBloomIllegalArg1() {
        assertThrows(AssertionError.class, new Executable() {
            @Override
            public void execute() throws Throwable {

                LongBitsetBloomFilter bf = new LongBitsetBloomFilter(0, 0);
            }
        });
    }

    @Test
    void testBloomIllegalArg2() {
        assertThrows(AssertionError.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                LongBitsetBloomFilter bf = new LongBitsetBloomFilter(0, 0.1);
            }
        });
    }

    @Test
    void testBloomIllegalArg3() {
        assertThrows(AssertionError.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                LongBitsetBloomFilter bf = new LongBitsetBloomFilter(1, 0.0);
            }
        });
    }

    @Test
    void testBloomIllegalArg4() {
        assertThrows(AssertionError.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                LongBitsetBloomFilter bf = new LongBitsetBloomFilter(1, 1.0);
            }
        });
    }

    @Test
    void testBloomIllegalArg5() {
        assertThrows(AssertionError.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                LongBitsetBloomFilter bf = new LongBitsetBloomFilter(-1, -1);
            }
        });
    }


    @Test
    void testBloomNumBits() {
        assertEquals(0, LongBitsetBloomFilter.optimalNumOfBits(0, 0));
        assertEquals(1549, LongBitsetBloomFilter.optimalNumOfBits(1, 0));
        assertEquals(0, LongBitsetBloomFilter.optimalNumOfBits(0, 1));
        assertEquals(0, LongBitsetBloomFilter.optimalNumOfBits(1, 1));
        assertEquals(7, LongBitsetBloomFilter.optimalNumOfBits(1, 0.03));
        assertEquals(72, LongBitsetBloomFilter.optimalNumOfBits(10, 0.03));
        assertEquals(729, LongBitsetBloomFilter.optimalNumOfBits(100, 0.03));
        assertEquals(7298, LongBitsetBloomFilter.optimalNumOfBits(1000, 0.03));
        assertEquals(72984, LongBitsetBloomFilter.optimalNumOfBits(10000, 0.03));
        assertEquals(729844, LongBitsetBloomFilter.optimalNumOfBits(100000, 0.03));
        assertEquals(7298440, LongBitsetBloomFilter.optimalNumOfBits(1000000, 0.03));
        assertEquals(6235224, LongBitsetBloomFilter.optimalNumOfBits(1000000, 0.05));
    }

    @Test
    void testBloomNumHashFunctions() {
        assertEquals(1, LongBitsetBloomFilter.optimalNumOfHashFunctions(-1, -1));
        assertEquals(1, LongBitsetBloomFilter.optimalNumOfHashFunctions(0, 0));
        assertEquals(1, LongBitsetBloomFilter.optimalNumOfHashFunctions(10, 0));
        assertEquals(1, LongBitsetBloomFilter.optimalNumOfHashFunctions(10, 10));
        assertEquals(7, LongBitsetBloomFilter.optimalNumOfHashFunctions(10, 100));
        assertEquals(1, LongBitsetBloomFilter.optimalNumOfHashFunctions(100, 100));
        assertEquals(1, LongBitsetBloomFilter.optimalNumOfHashFunctions(1000, 100));
        assertEquals(1, LongBitsetBloomFilter.optimalNumOfHashFunctions(10000, 100));
        assertEquals(1, LongBitsetBloomFilter.optimalNumOfHashFunctions(100000, 100));
        assertEquals(1, LongBitsetBloomFilter.optimalNumOfHashFunctions(1000000, 100));
    }

    @Test
    void testBloomFilterBytes() {
        LongBitsetBloomFilter bf = new LongBitsetBloomFilter(10000);
        byte[] val = {1, 2, 3};

        assertEquals(false, bf.test(val));
        byte[] val1 = {1, 2, 3, 4};
        assertEquals(false, bf.test(val1));
        byte[] val2 = {1, 2, 3, 4, 5};
        assertEquals(false, bf.test(val2));
        byte[] val3 = {1, 2, 3, 4, 5, 6};
        assertEquals(false, bf.test(val3));
        bf.add(val);
        assertEquals(true, bf.test(val));
        assertEquals(false, bf.test(val1));
        assertEquals(false, bf.test(val2));
        assertEquals(false, bf.test(val3));
        bf.add(val1);
        assertEquals(true, bf.test(val));
        assertEquals(true, bf.test(val1));
        assertEquals(false, bf.test(val2));
        assertEquals(false, bf.test(val3));
        bf.add(val2);
        assertEquals(true, bf.test(val));
        assertEquals(true, bf.test(val1));
        assertEquals(true, bf.test(val2));
        assertEquals(false, bf.test(val3));
        bf.add(val3);
        assertEquals(true, bf.test(val));
        assertEquals(true, bf.test(val1));
        assertEquals(true, bf.test(val2));
        assertEquals(true, bf.test(val3));

        byte[] randVal = new byte[COUNT];
        for (int i = 0; i < COUNT; i++) {
            rand.nextBytes(randVal);
            bf.add(randVal);
        }
        
        assertEquals(true, bf.test(randVal));
        
        randVal[0] = 0;
        randVal[1] = 0;
        randVal[2] = 0;
        randVal[3] = 0;
        randVal[4] = 0;
        assertEquals(false, bf.test(randVal));

        assertEquals(7800, bf.sizeInBytes());
    }

    @Test
    void testBloomFilterByte() {
        LongBitsetBloomFilter bf = new LongBitsetBloomFilter(10000);
        byte val = Byte.MIN_VALUE;

        assertEquals(false, bf.testByte(val));
        byte val1 = 1;
        assertEquals(false, bf.testByte(val1));
        byte val2 = 2;
        assertEquals(false, bf.testByte(val2));
        byte val3 = Byte.MAX_VALUE;
        assertEquals(false, bf.testByte(val3));
        bf.addByte(val);
        assertEquals(true, bf.testByte(val));
        assertEquals(false, bf.testByte(val1));
        assertEquals(false, bf.testByte(val2));
        assertEquals(false, bf.testByte(val3));
        bf.addByte(val1);
        assertEquals(true, bf.testByte(val));
        assertEquals(true, bf.testByte(val1));
        assertEquals(false, bf.testByte(val2));
        assertEquals(false, bf.testByte(val3));
        bf.addByte(val2);
        assertEquals(true, bf.testByte(val));
        assertEquals(true, bf.testByte(val1));
        assertEquals(true, bf.testByte(val2));
        assertEquals(false, bf.testByte(val3));
        bf.addByte(val3);
        assertEquals(true, bf.testByte(val));
        assertEquals(true, bf.testByte(val1));
        assertEquals(true, bf.testByte(val2));
        assertEquals(true, bf.testByte(val3));

        byte randVal = 0;
        for (int i = 0; i < COUNT; i++) {
            randVal = (byte) rand.nextInt(Byte.MAX_VALUE);
            bf.addByte(randVal);
        }
        
        assertEquals(true, bf.testByte(randVal));
        
        assertEquals(false, bf.testByte((byte) -120));

        assertEquals(7800, bf.sizeInBytes());
    }

    @Test
    void testBloomFilterInt() {
        LongBitsetBloomFilter bf = new LongBitsetBloomFilter(10000);
        int val = Integer.MIN_VALUE;

        assertEquals(false, bf.testInt(val));
        int val1 = 1;
        assertEquals(false, bf.testInt(val1));
        int val2 = 2;
        assertEquals(false, bf.testInt(val2));
        int val3 = Integer.MAX_VALUE;
        assertEquals(false, bf.testInt(val3));
        bf.addInt(val);
        assertEquals(true, bf.testInt(val));
        assertEquals(false, bf.testInt(val1));
        assertEquals(false, bf.testInt(val2));
        assertEquals(false, bf.testInt(val3));
        bf.addInt(val1);
        assertEquals(true, bf.testInt(val));
        assertEquals(true, bf.testInt(val1));
        assertEquals(false, bf.testInt(val2));
        assertEquals(false, bf.testInt(val3));
        bf.addInt(val2);
        assertEquals(true, bf.testInt(val));
        assertEquals(true, bf.testInt(val1));
        assertEquals(true, bf.testInt(val2));
        assertEquals(false, bf.testInt(val3));
        bf.addInt(val3);
        assertEquals(true, bf.testInt(val));
        assertEquals(true, bf.testInt(val1));
        assertEquals(true, bf.testInt(val2));
        assertEquals(true, bf.testInt(val3));

        int randVal = 0;
        for (int i = 0; i < COUNT; i++) {
            randVal = rand.nextInt();
            bf.addInt(randVal);
        }
        
        assertEquals(true, bf.testInt(randVal));
        
        assertEquals(false, bf.testInt(-120));

        assertEquals(7800, bf.sizeInBytes());
    }

    @Test
    void testBloomFilterLong() {
        LongBitsetBloomFilter bf = new LongBitsetBloomFilter(10000);
        long val = Long.MIN_VALUE;

        assertEquals(false, bf.testLong(val));
        long val1 = 1;
        assertEquals(false, bf.testLong(val1));
        long val2 = 2;
        assertEquals(false, bf.testLong(val2));
        long val3 = Long.MAX_VALUE;
        assertEquals(false, bf.testLong(val3));
        bf.addLong(val);
        assertEquals(true, bf.testLong(val));
        assertEquals(false, bf.testLong(val1));
        assertEquals(false, bf.testLong(val2));
        assertEquals(false, bf.testLong(val3));
        bf.addLong(val1);
        assertEquals(true, bf.testLong(val));
        assertEquals(true, bf.testLong(val1));
        assertEquals(false, bf.testLong(val2));
        assertEquals(false, bf.testLong(val3));
        bf.addLong(val2);
        assertEquals(true, bf.testLong(val));
        assertEquals(true, bf.testLong(val1));
        assertEquals(true, bf.testLong(val2));
        assertEquals(false, bf.testLong(val3));
        bf.addLong(val3);
        assertEquals(true, bf.testLong(val));
        assertEquals(true, bf.testLong(val1));
        assertEquals(true, bf.testLong(val2));
        assertEquals(true, bf.testLong(val3));

        long randVal = 0;
        for (int i = 0; i < COUNT; i++) {
            randVal = rand.nextLong();
            bf.addLong(randVal);
        }
        
        assertEquals(true, bf.testLong(randVal));
        
        assertEquals(false, bf.testLong(-120));

        assertEquals(7800, bf.sizeInBytes());
    }

    @Test
    void testBloomFilterFloat() {
        LongBitsetBloomFilter bf = new LongBitsetBloomFilter(10000);
        float val = Float.NEGATIVE_INFINITY;

        assertEquals(false, bf.testFloat(val));
        float val1 = 1.1f;
        assertEquals(false, bf.testFloat(val1));
        float val2 = 2.2f;
        assertEquals(false, bf.testFloat(val2));
        float val3 = Float.POSITIVE_INFINITY;
        assertEquals(false, bf.testFloat(val3));
        bf.addFloat(val);
        assertEquals(true, bf.testFloat(val));
        assertEquals(false, bf.testFloat(val1));
        assertEquals(false, bf.testFloat(val2));
        assertEquals(false, bf.testFloat(val3));
        bf.addFloat(val1);
        assertEquals(true, bf.testFloat(val));
        assertEquals(true, bf.testFloat(val1));
        assertEquals(false, bf.testFloat(val2));
        assertEquals(false, bf.testFloat(val3));
        bf.addFloat(val2);
        assertEquals(true, bf.testFloat(val));
        assertEquals(true, bf.testFloat(val1));
        assertEquals(true, bf.testFloat(val2));
        assertEquals(false, bf.testFloat(val3));
        bf.addFloat(val3);
        assertEquals(true, bf.testFloat(val));
        assertEquals(true, bf.testFloat(val1));
        assertEquals(true, bf.testFloat(val2));
        assertEquals(true, bf.testFloat(val3));

        float randVal = 0;
        for (int i = 0; i < COUNT; i++) {
            randVal = rand.nextFloat();
            bf.addFloat(randVal);
        }
        
        assertEquals(true, bf.testFloat(randVal));
        
        assertEquals(false, bf.testFloat(-120.2f));

        assertEquals(7800, bf.sizeInBytes());
    }

    @Test
    void testBloomFilterDouble() {
        LongBitsetBloomFilter bf = new LongBitsetBloomFilter(10000);
        double val = Double.MIN_VALUE;

        assertEquals(false, bf.testDouble(val));
        double val1 = 1.1d;
        assertEquals(false, bf.testDouble(val1));
        double val2 = 2.2d;
        assertEquals(false, bf.testDouble(val2));
        double val3 = Double.MAX_VALUE;
        assertEquals(false, bf.testDouble(val3));
        bf.addDouble(val);
        assertEquals(true, bf.testDouble(val));
        assertEquals(false, bf.testDouble(val1));
        assertEquals(false, bf.testDouble(val2));
        assertEquals(false, bf.testDouble(val3));
        bf.addDouble(val1);
        assertEquals(true, bf.testDouble(val));
        assertEquals(true, bf.testDouble(val1));
        assertEquals(false, bf.testDouble(val2));
        assertEquals(false, bf.testDouble(val3));
        bf.addDouble(val2);
        assertEquals(true, bf.testDouble(val));
        assertEquals(true, bf.testDouble(val1));
        assertEquals(true, bf.testDouble(val2));
        assertEquals(false, bf.testDouble(val3));
        bf.addDouble(val3);
        assertEquals(true, bf.testDouble(val));
        assertEquals(true, bf.testDouble(val1));
        assertEquals(true, bf.testDouble(val2));
        assertEquals(true, bf.testDouble(val3));

        double randVal = 0;
        for (int i = 0; i < COUNT; i++) {
            randVal = rand.nextDouble();
            bf.addDouble(randVal);
        }
        
        assertEquals(true, bf.testDouble(randVal));
        
        assertEquals(false, bf.testDouble(-120.2d));

        assertEquals(7800, bf.sizeInBytes());
    }

    @Test
    void testBloomFilterString() {
        LongBitsetBloomFilter bf = new LongBitsetBloomFilter(10000);
        String val = "bloo";

        assertEquals(false, bf.testString(val));
        String val1 = "bloom fil";
        assertEquals(false, bf.testString(val1));
        String val2 = "bloom filter";
        assertEquals(false, bf.testString(val2));
        String val3 = "cuckoo filter";
        assertEquals(false, bf.testString(val3));
        bf.addString(val);
        assertEquals(true, bf.testString(val));
        assertEquals(false, bf.testString(val1));
        assertEquals(false, bf.testString(val2));
        assertEquals(false, bf.testString(val3));
        bf.addString(val1);
        assertEquals(true, bf.testString(val));
        assertEquals(true, bf.testString(val1));
        assertEquals(false, bf.testString(val2));
        assertEquals(false, bf.testString(val3));
        bf.addString(val2);
        assertEquals(true, bf.testString(val));
        assertEquals(true, bf.testString(val1));
        assertEquals(true, bf.testString(val2));
        assertEquals(false, bf.testString(val3));
        bf.addString(val3);
        assertEquals(true, bf.testString(val));
        assertEquals(true, bf.testString(val1));
        assertEquals(true, bf.testString(val2));
        assertEquals(true, bf.testString(val3));

        long randVal = 0;
        for (int i = 0; i < COUNT; i++) {
            randVal = rand.nextLong();
            bf.addString(Long.toString(randVal));
        }
        
        assertEquals(true, bf.testString(Long.toString(randVal)));
        
        assertEquals(false, bf.testString(Long.toString(-120)));

        assertEquals(7800, bf.sizeInBytes());
    }

    @Test
    void testMerge() {
        LongBitsetBloomFilter bf = new LongBitsetBloomFilter(10000);
        String val = "bloo";
        bf.addString(val);
        String val1 = "bloom fil";
        bf.addString(val1);
        String val2 = "bloom filter";
        bf.addString(val2);
        String val3 = "cuckoo filter";
        bf.addString(val3);

        LongBitsetBloomFilter bf2 = new LongBitsetBloomFilter(10000);
        String v = "2_bloo";
        bf2.addString(v);
        String v1 = "2_bloom fil";
        bf2.addString(v1);
        String v2 = "2_bloom filter";
        bf2.addString(v2);
        String v3 = "2_cuckoo filter";
        bf2.addString(v3);

        assertEquals(true, bf.testString(val));
        assertEquals(true, bf.testString(val1));
        assertEquals(true, bf.testString(val2));
        assertEquals(true, bf.testString(val3));
        assertEquals(false, bf.testString(v));
        assertEquals(false, bf.testString(v1));
        assertEquals(false, bf.testString(v2));
        assertEquals(false, bf.testString(v3));

        bf.merge(bf2);

        assertEquals(true, bf.testString(val));
        assertEquals(true, bf.testString(val1));
        assertEquals(true, bf.testString(val2));
        assertEquals(true, bf.testString(val3));
        assertEquals(true, bf.testString(v));
        assertEquals(true, bf.testString(v1));
        assertEquals(true, bf.testString(v2));
        assertEquals(true, bf.testString(v3));
    }
}