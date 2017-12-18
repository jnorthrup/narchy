package spacegraph.widget.console;

import com.googlecode.lanterna.TerminalPosition;
import com.jcraft.jsch.JSchException;
import com.jogamp.newt.event.KeyEvent;
import org.fusesource.jansi.AnsiOutputStream;
import spacegraph.SpaceGraph;
import spacegraph.net.SSHClient;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * Created by me on 11/13/16.
 */
public class SSHConsole extends ConsoleTerminal {


    public static void main(String[] args) throws IOException, JSchException {

        SpaceGraph.window(new SSHConsole(
                "gest", "localhost", "tseg",
                80, 24 ), 1000, 600);
    }


    final PipedOutputStream ins = new PipedOutputStream();

    private final SSHClient ssh;


    public SSHConsole(String user, String host, String password, int cols, int rows) throws IOException, JSchException {
        super(cols, rows);

        ssh = new SSHClient("gest", "localhost", "tseg",

                new PipedInputStream(ins),

                new AnsiOutputStream(new OutputStream() {

                    @Override
                    public void write(int i) {
                        term.putCharacter((char) i);
                    }

                    @Override
                    public void flush() {
                        term.flush();
                    }
                }) {

                    @Override
                    protected void processCursorTo(int row, int col) {
                        term.setCursorPosition(col, row);
                    }

                    @Override
                    protected void processCursorDown(int count) {
                        addCursorPosition(0, count);
                    }

                    @Override
                    protected void processCursorLeft(int count) {
                        addCursorPosition(-count, 0);
                    }

                    @Override
                    protected void processCursorRight(int count) {
                        addCursorPosition(count, 0);
                    }

                    private void addCursorPosition(int dCol, int dRow) {
                        TerminalPosition p = term.getCursorPosition();
                        int c = p.getColumn();
                        int r = p.getRow();
                        term.setCursorPosition(c + dCol, r + dRow);
                    }

                    @Override
                    protected void processCursorUp(int count) {
                        addCursorPosition(0, -count);
                    }


                    @Override
                    protected void processCursorToColumn(int c) {
                        term.setCursorPosition(c, term.getCursorPosition().getRow());
                    }

                    @Override
                    protected void processCursorUpLine(int count) {
                        System.out.println("unhandled processCursorUpLine " + count);
                    }


                    @Override
                    protected void processSaveCursorPosition() {
                        System.out.println("unhandled save cursor position");
                    }

                    @Override
                    protected void processRestoreCursorPosition() {
                        System.out.println("unhandled restore cursor position");
                    }

                    @Override
                    protected void processEraseLine(int eraseOption) {

//                        protected static final int ERASE_LINE_TO_END = 0;
//                        protected static final int ERASE_LINE_TO_BEGINING = 1;
//                        protected static final int ERASE_LINE = 2;
                        switch (eraseOption) {
                            case 0:
                                TerminalPosition p = term.getCursorPosition();

                                //WTF lanterna why cant i access the buffer directly its private
                                int start = p.getColumn();
                                for (int i = start; i < term.getTerminalSize().getColumns(); i++)
                                    term.putCharacter(' ');

                                //return
                                term.setCursorPosition(start, p.getRow());

                                break;
                            default:
                                System.out.println("unhandled erase: " + eraseOption);
                                break;
                        }
                    }

                    @Override
                    protected void processEraseScreen(int eraseOption) {
//                        protected static final int ERASE_SCREEN_TO_END = 0;
//                        protected static final int ERASE_SCREEN_TO_BEGINING = 1;
//                        protected static final int ERASE_SCREEN = 2;

                        switch (eraseOption) {
                            case ERASE_SCREEN:
                                term.clearScreen();
                                break;
                            case ERASE_SCREEN_TO_BEGINING:
                                break;
                            case ERASE_SCREEN_TO_END:
                                //TODO make sure this is correct
                                term.clearScreen();
                                break;
                        }
                    }
                }
        );
    }


    @Override
    public boolean onKey(KeyEvent e, boolean pressed) {

        //http://hackipedia.org/Protocols/Terminal,%20DEC%20VT100/html/VT100%20Escape%20Codes.html
        //https://github.com/mintty/mintty/wiki/Keycodes
        //http://nemesis.lonestar.org/reference/telecom/codes/ascii.html

        //only interested on release
        if (pressed)
            return false;

        if (e.isModifierKey()) {
            return true;
        } else if (e.isControlDown()) {

            //System.out.println("ctrl: ^" + ((char)e.getKeyCode()) + "\t" + e);
            switch (e.getKeyCode()) {
                case 'C':
                    return send((byte) 3); //ASCII end of text
                case 'D':
                    return send((byte) 4); //ASCII end of transmission
                case 'L':
                    return send((byte) 12);
                case 'X':
                    return send((byte) 24); //cancel
                case 'Z':
                    return send((byte) 26); //substitute
            }


            //return send((byte)27, (byte)e.getKeyCode());
            return true;

        } else if (e.isActionKey()) {// isActionKey()) {

            short code = e.getKeyCode();

            //intercept special codes
            switch (code) {
                case KeyEvent.VK_UP:
                    return sendEscape( 'A');
                case KeyEvent.VK_DOWN:
                    return sendEscape('B');
                case KeyEvent.VK_RIGHT:
                    return sendEscape( 'C');
                case KeyEvent.VK_LEFT:
                    return sendEscape( 'D');
                case KeyEvent.VK_HOME:
                    return sendEscape('[', 'H');
                case KeyEvent.VK_END:
                    return sendEscape('[', 'F');
                case KeyEvent.VK_BACK_SPACE:
                    return send((byte) 8);
                case KeyEvent.VK_DELETE:
                    return send((byte) 127);
                case KeyEvent.VK_NUMPAD0:
                    return sendEscape('O', 'p');
                case KeyEvent.VK_NUMPAD1:
                    return sendEscape('O', 'q');


//                        case KeyEvent.VK_PAGE_UP: type = KeyType.PageUp; break;
//                        case KeyEvent.VK_PAGE_DOWN: type = KeyType.PageDown; break;
                default:
//                            System.err.println("unhandled key: "+ e.getKeyCode());
                    break; //ignore
            }

            //System.out.println("action: " + e);
            return send((byte) code);

        } else {

            //System.out.println("key: " + e);
            try {
                ins.write((byte) e.getKeyChar());
                ins.flush();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            return true;
        }
    }

    private boolean sendEscape(char x) {
        return sendEscape((byte) x);
    }

    private boolean sendEscape(char x, char y) {
        return sendEscape((byte) x, (byte) y);
    }

    private boolean sendEscape(char a, char b, char c) {
        return sendEscape((byte) a, (byte) b, (byte) c);
    }

    private boolean sendEscape(char a, char b, char c, char d, char e) {
        return sendEscape((byte) a, (byte) b, (byte) c, (byte) d, (byte) e);
    }

    private boolean sendEscape(byte... x) {
        try {
            ins.write(escapeHeader);
            ins.write(x);
            ins.flush();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return true;
    }

    private boolean send(byte... x) {
        try {
            ins.write(x);
            ins.flush();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return true;
    }


    final static byte[] escapeHeader = {27, (byte) '['};

}
