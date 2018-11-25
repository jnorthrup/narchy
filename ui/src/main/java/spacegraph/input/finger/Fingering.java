package spacegraph.input.finger;

import org.jetbrains.annotations.Nullable;

/** exclusive finger control state which a surface can activate in reaction to input events.
 * it has a specified termination condition (ex: button release) and
 * while active, locks any other surface from receiving interaction events.
 *
 * it can be used to drag move/resize with additional functionality or constraints
 * applied in real-time.
 * */
abstract public class Fingering {

    /** return true to allow begin */
    protected abstract boolean start(Finger f);

    /** return false to finish */
    protected abstract boolean update(Finger f);

    public void stop(Finger finger) {

    }

    /** whether this is allowed to continue updating the finger's currently
     * touched widget after it activates.
     */
    protected boolean escapes() {
        return false;
    }

    /** override to provide a custom renderer (cursor) */
    @Nullable public FingerRenderer renderer() {
        return null;
    }

    /** whether this state should automatically defer to a new incoming state
     * @param finger*/
    public boolean defer(Finger finger) {
        return false;
    }

    public static final Fingering Null = new Fingering() {

        @Override
        public String toString() {
            return "Null_Fingering";
        }

        @Override
        protected boolean start(Finger f) {
            return true;
        }

        @Override
        protected boolean update(Finger f) {
            return true;
        }

        @Override
        public boolean defer(Finger finger) {
            return true;
        }

        @Override
        protected boolean escapes() {
            return true;
        }
    };

}
