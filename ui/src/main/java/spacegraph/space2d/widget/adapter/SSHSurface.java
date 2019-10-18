package spacegraph.space2d.widget.adapter;

import com.jcraft.jcterm.*;
import com.jcraft.jsch.JSchException;
import spacegraph.SpaceGraph;
import spacegraph.input.finger.Finger;
import spacegraph.input.key.KeyPressed;
import spacegraph.input.key.impl.Keyboard;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.unit.MutableUnitContainer;
import spacegraph.space2d.widget.meta.MetaFrame;
import spacegraph.video.Tex;
import spacegraph.video.TexSurface;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Created by me on 11/13/16.
 */
public class SSHSurface extends MutableUnitContainer implements Terminal, KeyPressed {


    private TexSurface tex;

    public static void main(String[] args) throws IOException, JSchException {

        SSHSurface s = new SSHSurface();
        SpaceGraph.window(new MetaFrame(s), 800, 600);

        s.start(new JCTermSwingFrame().connect("me", "localhost", 22));
    }


    private static final ConfigurationRepository defaultCR =
            new ConfigurationRepository() {
                private final Configuration conf = new Configuration();

                public Configuration load(String name) {
                    return conf;
                }

                public void save(Configuration conf) {
                }
            };
    private static ConfigurationRepository cr = defaultCR;

    private final Object[] colors = {Color.black, Color.red, Color.green,
            Color.yellow, Color.blue, Color.magenta, Color.cyan, Color.white};
    private OutputStream out;
    private TerminalEmulator emulator = null;
    private Connection connection = null;
    private BufferedImage img, background;
    private Graphics2D cursor_graphics, graphics;
    private Color defaultbground = Color.black;
    private Color defaultfground = Color.white;
    private Color bground = Color.black;
    private Color fground = Color.white;

    private Font font;
    private boolean bold = false, underline = false, reverse = false;
    private int term_width = 80, term_height = 24;
    private int descent = 0;
    private int x = 0, y = 0;
    private int char_width, char_height;

    private int line_space = -2;
    private boolean antialiasing = true;

    public SSHSurface() {
        super();
        setFont("Monospaced-18");

        pixelSize(getTermWidth(), getTermHeight());

        clear();


    }

    @Override
    public Surface finger(Finger finger) {
        return this;
    }

    static Color toColor(Object o) {
        if (o instanceof String) {
            try {
                return Color.decode(((String) o).trim());
            } catch (NumberFormatException e) {
            }
            return Color.getColor(((String) o).trim());
        }
        if (o instanceof Color) {
            return (Color) o;
        }
        return Color.white;
    }

    public static synchronized ConfigurationRepository getCR() {
        return cr;
    }

    public static synchronized void setCR(ConfigurationRepository _cr) {
        if (_cr == null)
            _cr = defaultCR;
        cr = _cr;
    }

    void setFont(String fname) {
        font = Font.decode(fname);
        BufferedImage b = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = (Graphics2D) (b.getGraphics());
        graphics.setFont(font);

        {
            FontMetrics fo = graphics.getFontMetrics();
            descent = fo.getDescent();
      /*
      System.out.println(fo.getDescent());
      System.out.println(fo.getAscent());
      System.out.println(fo.getLeading());
      System.out.println(fo.getHeight());
      System.out.println(fo.getMaxAscent());
      System.out.println(fo.getMaxDescent());
      System.out.println(fo.getMaxDecent());
      System.out.println(fo.getMaxAdvance());
      */
            char_width = fo.charWidth('@');
            char_height = fo.getHeight() + (line_space * 2);
            descent += line_space;
        }

        b.flush();
        graphics.dispose();
        background = new BufferedImage(char_width, char_height,
                BufferedImage.TYPE_INT_RGB);
        {
            Graphics2D foog = (Graphics2D) (background.getGraphics());
            foog.setColor(getBackGround());
            foog.fillRect(0, 0, char_width, char_height);
            foog.dispose();
        }
    }

    public void pixelSize(int w, int h) {

        Dimension pixelSize = new Dimension(getTermWidth(), getTermHeight());

        BufferedImage imgOrg = img;
        if (graphics != null)
            graphics.dispose();

        int column = w / getCharWidth();
        int row = h / getCharHeight();
        term_width = column;
        term_height = row;

        if (emulator != null)
            emulator.reset();

        img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        set(tex = Tex.view(img));
        img.setAccelerationPriority(1f);
        graphics = (Graphics2D) (img.getGraphics());
        graphics.setFont(font);

        clear_area(0, 0, w, h);
        redraw(0, 0, w, h);

        if (imgOrg != null) {
            Shape clip = graphics.getClip();
            graphics.setClip(0, 0, getTermWidth(), getTermHeight());
            graphics.drawImage(imgOrg, 0, 0, null);
            graphics.setClip(clip);
        }

        resetCursorGraphics();

        setAntiAliasing(antialiasing);

        if (connection != null) {
            connection.requestResize(this);
        }

        if (imgOrg != null) {
            imgOrg.flush();
            imgOrg = null;
        }
    }

