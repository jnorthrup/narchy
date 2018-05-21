//package jcog.net;
//
//import jcog.net.attn.HashMapTagSet;
//import jcog.net.attn.TagDigest;
//import org.junit.jupiter.api.Test;
//
//import java.io.IOException;
//import java.util.Arrays;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotSame;
//
///**
// * Created by me on 5/2/17.
// */
//public class TagDigestTest {
//
//    @Test
//    public void testAdd() throws IOException {
//        TagDigest d = new TagDigest("z", 32);
//
//        d.pri("a", 0.3f);
//        assertEquals(d.pri("a"), 0.3f, 0.1f);
//
//        d.pri("b", 0.8f);
//        assertEquals(d.pri("b"), 0.8f, 0.1f);
//
//        //change 'b'
//        d.pri("b", 0.5f);
//        assertEquals(d.pri("b"), 0.5f, 0.1f);
//
//        //String jd = new GsonBuilder().create().toJson(d);
//
////        byte[] x = Util.pack(d);
////
////        System.out.println(x.length + " bytes:  " + Arrays.toString(x));
////
////        TagDigest e = Util.unpack(x, TagDigest.class);
////
////        assertEquals(e.pri("a"), 0.2f, 0.1f);
////        assertEquals(e.pri("b"), 0.5f, 0.1f);
//
//    }
//
//    @Test
//    public void testHashMapTagSet() throws IOException {
//        HashMapTagSet x = new HashMapTagSet("z");
//        x.pri("a", 0.5f);
//        x.pri("b", 0.2f);
//        x.pri("c", 0.9f);
//
//        byte[] y = x.toBytes();
//        System.out.println(y.length + " bytes:  " + Arrays.toString(y));
//
//        HashMapTagSet z = HashMapTagSet.fromBytes(y);
//        assertNotSame(x, z);
//        assertEquals(x, z);
//
//    }
//
//}