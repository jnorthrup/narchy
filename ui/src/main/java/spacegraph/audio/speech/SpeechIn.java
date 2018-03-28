//package spacegraph.audio;
//
//import edu.cmu.sphinx.api.Configuration;
//import edu.cmu.sphinx.api.LiveSpeechRecognizer;
//import edu.cmu.sphinx.api.SpeechResult;
//import edu.cmu.sphinx.result.WordResult;
//
//
///**
// * http://cmusphinx.sourceforge.net/wiki/tutorialsphinx4
// */
//public class SpeechIn {
//
//    public static void main(String[] args) throws java.io.IOException {
//
//        Configuration configuration = new Configuration();
//        configuration
//                .setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
//        configuration
//                .setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
//        configuration
//                .setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");
//
//        configuration.setSampleRate(16000);
//        configuration.setUseGrammar(false);
//
//
//
//
//
//        LiveSpeechRecognizer recognizer = new LiveSpeechRecognizer(configuration);
//
//
//        System.gc();
//
//        recognizer.startRecognition(true);
//        while (true) {
//            System.out.println("recognition start");
//
//            SpeechResult result = recognizer.getResult();
//            System.out.println(result.getHypothesis());
//
//
//            for (WordResult r : result.getWords()) {
//                System.out.println("\t" + r);
//            }
//
//            System.out.println("\t" + result.getNbest(2));
//            //System.out.println("\t" + result.getResult().getActiveTokens());
//            //System.out.println("\t" + result.getLattice());
//            //System.out.println("\t" + result);
//        }
//
//    }
//
//}
