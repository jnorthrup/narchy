package jhelp.sample;

import jhelp.engine.*;
import jhelp.engine.anim.AnimationEquation;
import jhelp.engine.anim.AnimationPositionNode;
import jhelp.engine.anim.MultiAnimation;
import jhelp.engine.geom.Sphere;
import jhelp.engine.gui.FrameView3D;
import jhelp.engine.util.Math3D;
import jhelp.engine.util.PositionNode;
import jhelp.util.math.formal.Function;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Frame tha shows the node chain sample
 * 
 * @author JHelp
 */
public class NodeChainFrame
      extends FrameView3D
      implements KeyListener
{
   /** Number of additional balls (Must be >=1) */
   private static final int   QUEUE_SIZE = 4;
   /** Space bettwen balls center */
   private static final float STEP       = 2.2f;
   /** Animation started with space key */
   private final Animation    animationSpace;
   /** Head of the chain. The blue ball */
   private final Object3D     head;

   public static void main(String[] args) {
      final NodeChainFrame nodeChainFrame = new NodeChainFrame();
      SwingUtilities.invokeLater( () -> nodeChainFrame.showFrame() );


   }
   /**
    * Create a new instance of NodeChainFrame
    */
   public NodeChainFrame()
   {
      super("Node chain sample", 1024, 1024);

      
      final Scene scene = this.getScene();
      scene.setPosition(0, 0, -32);

      final JHelpSceneRenderer sceneRenderer = this.getSceneRenderer();

      
      this.head = new Sphere();
      scene.add(this.head);

      
      final Node[] queue = new Node[NodeChainFrame.QUEUE_SIZE];
      float x = NodeChainFrame.STEP;
      for(int i = 0; i < NodeChainFrame.QUEUE_SIZE; i++, x += NodeChainFrame.STEP)
      {
         queue[i] = new ObjectClone(this.head);
         queue[i].translate(x, 0, 0);
         scene.add(queue[i]);
      }

      
      final NodeChain nodeChain = new NodeChain(this.head, queue);
      sceneRenderer.playAnimation(nodeChain);

      
      final Material materialHead = new Material("HEAD");
      materialHead.setColorDiffuse(Color4f.BLUE);
      this.head.setMaterial(materialHead);

      
      scene.flush();

      
      this.manipulateAllScene();

      
      this.animationSpace = new MultiAnimation(true);

      
      
      final AnimationPositionNode animationPositionNode = new AnimationPositionNode(this.head);
      final PositionNode positionNode = new PositionNode();
      positionNode.x = 10;
      positionNode.y = -2 * Math3D.TWO_PI;
      positionNode.z = 0;
      animationPositionNode.addFrame(100, positionNode);

      ((MultiAnimation) this.animationSpace).addAnimation(animationPositionNode);

      
      ((MultiAnimation) this.animationSpace).addAnimation(new AnimationEquation(Function.parse("10*cos(t)"), Function.parse("t"), Function.parse("10*sin(t)"), -2 * Math3D.TWO_PI, 2 * Math3D.TWO_PI, 1000, this.head));

      
      sceneRenderer.addKeyListener(this);
      
      sceneRenderer.setAnimationsFps(100);
   }

   /**
    * Called when key pressed <br>
    * <br>
    * <b>Parent documentation:</b><br>
    * {@inheritDoc}
    * 
    * @param e
    *           Key event description
    * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
    */
   @Override
   public void keyPressed(final KeyEvent e)
   {
      switch(e.getKeyCode())
      {
         case KeyEvent.VK_LEFT:
            this.head.translate(-1f, 0, 0);
         break;
         case KeyEvent.VK_RIGHT:
            this.head.translate(1f, 0, 0);
         break;
         case KeyEvent.VK_UP:
            this.head.translate(0, 1f, 0);
         break;
         case KeyEvent.VK_DOWN:
            this.head.translate(0, -1f, 0);
         break;
         case KeyEvent.VK_PAGE_UP:
            this.head.translate(0, 0, -1f);
         break;
         case KeyEvent.VK_PAGE_DOWN:
            this.head.translate(0, 0, 1f);
         break;
         case KeyEvent.VK_SPACE:
            this.getSceneRenderer().playAnimation(this.animationSpace);
         break;
         case KeyEvent.VK_ESCAPE:
            this.getSceneRenderer().stopAnimation(this.animationSpace);
         break;
         case KeyEvent.VK_S:
            this.getSceneRenderer().setShowFPS(true);
         break;
         case KeyEvent.VK_H:
            this.getSceneRenderer().setShowFPS(false);
         break;
      }
   }

   /**
    * Called when key released <br>
    * <br>
    * <b>Parent documentation:</b><br>
    * {@inheritDoc}
    * 
    * @param e
    *           Key event description
    * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
    */
   @Override
   public void keyReleased(final KeyEvent e)
   {
   }

   /**
    * Called when key typed <br>
    * <br>
    * <b>Parent documentation:</b><br>
    * {@inheritDoc}
    * 
    * @param e
    *           Key event description
    * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
    */
   @Override
   public void keyTyped(final KeyEvent e)
   {
   }
}