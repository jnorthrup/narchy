package nars.nal.nal6;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.junit.jupiter.api.Test;

/**
 * Created by me on 11/6/15.
 */
class SecondLevelUnificationTest {


    @Test
    void test1() throws Narsese.NarseseException {
        
        NAR n = new NARS().get();

        




        n.believe("<<$1 --> x> ==> (&&,<#2 --> y>,open(#2,$1))>", 1.00f, 0.90f); 
        n.believe("<{z} --> y>", 1.00f, 0.90f); 
        
        n.run(250);
    }
    @Test
    void test2() throws Narsese.NarseseException {
        
        NAR n = new NARS().get();

        




        n.believe("<<$1 --> x> ==> (&&,<#2 --> y>,<$1 --> #2>)>", 1.00f, 0.90f); 
        n.believe("<{z} --> y>", 1.00f, 0.90f); 
        
        n.run(250);
    }





























}
