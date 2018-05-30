package nars.term;

import nars.*;
import nars.index.concept.ConceptIndex;
import nars.index.concept.MapConceptIndex;
import nars.index.concept.MaplikeConceptIndex;
import nars.term.atom.Atomic;
import nars.util.TimeAware;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;


public class ConceptIndexTest {





    @Test
    public void testTaskTermSharing1() throws Narsese.NarseseException {

        NAR t = NARS.shell();

        String term = "<a --> b>.";

        Task t1 = t.inputTask(term);
        Task t2 = t.inputTask(term);
        t.run();

        testShared(t.concept(t1), t.concept(t2));

    }

    static void testIndex(@NotNull ConceptIndex i) throws Narsese.NarseseException {
        NAR t = NARS.shell();
        i.init(t);

        testTermSharing(i);
        
    }



























    @Disabled
    @Test public void testTermSharing5c() throws Narsese.NarseseException {
        testIndex(
                new MapConceptIndex(new HashMap(1024))
        );
        
    }













    static void testTermSharing(@NotNull ConceptIndex tt) throws Narsese.NarseseException {

        tt.init(NARS.shell());
        testShared(tt, "<<x-->w> --> <y-->z>>");
        testShared(tt, "<a --> b>");
        testShared(tt, "(c, d)");
        testShared(tt, "<e <=> f>");
        

        
        

    }















    private void testNotShared(@NotNull TimeAware n, @NotNull String s) throws Narsese.NarseseException {
        Termed t1 = $.$(s); 
        Termed t2 = $.$(s); 
        assertEquals(t1, t2);
        assertNotSame(t1, t2);
    }

    private static void testShared(@NotNull ConceptIndex i, @NotNull String s) throws Narsese.NarseseException {

        int t0 = i.size();
        

        Term a = i.get(Narsese.term(s, true), true).term(); 

        int t1 = i.size();
        

        
        if (a instanceof Compound) {
            assertTrue(t0 < t1);
        }

        Term a2 = i.get(Narsese.term(s, true), true).term(); 
        testShared(a, a2);

        assertEquals(i.size(), t1 /* unchanged */);
        

        

        
        Compound b = (Compound) i.get(Narsese.term('(' + s + ')', true), true).term();
        testShared(a.term(), b.sub(0));

        assertEquals(i.size(), t1 + 1 /* one more for the product container */);

        

        

        
        
    }

    static void testShared(@NotNull Termed t1, @NotNull Termed t2) {
        

        assertEquals(t1.term(), t2.term());
        if (t1 != t2)
            System.err.println("share failed: " + t1 + ' ' + t1.getClass() + ' ' + t2 + ' ' + t2.getClass());

        assertEquals(t1, t2);
        assertSame(t1, t2);

        if (t1 instanceof Compound) {
            
            for (int i = 0; i < t1.term().subs(); i++)
                testShared(t1.sub(i), t2.sub(i));
        }
    }

    @Disabled
    @Test
    public void testRuleTermsAddedToMemoryTermIndex() {
        
        NAR d = NARS.shell();
        Set<Term> t = new TreeSet();
        d.concepts.forEach(x -> t.add(x.term()));

        assertTrue(t.size() > 100); 

        

    }




























    @Test public void testCommonPrefix1() {
        testCommonPrefix(true);
    }
    @Test public void testCommonPrefix2() {
        testCommonPrefix(false);
    }

    public static void testCommonPrefix(boolean direction) {
        MaplikeConceptIndex i = (MaplikeConceptIndex)(NARS.shell().concepts);
        Atomic sui = Atomic.the("substituteIfUnifies");
        Atomic su = Atomic.the("substitute");

        if (direction) {
            i.get(sui, true);
            i.get(su, true);
        } else { 
            i.get(su, true);
            i.get(sui, true);
        }


        System.out.println(i);
        i.print(System.out);

        
        assertEquals(sui, i.concept(sui, false).term());
        assertEquals(su, i.concept(su, false).term());
        assertNotEquals(sui, i.concept(su, false).term());

    }

    @Test public void testConceptualizable() throws Narsese.NarseseException {
        Compound c = $.$("(((#1,#2,a02)-->#3)&&((#1,#2,a32)-->#3))");
        assertTrue(c.isNormalized());
        assertTrue(Task.validTaskTerm(c, (byte) 0, true));
    }
}