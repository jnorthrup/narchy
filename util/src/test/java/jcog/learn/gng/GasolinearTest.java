package jcog.learn.gng;

import org.junit.jupiter.api.Test;

import static java.lang.System.out;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GasolinearTest {

    @Test
    public void testSimpleDiscretization3() {

        Gasolinear g = Gasolinear.of(3, 0, 0.5f, 1f

//0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0 //even


//     0.1, 0.2, 0.3, 0.25, 0.14, 0.14,
//
//            0.66, 0.67, 0.68,
//
//            1f, 0.8f, 0.95f, 0.83f
        );

        //warmup
        for (int i = 0; i < 20; i++) {
            out.println(g);
            System.out.println();

            g.index(Math.random());
        }




        assertEquals(0, g.index(-0.1));
        assertEquals(0, g.index(0.1));

        out.println(g);
        assertEquals(1, g.index(0.4));
        assertEquals(1, g.index(0.5));

        out.println(g);
        assertEquals(2, g.index(0.9));
        assertEquals(2, g.index(1.1));
    }

       @Test
    public void testSimpleDiscretization4() {

        Gasolinear g = Gasolinear.of(5,

0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0 //even


//     0.1, 0.2, 0.3, 0.25, 0.14, 0.14,
//
//            0.66, 0.67, 0.68,
//
//            1f, 0.8f, 0.95f, 0.83f
        );
        out.println(g);

//        assertEquals(0, g.which(-0.1));
//        assertEquals(0, g.which(0.1));
//        assertEquals(0, g.which(0.2));
//
//        assertEquals(1, g.which(0.5));
//        assertEquals(1, g.which(0.6));
//
//        assertEquals(2, g.which(0.9));
//        assertEquals(2, g.which(1.1));
    }
}