package nars;

import org.junit.jupiter.api.Test;

/**
 * Created by me on 5/2/16.
 */
public class DecisionTest {

    @Test
    public void testDecision1() throws Narsese.NarseseException {
        NAR n = new NARS().get();
        n.input("(add($x, $x, #y) ==>+0 zero($x)).");
        n.input("add(0,0,#y)!");
        n.input("add(1,1,#y)!");
        n.input("add(2,2,#y)!");
        n.input("zero(#y)?");

        
        
        n.log();
        n.run(400);
    }
}
