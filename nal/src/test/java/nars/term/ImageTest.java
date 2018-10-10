package nars.term;

import nars.$;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.term.atom.Int;
import nars.term.util.Image;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static nars.term.atom.Bool.Null;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageTest {

    @Test
    void testNormlizeExt() {
        assertEquals(
                "reaction(acid,base)",
                Image.imageNormalize($$("(acid --> (reaction,/,base))")).toString()
        );
        assertEquals(
                "reaction(acid)",
                Image.imageNormalize($$("(acid --> (reaction,/))")).toString()
        );

    }

    @Test
    void testNormlizeInt() {
        assertEquals(
                "(neutralization-->(acid,base))",
                Image.imageNormalize($$("((neutralization,\\,base) --> acid)")).toString()
        );
        assertEquals(
                "(neutralization-->(acid))",
                Image.imageNormalize($$("((neutralization,\\) --> acid)")).toString()
        );
    }

    @Test
    void testCanNotNormlizeIntExt() {
        assertEquals(
                "((neutralization,\\,base)-->(reaction,/,base))",
                Image.imageNormalize($$("((neutralization,\\,base) --> (reaction,/,base))")).toString()
        );
    }

    @Test
    void testNormalizeSubtermsSIM() {

        assertEquals(
                "(reaction(acid,base)<->x)",
                $$("(x <-> (acid --> (reaction,/,base)))").toString()
        );
        assertEquals(
                "(reaction(acid)<->x)",
                $$("(x <-> (acid --> (reaction,/)))").toString()
        );
    }

    @Test
    void testImageIntWithNumbers() {
        String x = "((0,1,0)-->bitmap)";
        Term xx = $$(x);

        assertEquals("(0-->(bitmap,/,1,/))",
                Image.imageExt(xx, Int.the(0)).toString());

        assertEquals($$("(0-->(bitmap,/,1,/))"),
                Image.imageExt(xx, Int.the(0)));

        assertEquals("(1-->(bitmap,0,/,0))",
                Image.imageExt(xx, Int.the(1)).toString());

        assertEquals(xx, Image.imageNormalize(Image.imageExt(xx, Int.the(1))));
        assertEquals(xx, Image.imageNormalize(Image.imageExt(xx, Int.the(0))));

    }

    @Test
    void testImagizeDepVar() {
        Term x = $$("reaction(acid,#1)");
        Term y = Image.imageExt(x, $.varDep(1));
        assertEquals("(#1-->(reaction,acid,/))", y.toString());
        assertEquals(x, Image.imageNormalize(y));
    }

    @Test void testOneArgFunctionAsImage() {
        assertEquals("(y-->(x,/))", $.funcImageLast($.the("x"), $.the("y")).toString());
    }
    @Test void testConceptualizationNormalizesImages() throws Narsese.NarseseException {
        //"$.04 ((|,(--,(cart,"+")),(--,(cart,"-")),(--,angX))-->(believe,"-ß2~czîÊeå",/))! 406461⋈406503 %.06;.04%"
        NAR n = NARS.shell();
        assertEquals("(x,z(y))", n.conceptualize("(x, (y --> (z,/)))").term().toString());
    }
    @Test void testRecursiveUnwrapping() {
        //assertEquals(
                //"reaction(acid,base)",
        Term a1 = $$("(((chemical,reaction),base)-->acid)");

        Term a2Bad = Image.imageExt(a1, $$("reaction"));
        assertEquals(Null, a2Bad); //not in the next reachable level

        Term a2 = Image.imageExt(a1, $$("(chemical,reaction)"));
        assertEquals("((chemical,reaction)-->(acid,/,base))", a2.toString());

        Term a3Bad = Image.imageInt(a2, $$("reaction"));
        assertEquals(Null, a3Bad);
//
        Term a3 = Image.imageExt(a2, $$("reaction"));
        assertEquals("(reaction-->((acid,/,base),chemical,/))", a3.toString());

        //reverse

        Term b2 = Image.imageNormalize(a3);
        assertEquals(a1, b2);

//        Term b1 = Image.imageNormalize(b2);
//        assertEquals(a1, b1);

    }

    @Test void testNonRepeatableImage() {
        Term ii = Image.imageExt($$("a(b,/,c)"), $$("c"));
        assertEquals(Null, ii);
    }

}