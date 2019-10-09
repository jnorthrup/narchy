package nars.term;

import jcog.random.XoRoShiRo128PlusRandom;
import jcog.util.ArrayUtil;
import nars.$;
import nars.Narsese;
import nars.Op;
import nars.subterm.*;
import nars.term.anon.Anom;
import nars.term.anon.Anon;
import nars.term.anon.AnonWithVarShift;
import nars.term.anon.Intrin;
import nars.term.atom.Atomic;
import nars.term.atom.Int;
import nars.term.compound.LightCompound;
import nars.term.util.builder.HeapTermBuilder;
import nars.term.util.builder.InterningTermBuilder;
import nars.term.util.builder.TermConstructor;
import nars.term.util.transform.UnifyTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.roaringbitmap.RoaringBitmap;

import java.util.Random;

import static nars.$.$;
import static nars.$.$$;
import static nars.Op.*;
import static nars.term.util.TermTest.assertEq;
import static org.junit.jupiter.api.Assertions.*;

public class IntrinTest {

    private static Anon assertAnon(String expect, String test) {
        return assertAnon(expect, $$(test));
    }

    /**
     * roundtrip test
     */
    private static Anon assertAnon(String expect, Term x) {
        Anon a = new Anon();
        Term y = a.put(x);
        Term z = a.get(y);
        assertEquals(expect, y.toString());
        assertEquals(x, z);
        return a;
    }

    private static void testAnonTermVectorProducedByTermBuilder(TermConstructor b) {
        {
            Subterms tri = b.subterms($.varDep(1), Anom.the(2), Anom.the(1));
            assertEquals(IntrinSubterms.class, tri.getClass());
        }

        {
            Term triSub = b.compound(Op.PROD, $.varDep(1), Anom.the(2), Anom.the(1));
            assertEquals(IntrinSubterms.class, triSub.subterms().getClass());
        }

        {
            Subterms bi = b.subterms($.varDep(1), Anom.the(2));
            assertEquals(IntrinSubterms.class, bi.getClass());
        }

        {
            Term biSub = b.compound(Op.PROD, $.varDep(1), Anom.the(2));
            assertEquals(IntrinSubterms.class, biSub.subterms().getClass());
        }

        {
            Subterms uni = b.subterms($.varDep(1));
            assertEquals(IntrinSubterms.class, uni.getClass());
        }
//        {
//            Term uniSub = b.compound(PROD, $.varDep(1));
//            assertEquals(IntrinSubterms.class, uniSub.subterms().getClass());
//        }
    }

    @Test
    void testAtoms() {
        assertAnon("_1", "abc");
        assertAnon("#1", $.varDep(1));

        assertNotEquals(Anom.the(0), $.the(0));
        assertNotEquals(Anom.the(2), $.the(2));
    }

    @Test
    void testThatAnonDoesntEatEllipsis() throws Narsese.NarseseException {
        assertEquals("{_1,%1..*}", UnifyTest.pattern("{abc, %X..*}").anon().toString());
    }

    @Test
    void testCompounds() {
        assertAnon("(_1-->_2)", "(abc-->bcd)");



        assertAnon("{_1}", "{abc}");
        assertAnon("{_1,_2}", "{abc,b1}");
        assertAnon("{_1,_2,_3}", "{abc,b1,c1}");
        assertAnon("(_1 ==>+- _2)", "(x1 ==>+- y1)");
        {
            Anon a = assertAnon("((_1&&_2) ==>+- _3)", "((x1&&y1) ==>+- z1)");
            assertEquals("(x1&&y1)", a.get(CONJ.the(Anom.the(1), Anom.the(2))).toString());
        }


        assertAnon("(((_1-->(_2,_3,#1))==>(_4,_5)),?2)",
                "(((a1-->(b1,c1,#2))==>(e1,f1)),?1)");

    }

