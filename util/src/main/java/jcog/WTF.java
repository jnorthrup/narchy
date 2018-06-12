package jcog;

/** shorter than typing RuntimeException */
@Skill({"Debugging", "Concern"}) @Paper
public class WTF extends RuntimeException {
    public WTF() {
        super();
    }
    public WTF(Throwable wrap) {
        super(wrap);
    }
}
