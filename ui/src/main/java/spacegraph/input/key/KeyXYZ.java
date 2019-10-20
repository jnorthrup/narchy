package spacegraph.input.key;

import jcog.math.v3;
import spacegraph.video.JoglDisplay;

import static jcog.math.v3.v;

/** simple XYZ control using keys (ex: numeric keypad) */
public class KeyXYZ extends KeyXY {

    public KeyXYZ(JoglDisplay g) {
        super(g);



    }

    @Override void moveX(float speed) {
        var x = v(space.camFwd);
        
        x.cross(x, space.camUp);
        x.normalize();
        x.scaled(-speed);
        space.camPos.add(x);
    }

    @Override void moveY(float speed) {
        var y = v(space.camUp);
        y.normalize();
        y.scaled(speed);
        
        space.camPos.add(y);
    }


    @Override void moveZ(float speed) {
        var z = v(space.camFwd);
        
        z.scaled(speed);
        space.camPos.add(z);
    }






}
