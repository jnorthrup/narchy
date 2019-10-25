package nars.io;

import jcog.Texts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class TextsTest {

    @Test
    void testN2() {
        
        assertEquals("1.0", Texts.INSTANCE.n2(1.00f));
        assertEquals(".50", Texts.INSTANCE.n2(0.5f));
        assertEquals(".09", Texts.INSTANCE.n2(0.09f));
        assertEquals(".10", Texts.INSTANCE.n2(0.1f));
        assertEquals(".01", Texts.INSTANCE.n2(0.009f));
        assertEquals("0.0", Texts.INSTANCE.n2(0.001f));
        assertEquals(".01", Texts.INSTANCE.n2(0.01f));
        assertEquals("0.0", Texts.INSTANCE.n2(0.0f));
        
        
    }

    























































}
