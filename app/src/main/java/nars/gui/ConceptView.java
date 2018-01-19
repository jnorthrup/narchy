package nars.gui;

import com.googlecode.lanterna.TextColor;
import nars.NAR;
import nars.concept.Concept;
import nars.control.DurService;
import nars.term.Termed;
import org.jetbrains.annotations.Nullable;
import spacegraph.SurfaceBase;
import spacegraph.layout.Grid;
import spacegraph.widget.console.ConsoleTerminal;

import java.io.IOException;

public class ConceptView extends Grid {

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
        io.setBitmapFontSize(12);

        io.term.setForegroundColor(TextColor.ANSI.WHITE);

        set(io);

    }

    protected void update() {
        Concept c = nar.concept(term);
        if (c!=null) {

            sa.setLength(0);
            c.print(sa, false, false, true, false);

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
    public void start(@Nullable SurfaceBase parent) {
        super.start(parent);
        on = DurService.on(nar, this::update);
    }

    @Override
    public void stop() {
        synchronized (this) {
            if (on != null) {
                on.off();
                on = null;
            }
            super.stop();
        }
    }
}
