package spacegraph.space2d.widget.adapter;

import com.jcraft.jcterm.JCTermSwingFrame;
import spacegraph.SpaceGraph;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.windo.Dyn2DSurface;

import javax.swing.*;

/**
 * Created by me on 11/13/16.
 */
public class SSHSurface extends AWTSurface {

    public SSHSurface() {
        super(new JCTermSwingFrame(), 800, 600);
    }

    public static void main(String[] args) {

        Dyn2DSurface w = SpaceGraph.wall(800, 600);
        w.put(new Gridding(new SSHSurface()), 8, 6);
        w.put(new Gridding(new AWTSurface(new JColorChooser(), 200, 200)),
                3, 3);

    }


}

//ui.setLayout(new BorderLayout());

//        JPanel ui = new JPanel(new FlowLayout());
//        JTextField x = new JTextField("XYZ");
//        ui.add(x);
//
//        ui.add(new JSlider());
//        ui.add(new JButton("XYZ"));
//        ui.add(new JSlider());

//        Component ui = new JColorChooser().getComponents()[0];

//        JComboBox ui = new JComboBox();
//        ui.addItem("a");
//        ui.addItem("b");
//        ui.addItem("c");

//Component ui = new JButton("XYZ");
//        Component ui = new JSlider(SwingConstants.VERTICAL);

//        Component ui = new JTextArea();
//        ui.setSize(300, 300);


