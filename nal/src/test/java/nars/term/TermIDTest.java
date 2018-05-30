package nars.term;

import nars.$;
import nars.NARS;
import nars.Narsese;
import nars.util.TimeAware;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Created by me on 6/3/15.
 */
public class TermIDTest {

    final TimeAware timeAware = NARS.shell();


    /* i will make these 3 pass soon, this is an improvement on the representation
    that will make these tests pass once implemented. */

    
    @Test
    public void testInternalRepresentation28() {
        testBytesRepresentation("(a&&b)", 5);
    }

    @Test
    public void testInternalRepresentation28cc() {
        testBytesRepresentation("((--,(b,c))&&a)", 5);
    }







    
    @Test
    public void testInternalRepresentation2z() {
        testBytesRepresentation("(a,b)", 5);
    }


    /**
     * tests whether NALOperators has been reduced to the
     * compact control character (8bits UTF) that represents it
     */

    @Test
    public void testInternalRepresentation23() {
        testBytesRepresentation("x", 1);
    }

    @Test
    public void testInternalRepresentation24() {
        testBytesRepresentation("xyz", 3);
    }

    @Test
    public void testInternalRepresentation25() {
        testBytesRepresentation("\u00ea", 2);
    }

    @Test
    public void testInternalRepresentation26() {
        testBytesRepresentation("xyz\u00e3", 3 + 2);
    }

    
    @Test
    public void testInternalRepresentation27() {
        testBytesRepresentation("(a-->b)", 5);
    }












    


    @NotNull
    public Term testBytesRepresentation(@NotNull String expectedCompactOutput, int expectedLength) {
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

    @NotNull
    public Term testBytesRepresentation(@Nullable String expectedCompactOutput, @NotNull String expectedPrettyOutput, int expectedLength) throws Narsese.NarseseException {
        
        Termed i = $.$(expectedPrettyOutput);
        
        

        if (expectedCompactOutput != null)
            assertEquals(expectedCompactOutput, i.toString());

        areEqualAndIfNotWhy(expectedPrettyOutput, i.toString());


        
        return i.term();
    }

    static void areEqualAndIfNotWhy(@NotNull String a, @NotNull String b) {
        assertEquals(a, b, charComparison(a, b));
    }

    static String charComparison(String a, String b) {
        return Arrays.toString(a.toCharArray()) + " != " + Arrays.toString(b.toCharArray());
    }












































}
