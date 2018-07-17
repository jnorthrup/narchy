package nars.web.util;

import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

@JSFunctor
@FunctionalInterface
public interface JSRunnable extends JSObject {
    public void run();
}