package nars.io;

import com.google.common.collect.Sets;
import nars.*;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static java.lang.System.out;
import static nars.$.$;
import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Term serialization
 */
public class TermIOTest {

    final NAR nar = NARS.shell();

    void assertEqualSerialize(@NotNull String orig) throws Narsese.NarseseException {
        assertEqualSerialize($.$(orig).term());
    }

    void assertEqualTask(@NotNull String orig) throws Narsese.NarseseException {
        assertEqualSerialize((Object)nar.inputTask(orig));
    }

    static void assertEqualSerialize(@NotNull Object orig) {
        //final IO.DefaultCodec codec = new IO.DefaultCodec(nar.index);


        byte barray[];
        if (orig instanceof Task) {
            Task torig = (Task) orig;
            if (torig.isDeleted())
                throw new RuntimeException("task is deleted already");
            barray = IO.asBytes(torig);
        } else if (orig instanceof Term)
            barray = IO.termToBytes((Term) orig);
        else
            throw new RuntimeException("");

        out.println(orig + "\n\tserialized: " + barray.length + " bytes " + Arrays.toString(barray));


        Object copy;
        if (orig instanceof Task)
            copy = IO.taskFromBytes(barray);
        else if (orig instanceof Term)
            copy = IO.termFromBytes(barray);
        else
            throw new RuntimeException("");

        //if (copy instanceof Task) {
        //((MutableTask)copy).invalidate();
        //((Task)copy).normalize(nar);
        //out.println("\t\t" +((Task)orig).explanation());
        //out.println("\t\t" +((Task)copy).explanation());
        //}

        //Terms.printRecursive(System.out, (Task)orig, 10);

        //System.out.println("\tbytes: " + Arrays.toString(barray));
        out.println("\tcopy: " + copy);

        //Terms.printRecursive(System.out, (Term)copy, 10);

        //assertTrue(copy != orig);
        assertEquals(orig, copy);
        assertEquals(copy, orig);
        assertEquals(orig.toString(), copy.toString());
        assertEquals(orig.hashCode(), copy.hashCode());
        //assertEquals(0, orig.compareTo(copy));

        //assertEquals(copy.getClass(), orig.getClass());
    }


    //    /* https://github.com/RuedigerMoeller/fast-serialization/wiki/Serialization*/
    @Test
    public void testTermSerialization() throws Narsese.NarseseException {

        assertEqualSerialize("<a-->b>" /* term, not the concept */);
        assertEqualSerialize("<aa-->b>" /* term, not the concept */);
        assertEqualSerialize("<aa--><b<->c>>" /* term, not the concept */);
        //assertEqualSerialize(("(/, x, _, y)") /* term, not the concept */);
        assertEqualSerialize(("exe(a,b)") /* term, not the concept */);
    }

    @Test
    public void testNegationSerialization() throws Narsese.NarseseException {
        assertEqualSerialize("--x");

        //neg op serializes one byte less than a similar PROD compound
        assertEquals(1,
                IO.termToBytes($$("(x)")).length -
                IO.termToBytes($$("(--,x)")).length);
    }

    @Test
    public void testTemporalSerialization() throws Narsese.NarseseException {

        assertEqualSerialize(("(a &&+1 b)") /* term, not the concept */);
        assertEqualSerialize("(a &&+1 (a &&+1 a))" /* term, not the concept */);
        assertEqualSerialize("(a ==>+1 b)" /* term, not the concept */);
        assertEqualSerialize(("(b ==>+1 b)") /* term, not the concept */);

        assertEqualSerialize(("(a ==>+- b)"));
        assertEqualSerialize(("(a ==>+- a)"));
        assertEqualSerialize(("(a ==> b)"));

    }

    @Test
    public void testImageSerialization() throws Narsese.NarseseException {
        assertEqualSerialize(("/"));
        assertEqualSerialize(("\\"));
        assertEqualSerialize(("(a,/,1)"));
        assertEqualSerialize(("(a,/,1,/,x)"));
        assertEqualSerialize(("(x --> (a,/,1))"));
        assertEqualSerialize(("(a,\\,1)"));
        assertEqualSerialize(("(a,\\,1,\\,2)"));
        assertEqualSerialize(("((a,\\,1)--> y)"));
    }

    @Test
    public void testTermSerialization2() throws Narsese.NarseseException {
        assertTermEqualSerialize("<a-->(be)>");
    }

    @Test
    public void testTermSerialization3() throws Narsese.NarseseException {
        assertTermEqualSerialize("(#1 --> b)");
    }

    @Test
    public void testTermSerialization3_2() throws Narsese.NarseseException {
        //multiple variables

        Variable q = $.varQuery(1);
        Compound twoB = $.inh($.varDep(2), Atomic.the("b"));
        assertNotEquals(
                q.compareTo(twoB),
                twoB.compareTo(q));

        assertTermEqualSerialize("((#a --> b) <-> ?c)");

        Term a = $("(#2-->b)");
        Term b = $("?1");
        int x = a.compareTo(b);
        int y = b.compareTo(a);
        assertNotEquals((int) Math.signum(x), (int) Math.signum(y));

    }

