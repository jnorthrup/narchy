package nars.derive;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class BasicRulesTest {

    @Test
    public void testNAL1() throws Narsese.NarseseException {
        //Deriver d = Deriver.defaults;

        NAR n = NARS.shell();

        /*new NARStream(n).forEachCycle(() -> {
            n.memory.getControl().forEach(p -> {
                System.out.println(p.getBudget().getBudgetString() + " " + p);
            });
        });*/

        n.input("<a --> b>. <b --> c>.");

        //NARTrace.out(n);
        //TextOutput.out(n);


        n.run(150);
    }

    @Test public void testSubstitution() throws Narsese.NarseseException {
        // (($1 --> M) ==> C), (S --> M), substitute($1,S) |- C, (Truth:Deduction, Order:ForAllSame)
        NAR n = NARS.shell();
        n.input("<<$1 --> M> ==> <C1 --> C2>>. <S --> M>.");
        //OUT: <C1 --> C2>. %1.00;0.81% {70: 1;2}

        //TextOutput.out(n);
        n.run(50);

        //<<$1 --> drunk> ==> <$1--> dead>>. <S --> drunk>.     |-  <S --> dead>.

    }

    @Test public void testSubstitution2() throws Narsese.NarseseException {
        // (($1 --> M) ==> C), (S --> M), substitute($1,S) |- C, (Truth:Deduction, Order:ForAllSame)
        NAR n = NARS.shell();
        n.input("<<$1 --> happy> ==> <$1--> dead>>. <S --> happy>.");
        //<<$1 --> drunk> ==> <$1--> dead>>. <S --> drunk>.     |-  <S --> dead>.
        //OUT: <S --> dead>. %1.00;0.81% {58: 1;2}

        //TextOutput.out(n);
        n.run(150);



    }

}
