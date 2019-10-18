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

import boofcv.visualize.PointCloudViewer;
import georegression.struct.ConvertFloatType;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F32;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.GrowQueue_F32;
import org.ddogleg.struct.GrowQueue_I32;

import javax.swing.*;
import java.util.List;

/**
 * Wrapper around {@link PointCloudViewerPanelSwing} for {@link PointCloudViewer}.
 *
 * @author Peter Abeles
 */
public class PointCloudViewerSwing implements PointCloudViewer {
	PointCloudViewerPanelSwing panel;

	public PointCloudViewerSwing() {
		panel = new PointCloudViewerPanelSwing((float)60,(float)1.0 );
	}

	@Override
	public void setShowAxis(boolean show) {
		// Not implemented yet. Doing correctly is a bit of work. Need to compute the 3D location of each rendered
		// point on a line
	}

	@Override
	public void setTranslationStep(double step) {
		BoofSwingUtil.invokeNowOrLater(()->panel.stepSize = (float)step);
	}

	@Override
	public void setDotSize(int pixels) {
		BoofSwingUtil.invokeNowOrLater(()->panel.setDotRadius(pixels));
	}

	@Override
	public void setClipDistance(double distance) {
		BoofSwingUtil.invokeNowOrLater(()->panel.maxRenderDistance = (float)distance);
	}

	@Override
	public void setFog(boolean active) {
		BoofSwingUtil.invokeNowOrLater(()->panel.fog = active);
	}

	@Override
	public void setBackgroundColor(int rgb) {
		BoofSwingUtil.invokeNowOrLater(()->panel.backgroundColor=rgb);
	}

	@Override
	public void addCloud(List<Point3D_F64> cloudXyz, int[] colorsRgb) {
		if( cloudXyz.size() > colorsRgb.length ) {
			throw new IllegalArgumentException("Number of points do not match");
		}
		if( SwingUtilities.isEventDispatchThread() ) {
			for (int i = 0; i < cloudXyz.size(); i++) {
				Point3D_F64 p = cloudXyz.get(i);
				panel.addPoint((float) p.x, (float) p.y, (float) p.z, colorsRgb[i]);
			}
		} else {
			SwingUtilities.invokeLater(() -> {
				for (int i = 0; i < cloudXyz.size(); i++) {
					Point3D_F64 p = cloudXyz.get(i);
					panel.addPoint((float) p.x, (float) p.y, (float) p.z, colorsRgb[i]);
				}
			});
		}
	}

	@Override
	public void addCloud(List<Point3D_F64> cloud) {
		if( SwingUtilities.isEventDispatchThread() ) {
			for (int i = 0; i < cloud.size(); i++) {
				Point3D_F64 p = cloud.get(i);
				panel.addPoint((float) p.x, (float) p.y, (float) p.z, 0xFF0000);
			}
		} else {
			SwingUtilities.invokeLater(() -> {
				for (int i = 0; i < cloud.size(); i++) {
					Point3D_F64 p = cloud.get(i);
					panel.addPoint((float) p.x, (float) p.y, (float) p.z, 0xFF0000);
				}
			});
		}
	}

	@Override
	public void addCloud(GrowQueue_F32 cloudXYZ, GrowQueue_I32 colorRGB) {
		if( cloudXYZ.size/3 != colorRGB.size ) {
			throw new IllegalArgumentException("Number of points do not match");
		}
		if( SwingUtilities.isEventDispatchThread() ) {
			panel.addPoints(cloudXYZ.data, colorRGB.data, cloudXYZ.size / 3);
		} else {
			SwingUtilities.invokeLater(() -> panel.addPoints(cloudXYZ.data, colorRGB.data, cloudXYZ.size / 3));
		}
	}

	@Override
	public void addPoint(double x, double y, double z, int rgb) {
		if( SwingUtilities.isEventDispatchThread() ) {
			panel.addPoint((float)x,(float)y,(float)z,rgb);
		} else {
			SwingUtilities.invokeLater(() -> panel.addPoint((float) x, (float) y, (float) z, rgb));
		}
	}

	@Override
	public void clearPoints() {
		if( SwingUtilities.isEventDispatchThread() ) {
			panel.clearCloud();
		} else {
			SwingUtilities.invokeLater(() -> panel.clearCloud());
		}
	}

	@Override
	public void setCameraHFov(double radians) {
		BoofSwingUtil.invokeNowOrLater(()->panel.setHorizontalFieldOfView((float)radians));
	}

	@Override
	public void setCameraToWorld(Se3_F64 cameraToWorld) {
		Se3_F64 worldToCamera = cameraToWorld.invert(null);
		Se3_F32 worldToCameraF32 = new Se3_F32();
		ConvertFloatType.convert(worldToCamera,worldToCameraF32);
		BoofSwingUtil.invokeNowOrLater(()->panel.setWorldToCamera(worldToCameraF32));
	}

	@Override
	public JComponent getComponent() {
		return panel;
	}
}
