package nars.io;

import nars.$;
import nars.DummyNAL;
import nars.Narsese;
import nars.Task;
import nars.task.util.TaskException;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.function.Executable;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;


public abstract class NarseseTest {


    public static <T extends Term> T term(@NotNull String s) throws Narsese.NarseseException {

        return (T) Narsese.term(s);
    }


    static void testProductABC(@NotNull Compound p) {
        assertEquals(3, p.subs(), new Supplier<String>() {
            @Override
            public String get() {
                return p + " should have 3 sub-terms";
            }
        });
        assertEquals("a", p.sub(0).toString());
        assertEquals("b", p.sub(1).toString());
        assertEquals("c", p.sub(2).toString());
    }


    static void taskParses(@NotNull String s) throws Narsese.NarseseException {
        Task t = task(s);
        assertNotNull(t);


    }


    static List<Task> tasks(@NotNull String s) throws Narsese.NarseseException {


        List<Task> l = $.INSTANCE.newArrayList(1);
        Narsese.tasks(s, l, new DummyNAL());
        return l;
    }


    static Task task(@NotNull String s) throws Narsese.NarseseException {
        List<Task> l = tasks(s);
        if (l.size() != 1)
            throw new RuntimeException("Expected 1 task, got: " + l);
        return l.get(0);
    }

    static void testTruth(String t, float freq, float conf) throws Narsese.NarseseException {
        String s = "a:b. " + t;

        Truth truth = task(s).truth();
        assertEquals(freq, truth.freq(), 0.001);
        assertEquals(conf, truth.conf(), 0.001);
    }

    public static void assertInvalidTasks(@NotNull String... inputs) {
        for (String s : inputs) {
            assertThrows(Exception.class, new Executable() {
                @Override
                public void execute() throws Throwable {
                    Task e = task(s);
                }
            });
        }
    }


    public static void assertInvalidTasks(Supplier<Task> s) {

        try {
            s.get();
            fail("");
        } catch (TaskException good) {
            assertTrue(true);
        } catch (Exception e) {
            fail(e::toString);
        }

    }


}









































































































































































































































































































































































































































































































































































































































































































































