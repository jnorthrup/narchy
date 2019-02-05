package alice.tuprolog;

import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

public class TestGolog {
    
    @Test
    public void golog1() throws Exception {


        Prolog p = new Prolog();

        p.setSpy(true);
        p.addExceptionListener(System.out::println);
        p.addOutputListener(System.out::println);
        


        p.addLibrary("alice.tuprolog.lib.EDCGLibrary");
        p.input(Theory.resource(TestGolog.class, "golog.pl"));
        p.input(Theory.resource(TestGolog.class, "golog.elevator.pl"));


        p.solve(p.term("nextFloor(M,s0)."), (Consumer)System.out::println);




    }

}
