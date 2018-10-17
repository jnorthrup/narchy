package kashiki.keybind;


import kashiki.Editor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;

public class EmacsKeyListenerTest {
    private EmacsKeyListener listener;

    @BeforeEach
    public void setUp() {
        listener = new EmacsKeyListener(new Editor());
    }

//    @Test
//    public void testSetUp() {
//        try {
//            System.out.println(listener.keybinds);
//        } catch (IOException e) {
//            fail(e.getMessage());
//        }
//    }

    @Test
    public void testKeyPressed() {
        listener.keyPressed(SupportKey.CTRL, KeyEvent.VK_Q, System.currentTimeMillis());
    }

}
