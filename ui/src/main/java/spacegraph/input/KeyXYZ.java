package spacegraph.input;

import spacegraph.math.v3;
import spacegraph.render.JoglSpace;

import static spacegraph.math.v3.v;

/** simple XYZ control using keys (ex: numeric keypad) */
public class KeyXYZ extends KeyXY {

    public KeyXYZ(JoglSpace g) {
        super(g);



    }

    @Override void moveX(float speed) {
        v3 x = v(space.camFwd);
        //System.out.println("x " + x);
        x.cross(x, space.camUp);
        x.normalize();
        x.scale(-speed);
        space.camPos.add(x);
    }

    @Override void moveY(float speed) {
        v3 y = v(space.camUp);
        y.normalize();
        y.scale(speed);
        //System.out.println("y " + y);
        space.camPos.add(y);
    }


    @Override void moveZ(float speed) {
        v3 z = v(space.camFwd);
        //System.out.println("z " + z);
        z.scale(speed);
        space.camPos.add(z);
    }

//        @Override
//        public void keyPressed(KeyEvent e) {
//            super.keyPressed(e);
//        }

}