    @Test void NormalizedVariables() {
        assertAnon("(_1-->#1)", "(abc-->#1)");
    }
    @Test void Integers() {
        assertEquals((Intrin.INT_POSs<<8) | 0, Intrin.id(Int.the(0)));
        assertEquals((Intrin.INT_POSs<<8) | 1, Intrin.id(Int.the(1)));
        assertEquals((Intrin.INT_POSs<<8) | 126, Intrin.id(Int.the(126)));
        assertEquals((Intrin.INT_POSs<<8) | 127, Intrin.id(Int.the(127)));
        assertEquals(0, Intrin.id(Int.the(128)));
        assertEquals((Intrin.INT_NEGs<<8) | 1, Intrin.id(Int.the(-1)));
        assertEquals((Intrin.INT_NEGs<<8) | 126, Intrin.id(Int.the(-126)));
        assertEquals((Intrin.INT_NEGs<<8) | 127, Intrin.id(Int.the(-127)));

        assertAnon("(_1-->1)", "(abc-->1)");
        assertAnon("(_1-->0)", "(abc-->0)");
        assertAnon("(_1-->-1)", "(abc-->-1)");

        assertAnon("(--,0)", "(--,0)");
        assertAnon("(--,-1)", "(--,-1)");
        assertAnon("(--,1)", "(--,1)");

        assertTrue( $$("(--,1)") instanceof Neg.NegIntrin );
        assertTrue( $$("(1,2,3)").subterms() instanceof IntrinSubterms );
        assertTrue( $$("(1,-2,3)").subterms() instanceof IntrinSubterms );
        assertTrue( $$("((--,1),-2,3)").subterms() instanceof IntrinSubterms );
    }
    @Test void Chars() {
        assertEquals((Intrin.CHARs << 8) | 'a', Intrin.id(Atomic.the('a')));
        assertEquals((Intrin.CHARs << 8) | 'A', Intrin.id(Atomic.the('A')));
        assertEquals((Intrin.CHARs << 8) | 'z', Intrin.id(Atomic.the('z')));

        assertAnon("(a-->1)", "(a-->1)");
        assertAnon("(a-->0)", "(a-->0)");
        assertAnon("(a-->-1)", "(a-->-1)");

        assertAnon("(--,a)", "(--,a)");
        assertAnon("(--,b)", "(--,b)");
        assertAnon("(--,c)", "(--,c)");

        assertTrue($$("(--,a)") instanceof Neg.NegIntrin);
    }
    @Test void Chars_Subterms() {
        assertTrue($$("(a,b,c)").subterms() instanceof IntrinSubterms);
        assertTrue($$("((--,a),b,c)").subterms() instanceof IntrinSubterms);
    }

    @Test
    void testCompoundsWithNegations() {
        assertAnon("((--,_1),_1,_2)", "((--,a1), a1, c1)");
        assertAnon("(--,((--,_1),_1,_2))", "--((--,a1), a1, c1)");
    }

    @Test
    @Disabled
    void testIntRange() throws Narsese.NarseseException {
        assertEquals("(4..6-->x)", $("((|,4,5,6)-->x)").toString());
        assertAnon("(_0-->_1)", "((|,4,5,6)-->x)");
    }

    @Test
    void testAnomVector() {

        Term[] x = {Anom.the(3), Anom.the(1), Anom.the(2)};

        assertEq(new UniSubterm(x[0]), new IntrinSubterms(x[0]));
        assertEq(new UniSubterm(x[0]), new TermList(x[0]));

        assertEq(new BiSubterm(x[0], x[1]), new IntrinSubterms(x[0], x[1]));
        assertEq(new BiSubterm(x[0], x[1]), new TermList(x[0], x[1]));

        assertEq(new ArrayTermVector(x), new IntrinSubterms(x));
        assertEq(new ArrayTermVector(x), new TermList(x));

    }

    static final Term[] x = {Anom.the(3), Anom.the(1), Anom.the(2).neg()};

    @Test
    void testAnomVectorNegations() {


        IntrinSubterms av = new IntrinSubterms(x);
        ArrayTermVector bv = new ArrayTermVector(x);
        assertEq(bv, av);

        assertFalse(av.contains(x[0].neg()));
        assertFalse(av.containsRecursively(x[0].neg(), false, null));

        assertFalse(av.contains($$("x")));
        assertFalse(av.containsRecursively($$("x"), false, null));
        assertFalse(av.contains($$("x").neg()));
        assertFalse(av.containsRecursively($$("x").neg(), false, null));

        Term twoNeg = x[2];
        assertTrue(av.contains(twoNeg));
        assertTrue(av.containsRecursively(twoNeg, false, null));
        assertTrue(av.containsRecursively(twoNeg.neg(), false, null), () -> av + " containsRecursively " + twoNeg.neg());


    }

