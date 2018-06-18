package nars.term.compound;

import nars.Narsese;
import nars.term.Compound;
import org.junit.jupiter.api.Test;

import static nars.$.$;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by me on 2/19/17.
 */
class SerialCompoundTest {

    @Test
    void test1() throws Narsese.NarseseException {

        
        
        
        assertEqual("a:b");
        assertEqual("(a ==>+1 b)");
        assertEqual("(&&,(MedicalCode-->MedicalIntangible),(MedicalIntangible-->#1),(SuperficialAnatomy-->#1),label(MedicalCode,MedicalCode),label(MedicalIntangible,MedicalIntangible),label(SuperficialAnatomy,SuperficialAnatomy))");

    }

    private static void assertEqual(String x) throws Narsese.NarseseException {
        assertEqual($(x));
    }

    private static void assertEqual(Compound x) {
        SerialCompound y = new SerialCompound(x);

        System.out.println(x + " encoded to " + y.length() + " bytes");

        assertTrue(y.length() > 1);

        Compound z = y.build();

        
        assertEquals(x, z);
        assertEquals(x.subs(), z.subs());
        assertEquals(x.volume(), z.volume());

        assertEquals(x.toString(), z.toString());
    }
}