    static void assertTermEqualSerialize(String s) throws Narsese.NarseseException {
        Termed t = $.$(s);
        assertTrue(t.isNormalized());
        assertTrue(t.term().isNormalized());
        assertEqualSerialize(t.term() /* term, not the concept */);
    }

    @Test
    public void testTaskSerialization() throws Narsese.NarseseException {
        assertEqualTask(("<a-->b>."));
        assertEqualTask(("<a-->(b,c)>!"));
        assertEqualTask(("<a-->(b==>c)>?"));
        assertEqualTask(("$0.1 (b-->c)! %1.0;0.8%"));
        assertEqualTask(("$0.1 (b-->c)! :|: %1.0;0.8%"));
        assertEqualTask(("$0.1 (a ==>+4 (b-->c)). :|: %1.0;0.8%"));
        assertEqualTask(("$0.1 (1 ==>+4 (2-->3)). :|: %1.0;0.8%"));

        assertEqualTask(("(x ==>+- y)?"));
        assertEqualTask(("(x ==>+- y)? :|:"));
        assertEqualTask(("(x ==>+- x)?"));
        assertEqualTask(("(x ==>+- x)? :|:"));
        assertEqualTask(("(x &&+- y)?"));
        assertEqualTask(("(x &&+- y)? :|:"));
        assertEqualTask(("(x &&+- x)?"));
        assertEqualTask(("(x &&+- x)? :|:"));
    }

    @Test
    public void testTaskSerialization2() throws Narsese.NarseseException {
        assertEqualSerialize((Object)nar.inputTask("$0.3 (a-->(bd))! %1.0;0.8%"));
    }

    @Test
    public void testNARTaskDump() throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream(16384);

        final AtomicInteger count = new AtomicInteger();

//

        NAR a = NARS.tmp()
//                .log()
                .input("a:b.", "b:c.", "c:d!");
        a
                .run(16);
        a
                .synch()
                .outputBinary(baos, (Predicate)(t)->{
                    count.incrementAndGet();
                    return true;
                })
                //.stop()
        ;

        byte[] x = baos.toByteArray();
        out.println(count.get() + " tasks serialized in " + x.length + " bytes");

        NAR b = NARS.shell()
                .inputBinary(new ByteArrayInputStream(x));
                //.next()
                //.forEachConceptTask(true,true,true,true, out::println)
                //.forEachConcept(System.out::println)
        //b.synch();

        //dump all tasks to a set of sorted strings and compare their equality:
        Set<String> ab = new TreeSet();
        a.tasks().forEach(t -> ab.add(t.toStringWithoutBudget()));

        assertEquals(count.get(), ab.size());

        Set<String> bb = new TreeSet();
        b.tasks().forEach(t -> bb.add(t.toStringWithoutBudget()));

        assertEquals(ab, bb,
                ()->"difference: " + Sets.symmetricDifference(ab, bb));
        //assertEquals(count.get(), bb.size());

//        //measure with budgets but allow only a certain one budget difference, due to rounding issues
//        Set<String> abB = new HashSet();
//        Set<String> bbB = new HashSet();
//        a.forEachConceptTask(t->abB.add(t.toString()), true,true,true,true);
//        b.forEachConceptTask(t->bbB.add(t.toString()), true,true,true,true);
//        Sets.SetView<String> diff = Sets.symmetricDifference(abB, bbB);
//        assertTrue("diff: " + diff.toString() + "\n\t" + abB + "\n\t" + bbB, 2 >= diff.size());
    }

    @Test
    public void testByteMappingAtom() throws Exception {
        assertEquals("(0,0)=. ", map("x"));
    }


    @Test
    public void testByteMappingInh() throws Exception {
        assertEquals("(0,0)=--> (1,2)=. (1,6)=. ", map("a:b"));
    }

    @Test
    public void testByteMappingCompoundDT() throws Exception {
        assertEquals("(0,0)===> (1,2)=. (1,6)=. ",
                map("(a ==>+1 b)"));
    }

    @Test
    public void testByteMappingCompoundDTExt() throws Exception {
        assertEquals("(0,0)=--> (1,2)===> (2,4)=. (2,8)=. (1,16)=. ",
                map("((a ==>+1 b) --> c)"));
    }

    @Test
    public void testByteMappingCompound() throws Exception {
        assertEquals("(0,0)===> (1,2)=--> (2,4)=* (3,6)=. (3,10)=. (2,16)=. (1,20)=. ",
                map("(a(b,\"c\") ==>+1 d)"));
    }

    public String map(String x) throws IOException, Narsese.NarseseException {
        return map($.$(x));
    }

    public String map(Term x) throws IOException {
        byte[] xb = IO.termToBytes(x);
        StringBuilder sb = new StringBuilder();
        IO.mapSubTerms(xb, (o, depth, i) -> {
            String msg = "(" + depth + "," + i + ")=" + o + " ";
            //System.out.println(msg);
            sb.append(msg);
        });
        return sb.toString();
    }


//    @Test public void testJacksonCompound() throws Narsese.NarseseException, IOException {
//        Compound c = $.$("(a-->b)");
//        byte[] b = Util.toBytes(c);
//        System.out.println(b.length + " " + Arrays.toString(b));
//        Compound c2 = Util.fromBytes(b, c.getClass() /*Compound.class*/);
//        System.out.println(c2);
//        assertEquals(c, c2);
//    }
}

