package alice.tuprolog;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static alice.tuprolog.Theory.resource;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestEinsteinRiddle {

    @Test
    public void einsteinsRiddle() throws IOException, InvalidTheoryException {

        final boolean[] solved = {false};


        Prolog p = new Prolog()
                .input(resource(TestEinsteinRiddle.class, "einsteinsRiddle.pl"));
        for (int i = 0; i < 1; i++) {
            p.solveWhile("einstein(_,X), write(X).", solution -> {

                System.out.println(solution);

                if (solution.isSuccess()) {
                    Assertions.assertEquals("yes.\nX / german", solution.toString());
                    solved[0] = true;
                }

                return true; //keep trying but only one solution exist so it should try this at most once more
            });
        }

        assertTrue(solved[0]);
    }

}
