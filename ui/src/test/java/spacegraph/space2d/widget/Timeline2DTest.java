package spacegraph.space2d.widget;

import com.jogamp.opengl.GL2;
import spacegraph.SpaceGraph;
import spacegraph.space2d.widget.button.PushButton;
import spacegraph.video.Draw;

import static org.junit.jupiter.api.Assertions.*;

public class Timeline2DTest {

    public static void main(String[] args) {

        Timeline2D.SimpleTimelineModel dummyModel = new Timeline2D.SimpleTimelineModel();
        dummyModel.add(new Timeline2D.SimpleEvent("x", 0, 1));
        dummyModel.add(new Timeline2D.SimpleEvent("y", 1, 3));
        dummyModel.add(new Timeline2D.SimpleEvent("z", 2, 5));
        dummyModel.add(new Timeline2D.SimpleEvent("w", 3, 3)); //point

        SpaceGraph.window(new Timeline2D<>(dummyModel, e->new PushButton(e.name)){
            @Override
            protected void paintBelow(GL2 gl) {
                gl.glColor3f(0, 0, 0.1f);
                Draw.rect(gl, bounds);
            }
        }.view(0, 5), 800, 600);
    }
}