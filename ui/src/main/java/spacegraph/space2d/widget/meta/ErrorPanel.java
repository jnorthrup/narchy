package spacegraph.space2d.widget.meta;

import spacegraph.space2d.widget.textedit.TextEdit;

/** TODO make better */
public class ErrorPanel extends TextEdit {

    public ErrorPanel(String message) {
        super(message);
    }

    public ErrorPanel(Throwable t) {
        super(t.toString());
        StackWalker.getInstance().forEach((StackWalker.StackFrame f)->{
           insert(f.toString());
        });
    }

    public ErrorPanel(Throwable t, Object cause) {
        this(t);
        insert("because:\n" + cause);
    }
}
