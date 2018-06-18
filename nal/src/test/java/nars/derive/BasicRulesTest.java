package nars.derive;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
class BasicRulesTest {

    @Test
    void testNAL1() throws Narsese.NarseseException {
        

        NAR n = NARS.shell();

        /*new NARStream(n).forEachCycle(() -> {
            n.memory.getControl().forEach(p -> {
                System.out.println(p.getBudget().getBudgetString() + " " + p);
            });
        });*/

        n.input("<a --> b>. <b --> c>.");

        
        


        n.run(150);
    }

    @Test
    void testSubstitution() throws Narsese.NarseseException {
        
        NAR n = NARS.shell();
        n.input("<<$1 --> M> ==> <C1 --> C2>>. <S --> M>.");
        

        
        n.run(50);

        

    }

    @Test
    void testSubstitution2() throws Narsese.NarseseException {
        
        NAR n = NARS.shell();
        n.input("<<$1 --> happy> ==> <$1--> dead>>. <S --> happy>.");
        
        

        
        n.run(150);



    }

}
