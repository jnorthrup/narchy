package nars.term;

import jcog.math.random.XorShift128PlusRandom;
import nars.*;
import nars.concept.builder.DefaultConceptBuilder;
import nars.index.term.TermIndex;
import nars.index.term.map.MapTermIndex;
import nars.index.term.map.MaplikeTermIndex;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;


public class TermIndexTest {

    public static final XorShift128PlusRandom rng = new XorShift128PlusRandom(2);
    public static final DefaultConceptBuilder defaultConceptBuilder = new DefaultConceptBuilder(
    );

    @Test
    public void testTaskTermSharing1() throws Narsese.NarseseException {

        NAR t = NARS.shell();

        String term = "<a --> b>.";

        Task t1 = t.inputTask(term);
        Task t2 = t.inputTask(term);
        t.run();

        testShared(t.concept(t1), t.concept(t2));

    }

    static void testIndex(@NotNull TermIndex i) throws Narsese.NarseseException {
        NAR t = NARS.shell();
        i.start(t);

        testTermSharing(i);
        //testSequenceNotShared(i);
    }

//    @Test public void testTermSharing1() {
//        testIndex(new MapIndex(new HashMap(), new HashMap()));
//    }
//    @Test public void testTermSharing2() {
//        testIndex(new MapIndex(new UnifriedMap(), new UnifriedMap()));
//    }
//    @Test public void testTermSharing3() {
//        testIndex(new MapIndex(new SoftValueHashMap(), new SoftValueHashMap()));
//    }

//
//    @Test public void testTermSharing5a() {
//        testIndex(new GroupedMapIndex(new HashMap<>(),
//                new DefaultConceptBuilder(
//                    new XorShift128PlusRandom(2)
//                )));
//    }
//    @Test public void testTermSharing5b() {
//        testIndex(
//                new GroupedMapIndex(Global.newHashMap(),
//                    new DefaultConceptBuilder(
//                        new XorShift128PlusRandom(2)
//                    ))
//        );
//        //testIndex(new MapIndex2(newHashMap(), conceptBuilder));
//    }
    @Disabled
    @Test public void testTermSharing5c() throws Narsese.NarseseException {
        testIndex(
                new MapTermIndex(new HashMap(1024))
        );
        //testIndex(new MapIndex2(newHashMap(), conceptBuilder));
    }
//    @Test public void testTermSharing5d() {
//        testIndex(new MapIndex1(Terms.terms, defaultConceptBuilder, new HashMap()));
//
//    }

//    @Test public void testTermSharing4() {
//        testIndex(new MapIndex(new WeakHashMap(), new WeakHashMap()));
//    }
//    @Test public void testTermSharingGuava() {
//        testIndex(new GuavaIndex());
//    }


    static void testTermSharing(@NotNull TermIndex tt) throws Narsese.NarseseException {

        tt.start(NARS.shell());
        testShared(tt, "<<x-->w> --> <y-->z>>");
        testShared(tt, "<a --> b>");
        testShared(tt, "(c, d)");
        testShared(tt, "<e <=> f>");
        //testShared(tt, "g"); //atoms are special, their concept and the atom are not the same

        //tt.print(System.out);
        //System.out.println();

    }


//    public void testSequenceNotShared(TermIndex i) {
//        NAR n = new Terminal(i);
//
//        Termed a = n.term("(&/, 1, /2)");
//        assertNull(n.memory.index.getTermIfPresent(a));
//
//        Termed b = n.term("(&/, 1, /3)");
//        assertNull(n.memory.index.getTermIfPresent(b));
//
//        assertFalse(((Sequence)a).equals2((Sequence) b.term()));
//
//    }

    private void testNotShared(@NotNull NAR n, @NotNull String s) throws Narsese.NarseseException {
        Termed t1 = $.$(s); //create by parsing
        Termed t2 = $.$(s); //create by parsing again
        assertEquals(t1, t2);
        assertNotSame(t1, t2);
    }

    private static void testShared(@NotNull TermIndex i, @NotNull String s) throws Narsese.NarseseException {

        int t0 = i.size();
        //int s0 = i.subtermsCount();

        Term a = i.get(Narsese.term(s, true), true).term(); //create by parsing then manually intern it

        int t1 = i.size();
        //int s1 = i.subtermsCount();

        //some terms and subterms were added
        if (a instanceof Compound) {
            assertTrue(t0 < t1);
        }

        Term a2 = i.get(Narsese.term(s, true), true).term(); //create by parsing again
        testShared(a, a2);

        assertEquals(i.size(), t1 /* unchanged */);
        //assertEquals(i.subtermsCount(), s1 /* unchanged */);

        //i.print(System.out); System.out.println();

        //create by composition
        Compound b = (Compound) i.get(Narsese.term('(' + s + ')', true), true).term();
        testShared(a.term(), b.sub(0));

        assertEquals(i.size(), t1 + 1 /* one more for the product container */);

        //i.print(System.out); System.out.println();

        //assertEquals(i.subtermsCount(), s1 + 1 /* unchanged */);

        //create by transformation (substitution)
        //testShared(a, n.term(..).substMap(..
    }

    static void testShared(@NotNull Termed t1, @NotNull Termed t2) {
        //t.memory.terms.forEachTerm(System.out::println);

        assertEquals(t1.term(), t2.term());
        if (t1 != t2)
            System.err.println("share failed: " + t1 + ' ' + t1.getClass() + ' ' + t2 + ' ' + t2.getClass());

        assertEquals(t1, t2);
        assertSame(t1, t2);

        if (t1 instanceof Compound) {
            //test all subterms are shared
            for (int i = 0; i < t1.term().subs(); i++)
                testShared(t1.sub(i), t2.sub(i));
        }
    }

    @Disabled
    @Test
    public void testRuleTermsAddedToMemoryTermIndex() {
        //this.activeTasks = activeTasks;
        NAR d = NARS.shell();
        Set<Term> t = new TreeSet();
        d.terms.forEach(x -> t.add(x.term()));

        assertTrue(t.size() > 100); //approximate

        //t.forEach(System.out::println);

    }



//    @Test public void testSubtermIntern() {
//        Default n = new Default();
//        MaplikeIndex i = (MaplikeIndex)(n.concepts);
//
//        Term at = $("a");
//        TermVector a = TermVector.the(at, $("b"), $("cd"));
//        TermContainer b = ((Compound)n.concept(p(a), true).term()).subterms();
//        assertEquals(a, b);
//
//        //i.print(System.out);
//
//        //System.out.println(a.term(0));
//        //System.out.println(a.term(0));
//        //System.out.println(i.data);
//        a.forEach(bb->System.out.println(bb + " " + bb.getClass()));
//
//        Term B = b.term(0);
//        assertTrue(B instanceof AtomConcept);
//        assertTrue(at!= B);
//        assertTrue(B.term() == B);
//        assertEquals(at.toString(), B.toString());
//
//
//    }

    @Test public void testCommonPrefix1() {
        testCommonPrefix(true);
    }
    @Test public void testCommonPrefix2() {
        testCommonPrefix(false);
    }

    public static void testCommonPrefix(boolean direction) {
        MaplikeTermIndex i = (MaplikeTermIndex)(NARS.shell().terms);
        Atomic sui = Atomic.the("substituteIfUnifies");
        Atomic su = Atomic.the("substitute");

        if (direction) {
            i.get(sui, true);
            i.get(su, true);
        } else { //reverse
            i.get(su, true);
            i.get(sui, true);
        }


        System.out.println(i);
        i.print(System.out);

        //assertEquals(20 + 2, i.atoms.size());
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