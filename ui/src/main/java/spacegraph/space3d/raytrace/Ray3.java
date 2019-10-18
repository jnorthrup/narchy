package spacegraph.space3d.raytrace;

import jcog.math.vv3;

public class Ray3 {

    public final vv3 position;
    public final vv3 direction;

    Ray3(vv3 position, vv3 direction) {
        this.position = position;
        this.direction = direction;
    }
}
