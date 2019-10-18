/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.examples.stereo;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS32;
import georegression.geometry.ConvertRotation3D_F32;
import georegression.metric.UtilAngle;
import georegression.struct.EulerType;
import georegression.struct.point.Point3D_F32;
import georegression.struct.point.Vector3D_F32;
import georegression.struct.se.Se3_F32;
import georegression.transform.se.SePointOps_F32;
import org.ddogleg.struct.GrowQueue_F32;
import org.ddogleg.struct.GrowQueue_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Renders a 3D point cloud using a perspective pinhole camera model. Points are rendered as sprites which are
 * always the same size. The image is then converted into a BufferedImage for output
 * </p>
 *
 * @author Peter Abeles
 */
public class PointCloudViewerPanelSwing extends JPanel
		implements MouseMotionListener, MouseListener, MouseWheelListener {

	// TODO right-click or shift-click  perform a roll

	// TODO Mouse rotation rotates so that the point clicked is moved to where the mouse is
	// use vectors to figure out that rotation

	// Storage for xyz coordinates of points in the count
	GrowQueue_F32 cloudXyz = new GrowQueue_F32();
	// Storage for rgb values of points in the cloud
	GrowQueue_I32 cloudColor = new GrowQueue_I32();

	// Maximum render distance
	float maxRenderDistance = Float.MAX_VALUE;
	// If true then fog is rendered. This makes points fade to background color at a distance
	boolean fog;

	// intrinsic camera parameters
	float hfov = UtilAngle.radian(50);

	// transform from world frame to camera frame
	Se3_F32 worldToCamera = new Se3_F32();

	// how far it moves in the world frame for each key press
	float stepSize;

	final Object imageLock = new Object();
	GrayS32 imageRgb = new GrayS32(1,1);
	GrayF32 imageDepth = new GrayF32(1,1);

	BufferedImage imageOutput = new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);

	private int dotRadius = 2;

	int backgroundColor = 0;

	// previous mouse location
	int prevX;
	int prevY;

	Keyboard keyboard = new Keyboard();
	ScheduledExecutorService pressedTask;

	public PointCloudViewerPanelSwing(float keyStepSize ) {

		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		setFocusable(true);
		requestFocus();
		this.stepSize = keyStepSize;

		addFocusListener(new FocusListener(){
			@Override
			public void focusGained(FocusEvent e) {
//				System.out.println("focus gained");
				KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyboard);

				// start a timed task which checks current key presses. Less OS dependent this way
				pressedTask = Executors.newScheduledThreadPool(1);
				pressedTask.scheduleAtFixedRate(new KeypressedTask(),100,30, TimeUnit.MILLISECONDS);
			}

			@Override
			public void focusLost(FocusEvent e) {
//				System.out.println("focus lost");
				KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyboard);
				pressedTask.shutdown();
				pressedTask = null;
				resetKey();
			}
		});
	}

	public PointCloudViewerPanelSwing( float hfov, float keyStepSize) {
		this(keyStepSize);
		setHorizontalFieldOfView(hfov);
	}

	public void resetKey() {
		pressed.clear();
		shiftPressed = false;
	}

	public synchronized void setWorldToCamera(Se3_F32 worldToCamera ) {
		this.worldToCamera.set(worldToCamera);
	}

	public void setHorizontalFieldOfView( float radians ) {
		this.hfov = radians;
	}

	public synchronized void clearCloud() {
		cloudXyz.reset();
		cloudColor.reset();
	}

	public synchronized void addPoint(float x , float y , float z , int rgb ) {
		cloudXyz.add(x);
		cloudXyz.add(y);
		cloudXyz.add(z);
		cloudColor.add(rgb);
	}
	public synchronized void addPoints(float pointsXYZ[] , int pointsRGB[] , int length ) {
		int idxSrc = cloudXyz.size*3;

		cloudXyz.extend( cloudXyz.size + length*3 );
		cloudColor.extend( cloudColor.size + length );

		for (int i = 0, idx=0; i < length; i++) {
			cloudXyz.data[idxSrc++] = pointsXYZ[idx++];
			cloudXyz.data[idxSrc++] = pointsXYZ[idx++];
			cloudXyz.data[idxSrc++] = pointsXYZ[idx++];
			cloudColor.data[i] = pointsRGB[i];
		}
	}

	@Override
	public synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);

		projectScene();
		imageOutput = ConvertBufferedImage.checkDeclare(imageRgb.width,imageRgb.height,imageOutput,BufferedImage.TYPE_INT_RGB);
		DataBufferInt buffer = (DataBufferInt)imageOutput.getRaster().getDataBuffer();
		System.arraycopy(imageRgb.data,0,buffer.getData(),0,imageRgb.width*imageRgb.height);
		g.drawImage(imageOutput,0,0,null);
	}

	private Point3D_F32 worldPt = new Point3D_F32();
	private Point3D_F32 cameraPt = new Point3D_F32();
	private Point3D_F32 pixel = new Point3D_F32();
	private synchronized void projectScene() {
		int w = getWidth();
		int h = getHeight();

		imageDepth.reshape(w,h);
		imageRgb.reshape(w,h);

		CameraPinhole intrinsic = PerspectiveOps.createIntrinsic(w,h,UtilAngle.degree(hfov));

		float fx = (float)intrinsic.fx;
		float fy = (float)intrinsic.fy;
		float cx = (float)intrinsic.cx;
		float cy = (float)intrinsic.cy;

		ImageMiscOps.fill(imageDepth,Float.MAX_VALUE);
		ImageMiscOps.fill(imageRgb,backgroundColor);

		float maxDistanceSq = maxRenderDistance*maxRenderDistance;
		if( Float.isInfinite(maxDistanceSq))
			maxDistanceSq = Float.MAX_VALUE;

		int totalPoints = cloudXyz.size/3;
		for( int i = 0,pointIdx=0; i < totalPoints; i++ ) {
			worldPt.x = cloudXyz.data[pointIdx++];
			worldPt.y = cloudXyz.data[pointIdx++];
			worldPt.z = cloudXyz.data[pointIdx++];

			SePointOps_F32.transform(worldToCamera,worldPt,cameraPt);

			// can't render if it's behind the camera
			if( cameraPt.z < 0 )
				continue;

			float r2 = cameraPt.normSq();
			if( r2 > maxDistanceSq )
				continue;

			pixel.x = fx * cameraPt.x/cameraPt.z + cx;
			pixel.y = fy * cameraPt.y/cameraPt.z + cy;


			int x = (int)(pixel.x+0.5f);
			int y = (int)(pixel.y+0.5f);

			if( !imageDepth.isInBounds(x,y) )
				continue;

			int rgb = cloudColor.data[i];
			if( fog ) {
				rgb = applyFog(rgb, 1.0f-(float)Math.sqrt(r2)/maxRenderDistance );
			}
			renderDot(x,y,cameraPt.z,rgb);
		}
	}

	/**
	 * Fades color into background as a function of distance
	 */
	private int applyFog( int rgb , float fraction ) {
		// avoid floating point math
		int adjustment = (int)(1000*fraction);

		int r = (rgb >> 16)&0xFF;

        r = (r * adjustment + ((backgroundColor>>16)&0xFF)*(1000-adjustment)) / 1000;
        int g = (rgb >> 8) & 0xFF;
        g = (g * adjustment + ((backgroundColor>>8)&0xFF)*(1000-adjustment)) / 1000;
        int b = rgb & 0xFF;
        b = (b * adjustment + (backgroundColor&0xFF)*(1000-adjustment)) / 1000;

		return (r << 16) | (g << 8) | b;
	}

	/**
	 * Renders a dot as a square sprite with the specified color
	 */
	private void renderDot( int cx , int cy , float Z , int rgb ) {
		for (int i = -dotRadius; i <= dotRadius; i++) {
			int y = cy+i;
			if( y < 0 || y >= imageRgb.height )
				continue;
			for (int j = -dotRadius; j <= dotRadius; j++) {
				int x = cx+j;
				if( x < 0 || x >= imageRgb.width )
					continue;

				int pixelIndex = imageDepth.getIndex(x,y);
				float depth = imageDepth.data[pixelIndex];
				if( depth > Z ) {
					imageDepth.data[pixelIndex] = Z;
					imageRgb.data[pixelIndex] = rgb;
				}
			}
		}
	}

	boolean shiftPressed = false;
	private final Set<Integer> pressed = new HashSet<>();

	private class Keyboard implements KeyEventDispatcher {

		@Override
		public boolean dispatchKeyEvent(KeyEvent e) {
			switch (e.getID()) {

				case KeyEvent.KEY_PRESSED:
//					System.out.println("Key pressed "+e.getKeyChar());
					switch( e.getKeyCode() ) {
						case KeyEvent.VK_SHIFT: shiftPressed=true; break;
						default:pressed.add(e.getKeyCode());break;

					}
					break;

				case KeyEvent.KEY_RELEASED:
//					System.out.println("Key released "+e.getKeyChar());
					switch( e.getKeyCode() ) {
						case KeyEvent.VK_SHIFT: shiftPressed=false; break;
						default:
							if( !pressed.remove(e.getKeyCode()) ) {
								System.err.println("Possible Java / Mac OS X bug related to 'character accent menu'" +
										" if using Java 1.8 try upgrading to 11");
							}
							break;
					}
					break;
			}
			return false;
		}
	}

	private void handleKeyPress() {
		Vector3D_F32 T = worldToCamera.getT();

		synchronized (pressed) {
			double multiplier = shiftPressed?5:1;
			Integer[] keys = pressed.toArray(new Integer[0]);

			for( int k : keys ) {
				switch( k ) {
					case KeyEvent.VK_W:T.z -= stepSize*multiplier; break;
					case KeyEvent.VK_S:T.z += stepSize*multiplier; break;
					case KeyEvent.VK_A:T.x += stepSize*multiplier; break;
					case KeyEvent.VK_D:T.x -= stepSize*multiplier; break;
					case KeyEvent.VK_Q:T.y -= stepSize*multiplier; break;
					case KeyEvent.VK_E:T.y += stepSize*multiplier; break;
					case KeyEvent.VK_H:worldToCamera.reset(); break;
				}
			}
		}
		repaint();
	}

	private class KeypressedTask implements Runnable {

		@Override
		public void run() {
			synchronized (pressed) {
				handleKeyPress();
			}
		}
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
//		offsetZ -= e.getWheelRotation()*pixelToDistance;

		repaint();
	}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {
		requestFocus();
		prevX = e.getX();
		prevY = e.getY();
	}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public synchronized void mouseDragged(MouseEvent e) {
        float rotY = 0;

        rotY += (e.getX() - prevX)*0.002;
        float rotX = 0;
        rotX += (prevY - e.getY())*0.002;

		Se3_F32 rotTran = new Se3_F32();
        float rotZ = 0;
        ConvertRotation3D_F32.eulerToMatrix(EulerType.XYZ,rotX,rotY,rotZ,rotTran.getR());
		Se3_F32 temp = worldToCamera.concat(rotTran,null);
		worldToCamera.set(temp);

		prevX = e.getX();
		prevY = e.getY();

		repaint();
	}

	@Override
	public void mouseMoved(MouseEvent e) {}

	public int getDotRadius() {
		return dotRadius;
	}

	public void setDotRadius(int dotRadius) {
		this.dotRadius = dotRadius;
	}
}
