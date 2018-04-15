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
public class NLPTest {

//    @Test
//    public void testNLP1() throws IOException, Narsese.NarseseException {
//        NAR n = new Default();
//
//    }

    @Test @Disabled
    public void testNLP0() throws Narsese.NarseseException {
        //

        NAR n = new NARS().get();
        //n.DEFAULT_QUEST_PRIORITY = 0.1f;

        //n.quaMin.setValue(0.1f);
        n.freqResolution.set(0.1f);

        n.onOpN("say", (args, nn) -> {
            System.err.println(Joiner.on(" ").join(args));
        });

        //n.log();
        //n.logBudgetMin(System.out, 0.2f);
        //n.log(System.out, x -> x instanceof Task && ((Task)x).isGoal());

        n.input(
            //"( (RANGE:{$range} && SENTENCE:$x) ==> FRAG:slice($x, $range)).",

            //"((VERB:{$V} && FRAG($X,$V,$Y)) ==> (((/,MEANS,$X,_),(/,MEANS,$Y,_)) --> (/,MEANS,$V,_))).",

            //"((FRAG:$XY && ($XY <-> flat((FRAG($X), and, FRAG($Y))))) <=> ((/,MEANS,$X,_) && (/,MEANS,$Y,_))).",

            "((VERB:{$V} && SENTENCE($X,$V,$Y))                ==> (((/,MEANS,$X,_),(/,MEANS,$Y,_)) --> (/,MEANS,$V,_))).",
            "((&&, VERB:{$V}, ADV:{$A}, SENTENCE($X,$V,$A,$Y)) ==> (((/,MEANS,$X,_),(/,MEANS,$Y,_)) --> ((/,MEANS,$V,_)|(/,MEANS,$A,_)))).",
            "((&&, VERB:{$V}, DET:{#a}, SENTENCE($X,$V,#a,$Y)) ==> (((/,MEANS,$X,_),(/,MEANS,$Y,_)) --> (/,MEANS,$V,_))).",
            "((&&, VERB:{$V}, DET:{#a}, SENTENCE(#a,$X,$V,$Y)) ==> (((/,MEANS,$X,_),(/,MEANS,$Y,_)) --> (/,MEANS,$V,_))).",


            //"((WHERE_PREP:{#in} && FRAG(#in,$where)) ==> WHERE:(/,MEANS,$where,_))."

            //"(FRAG<->SENTENCE).", //sentence is also its own fragment

            //"RANGE:{(0,3),(1,4),(4,7),(0,7)}.",
                //"RANGE:{(0,3)}.",
                //"RANGE:{0,1,2,3,4,(0,1),(0,2),(0,3)}.",

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

//            "SENTENCE(yes,wants,no).",
//            "SENTENCE(yes,is,no).",
//            "SENTENCE(yes,isnt,no).",
//
//            "SENTENCE(it,is,my,good).",
//            "SENTENCE(it,is,bad,and,it,can,good).",
//            "SENTENCE(good,was,bad).",
//            "SENTENCE(right,willBe,wrong).",
//            "SENTENCE(true,can,false).",



            //"$0.9$ (?y --> (/,MEANS,?x,_))?",

            //"$0.9$ SENTENCE(?z,?x,?y)?"

            //"$0.9$ SENTENCE(?y)?",

            //"$0.9;0.9$ (VERB:{$v} ==> say(DO, $v))."
            //"$0.9;0.9$ say(#y)!",

            "$0.9;0.9$ (SENTENCE:#y ==> say:#y).",
            "$0.9;0.9$ (SENTENCE:#y && say:#y)!"
            //"say(cat,is,blue)!",
            //"say(#x,is,#y)!"


        );

        n.run(1550);

        //IO.saveTasksToTemporaryTextFile(n);
    }

}
