package nars.experiment.rover;

import nars.util.TimeAware;
import spacegraph.space3d.widget.CompoundSpatial;

/**
 * Created by me on 9/13/16.
 */
public class Rover extends CompoundSpatial {

    private final TimeAware timeAware;


    public Rover(TimeAware timeAware) {
        super(timeAware);
        this.timeAware = timeAware;


    }


    @Override
    public float radius() {
        return 1;
    }
}
