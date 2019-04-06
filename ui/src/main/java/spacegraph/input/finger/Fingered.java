package spacegraph.input.finger;

import spacegraph.space2d.Surface;

public interface Fingered {

    /**
     *
     * finger reaction
     *
     * @return non-null if the event has been absorbed by a speciifc sub-surface
     * or null if nothing absorbed the gesture
     *
     * */
    default Surface finger(Finger finger) {
        return null;
    }

}