    @Test
    void testMixedAnonVector() {

        Term[] x = {$.varDep(1), $.varIndep(2), $.varQuery(3), Anom.the(4)};
        Random rng = new XoRoShiRo128PlusRandom(1);
        for (int i = 0; i < 4; i++) {

            ArrayUtil.shuffle(x, rng);

            assertEq(new UniSubterm(x[0]), new IntrinSubterms(x[0]));
            assertEq(new BiSubterm(x[0], x[1]), new IntrinSubterms(x[0], x[1]));
            assertEq(new ArrayTermVector(x), new IntrinSubterms(x));
        }
    }

//    @Test public void testAnonSortingOfRepeats() {
//        assertAnon("(_1,_1,_2)", "(1,1,2)");
//        assertAnon("(_2,_1,_1)", "(1,2,2)");
//    }

    @Test
    void testAnonSorting() {
        assertAnon("(&&,(--,_1),_2,_3,_4,_5)", "(&&,x1,x2,--x3,x4,x5)");
        assertAnon("(&&,(--,_1),_2,_3,_4,_5)", "(&&,--x1,x2,x3,x4,x5)");
        assertAnon("(_2(_1)&&_3)", "(&&,b1(a1),x3)");
        assertAnon("(_2(_1)&&_3)", "(&&,b1(a1),x1)");
        assertAnon("((_2(_1)&&_3) &&+- _4)", "((&&,x3(a0),x1) &&+- x4)");
        assertAnon("((_2(_1)&&_3) &&+- _4)", "(x1 &&+- (&&,b1(a2),x4))");
    }

    @Test
    void testTermSubs() {
        Compound x = (Compound) $$("(%1,%2)").normalize();
        assertEquals(IntrinSubterms.class, x.subterms().getClass());
        for (Subterms t: new Subterms[]{x, x.subterms()}) {
            assertEquals(2, t.count(Op.VAR_PATTERN));
            assertEquals(0, t.count(Op.VAR_DEP));
        }

        Compound y = (Compound) $$("(%1,%2,(--,$3))").normalize();
        assertEquals(IntrinSubterms.class, y.subterms().getClass());
        for (Subterms t: new Subterms[]{y, y.subterms()}) {
            assertEquals(2, t.count(Op.VAR_PATTERN));
            assertEquals(2, t.count(Op.VAR_PATTERN));
            assertEquals(0, t.count(Op.VAR_INDEP));
            assertEquals(1, t.count(Op.NEG));
        }
    }

    @Test
    void testAutoNormalization() throws Narsese.NarseseException {
        for (String s: new String[]{"($1)", "($1,$2)", "($1,#2)", "(%1,%1,%2)"}) {
            Term t = $$(s);
            assertEquals(s, t.toString());
            assertTrue(
                    UniSubterm.class == t.subterms().getClass() ||
                            IntrinSubterms.class == t.subterms().getClass());
            assertTrue(t.isNormalized(), () -> t + " not auto-normalized but it could be");
        }
        for (String s: new String[]{"($2)", "($2,$1)", "($1,#3)", "(%1,%3,%2)"}) {
            Term t = Narsese.term(s, false);
            assertEquals(s, t.toString());
            assertTrue(
                    UniSubterm.class == t.subterms().getClass() ||
                            IntrinSubterms.class == t.subterms().getClass(),
                    () -> t.getClass().toString() + ' ' + t.subterms().getClass());
            assertFalse(t.isNormalized(), () -> t + " auto-normalized but should not be");
        }
    }

    @Test
    void testAnonTermVectorProducedByHeapTermBuilder() {
        testAnonTermVectorProducedByTermBuilder(HeapTermBuilder.the);
    }

    @Test
    void testAnonTermVectorProducedByInterningTermBuilder() {
        testAnonTermVectorProducedByTermBuilder(new InterningTermBuilder());
    }

    @Test
    void testAnonVectorTransform() {
        //TODO
    }

