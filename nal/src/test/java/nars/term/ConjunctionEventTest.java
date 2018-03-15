package nars.term;

import nars.$;
import org.junit.jupiter.api.Test;

import static nars.Op.False;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConjunctionEventTest {

    @Test
    public void testWrappingCommutiveConjunction() {

        {

            //AND, valid
            Term xEternal = $.$safe("((((--,angX) &&+4 x) &&+10244 angX) && y)");
            assertEquals("((((--,angX) &&+4 x) &&+10244 angX)&&y)",
                    xEternal.toString());

            //AND, valid after factoring
            Term xFactored = $.$safe("((x&&y) &&+1 (y&&z))");
            assertEquals("((x &&+1 z)&&y)", xFactored.toString());

            //AND, contradict
            Term xAndContradict = $.$safe("((x &&+1 x)&&--x)");
            assertEquals(False,
                    xAndContradict);

            //AND, redundant
            Term xAndRedundant = $.$safe("((x &&+1 x)&&x)");
            assertEquals("(x &&+1 x)",
                    xAndRedundant.toString());

            //AND, redundant parallel
            Term xAndRedundantParallel = $.$safe("(((x &| y) &| z)&&x)");
            assertEquals("(&|,x,y,z)",
                    xAndRedundantParallel.toString());

            //AND, contradiction parallel
            Term xAndContradictParallel = $.$safe("(((x &| y) &| z)&&--x)");
            assertEquals(False,
                    xAndContradictParallel);

            //AND, contradiction parallel multiple
            Term xAndContradictParallelMultiple = $.$safe("(&&,x,y,((x &| y) &| z))");
            assertEquals("(&|,x,y,z)",
                    xAndContradictParallelMultiple.toString());

            //AND contradiction
            Term xAndContradict2 = $.$safe("((((--,angX) &&+4 x) &&+10244 angX) && --x)");
            assertEquals(False, xAndContradict2);

            //AND contradiction2
            Term xAndContradict3 = $.$safe("((((--,angX) &&+4 x) &&+10244 angX) && angX)");
            assertEquals(False, xAndContradict3);

            //Ambiguous
            Term xParallel = $.$safe("((((--,angX) &&+4 x) &&+10244 angX) &&+0 y)");
            assertEquals(False, xParallel);

        }

        {
            //ambiguous simultaneity

            Term xParallelContradiction4 = $.$safe("((((--,angX) &&+4 x) &&+10244 angX) &&+0 angX)");
            assertEquals(False, xParallelContradiction4);
        }


        {
            Term x = $.$safe("((((--,angX) &&+4 x) &&+10244 angX) &| angX)");
            Term y = $.$safe("(angX &| (((--,angX) &&+4 x) &&+10244 angX))");
            assertEquals(x, y);
            //.
        }
    }

    @Test
    public void testFactorFromEventSequence() {
            Term yParallel1 = $.$safe("((((--,angX) &&+4 x) &&+10244 angX) &&+0 y)");
            String yParallel2Str = "((((--,angX)&|y) &&+4 (x&|y)) &&+10244 (angX&|y))";
            Term yParallel2 = $.$safe(yParallel2Str);
            assertEquals(yParallel1, yParallel2);
            assertEquals(yParallel2Str, yParallel1.toString());
    }
    @Test
    public void testFactorFromEventParallel() {
        Term yParallelOK = $.$safe("(((a&&x) &| (b&&x)) &| (c&&x))");
        assertEquals("", yParallelOK.toString());
        //not: (&|,a,b,c,x)

        Term yParallelContradict = $.$safe("((a&&x) &| (b&&--x))");
        assertEquals(False, yParallelContradict);
    }

}
