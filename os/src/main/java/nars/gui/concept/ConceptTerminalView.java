package nars.gui.concept;

import com.googlecode.lanterna.TextColor;
import nars.NAR;
import nars.concept.Concept;
import nars.term.Termed;
import spacegraph.space2d.widget.console.ConsoleTerminal;

import java.io.IOException;

public class ConceptTerminalView extends ConceptView {

    private final ConsoleTerminal io;
    private final StringBuilder sa;

    public ConceptTerminalView(Termed t, NAR n) {
        super(t, n);
        this.sa = new StringBuilder();
        this.io = new ConsoleTerminal(120,60);
        io.text.fontSize(12);

        io.term.setForegroundColor(TextColor.ANSI.WHITE);

        set(io);
    }

    @Override
    protected void update() {
        Concept c = concept();
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
}
