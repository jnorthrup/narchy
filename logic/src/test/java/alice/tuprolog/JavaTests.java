package alice.tuprolog;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;

public class JavaTests {
    @Test
    public void testHashTable() throws IOException, InvalidTheoryException, URISyntaxException {
        Prolog p = new Prolog().input(
            Theory.resource("../../../resources/hash_table.pl")
        );


        p.addOutputListener(System.err::println);
        p.solve("test(4).", (x)->{
           System.out.println(x);
        });
        p.solve("test2(4).", (x)->{
           System.out.println(x);
        });
    }

    @Test
    public void testPoints2() throws IOException, InvalidTheoryException, URISyntaxException {
        Prolog p = new Prolog().input(
            Theory.resource("../../../resources/points_test2.pl")
        );



        p.solve("test(2).", (x)-> System.out.println(x));
        p.solve("test(3).", (x)-> System.out.println(x));
    }

}