    @Test
    void testAnonVectorReplace() {
        IntrinSubterms xx = (IntrinSubterms)
            Op.terms.subterms($.varDep(1), Anom.the(2).neg(), Anom.the(1));

        Term x = new LightCompound(PROD, xx);
        
        {
            Subterms yAnon = x.replace($.varDep(1), Anom.the(3)).subterms();
            assertEquals("(_3,(--,_2),_1)", yAnon.toString());
            assertEquals(x.subterms().getClass(), yAnon.getClass(), "should remain AnonVector, not something else");

            Subterms yNotFound = x.replace($.varDep(4), Anom.the(3)).subterms();
            assertSame(x.subterms(), yNotFound);
        }

        {
            Subterms yAnon = x.replace(Anom.the(2).neg(), Anom.the(3)).subterms();
            assertEquals("(#1,_3,_1)", yAnon.toString());
            assertEquals(x.subterms().getClass(), yAnon.getClass(), "should remain AnonVector, not something else");

            Subterms yNotFound = x.replace(Anom.the(1).neg(), Anom.the(3)).subterms();
            assertSame(x.subterms(), yNotFound);
        }


        {
            Subterms yAnon = x.replace(Anom.the(2), Anom.the(3)).subterms();
            assertEquals("(#1,(--,_3),_1)", yAnon.toString());
            assertEquals(x.subterms().getClass(), yAnon.getClass(), "should remain AnonVector, not something else");
        }
        {
            Subterms yAnon = x.replace(Anom.the(2), Anom.the(3).neg()).subterms();
            assertEquals("(#1,_3,_1)", yAnon.toString());
            assertEquals(x.subterms().getClass(), yAnon.getClass(), "should remain AnonVector, not something else");
        }

        {
            Subterms yNonAnon = x.replace($.varDep(1), PROD.the($.the("X"))).subterms();
            assertEquals("((X),(--,_2),_1)", yNonAnon.toString());
            assertNotEquals(x.subterms().getClass(), yNonAnon.getClass());

            Subterms yNotFound = x.replace(PROD.the($.the("X")), PROD.the($.the("Y"))).subterms();
            assertSame(x.subterms(), yNotFound);

        }

        {
            Term xxx = new LightCompound(PROD, Op.terms.subterms($.varDep(1), Anom.the(2).neg(), Anom.the(2)));
            assertEquals("(#1,(--,_3),_3)", xxx.replace(Anom.the(2), Anom.the(3)).toString());
            assertEquals("(#1,_3,_2)", xxx.replace(Anom.the(2).neg(), Anom.the(3)).toString());
        }


        {
            Term xxx = new LightCompound(PROD, Op.terms.subterms($.varDep(1), Anom.the(2).neg(), Anom.the(2)));
            assertEquals("(#1,(--,()),())", xxx.replace(Anom.the(2), Op.EmptyProduct).toString());
            assertEquals("(#1,(),_2)", xxx.replace(Anom.the(2).neg(), Op.EmptyProduct).toString());
        }

    }

    @Test void ShiftWithIndexGap() {
        //in ths example,
        // because there is no #1 variable,
        // the shift must replace x's #2 with #3 (not #2) which would collapse against itself
        Term x = $$("(Level(low) ==>+1 ((--,At(#1))&&At(#2)))");
        Term b = $$("(_1($1) ==>+1 ((--,_1($1))&&_1(#2)))");
        Term y = new AnonWithVarShift(16, Op.VAR_DEP.bit | Op.VAR_QUERY.bit).putShift(x, b, null);
        assertEquals("(_2(_1) ==>+1 ((--,_3(#3))&&_3(#4)))", y.toString());
    }

    @Test void ConjSeq() {
        //0:((--,(tetris-->rotate))&&#_f),690:((--,(tetris-->right))&&(--,(tetris-->rotate))),800:(tetris-->left),3520:left(#1,#2)
        String t = "((((--,x)&&#_f) &&+690 ((--,x)&&(--,y))) &&+800 (z &&+3520 w))";
        Term T = $$(t);
        //assertEq(t, T);
        assertEquals(T.volume(), T.anon().volume(), ()->"difference:\n" + T + "\n" + T.anon());
    }

    @Test void testPosInts_gt127() {
        RoaringBitmap b = new RoaringBitmap();
        b.add(134); b.add(135); b.add(136);
        assertEq("x({{134,135,136}})", $.func("x", SETe.the($.sete(b)))); //beyond 127 intern limit
    }
    @Test void testNegInts_lt_minus127() {
        RoaringBitmap b = new RoaringBitmap();
        b.add(-134); b.add(-135); b.add(-136);
        assertEq("x({{-136,-135,-134}})", $.func("x", SETe.the($.sete(b)))); //beyond 127 intern limit
    }

}