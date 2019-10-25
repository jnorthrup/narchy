package nars.io;

import nars.$;
import nars.DummyNAL;
import nars.Narsese;
import nars.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Variable;
import nars.term.anon.Anom;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.IdempotentBool;
import nars.term.atom.IdempotInt;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

import static java.lang.System.out;
import static nars.$.*;
import static nars.$.*;
import static nars.Op.*;
import static nars.term.util.TermTest.assertEq;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Term serialization
 */
class TermIOTest {

//    private final NAR nar = NARS.shell();
    final DummyNAL nal = new DummyNAL();

    private static byte[] assertEqualSerialize(@NotNull String orig) throws Narsese.NarseseException, IOException {
        return assertEqualSerialize($.INSTANCE.$(orig).term());
    }

    private void assertEqualTask(@NotNull String orig) throws Narsese.NarseseException, IOException {
        //assertEqualSerialize((Object)nar.inputTask(orig));
        assertEqualSerialize(Narsese.task(orig, nal));
    }

    private static byte[] assertEqualSerialize(@NotNull Object orig) throws IOException {
        


        byte barray[];
        if (orig instanceof Task) {
            Task torig = (Task) orig;
            if (torig.isDeleted())
                throw new RuntimeException("task is deleted already");
            barray = IO.taskToBytes(torig);
        } else if (orig instanceof Term)
            barray = IO.termToBytes((Term) orig);
        else
            throw new RuntimeException("");

        out.println(orig + "\n\tserialized: " + barray.length + " bytes " + Arrays.toString(barray));


        Object copy;
        if (orig instanceof Task)
            copy = TaskIO.bytesToTask(barray);
        else if (orig instanceof Term)
            copy = IO.bytesToTerm(barray);
        else
            throw new RuntimeException("");

        
        
        
        
        
        

        

        
        out.println('\t' + (copy == orig ? "same" : "copy") + ": " + copy);

        

        
        assertEquals(orig, copy);
        assertEquals(copy, orig);
        assertEquals(orig.toString(), copy.toString());
        assertEquals(orig.hashCode(), copy.hashCode());
        

        
        return barray;
    }


    
    @Test
    void testTermSerialization() throws Exception {

        assertEqualSerialize("<a-->b>" /* target, not the concept */);
        assertEqualSerialize("<aa-->b>" /* target, not the concept */);
        assertEqualSerialize("<aa--><b<->c>>" /* target, not the concept */);
        
        assertEqualSerialize("exe(a,b)" /* target, not the concept */);
    }

    @Test
    void testNegationSerialization() throws Exception {
        assertEqualSerialize("--x");

        
        assertEquals(1,
                IO.termToBytes(INSTANCE.$$("(x)")).length -
                IO.termToBytes(INSTANCE.$$("(--,x)")).length);
    }

    @Test
    void testTemporalSerialization() throws Exception {

        byte[] atemporal = assertEqualSerialize("(a && b)" /* target, not the concept */);
        byte[] temporal1 = assertEqualSerialize("(a &&+1 b)" /* target, not the concept */);
        byte[] temporal2 = assertEqualSerialize("(a &&+100 b)" /* target, not the concept */);
        byte[] temporal3 = assertEqualSerialize("(a &&+100000 b)" /* target, not the concept */);
        byte[] temporal4 = assertEqualSerialize("(a &&+10000000 b)" /* target, not the concept */);

        //test zig-zag variable encoding
        assertEquals(temporal1.length, atemporal.length + 1);
        assertEquals(temporal2.length, temporal1.length + 1);
        assertEquals(temporal3.length, temporal2.length + 1);
        assertEquals(temporal4.length, temporal3.length + 1);

        assertEqualSerialize("(a &&+1 (a &&+1 a))" /* target, not the concept */);
        assertEqualSerialize("(a ==>+1 b)" /* target, not the concept */);
        assertEqualSerialize("(b ==>+1 b)" /* target, not the concept */);

        assertEqualSerialize("(a ==>+- b)");
        assertEqualSerialize("(a ==>+- a)");
        assertEqualSerialize("(a ==> b)");

    }

