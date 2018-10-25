package nars.gui;

import com.googlecode.lanterna.TextColor;
import nars.NAR;
import nars.concept.Concept;
import nars.control.DurService;
import nars.term.Termed;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.console.ConsoleTerminal;

import java.io.IOException;

public class ConceptView extends Gridding {

    private final Termed term;
    private final ConsoleTerminal io;
    private final NAR nar;
    private final StringBuilder sa;
    private DurService on;

    public ConceptView(Termed t, NAR n) {
        super();

        this.term = t;
        this.nar = n;
        this.sa = new StringBuilder();
        this.io = new ConsoleTerminal(120,60);
        io.text.fontSize(12);

        io.term.setForegroundColor(TextColor.ANSI.WHITE);

        set(io);

    }

    protected void update() {
        Concept c = nar.concept(term);
        if (c!=null) {

            sa.setLength(0);
            c.print(sa, false, false, false);

            io.term.clearScreen();
            io.append(sa);
            io.term.flush();

        } else {
            io.term.clearScreen();
            try {
                io.append(String.valueOf(term)).append(" unconceptualized");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean start(@Nullable SurfaceBase parent) {
        if (super.start(parent)) {
            on = DurService.on(nar, this::update);
            return true;
        }
        return false;
    }

    @Override
    public boolean stop() {
        if (super.stop()) {
            if (on != null) {
                on.off();
                on = null;
            }
            return true;
        }
        return false;
    }
}
