package spacegraph;

import spacegraph.render.JoglPhysics;
import spacegraph.render.JoglSpace;
import spacegraph.render.JoglWindow;

/**
 * Created by me on 6/20/16.
 */
public class SpaceGraph<X> extends JoglSpace<X> {







    /**
     * number of items that will remain cached, some (ideally most)
     * will not be visible but once were and may become visible again
     */
    public SpaceGraph() {
        super();
    }


    @Override
    protected void update(long dtMS) {

    }

    public static float r(float range) {
        return (-0.5f + (float) Math.random()) * 2f * range;
    }




//    void print(AbstractSpace s) {
//        System.out.println();
//        //+ active.size() + " active, "
//        System.out.println(s + ": " + this.atoms.estimatedSize() + " cached; " + "\t" + dyn.summary());
//        /*s.forEach(System.out::println);
//        dyn.objects().forEach(x -> {
//            System.out.println("\t" + x.getUserPointer());
//        });*/
//        System.out.println();
//    }





    //    public static class PickDragMouse extends SpaceMouse {
//
//        public PickDragMouse(JoglPhysics g) {
//            super(g);
//        }
//    }
//    public static class PickZoom extends SpaceMouse {
//
//        public PickZoom(JoglPhysics g) {
//            super(g);
//        }
//    }

}
