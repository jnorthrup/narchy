package jcog;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.slf4j.NDC;

public class Log {

    static {

        ch.qos.logback.classic.Logger root = root();
        LoggerContext c = root.getLoggerContext();
        c.reset();

        ConsoleAppender<ILoggingEvent> ca = new ConsoleAppender<>();
        ca.setContext(c);
        //ca.setWithJansi(true);
        ca.setName("*");

        PatternLayout layout = new PatternLayout();
        //layout.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        layout.setPattern(
                "%X{NDC0} %highlight(%.-1level) %logger - %msg%n"
                //"%highlight(%.-1level) %logger{36} - %msg [%thread]%n"
        );

        layout.setContext(c);
        layout.start();

        ca.setImmediateFlush(false);
        ca.setLayout(layout);

        LayoutWrappingEncoder<ILoggingEvent> encoder = new LayoutWrappingEncoder<>();
        encoder.setContext(c);
        encoder.setLayout(layout);
        ca.setEncoder(encoder);
        ca.start();

        root.addAppender(ca);
        root.setLevel(Level.INFO);

        ca.start();
    }

    public static ch.qos.logback.classic.Logger root() {
        return ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger(Logger.ROOT_LOGGER_NAME);
    }

    public static Logger logger(String c) {
        return LoggerFactory.getLogger(c);
    }

    public static Logger logger(Class c) {
        return LoggerFactory.getLogger(c);
    }


    /** https://logback.qos.ch/manual/receivers.html */
    public static class LogSend  extends ch.qos.logback.classic.net.server.ServerSocketAppender {
        //TODO
    }

    /** https://logback.qos.ch/manual/receivers.html */
    public static class LogReceive extends ch.qos.logback.classic.net.server.ServerSocketReceiver {
        //TODO
    }

//    public static void enter(String zone) {
//        NDC.push(zone);
//    }
//
//    public static void exit() {
//        NDC.pop();
//    }

}
