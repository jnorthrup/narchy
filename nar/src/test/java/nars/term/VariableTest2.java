package nars.term;

import nars.*;
import nars.util.Timed;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VariableTest2 {
    @Test
    void testDestructiveNormalization() throws Narsese.NarseseException {
        String t = "<$x --> y>";
        String n = "($1-->y)";
        Timed timed = NARS.shell();
        Termed x = $.INSTANCE.$(t);
        assertEquals(n, x.toString());


    }


    @Test
    void varNormTestIndVar() throws Narsese.NarseseException {


        NAR n = NARS.shell();

        String t = "<<($1, $2) --> bigger> ==> <($2, $1) --> smaller>>";

        Termed term = $.INSTANCE.$(t);
        Task task = Narsese.task(t + '.', n);

        System.out.println(t);
        assertEquals("(bigger($1,$2)==>smaller($2,$1))", task.term().toString());
        System.out.println(term);
        System.out.println(task);


        Task t2 = n.inputTask(t + '.');
        System.out.println(t2);


        n.run(10);

    }
}
