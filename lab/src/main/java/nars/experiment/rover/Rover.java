package nars.experiment.rover;

import nars.util.Timed;
import spacegraph.space3d.widget.CompoundSpatial;

/**
 * Created by me on 9/13/16.
 */
public class Rover extends CompoundSpatial {


    public Rover(Timed timed) {
        super(timed);


    }


    @Override
    public float radius() {
        return 1;
    }
}
