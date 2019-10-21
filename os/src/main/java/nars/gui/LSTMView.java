package nars.gui;

import com.jogamp.opengl.GL2;
import jcog.learn.lstm.SimpleLSTM;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.meter.MatrixView;
import spacegraph.video.Draw;

/**
 * Created by me on 11/22/16.
 */
public class LSTMView extends Gridding {

    public static final MatrixView.ViewFunction1D colorize = new MatrixView.ViewFunction1D() {
        @Override
        public float update(float x, GL2 gl) {
            float x1 = x / 2f + 0.5f;

            gl.glColor3f((float) 0, x1, x1 / 2.0F);
            return (float) 0;
        }
    };
    public static final MatrixView.ViewFunction1D colorize1 = new MatrixView.ViewFunction1D() {
        @Override
        public float update(float x, GL2 gl) {


            Draw.colorBipolar(gl, x);
            return (float) 0;
        }
    };
    public static final MatrixView.ViewFunction1D colorize2 = new MatrixView.ViewFunction1D() {
        @Override
        public float update(float x, GL2 gl) {
            float x1 = x / 2f + 0.5f;
            gl.glColor3f(x1, (float) 0, x1 / 2.0F);
            return (float) 0;
        }
    };
    public LSTMView(SimpleLSTM lstm) {
        super(
            new MatrixView(lstm.in, 8, colorize),
            new MatrixView(lstm.full_hidden, 4, colorize1),
            new MatrixView(lstm.weightsOut),
            new MatrixView(lstm.deltaOut, 4, colorize1),
            new MatrixView(lstm.out, 4, colorize2)
        );
    }

//    public static void main(String[] arg) {
//
//
//        Random r = new XorShift128PlusRandom(1234);
//
//        DistractedSequenceRecall task =
//                new DistractedSequenceRecall(r, 32, 8, 8, 100);
//
//        int cell_blocks = 16;
//        SimpleLSTM lstm = task.lstm(cell_blocks);
//
//        float lr = 0.1f;
//
//
//        task.scoreSupervised(lstm, lr);
//
//        SpaceGraph.window(new LSTMView(lstm), 800, 800);
//
//        int epochs = 5000;
//        for (int epoch = 0; epoch < epochs; epoch++) {
//            double fit = task.scoreSupervised(lstm, lr);
//            if (epoch % 10 == 0)
//                System.out.println("["+epoch+"] error = " + (1 - fit));
//            Util.sleepMS(1);
//        }
//        System.out.println("done.");
//    }
}
