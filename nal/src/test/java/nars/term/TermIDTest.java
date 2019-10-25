package nars.term;

import nars.$;
import nars.Narsese;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Created by me on 6/3/15.
 */
class TermIDTest {

//    final Timed timed = NARS.shell();


    /* i will make these 3 pass soon, this is an improvement on the representation
    that will make these tests pass once implemented. */

    
    @Test
    void testInternalRepresentation28() {
        testBytesRepresentation("(a&&b)", 5);
    }

    @Test
    void testInternalRepresentation28cc() {
        testBytesRepresentation("((--,(b,c))&&a)", 5);
    }







    
    @Test
    void testInternalRepresentation2z() {
        testBytesRepresentation("(a,b)", 5);
    }


    /**
     * tests whether NALOperators has been reduced to the
     * compact control character (8bits UTF) that represents it
     */

    @Test
    void testInternalRepresentation23() {
        testBytesRepresentation("x", 1);
    }

    @Test
    void testInternalRepresentation24() {
        testBytesRepresentation("xyz", 3);
    }

    @Test
    void testInternalRepresentation25() {
        testBytesRepresentation("\u00ea", 2);
    }

    @Test
    void testInternalRepresentation26() {
        testBytesRepresentation("xyz\u00e3", 3 + 2);
    }

    
    @Test
    void testInternalRepresentation27() {
        testBytesRepresentation("(a-->b)", 5);
    }












    


    private @NotNull
    static Term testBytesRepresentation(@NotNull String expectedCompactOutput, int expectedLength) {
        try {
            return testBytesRepresentation(
                    null,
                    expectedCompactOutput,
                    expectedLength);
        } catch (Narsese.NarseseException e) {
            fail(e);
            return null;
        }
    }

    private @NotNull
    static Term testBytesRepresentation(@Nullable String expectedCompactOutput, @NotNull String expectedPrettyOutput, int expectedLength) throws Narsese.NarseseException {
        
        Termed i = $.INSTANCE.$(expectedPrettyOutput);
        
        

        if (expectedCompactOutput != null)
            assertEquals(expectedCompactOutput, i.toString());

        areEqualAndIfNotWhy(expectedPrettyOutput, i.toString());


        
        return i.term();
    }

    private static void areEqualAndIfNotWhy(@NotNull String a, @NotNull String b) {
        assertEquals(a, b, new Supplier<String>() {
            @Override
            public String get() {
                return charComparison(a, b);
            }
        });
    }

    private static String charComparison(String a, String b) {
        return Arrays.toString(a.toCharArray()) + " != " + Arrays.toString(b.toCharArray());
    }












































}
