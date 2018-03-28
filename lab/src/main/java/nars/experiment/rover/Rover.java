package nars.experiment.rover;

import nars.NAR;
import spacegraph.space3d.widget.CompoundSpatial;

/**
 * Created by me on 9/13/16.
 */
public class Rover extends CompoundSpatial {

    private final NAR nar;


    public Rover(NAR nar) {
        super(nar);
        this.nar = nar;




    }


    @Override
    public float radius() {
        return 1;
    }
}
