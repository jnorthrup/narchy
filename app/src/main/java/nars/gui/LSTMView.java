package nars.gui;

import jcog.Util;
import jcog.learn.lstm.DistractedSequenceRecall;
import jcog.learn.lstm.SimpleLSTM;
import jcog.math.random.XorShift128PlusRandom;
import spacegraph.layout.Gridding;
import spacegraph.render.Draw;
import spacegraph.widget.meter.MatrixView;

import java.util.Random;

import static spacegraph.render.JoglPhysics.window;

/**
 * Created by me on 11/22/16.
 */
public class LSTMView extends Gridding {

    public static final MatrixView.ViewFunction1D colorize = (x, gl) -> {
        x = x/2f + 0.5f;
        //colorPolarized(gl, x);
        gl.glColor3f(0,x, x/2);
        return 0;
    };
    public static final MatrixView.ViewFunction1D colorize1 = (x, gl) -> {
//        x = x/2f + 0.5f;
//        gl.glColor3f(x,x/2, 0);
        Draw.colorBipolar(gl, x);
        return 0;
    };
    public static final MatrixView.ViewFunction1D colorize2 = (x, gl) -> {
        x = x/2f + 0.5f;
        gl.glColor3f(x,0,x/2);
        return 0;
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

    public static void main(String[] arg) {


        Random r = new XorShift128PlusRandom(1234);

        DistractedSequenceRecall task =
                new DistractedSequenceRecall(r, 32, 8, 8, 100);

        int cell_blocks = 16;
        SimpleLSTM lstm = task.lstm(cell_blocks);

        float lr = 0.1f;

        //initialize
        task.scoreSupervised(lstm, lr);

        window(new LSTMView(lstm), 800, 800);

        int epochs = 5000;
        for (int epoch = 0; epoch < epochs; epoch++) {
            double fit = task.scoreSupervised(lstm, lr);
            if (epoch % 10 == 0)
                System.out.println("["+epoch+"] error = " + (1 - fit));
            Util.sleep(1);
        }
        System.out.println("done.");
    }
}
