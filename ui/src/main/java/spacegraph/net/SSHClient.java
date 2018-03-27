package spacegraph.net;/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/**
 * This program will demonstrate how to exec 'sudo' on the remote.
 */

import com.jcraft.jsch.*;

import java.io.InputStream;
import java.io.OutputStream;


public class SSHClient {

    public final Session session;
    public final ChannelShell channel;

    public SSHClient(String host, String user, String pw, InputStream in, OutputStream out) throws JSchException {


        JSch jsch = new JSch();

        //jsch.setKnownHosts("/home/foo/.ssh/known_hosts");

        session = jsch.getSession(user, host, 22);
        session.setPassword(pw);


//            UserInfo ui = new MyUserInfo() {
//                public void showMessage(String message) {
//                    JOptionPane.showMessageDialog(null, message);
//                }
//
//                public boolean promptYesNo(String message) {
//                    Object[] options = {"yes", "no"};
//                    int foo = JOptionPane.showOptionDialog(null,
//                            message,
//                            "Warning",
//                            JOptionPane.DEFAULT_OPTION,
//                            JOptionPane.WARNING_MESSAGE,
//                            null, options, options[0]);
//                    return foo == 0;
//                }
//
//                // If password is not given before the invocation of Session#connect(),
//                // implement also following methods,
//                //   * UserInfo#getPassword(),
//                //   * UserInfo#promptPassword(String message) and
//                //   * UIKeyboardInteractive#promptKeyboardInteractive()
//
//            };

        session.setUserInfo(new MyUserInfo());

        // It must not be recommended, but if you want to skip host-key check,
        // invoke following,
        session.setConfig("StrictHostKeyChecking", "no");

        //session.connect();
        session.connect(30000);   // making a connection with timeout.

        channel = (ChannelShell) session.openChannel("shell");



        // Enable agent-forwarding.
        //((ChannelShell)channel).setAgentForwarding(true);

        channel.setInputStream(in);
      /*
      // a hack for MS-DOS prompt on Windows.
      channel.setInputStream(new FilterInputStream(System.in){
          public int read(byte[] b, int off, int len)throws IOException{
            return in.read(b, off, (len>1024?1024:len));
          }
        });
       */

        channel.setOutputStream(out);


        // Choose the pty-type "vt102".
        channel.setPtyType("ansi");


      /*
      // Set environment variable "LANG" as "ja_JP.eucJP".
      ((ChannelShell)channel).setEnv("LANG", "ja_JP.eucJP");
      */

        //channel.connect();
        channel.connect(3 * 1000);




    }




    public static class MyUserInfo
            implements UserInfo, UIKeyboardInteractive {
        @Override
        public String getPassword() {
            return null;
        }

        @Override
        public boolean promptYesNo(String str) {
            return false;
        }

        @Override
        public String getPassphrase() {
            return null;
        }

        @Override
        public boolean promptPassphrase(String message) {
            return false;
        }

        @Override
        public boolean promptPassword(String message) {
            return false;
        }

        @Override
        public void showMessage(String message) {
        }

        @Override
        public String[] promptKeyboardInteractive(String destination,
                                                  String name,
                                                  String instruction,
                                                  String[] prompt,
                                                  boolean[] echo) {
            return null;
        }
    }
}