package jcog;

/** shorter than typing RuntimeException */
@Skill({"Debugging", "Concern"}) @Paper
public class WTF extends RuntimeException {
    public WTF() {
        super();
    }
    public WTF(String s) {
        super(s);
    }
    public WTF(Throwable wrap) {
        super(wrap);
    }


    public static WTF WTF() { throw new WTF(); }
    public static WTF WTF(String message) { throw new WTF(message); }
}
