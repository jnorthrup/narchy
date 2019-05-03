//package nars.experiment.mario;
//
//import javax.swing.*;
//
//
//public class AppletLauncher extends JApplet {
//    private static final long serialVersionUID = -2238077255106243788L;
//
//    private MarioComponent mario;
//    private boolean started;
//
//    @Override
//    public void init() {
//        this.setSize(640, 480);
//    }
//
//    @Override
//    public void start() {
//        if (!started) {
//            started = true;
//            mario = new MarioComponent(getWidth(), getHeight());
//            setContentPane(mario);
//            setFocusable(false);
//            mario.setFocusCycleRoot(true);
//
//            mario.start();
//
//
//        }
//    }
//
//    @Override
//    public void stop() {
//        if (started) {
//            started = false;
//            removeKeyListener(mario);
//            mario.stop();
//            removeFocusListener(mario);
//        }
//    }
//}