    @Test
    void testImageSerialization() throws Exception {
        assertEqualSerialize("/");
        assertEqualSerialize("\\");
        assertEqualSerialize("(a,/,1)");
        assertEqualSerialize("(a,/,1,/,x)");
        assertEqualSerialize("(x --> (a,/,1))");
        assertEqualSerialize("(a,\\,1)");
        assertEqualSerialize("(a,\\,1,\\,2)");
        assertEqualSerialize("((a,\\,1)--> y)");
    }

    @Test
    void testUnnormalizedVariableSerialization() throws Exception {
        assertEqualSerialize("#abc");
        assertEqualSerialize("$abc");
        assertEqualSerialize("?abc");
        assertEqualSerialize("%abc");
    }

    @Test
    void testCommonVariableSerialization() throws Exception {
        assertEqualSerialize("##1#2");
    }

    @Test
    void testAnonSerialization() throws IOException {

        Term[] anons = {
                $.INSTANCE.v(VAR_DEP, (byte)1),
                $.INSTANCE.v(VAR_INDEP, (byte)1),
                $.INSTANCE.v(VAR_QUERY, (byte)1),
                $.INSTANCE.v(VAR_PATTERN, (byte)1),
                Atomic.the("x"),
                IdempotInt.the(1),
                IdempotInt.the(-1000),
                IdempotInt.the(Integer.MAX_VALUE),
                IdempotInt.the(-4),
                Anom.the(1),
                Anom.the(4),
                IdempotentBool.True, IdempotentBool.False, IdempotentBool.Null,
                ImgExt, ImgInt
        };

        for (Term a : anons) {
            a.printRecursive();
            byte[] b = assertEqualSerialize(a);

            
            if (a instanceof Anom) {
                assertEquals(2, b.length); 
            } else if (a.op() == INT) {
                assertTrue( b.length >=2 && b.length <= 6);
            } else if (a instanceof Atom) {
                assertEquals(4, b.length); 
            }
        }

        assertEqualSerialize($.INSTANCE.p( anons ));

        assertEqualSerialize($.INSTANCE.p( Anom.the(1), Anom.the(2) ));
        assertEqualSerialize($.INSTANCE.p( Anom.the(1), $.INSTANCE.varDep(2) ));
    }

    @Test void DepVarCombinations() throws IOException {
        Term a = INSTANCE.$$("(#1 &&+32 (#1 &&+24 #1))");
        Term b = INSTANCE.$$("#1");
        Term c = INSTANCE.$$("(--,#1)");
        assertEqualSerialize(a);
        assertEqualSerialize(b);
        assertEqualSerialize(c);
        assertEqualSerialize($.INSTANCE.p(a,b));
        assertEqualSerialize($.INSTANCE.p(b,a));
        assertEqualSerialize($.INSTANCE.p(b,c));
        assertEqualSerialize($.INSTANCE.p(c,b));
        assertEqualSerialize($.INSTANCE.p(a,c));
        assertEqualSerialize($.INSTANCE.p(c,a));
        assertEqualSerialize($.INSTANCE.p(a,b,c));
        assertEqualSerialize($.INSTANCE.p(c,b,a));
        assertEqualSerialize($.INSTANCE.p(c,a,b));
        assertEqualSerialize($.INSTANCE.p(b,c,a));
        assertEqualSerialize($.INSTANCE.p(b,a,c));
        assertEqualSerialize($.INSTANCE.p(a,c,b));
        assertEq("(((#1 &&+32 #1) &&+24 #1),#1,(--,#1))", $.INSTANCE.p(a,b,c));
    }

    @Test
    void testTermSerialization2() throws Exception {
        assertTermEqualSerialize("(a-->(be))");
    }

