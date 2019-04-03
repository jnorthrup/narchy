package spacegraph.input.key;

import jcog.math.v3;
import spacegraph.video.JoglSpace;

import static jcog.math.v3.v;

/** simple XYZ control using keys (ex: numeric keypad) */
public class KeyXYZ extends KeyXY {

    public KeyXYZ(JoglSpace g) {
        super(g);



    }

    @Override void moveX(float speed) {
        v3 x = v(space.camFwd);
        
        x.cross(x, space.camUp);
        x.normalize();
        x.scaled(-speed);
        space.camPos.add(x);
    }

    @Override void moveY(float speed) {
        v3 y = v(space.camUp);
        y.normalize();
        y.scaled(speed);
        
        space.camPos.add(y);
    }


    @Override void moveZ(float speed) {
        v3 z = v(space.camFwd);
        
        z.scaled(speed);
        space.camPos.add(z);
    }






}