    public synchronized void start(Connection connection) {

        if (emulator != null) {
            emulator.reset();
        }

        this.connection = connection;
        if (connection != null) {
            InputStream in = connection.getInputStream();
            out = connection.getOutputStream();
            emulator = new EmulatorVT100(this, in);
            emulator.start();
        } else {
            //stop
        }

        clear();
        redraw(0, 0, getTermWidth(), getTermHeight());
    }

    public void paintComponent(Graphics g) {

        if (img != null) {
            g.drawImage(img, 0, 0, null);
        }
    }

    @Override
    public boolean key(com.jogamp.newt.event.KeyEvent e, boolean pressedOrReleased) {

        //return super.key(e, pressed);
        //if (myFocus != null) {
        {

//            if (pressed && e.isPrintableKey()) {
//
//
//                event = new java.awt.event.KeyEvent(null,
//                        java.awt.event.KeyEvent.KEY_TYPED,
//                        System.currentTimeMillis(),
//                        modifers, VK_UNDEFINED, e.getKeyChar()
//                );
//
//            }

//            event = new java.awt.event.KeyEvent(null,
//                    pressed ? java.awt.event.KeyEvent.KEY_PRESSED : java.awt.event.KeyEvent.KEY_RELEASED,
//                    System.currentTimeMillis(),
//                    modifers, code, e.getKeyChar()
//            );

            int code = Keyboard.newtKeyCode2AWTKeyCode(e.getKeyCode());
            if (pressedOrReleased) {
                keyPressed( code, e.getKeyChar() );

            } else {
                keyTyped( e.getKeyChar() );
            }

        }
        return true;
    }

    public void processKeyEvent(KeyEvent e) {

        int id = e.getID();
        if (id == KeyEvent.KEY_PRESSED) {
            keyPressed(e);
        } else if (id == KeyEvent.KEY_RELEASED) {
            /*keyReleased(e);*/
            keyTyped(e);
        } else if (id == KeyEvent.KEY_TYPED) {
            //keyTyped(e);/*keyTyped(e);*/
        }
        e.consume();
    }

    public void keyPressed(KeyEvent e) {
        keyPressed(e.getKeyCode(), e.getKeyChar());
    }

    public void keyPressed(int keycode, char keychar) {
        byte[] code = null;
        switch (keycode) {
            case KeyEvent.VK_CONTROL:
            case KeyEvent.VK_SHIFT:
            case KeyEvent.VK_ALT:
            case KeyEvent.VK_CAPS_LOCK:
                return;
            case KeyEvent.VK_ENTER:
                code = emulator.getCodeENTER();
                break;
            case KeyEvent.VK_UP:
                code = emulator.getCodeUP();
                break;
            case KeyEvent.VK_DOWN:
                code = emulator.getCodeDOWN();
                break;
            case KeyEvent.VK_RIGHT:
                code = emulator.getCodeRIGHT();
                break;
            case KeyEvent.VK_LEFT:
                code = emulator.getCodeLEFT();
                break;
            case KeyEvent.VK_F1:
                code = emulator.getCodeF1();
                break;
            case KeyEvent.VK_F2:
                code = emulator.getCodeF2();
                break;
            case KeyEvent.VK_F3:
                code = emulator.getCodeF3();
                break;
            case KeyEvent.VK_F4:
                code = emulator.getCodeF4();
                break;
            case KeyEvent.VK_F5:
                code = emulator.getCodeF5();
                break;
            case KeyEvent.VK_F6:
                code = emulator.getCodeF6();
                break;
            case KeyEvent.VK_F7:
                code = emulator.getCodeF7();
                break;
            case KeyEvent.VK_F8:
                code = emulator.getCodeF8();
                break;
            case KeyEvent.VK_F9:
                code = emulator.getCodeF9();
                break;
            case KeyEvent.VK_F10:
                code = emulator.getCodeF10();
                break;
            case KeyEvent.VK_TAB:
                code = emulator.getCodeTAB();
                break;
        }
        if (code != null) {
            try {
                out.write(code, 0, code.length);
                out.flush();
            } catch (Exception ee) {
            }
            return;
        }


        if ((keychar & 0xff00) == 0) {
            try {
                out.write(keychar);
                out.flush();
            } catch (Exception ee) {
            }
        }
    }

    public void keyTyped(KeyEvent e) {
        char keychar = e.getKeyChar();
        keyTyped(keychar);
    }

