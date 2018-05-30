package nars.concept;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.control.Activate;
import nars.term.Termed;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.TreeSet;

import static nars.$.$;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TermLinkTest {

    final NAR n = NARS.shell();

    @Test
    public void testTemplates1() throws Narsese.NarseseException {

        //layer 1:
        testTemplates("open:door",
                "[door, open]");
    }

    @Test
    public void testTemplates2() throws Narsese.NarseseException {
        //layer 2:
        testTemplates("open(John,door)",
                //"[(John,door), John, door, open]"
                "[(John,door), John, door, open]"
        );
    }

    @Test
    public void testTemplates3() throws Narsese.NarseseException {
        //layer 3:
        testTemplates("(open(John,door) ==> #x)",
                //"[open(John,door), (John,door), John, door, open, #1]"
                "[open(John,door), (John,door), open, #1]"
        );
    }

    @Test
    public void testTemplates4() throws Narsese.NarseseException {
        //dont descend past layer 3:
        testTemplates("(open(John,portal:interdimensional) ==> #x)",
                //"[open(John,(interdimensional-->portal)), (John,(interdimensional-->portal)), (interdimensional-->portal), John, open, #1]"
                "[open(John,(interdimensional-->portal)), (John,(interdimensional-->portal)), open, #1]"

        );
    }

    @Test
    public void testTemplates4b() throws Narsese.NarseseException {
        testTemplates("(open(John,portal(a(d),b,c)) ==> #x)",
                //"[open(John,portal(a(d),b,c)), (John,portal(a(d),b,c)), portal(a(d),b,c), John, open, #1]"
                "[open(John,portal(a(d),b,c)), (John,portal(a(d),b,c)), open, #1]"
        );
    }

    @Test
    public void testFunction() throws Narsese.NarseseException {
        testTemplates("f(x)",
                //"[(x), f, x]"
                "[(x), f, x]"
        );
    }
    @Test
    public void testIntersection() throws Narsese.NarseseException {
        testTemplates("((0|1)-->2)",
                "[(0|1), 0, 1, 2]"
        );
    }

    @Test
    public void testTemplatesWithInt2() throws Narsese.NarseseException {
        testTemplates("num((0))",
                //"[((0)), (0), num]"
                "[((0)), (0), num]"
        );
    }

    @Test
    public void testTemplatesWithInt1() throws Narsese.NarseseException {
        testTemplates("(0)",
                "[0]");
    }

    @Test
    public void testTemplatesWithQueryVar() throws Narsese.NarseseException {
        testTemplates("(x --> ?1)",
                "[x, ?1]");
    }

    @Test
    public void testTemplatesWithDepVar() throws Narsese.NarseseException {
        testTemplates("(x --> #1)",
                "[x, #1]");
    }

    @Test
    public void testTemplateConj1() throws Narsese.NarseseException {
        testTemplates("(x && y)",
                "[x, y]");
    }

    @Test
    public void testTemplateConjInsideConj() throws Narsese.NarseseException {
        testTemplates("(x && (y &&+1 z))",
                "[x, y, z]");
    }
    @Test
    public void testTemplateConjInsideConjInsideImpl() throws Narsese.NarseseException {
        testTemplates("(a ==> (x && y))",
                "[(x&&y), a, x, y]");
    }

    @Test
    public void testTemplateConjInsideConjInsideImpl2() throws Narsese.NarseseException {
        testTemplates("((a && b) ==> (x && (y &&+1 z)))",
                "[((y&&z) &&+- x), (a&&b), a, b, x, y, z]");
    }

    @Test
    public void testTemplateConj1Neg() throws Narsese.NarseseException {
        testTemplates("(x &&+- --x)",
                "[x]");
    }

    @Test
    public void testTemplateConj2() throws Narsese.NarseseException {
        testTemplates("(&&,<#x --> lock>,(<$y --> key> ==> open($y,#x)))",
                "[(($1-->key)==>open($1,#2)), open($1,#2), (#2-->lock), ($1-->key), lock, #2]");

    }

    @Test
    public void testTemplateDiffRaw() throws Narsese.NarseseException {
        testTemplates("(x-y)",
                "[x, y]");
    }

    @Test
    public void testTemplateDiffRaw2() throws Narsese.NarseseException {
        testTemplates("((a,b)-y)",
                "[(a,b), y]");
    }

    @Test
    public void testTemplateProd() throws Narsese.NarseseException {
        testTemplates("(a,b)",
                "[a, b]");
    }

    @Test
    public void testTemplateProdWithCompound() throws Narsese.NarseseException {
        testTemplates("(a,(b,c))",
                "[(b,c), a]");
    }

    @Test
    public void testTemplateSimProd() throws Narsese.NarseseException {
        testTemplates("(c<->a)",
                "[a, c]");
    }
    @Test
    public void testInheritSet() throws Narsese.NarseseException {
        testTemplates("(x-->[y])",
                "[[y], x, y]");
    }
    @Test
    public void testImplicateInhSet() throws Narsese.NarseseException {
        testTemplates("(($1-->[melted])=|>($1-->[pliable]))",
                "[($1-->[pliable]), ($1-->[melted]), [pliable], [melted], $1]");
    }
    @Test
    public void testImageExt() throws Narsese.NarseseException {

        testTemplates("(chronic-->(trackXY,happy,/))",
                "[(trackXY,happy,/), happy, chronic, trackXY]");
    }

    @Test
    public void testImageExtWithNumbers() throws Narsese.NarseseException {
        testTemplates("(1-->(bitmap,0,/))",
                "[(bitmap,0,/), bitmap, 0, 1]");
    }


    @Test
    public void testTemplateSimProdCompound() throws Narsese.NarseseException {
        testTemplates("((a,b)<->#1)",
                "[(a,b), a, b, #1]");
    }

//    @Test
//    public void testTemplatesAreEternal() throws Narsese.NarseseException {
//        testTemplates("a:(x ==>+1 y)",
//                "[(x==>y), a, x, y]");
//    }

    void testTemplates(String term, String expect) throws Narsese.NarseseException {
        //n.believe(term + ".");
        Concept c = n.conceptualize($(term));
        Activate a = new Activate(c, 0.5f);
        Collection<Termed> t = new TreeSet(c.templates());
        assertEquals(expect, t.toString());
    }

}
