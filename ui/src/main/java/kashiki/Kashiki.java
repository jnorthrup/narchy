package kashiki;

import kashiki.keybind.ActionRepository;
import kashiki.keybind.EmacsKeyListener;
import spacegraph.SpaceGraph;

public class Kashiki {

  public static void main(String[] args) {
    Editor e = new Editor();
    e.actions = new ActionRepository();
    e.keys = new EmacsKeyListener(e);

    e.executeAction("type", "x");
    e.executeAction("type", "y");
    e.executeAction("type", "z");
    SpaceGraph.window(new KashikiSurface(e), 800, 800);

//    Frame frame = new Frame("Kashiki");
//
//
//    frame.addWindowListener(new WindowAdapter() {
//      public void windowClosing(WindowEvent e) {
//        System.exit(0);
//      }
//    });
//
//    frame.setSize(800, 600);
//    frame.setLocation(100, 100);
//
//    frame.add(kashiki);
//
//    frame.setVisible(true);
//    Editor.getInstance().setFrame(frame);
    
  }
}
