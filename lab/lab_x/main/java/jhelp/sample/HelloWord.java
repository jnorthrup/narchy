package jhelp.sample;

import jhelp.engine.JHelpSceneRenderer;
import jhelp.engine.Object3D;
import jhelp.engine.Scene;
import jhelp.engine.geom.Box;
import jhelp.engine.gui.JHelpFrame3D;
import jhelp.util.gui.UtilGUI;

/**
 * Hello word exemple, a simple cube in middle of the scene
 * 
 * @author JHelp
 */
public class HelloWord
{

   /**
    * Launch the hello word
    * 
    * @param args
    *           Unused
    */
   public static void main(final String[] args)
   {
      UtilGUI.initializeGUI();

      
      final JHelpFrame3D frame3d = new JHelpFrame3D(true, "Sample : Hello word");
      frame3d.setVisible(true);

      
      final JHelpSceneRenderer sceneRenderer = frame3d.getSceneRenderer();
      
      final Scene scene = sceneRenderer.getScene();

      
      final Object3D cube = new Box();

      
      scene.add(cube);

      
      scene.setPosition(0, 0, -5);
      
      scene.setAngleX(18f);
      scene.setAngleY(-26f);
      scene.setAngleZ(0f);

      
      scene.flush();
   }
}