package nars.nal.nal4;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.test.NALTest;
import nars.test.TestNAR;
import org.junit.jupiter.api.Test;

class NAL4MultistepTest extends NALTest {
    @Override
    protected NAR nar() {
        NAR n = NARS.tmp(4);
        n.termVolumeMax.set(18);
        return n;
    }


    @Test
    void nal4_everyday_reasoning() {
        int time = 3500;

        

        TestNAR tester = test;

        tester.nar.freqResolution.set(0.1f);

        

        

        tester.input("<{sky} --> [blue]>."); 
        tester.input("<{tom} --> cat>."); 
        tester.input("likes({tom},{sky})."); 

        tester.input("$0.9 likes(cat,[blue])?"); 

        
        tester.mustBelieve(time, "likes(cat,[blue])",
                1f,
                0.45f);
                

    }

    @Test
    void nal4_everyday_reasoning_easiest() throws Narsese.NarseseException {
        int time = 550;

        

        TestNAR tester = test;
        tester.believe("<sky --> blue>",1.0f,0.9f); 
        
        
        tester.believe("<sky --> likes>",1.0f,0.9f); 

        
        
        
        tester.ask("<blue --> likes>"); 

        
        tester.mustBelieve(time, "<blue --> likes>", 1.0f, 0.4f /* 0.45? */);

    }

    @Test
    void nal4_everyday_reasoning_easier() throws Narsese.NarseseException {
        int time = 2550;

        

        TestNAR tester = test;
        
        
        tester.believe("<sky --> blue>",1.0f,0.9f); 
        tester.believe("<tom --> cat>",1.0f,0.9f); 
        tester.believe("<(tom,sky) --> likes>",1.0f,0.9f); 



        tester.ask("<(cat,blue) --> likes>"); 
        
        


        tester.mustBelieve(time, "<(cat,blue) --> likes>", 1.0f, 0.45f); 

    }


















}
