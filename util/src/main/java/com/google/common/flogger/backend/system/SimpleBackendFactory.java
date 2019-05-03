package com.google.common.flogger.backend.system;

import com.google.common.flogger.backend.LoggerBackend;
import com.google.common.flogger.backend.slf4j.Slf4jBackendFactory;
import jcog.WTF;

public final class SimpleBackendFactory extends BackendFactory {
  private static final BackendFactory INSTANCE = Slf4jBackendFactory.getInstance();

  public static BackendFactory getInstance() {
    return INSTANCE;
  }

  private SimpleBackendFactory() {}

  @Override
  public LoggerBackend create(String loggingClass) {
//    // TODO(b/27920233): Strip inner/nested classes when deriving logger name.
//    Logger logger = Logger.getLogger(loggingClass.replace('$', '.'));
//    return new SimpleLoggerBackend(logger);
    throw new WTF();
  }

  @Override
  public String toString() {
    return "Best logger backend factory";
  }
}