    @Test
    void testTermSerialization3() throws Exception {
        assertTermEqualSerialize("(#1 --> b)");
    }

    @Test
    void testTermSerialization3_2() throws Exception {
        

        Variable q = $.INSTANCE.varQuery(1);
        Compound twoB = $.INSTANCE.inh($.INSTANCE.varDep(2), Atomic.the("b"));
        assertNotEquals(
                q.compareTo(twoB),
                twoB.compareTo(q));

        assertTermEqualSerialize("((#a --> b) <-> ?c)");

        Term a = INSTANCE.$("(#2-->b)");
        Term b = INSTANCE.$("?1");
        int x = a.compareTo(b);
        int y = b.compareTo(a);
        assertNotEquals((int) Math.signum(x), (int) Math.signum(y));

    }

    private static void assertTermEqualSerialize(String s) throws Narsese.NarseseException, IOException {
        Termed t = $.INSTANCE.$(s);
        assertTrue(t.term().isNormalized());
        assertEqualSerialize(t.term() /* target, not the concept */);
    }

    @Test
    void testTaskSerialization() throws Exception {
        assertEqualTask("(a-->b).");
        assertEqualTask("(a-->(b,c))!");
        assertEqualTask("(a-->(b==>c))?");
        assertEqualTask("$0.1 (b-->c)! %1.0;0.8%");
        assertEqualTask("$0.1 (b-->c)! | %1.0;0.8%");
        assertEqualTask("$0.1 (a ==>+4 (b-->c)). | %1.0;0.8%");
        assertEqualTask("$0.1 ((x,1) ==>+4 ((y,2)-->z)). | %1.0;0.8%");

        assertEqualTask("$0.3 (a-->(bd))! %1.0;0.8%");

        assertEqualTask("(x ==>+- y)?");
        assertEqualTask("(x ==>+- y)? |");
        assertEqualTask("(x ==>+- x)?");
        assertEqualTask("(x ==>+- x)? |");
        assertEqualTask("(x &&+- y)?");
        assertEqualTask("(x &&+- y)? |");
        assertEqualTask("(x &&+- x)?");
        assertEqualTask("(x &&+- x)? |");

        assertEqualTask("(x &&+- x)@");
        assertEqualTask("(x &&+- x)@ :|:");

        assertEqualTask("cmd(x,y,z);");
        assertEqualTask("(x &&+- x);");

    }




//    @Disabled
//    static class ByteMappingTest {
//
//        @Test
//        void testByteMappingAtom() throws Exception {
//            assertEquals("(0,0)=. ", map("x"));
//        }
//
//
//        @Test
//        void testByteMappingInh() throws Exception {
//            assertEquals("(0,0)=--> (1,2)=. (1,6)=. ", map("a:b"));
//        }
//
//        @Test
//        void testByteMappingCompoundDT() throws Exception {
//            assertEquals("(0,0)===> (1,2)=. (1,6)=. ",
//                    map("(a ==>+1 b)"));
//        }
//
//        @Test
//        void testByteMappingCompoundDTExt() throws Exception {
//            assertEquals("(0,0)=--> (1,2)===> (2,4)=. (2,8)=. (1,16)=. ",
//                    map("((a ==>+1 b) --> c)"));
//        }
//
//        @Test
//        void testByteMappingCompound() throws Exception {
//            assertEquals("(0,0)===> (1,2)=--> (2,4)=* (3,6)=. (3,10)=. (2,16)=. (1,20)=. ",
//                    map("(a(b,\"c\") ==>+1 d)"));
//        }
//
//        private String map(String x) throws IOException, Narsese.NarseseException {
//            return map($.$(x));
//        }
//
//        private String map(Term x) throws IOException {
//            byte[] xb = IO.termToBytes(x);
//            StringBuilder sb = new StringBuilder();
//            IO.mapSubTerms(xb, (o, depth, i) -> sb.append("(" + depth + "," + i + ")=" + o + " "));
//            return sb.toString();
//        }
//    }









}

