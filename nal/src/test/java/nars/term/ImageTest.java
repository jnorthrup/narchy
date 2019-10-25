package nars.term;

import nars.$;
import nars.Narsese;
import nars.subterm.util.TermMetadata;
import nars.term.atom.IdempotentBool;
import nars.term.atom.IdempotInt;
import nars.term.compound.CachedCompound;
import nars.term.compound.LighterCompound;
import nars.term.util.Image;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.$.*;
import static nars.Op.INH;
import static nars.term.atom.IdempotentBool.Null;
import static nars.term.atom.IdempotentBool.True;
import static nars.term.util.Image.imageNormalize;
import static nars.term.util.TermTest.assertEq;
import static org.junit.jupiter.api.Assertions.*;

/** the NAL "Image" operator, not bitmaps or photos */
class ImageTest {

    @Test void PreNormalize() {
        Term t = INSTANCE.$$("(acid --> (reaction,/,base))");
        assertEquals(CachedCompound.SimpleCachedCompound.class, t.getClass());
        t.isNormalized();
    }

    @Test void CompoundSubtermsNormalized() {
        assertEq("(x,reaction(acid,base))", "(x,(acid --> (reaction,/,base)))");
        assertEq("(x,reaction(#1,#2))", "(x,(#1 --> (reaction,/,#2)))");
        assertEq("(reaction(#1,#2)&&x)", "(x && (#1 --> (reaction,/,#2)))");
        assertEq("(x &&+1 reaction(#1,#2))", "(x &&+1 (#1 --> (reaction,/,#2)))");
        assertEq("((x &&+1 y) &&+1 reaction(#1,#2))", "(x &&+1 (y &&+1 (#1 --> (reaction,/,#2))))");
        assertEq("((x &&+1 (y||(--,x))) &&+1 reaction(#1,#2))", "(x &&+1 ((y||--x) &&+1 (#1 --> (reaction,/,#2))))");
    }

    @Test
    void testNormlizeExt() {
        assertEq(
                "reaction(acid,base)",
                imageNormalize(INSTANCE.$$("(acid --> (reaction,/,base))"))
        );
        assertEq(
                "reaction(acid,base)",
                imageNormalize(INSTANCE.$$("(base --> (reaction,acid,/))"))
        );

        assertEq(
                "reaction(acid)",
                imageNormalize(INSTANCE.$$("(acid --> (reaction,/))"))
        );


    }



    @Test
    void testNormlizeInt() {

        assertEq(
                "(neutralization-->(acid,base))",
                imageNormalize(INSTANCE.$$("((neutralization,\\,base) --> acid)"))
        );
        assertEq(
                "(neutralization-->(acid,base))",
                imageNormalize(INSTANCE.$$("((neutralization,acid,\\) --> base)"))
        );
        assertEq(
                "(neutralization-->(acid))",
                imageNormalize(INSTANCE.$$("((neutralization,\\) --> acid)"))
        );

    }

    @Test
    void testCanNotNormlizeIntExt() {
        assertEq(
                "((neutralization,\\,base)-->(reaction,/,base))",
                imageNormalize(INSTANCE.$$("((neutralization,\\,base) --> (reaction,/,base))"))
        );
    }

    @Test
    void testNormalizeSubtermsSIM() {

        assertEq(
                "(reaction(acid,base)<->x)",
                INSTANCE.$$("(x <-> (acid --> (reaction,/,base)))")
        );
        assertEq(
                "(reaction(acid)<->x)",
                INSTANCE.$$("(x <-> (acid --> (reaction,/)))")
        );
    }

    @Test
    void testImageIntWithNumbers() {
        String x = "((0,1,0)-->bitmap)";
        Term xx = INSTANCE.$$(x);

        assertEq("(0-->(bitmap,/,1,/))",
                Image.imageExt(xx, IdempotInt.the(0)));

        assertEq(INSTANCE.$$("(0-->(bitmap,/,1,/))"),
                Image.imageExt(xx, IdempotInt.the(0)));

        assertEq("(1-->(bitmap,0,/,0))",
                Image.imageExt(xx, IdempotInt.the(1)));

        assertEq(xx, imageNormalize(Image.imageExt(xx, IdempotInt.the(1))));
        assertEq(xx, imageNormalize(Image.imageExt(xx, IdempotInt.the(0))));

    }

    @Test
    void testImagizeDepVar() {
        Term x = INSTANCE.$$("reaction(acid,#1)");
        Term y = Image.imageExt(x, $.INSTANCE.varDep(1));
        assertEquals("(#1-->(reaction,acid,/))", y.toString());
        assertEquals(x, imageNormalize(y));
    }

