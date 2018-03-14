package nars.op.logic;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.Param;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.time.Tense.ETERNAL;

/**
 * Created by me on 4/17/17.
 */
@Disabled
public class TestProloglike {

//    static {
//        Param.TRACE = true;
//    }

    /**
     * http://www.doc.gold.ac.uk/~mas02gw/prolog_tutorial/prologpages/rules.html
     */
    @Test
    public void testProloglike1() throws Narsese.NarseseException {
        Param.DEBUG = true;
        NAR n = NARS.tmp();
        /*
        fun(X) :- red(X), car(X).
        fun(X) :- blue(X), bike(X).
        car(vw_beatle).
        car(ford_escort).
        bike(harley_davidson).
        red(vw_beatle).
        red(ford_escort).
        blue(harley_davidson).
        */

        n.freqResolution.set(0.1f);
        n.believe(
                "((red($x) && car($x))==>fun($x))",
                "((blue($x) && bike($x))==>fun($x))",
                "(car($x) <=> (--,bike($x)))",
                "(red($x) <=> (--,blue($x)))",
                "car(vw_beatle)", "car(ford_escort)", "bike(harley_davidson)", "red(vw_beatle)", "blue(ford_escort)", "blue(harley_davidson)"
        );
        //n.log();
        n.questionPriDefault.set(0.99f);
        n.question("fun(?x)", ETERNAL, (q, a) -> {
            //System.out.println(a.term() + " " + a.truth());
            System.out.println(a.proof());
        });
        n.run(1000);


    }

    @Test
    public void testRiddle1() throws Exception {
        //Param.DEBUG = true;
        NAR n = NARS.tmp();

        n.termVolumeMax.set(1024);
        n.log();
        n.inputNarsese(
                TestProloglike.class.getResource("einsteinsRiddle.nal")
        );
        n.run(128);


    }

    @Test
    public void testMetagol() throws Exception {
        NAR n = NARS.tmp();

        //n.termVolumeMax.setValue(1024);
        n.log();
        n.inputNarsese(
                TestProloglike.class.getResource("metagol.nal")
        );

        n.input("grandparent(ann,amelia).",
                "grandparent(steve,amelia).",
                "grandparent(ann,spongebob).",
                "grandparent(steve,spongebob).",
                "grandparent(linda,amelia).",
                "--grandparent(amy,amelia).",
                "parent(ann,andy).",
                "parent(steve,andy).",
                "parent(ann,amy).",
                "$0.99 identity(?x,?y)?",
                "$0.99 identity(grandparent,?y)?",
                "$0.99 curry(?x,?y)?",
                "$0.99 curry(#x,#y)?",
                "$0.99 curry(grandparent,?y)?"
        );
        n.run(1024);


    }

}
