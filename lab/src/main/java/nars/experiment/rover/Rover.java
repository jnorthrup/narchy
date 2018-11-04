package nars.experiment.rover;

import nars.util.Timed;
import spacegraph.space3d.widget.CompoundSpatial;

/**
 * Created by me on 9/13/16.
 */
public class Rover extends CompoundSpatial {

    private final Timed timed;


    public Rover(Timed timed) {
        super(timed);
        this.timed = timed;


    }


    @Override
    public float radius() {
        return 1;
    }
}