    @Test void OneArgFunctionAsImage() {
        assertEquals("(y-->(x,/))", $.INSTANCE.funcImg($.INSTANCE.the("x"), $.INSTANCE.the("y")).toString());
    }
    @Test void ConceptualizationNormalizesImages() throws Narsese.NarseseException {
        //"$.04 ((|,(--,(cart,"+")),(--,(cart,"-")),(--,angX))-->(believe,"-ß2~czîÊeå",/))! 406461⋈406503 %.06;.04%"

        Term t = Narsese.term("(x, (y --> (z,/)))",false);


        assertFalse(TermMetadata.normalized(t.subterms()));
        assertFalse(t.subterms().isNormalized());
        assertFalse(t.isNormalized());

    }
    @Test void RecursiveUnwrapping() {
        //assertEquals(
        //"reaction(acid,base)",
        Term a1 = INSTANCE.$$("(((chemical,reaction),base)-->acid)");

        Term a2Bad = Image.imageExt(a1, INSTANCE.$$("reaction"));
        assertEquals(Null, a2Bad); //not in the next reachable level

        Term a2 = Image.imageExt(a1, INSTANCE.$$("(chemical,reaction)"));
        assertEquals("((chemical,reaction)-->(acid,/,base))", a2.toString());

        Term a3Bad = Image.imageInt(a2, INSTANCE.$$("reaction"));
        assertEquals(Null, a3Bad);

        Term a3 = Image.imageExt(a2, INSTANCE.$$("reaction"));
        assertEquals("(reaction-->((acid,/,base),chemical,/))", a3.toString());

        //reverse

        Term b2 = imageNormalize(a3);
        assertEquals(a1, b2);

//        Term b1 = Image.imageNormalize(b2);
//        assertEquals(a1, b1);

    }

    @Test void NonRepeatableImage() {
        assertEq("(c-->(a,b,/,/))", Image.imageExt(INSTANCE.$$("a(b,/,c)"), INSTANCE.$$("c"))); //TODO test
    }
    @Test void RepeatableImageInSubterm() {
        assertEq("(b-->(a,/,(c,/)))", Image.imageExt(INSTANCE.$$("a(b,(c,/))"), INSTANCE.$$("b")));
        assertEq("((c,/)-->(a,b,/))", Image.imageExt(INSTANCE.$$("a(b,(c,/))"), INSTANCE.$$("(c,/)")));
    }

    @Disabled
    @Test void OppositeImageOK() {
        assertEq("(c-->(a,b,\\,/))", Image.imageExt(INSTANCE.$$("a(b,\\,c)"), INSTANCE.$$("c")));
    }

    @Test void NormalizeVsImageNormalize() {
        Term x = INSTANCE.$$("(acid-->(reaction,/))");
        assertTrue(x.isNormalized());
        assertEquals(
                "(acid-->(reaction,/))",
                x.normalize().toString()
        );
        assertNotEquals(x.normalize(), imageNormalize(x));
    }

    @Test void NormalizeVsImageNormalize2() throws Narsese.NarseseException {
        Term x = Narsese.term("((--,(4-->(ang,fz,/))) ==>-7600 ((race-->fz),((8,5)-->(cam,fz))))", false);
        assertFalse(x.isNormalized());

        String y = "((--,ang(fz,4)) ==>-7600 ((race-->fz),((8,5)-->(cam,fz))))";
        assertTrue(Narsese.term(y, false).isNormalized());

        Term xx = x.normalize();

        assertEquals(y, xx.toString());

        assertEquals(INSTANCE.$$(y), xx);
    }

    @Test void testImageExtNeg() {
        assertEq(    "(TRUE-->((isRow,tetris,/),14,/))",
            Image.imageExt(INSTANCE.$$("((14,TRUE)-->(isRow,tetris,/))"), INSTANCE.$$("TRUE")));
        assertEq("(--,(TRUE-->((isRow,tetris,/),14,/)))",
            Image.imageExt(INSTANCE.$$("((14,(--,TRUE))-->(isRow,tetris,/))"), INSTANCE.$$("--TRUE")));
    }

    @Test void ConjNegatedNormalizeWTF() {
        assertEq("((--,(delta-->vel)) &&+280 (--,vel(fz,y)))", "((--,(delta-->vel)) &&+280 (--,(y-->(vel,fz,/))))");
    }
    @Disabled @Test void bothImageTypesInProduct() {
        assertEq("TODO", Image.imageNormalize(INSTANCE.$$("((b,\\,c,/)-->a)")));
        assertEq("TODO", Image.imageNormalize(INSTANCE.$$("(a-->(b,\\,c,/))")));
    }
    @Test void ConjNegatedNormalizeWTF2() {
        assertEq("(((--,v(fz,x)) &&+2 (--,v(fz,y))) &&+1 z)",
                "(((--,(x-->(v,fz,/))) &&+2 (--,(y-->(v,fz,/)))) &&+1 z)");

        assertEq("((((--,v(fz,x))&&(--,v(fz,y))) &&+2 (--,v(fz,y))) &&+1 z)",
                "(((&&,(--,(x-->(v,fz,/))),(--,(y-->(v,fz,/)))) &&+2 (--,(y-->(v,fz,/)))) &&+1 z)");

        assertEq("(((&&,(--,v(fz,x)),(--,v(fz,y)),w) &&+2 (--,v(fz,y))) &&+1 z)",
                "(((&&,(--,(x-->(v,fz,/))),(--,(y-->(v,fz,/))),w) &&+2 (--,(y-->(v,fz,/)))) &&+1 z)");
    }

    @Test void ImageProductNormalized() throws Narsese.NarseseException {
        Term x = Narsese.term("(y,\\,x)", false);
        assertTrue(x.isNormalized());
    }
    @Test void ImageRecursionFilter() {


        Term x0 = INSTANCE.$$("(_ANIMAL-->((cat,ANIMAL),cat,/))");
        assertEq("((cat,_ANIMAL)-->(cat,ANIMAL))", imageNormalize(x0)); //to see what would happen

        assertEq(IdempotentBool.True, Image.normalize(new LighterCompound(INH, INSTANCE.$$("ANIMAL"), INSTANCE.$$("((cat,ANIMAL),cat,/)")), true, true));

        Term x = INSTANCE.$$("(ANIMAL-->((cat,ANIMAL),cat,/))");
        assertEquals(True, x);


    }
}