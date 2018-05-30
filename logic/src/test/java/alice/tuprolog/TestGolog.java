package alice.tuprolog;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class TestGolog {
    
    @Disabled
    @Test
    public void golog1() throws Exception {


        Prolog p = new Prolog();

        p.setSpy(true);
        p.addExceptionListener(System.out::println);
        p.addOutputListener(System.out::println);
        


        p.addLibrary("alice.tuprolog.lib.EDCGLibrary");
        p.input(Theory.resource("golog.pl"));
        p.input(Theory.resource("golog.elevator.pl"));


        p.solve(p.term("nextFloor(M,s0)."), System.out::println, 0);




    }

}
