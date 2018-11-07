package spacegraph.space2d;

import com.jogamp.opengl.GL2;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.hud.NewtKeyboard;
import spacegraph.space2d.hud.ZoomOrtho;
import spacegraph.video.JoglSpace;

public class SpaceGraphFlat extends JoglSpace {

    private final ZoomOrtho zoom;

    private final Finger finger;
    private final NewtKeyboard keyboard;

    public SpaceGraphFlat(Surface content) {
        super();
        finger = new Finger();
        keyboard = new NewtKeyboard();
        add(zoom = new ZoomOrtho(content, finger, keyboard));
        add(zoom.finger.cursorSurface());
        add(zoom.finger.zoomBoundsSurface(zoom.cam));
        //addOverlay(this.keyboard.keyFocusSurface(cam));

    }


    @Override
    protected void initDepth(GL2 gl) {
        
    }

}
