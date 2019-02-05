package alice.tuprolog;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static alice.tuprolog.Theory.resource;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProblemSolving {

    private static Theory file(String file) throws IOException, InvalidTheoryException {
        return resource(ProblemSolving.class, file);
    }

    @Test
    public void einsteinsRiddle() throws IOException, InvalidTheoryException {

        assertEquals("[','(einstein([[house,norwegian,cat,dunhill,water,yellow],[house,dane,horse,marlboro,tea,blue],[house,brit,bird,pallmall,milk,red],[house,german,fish,rothmans,coffee,green],[house,swede,dog,winfield,beer,white]],german),write(german))]",
                new Prolog()
                    .input(file("einsteinsRiddle.pl"))
                    .solutionList("einstein(_,X), write(X).").toString()
        );

    }
    @Test
    public void nqueens() throws IOException, InvalidTheoryException {

        assertEquals("",
                new Prolog()
                        .input(file("nqueens.pl"))
                        .solutionList("n_queens(5, Qs).").toString()
        );

    }

}
