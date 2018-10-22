package nars.concept;

import nars.NAR;
import nars.NARS;
import nars.term.Termed;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

import static java.util.stream.Collectors.toCollection;
import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TermLinkTest {

    private final NAR n = NARS.shell();

    @Test
    void testTemplates1() {

        
        testTemplates("open:door",
                "[door, open]");
    }

    @Test
    void testTemplates2() {
        
        testTemplates("open(John,door)",
                
                "[(John,door), open]"
        );
    }

    @Test
    void testTemplates3() {
        
        testTemplates("(open(John,door) ==> #x)",
                
                //"[open(John,door), (John,door), open, #1]"
                "[open(John,door), #1]"
        );
    }

    @Test
    void testTemplates4() {
        
        testTemplates("(open(John,portal:interdimensional) ==> #x)",
                
                //"[open(John,(interdimensional-->portal)), (John,(interdimensional-->portal)), open, #1]"
                "[open(John,(interdimensional-->portal)), #1]"

        );
    }

    @Test
    void testTemplates4b() {
        testTemplates("(open(John,portal(a(d),b,c)) ==> #x)",
                
                //"[open(John,portal(a(d),b,c)), (John,portal(a(d),b,c)), open, #1]"
                "[open(John,portal(a(d),b,c)), #1]"
        );
    }
    @Test void testImplConjTemplates2() {
        testTemplates("(( a($1,#2) &&+- b(#2)) ==>+- c($1))",
                "TODO"
        );
    }
    @Test void testImplConjTemplates2a() {
        testTemplates("(( a(z,#1) &&+- b(#1)) ==>+- c(z))",
                "TODO"
        );
    }

    @Test
    void testFunction() {
        testTemplates("f(x)",
                
                "[(x), f]"
        );
    }
    @Test
    void testIntersection() {


        String x = "[(0|1), 0, 1, 2]";
        testTemplates("(2-->(0|1))", x);
        testTemplates("((0|1)-->2)", x);
        String y = "[(0&1), 0, 1, 2]";
        testTemplates("(2-->(0&1))", y);
        testTemplates("((0&1)-->2)", y);
    }
    @Test
    void testEmbeddedIntersection() {
        testTemplates("(2-->(x<->(0|1)))", "[((0|1)<->x), 2]");
    }

    @Test
    void testTemplatesWithInt2() {
        testTemplates("num((0))", "[((0)), num]");
    }

    @Test
    void testTemplatesWithInt1() {
        testTemplates("(0)",
                "[0]");
    }

    @Test
    void testTemplatesWithQueryVar() {
        testTemplates("(x --> ?1)",
                "[x, ?1]");
    }

    @Test
    void testTemplatesWithDepVar() {
        testTemplates("(x --> #1)",
                "[x, #1]");
    }

    @Test
    void testTemplateConj1() {
        testTemplates("(x && y)",
                "[x, y]");
    }

    @Test
    void testConjEventsNotInternalDternals() {
        testTemplates("((a&&b) &&+- (b&&c))",
                //"[(a&&b), (b&&c)]"
                "[a, b, c]"
        );
    }

    @Test
    void testConjEventsNotInternalDternals2() {
        assertEquals("( &&+- ,a,b,c,d)",
                $$("(&&,(a&|b),(c&|d))").concept().toString());
        testTemplates("(&&,(a&|b),(c&|d))",
                "[a, b, c, d]"
                );
                //"( &&+- ,a,b,c,d)");
                //"[(a&&b), (c&&d)]");
    }

    @Test
    void testTemplateConjInsideConj() {
        testTemplates("(x && (y &&+1 z))",
                "[x, y, z]");

    }
    @Test
    void testTemplateConjInsideConj2() {
        testTemplates("(x &&+1 (y &&+1 z))",
                "[x, y, z]"
        );
    }

    @Test
    void testTemplateConjInsideConjInsideImpl() {
        testTemplates("(a ==> (x && y))",
                "[(x &&+- y), a, x, y]");
    }
    @Test
    void testTemplateConjInsideConjInsideImplVar() {
        testTemplates("(a ==> (x && #1))",
                "[(x &&+- #1), a, x, #1]");
    }

    @Test
    void testTemplateConjInsideConjInsideImpl2() {
        testTemplates("((a && b) ==> (x && (y &&+1 z)))",
                "[( &&+- ,x,y,z), (a &&+- b), a, b, x, y, z]");
    }

    @Test
    void testTemplateConj1Neg() {
        testTemplates("(x &&+- --x)",
                "[x]");
    }

    @Test
    void testTemplateConj2() {
        testTemplates("(&&,<#x --> lock>,(<$y --> key> ==> open($y,#x)))",
                //"[(($1-->key) ==>+- open($1,#2)), open($1,#2), (#2-->lock), ($1-->key), lock, #2]");
                "[(($1-->key) ==>+- open($1,#2)), (#2-->lock)]");

    }

    @Test
    void testTemplateDiffRaw() {
        testTemplates("(x-y)",
                "[x, y]");
    }

    @Test
    void testTemplateDiffRaw2() {
        testTemplates("((a,b)-y)",
                "[(a,b), y]");
    }

    @Test
    void testTemplateProd() {
        testTemplates("(a,b)",
                "[a, b]");
    }

    @Test
    void testTemplateProdWithCompound() {
        testTemplates("(a,(b,c))",
                "[(b,c), a]");
    }

    @Test
    void testTemplateSimProd() {
        testTemplates("(c<->a)",
                "[a, c]");
    }
    @Test
    void testTemplateSimWithIndep() {
        testTemplates("(x($1)<->y($1))",
                "[x($1), y($1), ($1), x, y]");
    }
    @Test
    void testInheritSet() {
        testTemplates("(x-->[y])",
                //"[[y], x]" +
        "[[y], x, y]");
    }
    @Test
    void testImplicateInhSet() {
        testTemplates("(($1-->[melted])=|>($1-->[pliable]))",
                "[($1-->[melted]), ($1-->[pliable]), [melted], [pliable], $1]");
    }
    @Test
    void testImageExt() {

        testTemplates("(chronic-->(trackXY,happy,/))",
                "[(trackXY,happy,/), chronic]");
    }

    @Test
    void testImageExtWithNumbers() {
        testTemplates("(1-->(bitmap,0,/))",
                "[(bitmap,0,/), 1]");
    }


    @Test
    void testTemplateSimProdCompound() {
        testTemplates("((a,b)<->#1)",
                "[(a,b), #1]");
    }

    private void testTemplates(String term, String expect) {
        
        Concept c = n.conceptualize($$(term));
        //Activate a = new Activate(c, 0.5f);
        Set<Termed> t = c.linker().targets().collect(toCollection(()->new TreeSet<>()));
        assertEquals(expect, t.toString());
    }

}
