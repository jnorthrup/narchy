package spacegraph.space2d.widget.text;


import jcog.Texts;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.container.unit.AspectAlign;
import spacegraph.space2d.widget.console.BitmapTextGrid;

import java.util.Arrays;

public class BitmapLabel extends AbstractLabel {

    private final BitmapTextGrid view;
    static final float minPixelsToBeVisible = 7;

    private volatile RectFloat textBounds;

    public BitmapLabel(String text) {
        super();

        textBounds = bounds;

        view = new MyBitmapTextGrid();
        view.start(this);

        text(text);
    }

    public BitmapLabel() {
        this("");
    }

    @Override
    public AbstractLabel text(String next) {
        String prev = text;
        if (prev==null || !prev.equals(next)) {

            int rows = 1 + Texts.count(next, '\n');
            boolean resized;
            if (rows == 1) {
                resized = view.resize(next.length(), 1);
            } else {
                //HACK do better
                int cols = Arrays.stream(next.split("\n")).mapToInt(String::length).max().getAsInt();
                resized = view.resize(cols, rows);
            }

            super.text(next);

            view.invalidate();

            layout();


        }
        return this;
    }

    @Override
    protected void doLayout(float dtS) {
        int c = view.cols, r = view.rows;
        if (c > 0 && r > 0) {
            textBounds = AspectAlign.innerBounds(bounds, (r * characterAspectRatio) / c);
        } else
            textBounds = bounds; //nothing
    }

    @Override
    protected void renderContent(ReSurface r) {
        view.pos(bounds);
        view.tryRender(r);
    }

    @Override
    protected boolean preRender(ReSurface r) {
        //return r.visP(bounds, minPixelsToBeVisible) > 0; //HACK TODO
        return true;
    }

    protected void layoutText() {


        if (view.cols > 0 && view.rows > 0) {
            textBounds = AspectAlign.innerBounds(bounds, (view.rows * characterAspectRatio) / view.cols);
        } else
            textBounds = bounds; //nothing
    }



    public AbstractLabel textColor(float rr, float gg, float bb) {
        fgColor.set((rr), (gg), (bb), 1f);
        return this;
    }

    public AbstractLabel backgroundColor(float rr, float gg, float bb) {
        bgColor.set((rr), (gg), (bb), 1f);
        return this;
    }


    private class MyBitmapTextGrid extends BitmapTextGrid {

        public MyBitmapTextGrid() {
            cursorCol = cursorRow = -1; //hidden
            setFillTextBackground(false);
        }

        @Override
        protected RectFloat textBounds() {
            return textBounds;
        }


        @Override
        public void doLayout(float dtS) {
        }


        @Override
        protected boolean renderText() {

            clearBackground(); //may not be necessary if only one line and all characters are used but in multiline the matrix currently isnt regular so some chars will not be redrawn

            int n = text.length();
            int row = 0, col =0;
            for (int i = 0; i < n; i++) {
                char c = text.charAt(i);
                if (c == '\n') {
                    row++;
                    col = 0;
                } else {
                    redraw(c, col++, row, fgColor, bgColor);
                }
            }
            return true;
        }


    }
}
