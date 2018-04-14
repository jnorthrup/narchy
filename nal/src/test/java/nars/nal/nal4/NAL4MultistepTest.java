package nars.nal.nal4;

import nars.Narsese;
import nars.test.TestNAR;
import nars.util.NALTest;
import org.junit.jupiter.api.Test;

public class NAL4MultistepTest extends NALTest {


    @Test
    public void nal4_everyday_reasoning() {
        int time = 3500;

        //Global.DEBUG = true;

        TestNAR tester = test;

        tester.nar.freqResolution.set(0.1f);

        //tester.log();

        //(({tom},{sky})-->likes).  <{tom} --> cat>. <({tom},{sky}) --> likes>. <(cat,[blue]) --> likes>?

        tester.input("<{sky} --> [blue]>."); //en("the sky is blue");
        tester.input("<{tom} --> cat>."); //en("tom is a cat");
        tester.input("likes({tom},{sky})."); //en("tom likes the sky");

        tester.input("$0.9 likes(cat,[blue])?"); //cats like blue?

        //return mustOutput(cycleStart, cycleEnd, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, occTimeAbsolute, occTimeAbsolute);
        tester.mustBelieve(time, "likes(cat,[blue])",
                1f,
                0.45f);
                //0.16F);

    }

    @Test
    public void nal4_everyday_reasoning_easiest() throws Narsese.NarseseException {
        int time = 550;

        //Global.DEBUG = true;

        TestNAR tester = test;
        tester.believe("<sky --> blue>",1.0f,0.9f); //en("the sky is blue");
        //tester.believe("<tom --> cat>",1.0f,0.9f); //en("tom is a cat");
        //tester.believe("<(tom,sky) --> likes>",1.0f,0.9f); //en("tom likes the sky");
        tester.believe("<sky --> likes>",1.0f,0.9f); //en("tom likes the sky");

        //tester.ask("<(cat,blue) --> likes>"); //cats like blue?
        //tester.askAt(time/3, "<(cat,blue) --> likes>"); //cats like blue?
        //tester.askAt(time/2, "<(cat,blue) --> likes>"); //cats like blue?
        tester.ask("<blue --> likes>"); //cats like blue?

        //tester.mustBelieve(time, "<(cat,blue) --> likes>", 1.0f, 0.42f);
        tester.mustBelieve(time, "<blue --> likes>", 1.0f, 0.4f /* 0.45? */);

    }

    @Test
    public void nal4_everyday_reasoning_easier() throws Narsese.NarseseException {
        int time = 2550;

        //Param.DEBUG = true;

        TestNAR tester = test;
        //tester.nar.log();
        tester.believe("<sky --> blue>",1.0f,0.9f); //en("the sky is blue");
        tester.believe("<tom --> cat>",1.0f,0.9f); //en("tom is a cat");
        tester.believe("<(tom,sky) --> likes>",1.0f,0.9f); //en("tom likes the sky");



        tester.ask("<(cat,blue) --> likes>"); //cats like blue?
        //tester.askAt(time/3, "<(cat,blue) --> likes>"); //cats like blue?
        //tester.askAt(time/2, "<(cat,blue) --> likes>"); //cats like blue?


        tester.mustBelieve(time, "<(cat,blue) --> likes>", 1.0f, 0.45f); //en("A base is something that has a reaction with an acid.");

    }



//    //like seen when changing the expected confidence in mustBelief, or also in the similar list here we have such a ghost task where I expect better budget:
//
//    @Test
//    public void multistep_budget_ok() throws Narsese.NarseseException {
//        TestNAR tester = test();
//        //tester.nar.log();
//        tester.believe("<{sky} --> [blue]>",1.0f,0.9f); //en("the sky is blue");
//        tester.believe("<{tom} --> cat>",1.0f,0.9f); //en("tom is a cat");
//        tester.believe("<({tom},{sky}) --> likes>",1.0f,0.9f); //en("tom likes the sky");
//        tester.askAt(500,"<(cat,[blue]) --> likes>"); //cats like blue?
//        //tester.mustAnswer(1000, "<(cat,[blue]) --> likes>", 1.0f, 0.42f, Tense.Eternal); //en("A base is something that has a reaction with an acid.");
//        tester.mustBelieve(1000, "<(cat,[blue]) --> likes>", 1.0f, 0.42f); //en("A base is something that has a reaction with an acid.");
//
//    }

}
