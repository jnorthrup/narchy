package spacegraph.web.util;

import org.jetbrains.annotations.Nullable;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.core.JSString;

public class JS {
    @JSBody(params = { "instance", "index" }, script = "return instance[index];")
    public static native JSObject get(JSObject instance, String index);

    @JSBody(params = { "instance", "index" }, script = "return instance[index];")
    public static native JSObject get(JSObject instance, int index);

    public static String getString(@Nullable JSObject instance, int index) {
        if (instance == null)
            return null;
        else
            return ((JSString)get(instance, index)).stringValue();
    }
    public static byte getByte(JSObject instance, int index) {
        return ((JSNumber)get(instance, index)).byteValue();
    }
    public static int getInt(JSObject instance, int index) {
        return ((JSNumber)get(instance, index)).intValue();
    }
    public static double getDouble(JSObject instance, int index) {
        return (((JSNumber)get(instance, index)).doubleValue());
    }
    public static long getLong(JSObject instance, int index) {
        return Math.round(getDouble(instance, index));
    }

    @JSBody(params = { "instance", "index", "obj" }, script = "instance[index] = obj;")
    public static native void set(JSObject instance, String index, JSObject obj);
}
