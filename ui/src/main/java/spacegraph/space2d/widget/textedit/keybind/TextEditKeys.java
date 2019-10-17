package spacegraph.space2d.widget.textedit.keybind;

import com.jogamp.newt.event.KeyEvent;
import spacegraph.space2d.widget.textedit.TextEditModel;

@FunctionalInterface
public interface TextEditKeys {

    boolean key(KeyEvent e, boolean pressedOrReleased, TextEditModel editor);

}