    public void keyTyped(char keychar) {
        if ((keychar & 0xff00) != 0) {
            char[] foo = new char[1];
            foo[0] = keychar;
            try {
                byte[] goo = new String(foo).getBytes("EUC-JP");
                out.write(goo, 0, goo.length);
                out.flush();
            } catch (Exception eee) {
            }
        }
    }

    public int getTermWidth() {
        return char_width * term_width;
    }

    public int getTermHeight() {
        return char_height * term_height;
    }

    public int getCharWidth() {
        return char_width;
    }

    public int getCharHeight() {
        return char_height;
    }

    public int getColumnCount() {
        return term_width;
    }

    public int getRowCount() {
        return term_height;
    }

    public void clear() {
        graphics.setColor(getBackGround());
        graphics.fillRect(0, 0, img.getWidth(), img.getHeight());
        graphics.setColor(getForeGround());
    }

    public void setCursor(int x, int y) {

        this.x = x;
        this.y = y;
    }

    public void draw_cursor() {
        cursor_graphics.fillRect(x, y - char_height, char_width, char_height);
        redraw(x, y - char_height, char_width, char_height);
    }

    @Override
    public void redraw(int x, int y, int width, int height) {
        //repaint(x, y, width, height);
        tex.set(img);
    }


    public void clear_area(int x1, int y1, int x2, int y2) {

        graphics.setColor(getBackGround());
        graphics.fillRect(x1, y1, x2 - x1, y2 - y1);
        graphics.setColor(getForeGround());
    }


    public void scroll_area(int x, int y, int w, int h, int dx, int dy) {

        graphics.copyArea(x, y, w, h, dx, dy);
        redraw(x + dx, y + dy, w, h);
    }

    public void drawBytes(byte[] buf, int s, int len, int x, int y) {


        graphics.drawBytes(buf, s, len, x, y - descent);
        if (bold)
            graphics.drawBytes(buf, s, len, x + 1, y - descent);

        if (underline) {
            graphics.drawLine(x, y - 1, x + len * char_width, y - 1);
        }

    }

    public void drawString(String str, int x, int y) {


        graphics.drawString(str, x, y - descent);
        if (bold)
            graphics.drawString(str, x + 1, y - descent);

        if (underline) {
            graphics.drawLine(x, y - 1, x + str.length() * char_width, y - 1);
        }

    }

    public void beep() {
        Toolkit.getDefaultToolkit().beep();
    }

    /**
     * Ignores key released events.
     */
    public void keyReleased(KeyEvent event) {
    }

    public void setLineSpace(int foo) {
        this.line_space = foo;
    }

    public boolean getAntiAliasing() {
        return antialiasing;
    }

    public void setAntiAliasing(boolean foo) {
        if (graphics == null)
            return;
        antialiasing = foo;
        Object mode = foo ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON
                : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;
        Map<Object, Object> hints = new RenderingHints(
                RenderingHints.KEY_TEXT_ANTIALIASING, mode);
        graphics.setRenderingHints(hints);
    }

    public static void setCompression(int compression) {
        if (compression < 0 || 9 < compression)
            return;
    }

    public void setDefaultForeGround(Object f) {
        defaultfground = toColor(f);
    }

    public void setDefaultBackGround(Object f) {
        defaultbground = toColor(f);
    }

    private Color getForeGround() {
        if (reverse)
            return bground;
        return fground;
    }

    public void setForeGround(Object f) {
        fground = toColor(f);
        graphics.setColor(getForeGround());
    }

    private Color getBackGround() {
        if (reverse)
            return fground;
        return bground;
    }

    public void setBackGround(Object b) {
        bground = toColor(b);
        Graphics2D foog = (Graphics2D) (background.getGraphics());
        foog.setColor(getBackGround());
        foog.fillRect(0, 0, char_width, char_height);
        foog.dispose();
    }

    void resetCursorGraphics() {
        if (cursor_graphics != null)
            cursor_graphics.dispose();

        cursor_graphics = (Graphics2D) (img.getGraphics());
        cursor_graphics.setColor(getForeGround());
        cursor_graphics.setXORMode(getBackGround());
    }

    public Object getColor(int index) {
        if (colors == null || index < 0 || colors.length <= index)
            return null;
        return colors[index];
    }

    public void setBold() {
        bold = true;
    }

    public void setUnderline() {
        underline = true;
    }

    public void setReverse() {
        reverse = true;
        if (graphics != null)
            graphics.setColor(getForeGround());
    }

    public void resetAllAttributes() {
        bold = false;
        underline = false;
        reverse = false;
        bground = defaultbground;
        fground = defaultfground;
        if (graphics != null)
            graphics.setColor(getForeGround());
    }
}










































































































































































































































































































































































































































































