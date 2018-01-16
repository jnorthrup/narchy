//package nars.video;
//
//import ch.qos.logback.classic.Logger;
//import ch.qos.logback.classic.spi.ILoggingEvent;
//import jcog.spatial.RTree;
//import jcog.spatial.Rect1D;
//import nars.$;
//import nars.decide.DefaultConsoleAppender;
//
///**
// * Created by me on 12/2/16.
// */
//public class LogIndex extends RTree<LogIndex.Logged> {
//
//    final DefaultConsoleAppender appender;
//
//    boolean enabled = true;
//
//    public LogIndex() {
//        this(((Logger)$.logger).getLoggerContext().getLogger("ROOT"), 2, 8, Split.LINEAR);
//    }
//
//    public LogIndex(org.slf4j.Logger logger, int mMin, int mMax, Split splitType) {
//        super((l) -> l, mMin, mMax, splitType);
//
//
//        appender = new DefaultConsoleAppender() {
//            @Override
//            public void append(ILoggingEvent event) {
//                if (enabled) {
//                    add(new Logged(event.getTimeStamp(), event.toString()));
//                }
//            }
//        };
//        appender.start();
//
//        ((ch.qos.logback.classic.Logger) logger).addAppender(appender);
//
//    }
//
//    //    private final class MyDefaultConsoleAppender extends DefaultConsoleAppender {
////
////        @Override
////        public void append(ILoggingEvent eventObject) {
////            try {
////
////                putLinePre(eventObject.toString());
////            } catch (IOException e) {
////                e.printStackTrace();
////            }
////        }
////    }
//    static public class Logged extends Rect1D implements Comparable<Logged> {
//
//        final long when;
//        final String text;
//
//        public Logged(long when, String text) {
//            this.when = when;
//            this.text = text;
//        }
//
//        @Override
//        public int compareTo(Logged logged) {
//            long l = when - logged.when;
//            if (l != 0)
//                return l > 0 ? 1 : -1;
//            return text.compareTo(logged.text);
//        }
//
//        @Override
//        public double from() {
//            return when;
//        }
//
//        @Override
//        public double to() {
//            return when;
//        }
//
//        @Override
//        public String toString() {
//            return when + ":\"" + text + '\"';
//        }
//    }
//
//
//}
//
