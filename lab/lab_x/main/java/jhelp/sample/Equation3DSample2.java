package jhelp.sample;

import jhelp.engine.*;
import jhelp.engine.NodeWithMaterial.TwoSidedState;
import jhelp.engine.geom.Equation3D;
import jhelp.engine.gui.JHelpFrame3D;
import jhelp.engine.twoD.Path;
import jhelp.engine.util.Math3D;
import jhelp.util.debug.Debug;
import jhelp.util.gui.UtilGUI;
import jhelp.util.resources.Resources;

import java.io.IOException;

public class Equation3DSample2
{
   /** Resources access */
   private static final Resources RESOURCES = new Resources(Equation3DSample2.class);

   /**
    * TODO Explains what does the method main in jhelp.engine.samples.equation3D [JHelpEngine]
    * 
    * @param args
    */
   public static void main(final String[] args)
   {
      UtilGUI.initializeGUI();

      
      final JHelpFrame3D frame3d = new JHelpFrame3D(true, "Sample : Equation 3D");
      frame3d.setSize(600,600);
      frame3d.setVisible(true);


      
      final JHelpSceneRenderer sceneRenderer = frame3d.getSceneRenderer();
      
      final Scene scene = sceneRenderer.getScene();

      
      final Path path = new Path();
      path.appendQuad(new Point2D(-0.5f, 0.5f), new Point2D(0, 0.75f), new Point2D(0.5f, 0.5f));
      path.appendQuad(new Point2D(0.5f, 0.5f), new Point2D(0.75f, 0f), new Point2D(0.5f, -0.5f));
      path.appendQuad(new Point2D(0.5f, -0.5f), new Point2D(0, -0.75f), new Point2D(-0.5f, -0.5f));
      path.appendQuad(new Point2D(-0.5f, -0.5f), new Point2D(-0.75f, 0), new Point2D(-0.5f, 0.5f));
      final Object3D knot = new Equation3D(path, 16, -Math3D.PI, Math3D.PI, Math3D.PI / 64f, "2*(sin(t)+2*sin(2*t))", "2*(cos(t)-2*cos(2*t))", "2*(-sin(3*t))");
      knot.setTwoSidedState(TwoSidedState.FORCE_TWO_SIDE);

      
      final Material material = new Material("MaterialEquation");
      try
      {
         Texture texture = new Texture("TextureDiffuse", Texture.REFERENCE_RESOURCES, Equation3DSample2.RESOURCES.obtainResourceStream("floor068.jpg"));
         material.setTextureDiffuse(texture);

         texture = new Texture("TextureSpherique", Texture.REFERENCE_RESOURCES, Equation3DSample2.RESOURCES.obtainResourceStream("emerald_bk.jpg"));
         material.setTextureSpheric(texture);
         material.setSphericRate(0.5f);
      }
      catch(final IOException exception)
      {
         Debug.printException(exception);
      }

      knot.setMaterial(material);

      
      scene.add(knot);

      
      
      scene.setPosition(0, 0, -20.279984f);
      
      scene.setAngleX(183.0f);
      scene.setAngleY(13.0f);
      scene.setAngleZ(0f);

      
      scene.flush();
   }
}