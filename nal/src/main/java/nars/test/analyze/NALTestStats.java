//package nars.test.analyze;
//
//import nars.control.MetaGoal;
//import nars.test.NALTest;
//import org.junit.jupiter.api.extension.AfterAllCallback;
//import org.junit.jupiter.api.extension.AfterEachCallback;
//import org.junit.jupiter.api.extension.ExtensionContext;
//
//public class NALTestStats implements AfterEachCallback, AfterAllCallback {
//
//
//
//
//    final MetaGoal.Report metagoals = new MetaGoal.Report();
//
//
//    @Override
//    public void afterAll(ExtensionContext context) {
//
//        metagoals.print(System.out);
//
//    }
//
//    @Override
//    public void afterEach(ExtensionContext context) {
//        NALTest n = ((NALTest) context.getTestInstance().get());
//
//        context.publishReportEntry(context.getUniqueId() + " NAR stats",
//                n.test.nar.stats().toString());
//
////        if (n.metagoals != null)
////            metagoals.addAt(n.metagoals);
//    }
//}
