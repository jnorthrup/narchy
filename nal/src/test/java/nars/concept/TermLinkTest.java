package nars.concept;

import nars.NAR;
import nars.NARS;
import nars.control.Activate;
import nars.term.Termed;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.TreeSet;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TermLinkTest {

    final NAR n = NARS.shell();

    @Test
    public void testTemplates1() {

        
        testTemplates("open:door",
                "[door, open]");
    }

    @Test
    public void testTemplates2() {
        
        testTemplates("open(John,door)",
                
                "[(John,door), John, door, open]"
        );
    }

    @Test
    public void testTemplates3() {
        
        testTemplates("(open(John,door) ==> #x)",
                
                "[open(John,door), (John,door), open, #1]"
        );
    }

    @Test
    public void testTemplates4() {
        
        testTemplates("(open(John,portal:interdimensional) ==> #x)",
                
                "[open(John,(interdimensional-->portal)), (John,(interdimensional-->portal)), open, #1]"

        );
    }

    @Test
    public void testTemplates4b() {
        testTemplates("(open(John,portal(a(d),b,c)) ==> #x)",
                
                "[open(John,portal(a(d),b,c)), (John,portal(a(d),b,c)), open, #1]"
        );
    }

    @Test
    public void testFunction() {
        testTemplates("f(x)",
                
                "[(x), f, x]"
        );
    }
    @Test
    public void testIntersection() {
        testTemplates("((0|1)-->2)",
                "[(0|1), 0, 1, 2]"
        );
    }

    @Test
    public void testTemplatesWithInt2() {
        testTemplates("num((0))",
                
                "[((0)), (0), num]"
        );
    }

    @Test
    public void testTemplatesWithInt1() {
        testTemplates("(0)",
                "[0]");
    }

    @Test
    public void testTemplatesWithQueryVar() {
        testTemplates("(x --> ?1)",
                "[x, ?1]");
    }

    @Test
    public void testTemplatesWithDepVar() {
        testTemplates("(x --> #1)",
                "[x, #1]");
    }

    @Test
    public void testTemplateConj1() {
        testTemplates("(x && y)",
                "[x, y]");
    }

    @Test public void testConjEventsNotInternalDternals() {
        testTemplates("((a&&b) &&+- (b&&c))",
                //"[(a&&b), (b&&c)]"
                "[a, b, c]"
        );
    }

    @Test public void testConjEventsNotInternalDternals2() {
        testTemplates("(&&,(a&|b),(c&|d))",
                "[(a&&b), (c&&d)]");
    }

    @Test
    public void testTemplateConjInsideConj() {
        testTemplates("(x && (y &&+1 z))",
                "[x, y, z]");
    }
    @Test
    public void testTemplateConjInsideConjInsideImpl() {
        testTemplates("(a ==> (x && y))",
                "[(x&&y), a, x, y]");
    }

    @Test
    public void testTemplateConjInsideConjInsideImpl2() {
        testTemplates("((a && b) ==> (x && (y &&+1 z)))",
                "[((y&&z) &&+- x), (a&&b)]");
    }

    @Test
    public void testTemplateConj1Neg() {
        testTemplates("(x &&+- --x)",
                "[x]");
    }

    @Test
    public void testTemplateConj2() {
        testTemplates("(&&,<#x --> lock>,(<$y --> key> ==> open($y,#x)))",
                "[(($1-->key)==>open($1,#2)), open($1,#2), (#2-->lock), ($1-->key), lock, #2]");

    }

    @Test
    public void testTemplateDiffRaw() {
        testTemplates("(x-y)",
                "[x, y]");
    }

    @Test
    public void testTemplateDiffRaw2() {
        testTemplates("((a,b)-y)",
                "[(a,b), y]");
    }

    @Test
    public void testTemplateProd() {
        testTemplates("(a,b)",
                "[a, b]");
    }

    @Test
    public void testTemplateProdWithCompound() {
        testTemplates("(a,(b,c))",
                "[(b,c), a]");
    }

    @Test
    public void testTemplateSimProd() {
        testTemplates("(c<->a)",
                "[a, c]");
    }
    @Test
    public void testInheritSet() {
        testTemplates("(x-->[y])",
                "[[y], x, y]");
    }
    @Test
    public void testImplicateInhSet() {
        testTemplates("(($1-->[melted])=|>($1-->[pliable]))",
                "[($1-->[pliable]), ($1-->[melted]), [pliable], [melted], $1]");
    }
    @Test
    public void testImageExt() {

        testTemplates("(chronic-->(trackXY,happy,/))",
                "[(trackXY,happy,/), happy, chronic, trackXY]");
    }

    @Test
    public void testImageExtWithNumbers() {
        testTemplates("(1-->(bitmap,0,/))",
                "[(bitmap,0,/), bitmap, 0, 1]");
    }


    @Test
    public void testTemplateSimProdCompound() {
        testTemplates("((a,b)<->#1)",
                "[(a,b), a, b, #1]");
    }







    void testTemplates(String term, String expect) {
        
        Concept c = n.conceptualize($$(term));
        Activate a = new Activate(c, 0.5f);
        Collection<Termed> t = new TreeSet(c.templates());
        assertEquals(expect, t.toString());
    }

}
