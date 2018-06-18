package nars.op.language;

import com.google.common.base.Joiner;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Created by me on 2/18/17.
 */
class NLPTest {







    @Test @Disabled
    void testNLP0() throws Narsese.NarseseException {
        

        NAR n = new NARS().get();
        

        
        n.freqResolution.set(0.1f);

        n.onOpN("say", (args, nn) -> {
            System.err.println(Joiner.on(" ").join(args));
        });

        
        
        

        n.input(
            

            

            

            "((VERB:{$V} && SENTENCE($X,$V,$Y))                ==> (((/,MEANS,$X,_),(/,MEANS,$Y,_)) --> (/,MEANS,$V,_))).",
            "((&&, VERB:{$V}, ADV:{$A}, SENTENCE($X,$V,$A,$Y)) ==> (((/,MEANS,$X,_),(/,MEANS,$Y,_)) --> ((/,MEANS,$V,_)|(/,MEANS,$A,_)))).",
            "((&&, VERB:{$V}, DET:{#a}, SENTENCE($X,$V,#a,$Y)) ==> (((/,MEANS,$X,_),(/,MEANS,$Y,_)) --> (/,MEANS,$V,_))).",
            "((&&, VERB:{$V}, DET:{#a}, SENTENCE(#a,$X,$V,$Y)) ==> (((/,MEANS,$X,_),(/,MEANS,$Y,_)) --> (/,MEANS,$V,_))).",


            

            

            
                
                

            "VERB:{is,maybe,isnt,was,willBe,wants,can,likes}.",
            "DET:{a,the}.",
            "PREP:{for,on,in,with}.",
            "ADV:{never,always,maybe}.",

            "SENTENCE(tom,is,never,sky).",
            "SENTENCE(tom,is,always,cat).",
            "SENTENCE(tom,is,cat).",
            "SENTENCE(tom,is,a,cat).",
            "SENTENCE(tom,likes,the,sky).",
            "SENTENCE(tom,likes,maybe,cat).",
            "SENTENCE(the,sky,is,blue).",
            "SENTENCE(a,cat,likes,blue).",
            "SENTENCE(sky,wants,the,blue).",
            "SENTENCE(sky,is,always,blue).",













            

            

            

            
            

            "$0.9;0.9$ (SENTENCE:#y ==> say:#y).",
            "$0.9;0.9$ (SENTENCE:#y && say:#y)!"
            
            


        );

        n.run(1550);

        
    }

}
