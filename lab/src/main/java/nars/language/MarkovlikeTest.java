//package nars.nlp;
//
//import nars.$;
//import nars.NAR;
//import nars.guifx.demo.NARide;
//import nars.nar.Default;
//import nars.op.in.Twenglish;
//import nars.term.Term;
//import nars.util.event.On;
//
//import java.util.Arrays;
//import java.util.concurrent.atomic.AtomicReference;
//
///**
// * Created by me on 1/13/16.
// */
//
//public class MarkovlikeTest {
//
//	public static String getSentence2() {
//		switch ((int) (Math.random() * 5)) {
//			case 0 :
//				return "how you? nice! what you doing? i know it.";
//			case 1 :
//				return "what is? ok! it is what new."; // what is #x
//			case 2 :
//				return "you want to know how i?  thanks i ok.";
//			case 3 :
//				return "what new? now that we talk i want to know how you!";
//			case 4 :
//				return "ok you! nice to talk. what you doing now?";
//		}
//		return null;
//	}
//	public static String getSentence() {
//		switch ((int) (Math.random() * 1)) {
//			case 0 :
//				return "aa bb cc dd ee";
//				// case 1: return "c d e f g h i j k l m";
//		}
//		return null;
//	}
//
//	public static void main(String[] args) {
//    //public void testLanguage() {
//
//        Default n = new Default(1000,1,2,3);
//
//        n.initNAL9();
//
//        //d.core.activationRate.setValue(0.5f);
//        n.shortTermMemoryHistory.set(5);
//
//        //d.memory.conceptForgetDurations.setValue(1);
//
//        //d.log();
//
//        final AtomicReference<Term> prev = new AtomicReference($.the(""));
//        final On l = n.onExecTerm("say", e -> {
//            Term r;
//            synchronized (prev) {
//                //$.logger.debug(e.task.getExplanation());
//                Term nn = e[0];
//                Term mm = prev.get();
////                if (nn.equals(mm)) {
////                    //return Atom.the("_stutter");
////                    return null;
////                }
//
//                $.logger.info(Arrays.toString(e));
//
//                //r = $.p(mm, nn);
//                //r = $.impl(mm, nn);
//                r = e[0];
//
//                prev.set(nn);
//
//            }
//            return r;
//        });
//
//        //d.input("$1.0$ ((echo(#a) ==> echo(#b)) <-> echo(#a,#b)). %1.0;0.99%");
//
//        int repeats = 8;
//        int wordDelay = 30;
//        int sentenceDelay = 500;
//        int speakTime = wordDelay * 16;
//        int silenceTime = 1000;
//
//        for (int i = 0; i < repeats; i++) {
//
//            $.logger.warn("train");
//
//            String sentenceID = Twenglish.learnSentence(n, wordDelay,
//                    getSentence2()
//                    //"this is a sentence for learning language."
//                    //"a b c."
//                    //"a b c d e f g."
//            );
//            n.run(sentenceDelay);
//
//            $.logger.warn("speak");
//
//            speak(n, sentenceID, speakTime);
//
//            $.logger.warn("off");
//
//            n.run(silenceTime); //verify it is quiet
//
//        }
//
//
//        NARide.show(n.loop(), e-> {});
//
//    }
//	public static void speak(NAR n, String sentenceID, int speakTime) {
//		// d.frame(1000);
//
//		n.input("speak:" + sentenceID + "! :|:");
//
//		n.run(speakTime);
//
//		// silence
//		// n.input("(--, say(?x))! :|:");
//	}
//
//}
