package spacegraph.space2d;

import spacegraph.input.finger.Finger;
import spacegraph.input.finger.NewtMouse;
import spacegraph.space2d.hud.NewtKeyboard;
import spacegraph.space2d.hud.ZoomOrtho;
import spacegraph.video.JoglSpace;

public class SpaceGraphFlat extends JoglSpace {

    private final ZoomOrtho zoom;
//    private final Ortho<MutableListContainer> hud;

    private final Finger finger;
    private final NewtKeyboard keyboard;

    public SpaceGraphFlat(Surface content) {
        super();


        keyboard = new NewtKeyboard(/*TODO this */);

        finger = new NewtMouse(this);


        zoom = new ZoomOrtho(content, finger, keyboard) {
            @Override
            protected void starting() {
                super.starting();
                io.window.setPointerVisible(false); //HACK
            }
        };
        add(zoom);
        add(finger.zoomBoundsSurface(zoom.cam));
        add(finger.cursorSurface());
        //addOverlay(this.keyboard.keyFocusSurface(cam));

//        Ortho<MutableListContainer> hud = new Ortho<>(new MutableListContainer(), finger, keyboard) {
//            @Override
//            protected boolean autosize() {
//                return true;
//            }
//        };
//        add(hud);
//        hud.content().add(new PushButton("x").pos(0, 0, 100f, 100f));





    }


    @Override
    protected void init() {
    }
}
