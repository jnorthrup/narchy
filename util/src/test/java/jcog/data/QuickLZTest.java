package jcog.data;

import jcog.Texts;
import jcog.io.lz.QuickLZ;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class QuickLZTest {

    private static float testCompressDecompress(String s, int level) {
        System.out.print(s + "\n\t");
        return testCompressDecompress(s.getBytes(), level);
    }

    private static float testCompressDecompress(byte[] input, int level) {
        byte[] compressed = QuickLZ.compress(input,level);
        byte[] decompress = QuickLZ.decompress(compressed);

        
        

        assertArrayEquals(input, decompress);

        float ratio = ((float)compressed.length) / (input.length);
        System.out.println(input.length + " input, " + compressed.length + " compressed = "  +
                Texts.n2(100f * ratio) + '%');
        return ratio;
    }



    @ParameterizedTest
    @ValueSource(ints={1,3})
    void testSome(int level) {








                testCompressDecompress("x", level);
                testCompressDecompress("abc", level);
                testCompressDecompress("abcsdhfjdklsfjdklsfjd;s fja;dksfj;adskfj;adsfkdas;fjadksfj;kasdf", level);
                testCompressDecompress("222222222222211111111111111112122222222222111111122222", level);
                float r = testCompressDecompress("(a --> (b --> (c --> (d --> e))))", level);







    }
}