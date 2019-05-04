package nars.nal.nal4;

import nars.NAR;
import nars.NARS;
import nars.term.Term;
import nars.term.util.Image;
import nars.test.NALTest;
import nars.test.TestNAR;
import org.junit.jupiter.api.Test;

import static nars.$.$$;

public class NAL4MultistepTest extends NALTest {
    @Override
    protected NAR nar() {
        NAR n = NARS.tmp(4);
//        n.confMin.setAt(0.25f);
        n.termVolumeMax.set(16);
        return n;
    }


    @Test
    void nal4_everyday_reasoning() {
        int time = 1000;


        TestNAR tester = test;

        tester.nar.termVolumeMax.set($$("(likes(cat,[blue]) <-> likes({tom},[blue]))").volume()+2);

        //tester.nar.freqResolution.setAt(0.05f);
        tester.nar.questionPriDefault.set(0.5f);
        tester.confTolerance(0.4f);

        tester.input("({sky} --> [blue]).");
        tester.input("({tom} --> cat).");
        tester.input("likes({tom},{sky}).");

        tester.input("likes(cat,[blue])?");

        tester.mustBelieve(time, "(likes(cat,[blue]) <-> likes(cat,{sky}))",1f,0.45f);
        tester.mustBelieve(time, "(likes(cat,[blue]) <-> likes({tom},[blue]))",1f,0.45f);
        tester.mustBelieve(time, "(likes(cat,[blue]) <-> likes({tom},{sky}))",1f,0.45f);

        tester.mustBelieve(time, "likes(cat,[blue])",
                1f,
                0.45f);


    }

    @Test
    void nal4_everyday_reasoning_easiest() {


        test.believe("blue:sky", 1.0f, 0.9f)
                .believe("likes:sky", 1.0f, 0.9f)
                .ask("likes:blue")
                .mustBelieve(100, "likes:blue", 1.0f, 0.4f /* 0.45? */);

    }

    @Test
    void nal4_everyday_reasoning_easier() {
        int time = 2550;

        test.confTolerance(0.2f);
        test.termVolMax(12);
//        test.nar.freqResolution.setAt(0.25f);
//        test.nar.confResolution.setAt(0.1f);

        Term cat = $$("cat");
        Term blue = $$("blue");
        Term likes = $$("likes");
        Term answer = $$("likes(cat,blue)");;

        test.believe("blue:sky")
            .believe("cat:tom")
            .believe("likes(tom,sky)")
            .ask("likes(cat,blue)")
            .mustBelieve(time, answer.toString(), 1.0f, 0.27f /*0.45f*/)
            .mustBelieve(time, Image.imageExt(answer, cat).toString() /* (cat-->(likes,/,blue))  */, 1.0f, 0.27f /*0.45f*/)
            .mustBelieve(time, Image.imageExt(answer, blue).toString() /* (blue-->(likes,cat,/)) */, 1.0f, 0.27f /*0.45f*/)

        ;

    }


}