///** old impl */
//class SSHSurface0 extends ConsoleTerminal {
//
//    static class SSHClient {
//
//        public final Session session;
//        public final ChannelShell channel;
//
//        public SSHClient(String host, String user, String pw, InputStream in, OutputStream out) throws JSchException {
//
//
//            JSch jsch = new JSch();
//
//            //jsch.setKnownHosts("/home/foo/.ssh/known_hosts");
//
//            session = jsch.getSession(user, host, 22);
//            session.setPassword(pw);
//
//
////            UserInfo ui = new MyUserInfo() {
////                public void showMessage(String message) {
////                    JOptionPane.showMessageDialog(null, message);
////                }
////
////                public boolean promptYesNo(String message) {
////                    Object[] options = {"yes", "no"};
////                    int foo = JOptionPane.showOptionDialog(null,
////                            message,
////                            "Warning",
////                            JOptionPane.DEFAULT_OPTION,
////                            JOptionPane.WARNING_MESSAGE,
////                            null, options, options[0]);
////                    return foo == 0;
////                }
////
////                // If password is not given before the invocation of Session#connect(),
////                // implement also following methods,
////                //   * UserInfo#getPassword(),
////                //   * UserInfo#promptPassword(String message) and
////                //   * UIKeyboardInteractive#promptKeyboardInteractive()
////
////            };
//
//            session.setUserInfo(new spacegraph.net.SSHClient.MyUserInfo());
//
//            // It must not be recommended, but if you want to skip host-key check,
//            // invoke following,
//            session.setConfig("StrictHostKeyChecking", "no");
//
//            //session.connect();
//            session.connect(30000);   // making a connection with timeout.
//
//            channel = (ChannelShell) session.openChannel("shell");
//
//
//
//            // Enable agent-forwarding.
//            //((ChannelShell)channel).setAgentForwarding(true);
//
//            channel.setInputStream(in);
//      /*
//      // a hack for MS-DOS prompt on Windows.
//      channel.setInputStream(new FilterInputStream(System.in){
//          public int read(byte[] b, int off, int len)throws IOException{
//            return in.read(b, off, (len>1024?1024:len));
//          }
//        });
//       */
//
//            channel.setOutputStream(out);
//
//
//            // Choose the pty-type "vt102".
//            channel.setPtyType("ansi");
//
//
//      /*
//      // Set environment variable "LANG" as "ja_JP.eucJP".
//      ((ChannelShell)channel).setEnv("LANG", "ja_JP.eucJP");
//      */
//
//            //channel.connect();
//            channel.connect(3 * 1000);
//
//
//
//
//        }
//
//
//
//
//        public static class MyUserInfo
//                implements UserInfo, UIKeyboardInteractive {
//            @Override
//            public String getPassword() {
//                return null;
//            }
//
//            @Override
//            public boolean promptYesNo(String str) {
//                return false;
//            }
//
//            @Override
//            public String getPassphrase() {
//                return null;
//            }
//
//            @Override
//            public boolean promptPassphrase(String message) {
//                return false;
//            }
//
//            @Override
//            public boolean promptPassword(String message) {
//                return false;
//            }
//
//            @Override
//            public void showMessage(String message) {
//            }
//
//            @Override
//            public String[] promptKeyboardInteractive(String destination,
//                                                      String name,
//                                                      String instruction,
//                                                      String[] prompt,
//                                                      boolean[] echo) {
//                return null;
//            }
//        }
//    }
//    public static void main(String[] args) throws IOException, JSchException {
//
//        SpaceGraph.window(new SSHSurface0(
//                args[0], args[1], args[2],
//                80, 24), 1000, 600);
//    }
//
//
//    final PipedOutputStream ins = new PipedOutputStream();
//
//    private final SSHClient ssh;
//
//    static final TextColor defaultTextColor = TextColor.ANSI.WHITE;
//    static final TextColor defaultBackgroundColor = TextColor.ANSI.BLACK;
//
//
//    public SSHSurface0(String host, String user, String password, int cols, int rows) throws IOException, JSchException {
//        super(cols, rows);
//
//        term.setForegroundColor(defaultTextColor);
//        term.setBackgroundColor(defaultBackgroundColor);
//
//        ssh = new SSHClient(host, user, password,
//
//                new PipedInputStream(ins),
//
//                new AnsiOutputStream(new OutputStream() {
//
//                    @Override
//                    public void write(int i) {
//                        term.putCharacter((char) i);
//                    }
//
//                    @Override
//                    public void flush() {
//                        term.flush();
//                    }
//                }) {
//
//                    @Override
//                    protected void processCursorTo(int row, int col) {
//                        term.setCursorPosition(col, row);
//                    }
//
//                    @Override
//                    protected void processCursorDown(int count) {
//                        addCursorPosition(0, count);
//                    }
//
//                    @Override
//                    protected void processCursorLeft(int count) {
//                        addCursorPosition(-count, 0);
//                    }
//
//                    @Override
//                    protected void processCursorRight(int count) {
//                        addCursorPosition(count, 0);
//                    }
//
//                    @Override
//                    protected void processCursorUp(int count) {
//                        addCursorPosition(0, -count);
//                    }
//
//
//                    private void addCursorPosition(int dCol, int dRow) {
//                        TerminalPosition p = term.getCursorPosition();
//                        int c = p.getColumn();
//                        int r = p.getRow();
//                        term.setCursorPosition(c + dCol, r + dRow);
//                    }
//
//                    @Override
//                    protected void processCursorToColumn(int c) {
//                        term.setCursorPosition(c, term.getCursorPosition().getRow());
//                    }
//
//                    @Override
//                    protected void processCursorUpLine(int count) {
//                        System.out.println("unhandled processCursorUpLine " + count);
//                    }
//
//                    @Override
//                    protected void processScrollDown(int optionInt) {
//                        System.out.println("unhandled processScrollDown " + optionInt);
//                    }
//
//                    @Override
//                    protected void processSaveCursorPosition() {
//                        System.out.println("unhandled save cursor position");
//                    }
//
//                    @Override
//                    protected void processRestoreCursorPosition() {
//                        System.out.println("unhandled restore cursor position");
//                    }
//
//                    @Override
//                    protected void processEraseLine(int eraseOption) {
//
////                        protected static final int ERASE_LINE_TO_END = 0;
////                        protected static final int ERASE_LINE_TO_BEGINING = 1;
////                        protected static final int ERASE_LINE = 2;
//                        switch (eraseOption) {
//                            case 0:
//                                TerminalPosition p = term.getCursorPosition();
//
//                                //WTF lanterna why cant i access the buffer directly its private
//                                int start = p.getColumn();
//                                for (int i = start; i < term.getTerminalSize().getColumns(); i++)
//                                    term.putCharacter(' ');
//
//                                //return
//                                term.setCursorPosition(start, p.getRow());
//
//                                break;
//                            default:
//                                System.out.println("unhandled erase: " + eraseOption);
//                                break;
//                        }
//                    }
//
//                    @Override
//                    protected void processDefaultTextColor() {
//                        term.setForegroundColor(defaultTextColor);
//                        term.setBackgroundColor(defaultBackgroundColor);
//                    }
//
//                    @Override
//                    protected void processSetForegroundColor(int color) {
//                        term.setForegroundColor(TextColor.ANSI.values()[color]);
//                    }
//
//                    @Override
//                    protected void processSetBackgroundColor(int color) {
//                        term.setBackgroundColor(TextColor.ANSI.values()[color]);
//                    }
//
//                    @Override
//                    protected void processSetBackgroundColor(int color, boolean bright) {
//                        processSetBackgroundColor(color);
//                        //TODO bold
//                    }
//
//                    @Override
//                    protected void processSetForegroundColor(int color, boolean bright)  {
//                        processSetForegroundColor(color);
//                        //TODO bold
//                    }
//
//                    @Override
//                    protected void processEraseScreen(int eraseOption) {
////                        protected static final int ERASE_SCREEN_TO_END = 0;
////                        protected static final int ERASE_SCREEN_TO_BEGINING = 1;
////                        protected static final int ERASE_SCREEN = 2;
//
//                        switch (eraseOption) {
//                            case ERASE_SCREEN:
//                                term.clearScreen();
//                                break;
//                            case ERASE_SCREEN_TO_BEGINING:
//                                break;
//                            case ERASE_SCREEN_TO_END:
//                                //TODO make sure this is correct
//                                term.clearScreen();
//                                break;
//                        }
//                    }
//
//                }
//        );
//    }
//
//
//    @Override
//    public boolean onKey(KeyEvent e, boolean pressed) {
//
//        //http://hackipedia.org/Protocols/Terminal,%20DEC%20VT100/html/VT100%20Escape%20Codes.html
//        //https://github.com/mintty/mintty/wiki/Keycodes
//        //http://nemesis.lonestar.org/reference/telecom/codes/ascii.html
//
//        //only interested on release
//        if (pressed)
//            return false;
//
//        if (e.isModifierKey()) {
//            return true;
//        } else if (e.isControlDown()) {
//
//            //System.out.println("ctrl: ^" + ((char)e.getKeyCode()) + "\t" + e);
//            switch (e.getKeyCode()) {
//                case 'C':
//                    return send((byte) 3); //ASCII end of text
//                case 'D':
//                    return send((byte) 4); //ASCII end of transmission
//                case 'L':
//                    return send((byte) 12);
//                case 'X':
//                    return send((byte) 24); //cancel
//                case 'Z':
//                    return send((byte) 26); //substitute
//            }
//
//
//            //return send((byte)27, (byte)e.getKeyCode());
//            return true;
//
//        } else if (e.isActionKey()) {// isActionKey()) {
//
//            short code = e.getKeyCode();
//
//            //intercept special codes
//            switch (code) {
//                case KeyEvent.VK_UP:
//                    return sendEscape( 'A');
//                case KeyEvent.VK_DOWN:
//                    return sendEscape('B');
//                case KeyEvent.VK_RIGHT:
//                    return sendEscape( 'C');
//                case KeyEvent.VK_LEFT:
//                    return sendEscape( 'D');
//                case KeyEvent.VK_HOME:
//                    return sendEscape('[', 'H');
//                case KeyEvent.VK_END:
//                    return sendEscape('[', 'F');
//                case KeyEvent.VK_BACK_SPACE:
//                    return send((byte) 8);
//                case KeyEvent.VK_DELETE:
//                    return send((byte) 127);
//                case KeyEvent.VK_NUMPAD0:
//                    return sendEscape('O', 'p');
//                case KeyEvent.VK_NUMPAD1:
//                    return sendEscape('O', 'q');
//
//
////                        case KeyEvent.VK_PAGE_UP: type = KeyType.PageUp; break;
////                        case KeyEvent.VK_PAGE_DOWN: type = KeyType.PageDown; break;
//                default:
////                            System.err.println("unhandled key: "+ e.getKeyCode());
//                    break; //ignore
//            }
//
//            //System.out.println("action: " + e);
//            return send((byte) code);
//
//        } else {
//
//            //System.out.println("key: " + e);
//            try {
//                ins.write((byte) e.getKeyChar());
//                ins.flush();
//            } catch (IOException e1) {
//                e1.printStackTrace();
//            }
//
//            return true;
//        }
//    }
//
//    private boolean sendEscape(char x) {
//        return sendEscape((byte) x);
//    }
//
//    private boolean sendEscape(char x, char y) {
//        return sendEscape((byte) x, (byte) y);
//    }
//
//    private boolean sendEscape(char a, char b, char c) {
//        return sendEscape((byte) a, (byte) b, (byte) c);
//    }
//
//    private boolean sendEscape(char a, char b, char c, char d, char e) {
//        return sendEscape((byte) a, (byte) b, (byte) c, (byte) d, (byte) e);
//    }
//
//    private boolean sendEscape(byte... x) {
//        try {
//            ins.write(escapeHeader);
//            ins.write(x);
//            ins.flush();
//        } catch (IOException e1) {
//            e1.printStackTrace();
//        }
//        return true;
//    }
//
//    private boolean send(byte... x) {
//        try {
//            ins.write(x);
//            ins.flush();
//        } catch (IOException e1) {
//            e1.printStackTrace();
//        }
//        return true;
//    }
//
//
//    final static byte[] escapeHeader = {27, (byte) '['};
//
//}
