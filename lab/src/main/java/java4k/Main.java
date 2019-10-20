/*
 * Lattice Effect
 * Copyright (C) 2009 meatfighter.com
 *
 * This file is part of Lattice Effect.
 *
 * Lattice Effect is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Lattice Effect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http:
 *
 */

package java4k;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Main extends JFrame {

  private static final int WIDTH = 640;
  private static final int HEIGHT = 400;
  private static final int SCALE = 2;
  private static final boolean FISH_EYE_LENS = true;

  private static final int ITERATIONS = 16;
  private static final float GRID_SIZE = 256.0f;
  private static final float SPHERE_RADIUS = 64.0f;
  private static final float CYLINDER_RADIUS = 16.0f;
  private static final float Z0 = 320.0f;
  private static final float VELOCITY_X = 0f;
  private static final float VELOCITY_Y = -20f;
  private static final float VELOCITY_Z = 0f;
  private static final float VELOCITY_THETA = 0.002f;
  private static final float VELOCITY_PHI = 0.004f;

  private static final int DISPLAY_WIDTH = SCALE * WIDTH;
  private static final int DISPLAY_HEIGHT = SCALE * HEIGHT;
  private static final int HALF_WIDTH = WIDTH / 2;
  private static final int HALF_HEIGHT = HEIGHT / 2;
  private static final int PIXELS = WIDTH * HEIGHT;
  private static final float INVERSE_GRID_SIZE = 1.0f / GRID_SIZE;
  private static final float HALF_GRID_SIZE = GRID_SIZE / 2f;
  private static final float SPHERE_RADIUS_2 = SPHERE_RADIUS * SPHERE_RADIUS;
  private static final float INVERSE_SPHERE_RADIUS = 1f / SPHERE_RADIUS;
  private static final float CYLINDER_RADIUS_2
      = CYLINDER_RADIUS * CYLINDER_RADIUS;
  private static final float INVERSE_CYLINDER_RADIUS = 1f / CYLINDER_RADIUS;

  public void launch() {

    var rays = new float[PIXELS][3];

    if (FISH_EYE_LENS) {
      float MAX = Math.max(WIDTH, HEIGHT);
      float X_OFFSET = WIDTH < HEIGHT ? (HEIGHT - WIDTH) / 2 : 0;
      float Y_OFFSET = HEIGHT < WIDTH ? (WIDTH - HEIGHT) / 2 : 0;
      for(int y = 0, k = 0; y < HEIGHT; y++) {
        for(var x = 0; x < WIDTH; x++, k++) {
          var theta = (float)(Math.PI * (0.5f + y + Y_OFFSET) / MAX);
          var phi = (float)(Math.PI * (0.5f + x + X_OFFSET) / MAX);
          var rx = (float)(Math.cos(phi) * Math.sin(theta));
          var ry = (float)(Math.sin(phi) * Math.sin(theta));
          var rz = (float)(Math.cos(theta));
          var ray = rays[k];
          ray[0] = rx;
          ray[1] = ry;
          ray[2] = rz;
        }
      }
    } else {
      for(int y = 0, k = 0; y < HEIGHT; y++) {
        for(var x = 0; x < WIDTH; x++, k++) {
          var X = x - HALF_WIDTH + 0.5f;
          var Y = -(y - HALF_HEIGHT + 0.5f);
          var ray = rays[k];
          var rx = X;
          var ry = Y;
          var rz = -Z0;
          var inverseMag = 1f / (float)Math.sqrt(rx * rx + ry * ry + rz * rz);
          ray[0] = rx * inverseMag;
          ray[1] = ry * inverseMag;
          ray[2] = rz * inverseMag;
        }
      }
    }

    var panel = (JPanel)getContentPane();
    panel.setIgnoreRepaint(true);
    panel.setPreferredSize(new Dimension(DISPLAY_WIDTH, DISPLAY_HEIGHT));
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setResizable(false);
    pack();    
    setLocationRelativeTo(null);
    setVisible(true);

    var image = new BufferedImage(
        WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
    image.setAccelerationPriority(1f);
    var panelGraphics = panel.getGraphics();

    float ox = 0;
    float oy = 0;
    float oz = 0;

    float phi = 0;
    float theta = 0;

    var frame = 0;
    var startTime = System.currentTimeMillis() - 1;
    var pixels = new int[PIXELS];
      while(true) {

        var cosPhi = (float)Math.cos(phi);
        var sinPhi = (float)Math.sin(phi);
        var cosTheta = (float)Math.cos(theta);
        var sinTheta = (float)Math.sin(theta);

        var ux = cosPhi * sinTheta;
        var uy = sinPhi * sinTheta;
        var uz = cosTheta;

        var vx = cosPhi * cosTheta;
        var vy = sinPhi * cosTheta;
        var vz = -sinTheta;

        var wx = uy * vz - uz * vy;
        var wy = uz * vx - ux * vz;
        var wz = ux * vy - uy * vx;

        var LIGHT_DIRECTION_X = wx;
        var LIGHT_DIRECTION_Y = wy;
        var LIGHT_DIRECTION_Z = wz;

      if (FISH_EYE_LENS) {
        LIGHT_DIRECTION_X = -vx;
        LIGHT_DIRECTION_Y = -vy;
        LIGHT_DIRECTION_Z = -vz;
      }

      ox += VELOCITY_X;
      oy += VELOCITY_Y;
      oz += VELOCITY_Z;
      theta += VELOCITY_THETA;
      phi += VELOCITY_PHI;
      if ((++frame & 0x3F) == 0) {
        setTitle((1000 * frame)
            / (System.currentTimeMillis() - startTime) + " fps");
      }

        var GX = (int)Math.floor(ox * INVERSE_GRID_SIZE);
        var GY = (int)Math.floor(oy * INVERSE_GRID_SIZE);
        var GZ = (int)Math.floor(oz * INVERSE_GRID_SIZE);

      for(var k = 0; k < PIXELS; k++) {

        var gx = GX;

        var ray = rays[k];
        var Rx = ray[0];
        var Ry = ray[1];
        var Rz = ray[2];

        var rx = ux * Rx + vx * Ry + wx * Rz;
        var ry = uy * Rx + vy * Ry + wy * Rz;
        var rz = uz * Rx + vz * Ry + wz * Rz;

        var irx = 1f / rx;

        var dgx = 0;

          float tx = 0;

          if (rx > 0) {
          dgx = 1;
          tx = ((GRID_SIZE * (gx + 1)) - ox) * irx;
        } else {
          dgx = -1;
          tx = ((GRID_SIZE * gx) - ox) * irx;
        }
          float ty = 0;
        var dgy = 0;
        var gy = GY;
        var iry = 1f / ry;
        if (ry > 0) {
          dgy = 1;
          ty = ((GRID_SIZE * (gy + 1)) - oy) * iry;
        } else {
          dgy = -1;
          ty = (GRID_SIZE * gy - oy) * iry;
        }
          float tz = 0;
        var dgz = 0;
        var gz = GZ;
        var irz = 1f / rz;
        if (rz > 0) {
          dgz = 1;
          tz = ((GRID_SIZE * (gz + 1)) - oz) * irz;
        } else {
          dgz = -1;
          tz = ((GRID_SIZE * gz) - oz) * irz;
        }

        var dtz = Math.abs(GRID_SIZE * irz);
        var dty = Math.abs(GRID_SIZE * iry);
        var dtx = Math.abs(GRID_SIZE * irx);
        var blue = 0;
        var green = 0;
        var red = 0;
          for(var i = 0; i < ITERATIONS; i++) {

            var minT = Float.MAX_VALUE;

            var minY = GRID_SIZE * gy;

            var j = gx * GRID_SIZE + HALF_GRID_SIZE;
            var l = gz * GRID_SIZE + HALF_GRID_SIZE;
            var P = ox - j;
            var Q = oz - l;
            var A = rx * rx + rz * rz;
            var B = 2 * (P * rx + Q * rz);
            var C = P * P + Q * Q - CYLINDER_RADIUS_2;
            var D = B * B - 4 * A * C;
          if (D > 0) {
            var t = (-B - (float)Math.sqrt(D)) / (2 * A);
            if (t > 0) {
              var y = oy + ry * t;
              var maxY = minY + GRID_SIZE;
                if (y >= minY && y <= maxY) {
                minT = t;
                i = ITERATIONS;
                  var nx = ox + rx * t - j;
                  var nz = oz + rz * t - l;
                  var diffuse =  (nx * LIGHT_DIRECTION_X
                    + nz * LIGHT_DIRECTION_Z);
                green = 128;
                if (diffuse > 0) {
                  green += (int)(127f * diffuse * INVERSE_CYLINDER_RADIUS);
                }
                red = 0;
                blue = 0;
              }
            }
          }

            var minX = GRID_SIZE * gx;

              j = gy * GRID_SIZE + HALF_GRID_SIZE;
          l = gz * GRID_SIZE + HALF_GRID_SIZE;
          P = oy - j;
          Q = oz - l;
          A = ry * ry + rz * rz;
          B = 2 * (P * ry + Q * rz);
          C = P * P + Q * Q - CYLINDER_RADIUS_2;
          D = B * B - 4 * A * C;
          if (D > 0) {
            var t = (-B - (float)Math.sqrt(D)) / (2 * A);
            if (t > 0 && t < minT) {
              var x = ox + rx * t;
              var maxX = minX + GRID_SIZE;
                if (x >= minX && x <= maxX) {
                minT = t;
                i = ITERATIONS;
                  var ny = oy + ry * t - j;
                  var nz = oz + rz * t - l;
                  var diffuse =  (ny * LIGHT_DIRECTION_Y
                    + nz * LIGHT_DIRECTION_Z);
                red = 128;
                if (diffuse > 0) {
                  red += (int)(127f * diffuse * INVERSE_CYLINDER_RADIUS);
                }
                green = red;
                blue = 0;
              }
            }
          }

            var minZ = GRID_SIZE * gz;

              j = gy * GRID_SIZE + HALF_GRID_SIZE;
          l = gx * GRID_SIZE + HALF_GRID_SIZE;
          P = oy - j;
          Q = ox - l;
          A = ry * ry + rx * rx;
          B = 2 * (P * ry + Q * rx);
          C = P * P + Q * Q - CYLINDER_RADIUS_2;
          D = B * B - 4 * A * C;
          if (D > 0) {
            var t = (-B - (float)Math.sqrt(D)) / (2 * A);
            if (t > 0 && t < minT) {
              var z = oz + rz * t;
              var maxZ = minZ + GRID_SIZE;
                if (z >= minZ && z <= maxZ) {
                minT = t;
                i = ITERATIONS;
                  var ny = oy + ry * t - j;
                  var nx = ox + rx * t - l;
                  var diffuse =  (ny * LIGHT_DIRECTION_Y
                    + nx * LIGHT_DIRECTION_X);
                blue = 128;
                if (diffuse > 0) {
                  blue += (int)(127f * diffuse * INVERSE_CYLINDER_RADIUS);
                }
                red = 0;
                green = 0;
              }
            }
          }

            var sx = gx * GRID_SIZE + HALF_GRID_SIZE;
            var sy = gy * GRID_SIZE + HALF_GRID_SIZE;
            var sz = gz * GRID_SIZE + HALF_GRID_SIZE;

            var dx = ox - sx;
            var dy = oy - sy;
            var dz = oz - sz;

          B = dx * rx + dy * ry + dz * rz;
          C = dx * dx + dy * dy + dz * dz - SPHERE_RADIUS_2;
          D = B * B - C;
          if (D > 0) {
            var t = -B - (float)Math.sqrt(D);
            if (t > 0 && t < minT) {
              i = ITERATIONS;
              var nx = ox + rx * t - sx;
              var ny = oy + ry * t - sy;
              var nz = oz + rz * t - sz;
              var diffuse =  (nx * LIGHT_DIRECTION_X
                  + ny * LIGHT_DIRECTION_Y
                  + nz * LIGHT_DIRECTION_Z);
              red = 128;
              if (diffuse > 0) {
                red += (int)(127f * diffuse * INVERSE_SPHERE_RADIUS);
              }
              green = 0;
              blue = 0;
            }
          }

          if (tx < ty) {
            if (tx < tz) {
              tx += dtx;
              gx += dgx;
            } else {
              tz += dtz;
              gz += dgz;
            }
          } else if (ty < tz) {
            ty += dty;
            gy += dgy;
          } else {
            tz += dtz;
            gz += dgz;
          }
        }
        pixels[k] = (red << 16) | (green << 8) | blue;
      }

        image.setRGB(0, 0, WIDTH, HEIGHT, pixels, 0, WIDTH);
        panelGraphics.drawImage(
            image, 0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT, null);
    }
  }

  public static void main(String[] args) {
    var main = new Main();
    main.launch();
  }

}
