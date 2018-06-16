package nars.nal.multistep;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.junit.jupiter.api.Test;

/**
 * https:
 */
public class STRIPSTest {

    @Test
    public void testBanana1() throws Narsese.NarseseException {
        NAR n = new NARS().tmp();
        //n.log();
        n.input(
                /*
                A monkey is at location A in a lab. There is a box in location C. The monkey wants the bananas that are hanging from the ceiling in location B, but it needs to move the box and climb onto it in order to reach them.
                At(A), Level(low), BoxAt(C), BananasAt(B)
                */

                "At(A). :|:",
                "Level(low). :|:",
                "BoxAt(C). :|:",
                "BananasAt(B). :|:",

                /* Goal state:    Eat(bananas) */
                "Eat(bananas)!",

                
                "((At($X) &&+0 Level(low)) ==>+1 (--At($X) &&+0 At(#Y))).",

                
                "(((At(#Location) &&+0 BoxAt(#Location)) &&+0 Level(low)) ==>+1 (Level(high) &&+0 --Level(low))).",

                
                "(((At(#Location) &&+0 BoxAt(#Location)) &&+0 Level(high)) ==>+1 (Level(low), --Level(high))).",


                
               /* Preconditions:  At(X), BoxAt(X), Level(low)
               Postconditions: BoxAt(Y), not BoxAt(X), At(Y), not At(X) */
                "(((At($X) &&+0 BoxAt($X)) &&+0 Level(low)) ==>+1 ((((At(#Y) &&+0 BoxAt(#Y)) &&+0 --BoxAt($X)) &&+0 --At($X)))).",


                
               /* Preconditions:  At(Location), BananasAt(Location), Level(high)
               Postconditions: Eat(bananas) */
                "(((At(#Location) &&+0 BananasAt(#Location)) &&+0 Level(high)) ==>+1 Eat(bananas))."
        );
        n.run(1000);

    }
}
