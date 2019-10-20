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

      float[][] rays = new float[PIXELS][3];

    if (FISH_EYE_LENS) {
      float MAX = (float) Math.max(WIDTH, HEIGHT);
      float X_OFFSET = (float) (WIDTH < HEIGHT ? (HEIGHT - WIDTH) / 2 : 0);
      float Y_OFFSET = (float) (HEIGHT < WIDTH ? (WIDTH - HEIGHT) / 2 : 0);
      for(int y = 0, k = 0; y < HEIGHT; y++) {
        for(int x = 0; x < WIDTH; x++, k++) {
            float theta = (float)(Math.PI * (double) (0.5f + (float) y + Y_OFFSET) / (double) MAX);
            float phi = (float)(Math.PI * (double) (0.5f + (float) x + X_OFFSET) / (double) MAX);
            float rx = (float)(Math.cos((double) phi) * Math.sin((double) theta));
            float ry = (float)(Math.sin((double) phi) * Math.sin((double) theta));
            float rz = (float)(Math.cos((double) theta));
            float[] ray = rays[k];
          ray[0] = rx;
          ray[1] = ry;
          ray[2] = rz;
        }
      }
    } else {
      for(int y = 0, k = 0; y < HEIGHT; y++) {
        for(int x = 0; x < WIDTH; x++, k++) {
            float X = (float) (x - HALF_WIDTH) + 0.5f;
            float Y = -((float) (y - HALF_HEIGHT) + 0.5f);
            float[] ray = rays[k];
            float rx = X;
            float ry = Y;
            float rz = -Z0;
            float inverseMag = 1f / (float)Math.sqrt((double) (rx * rx + ry * ry + rz * rz));
          ray[0] = rx * inverseMag;
          ray[1] = ry * inverseMag;
          ray[2] = rz * inverseMag;
        }
      }
    }

      JPanel panel = (JPanel)getContentPane();
    panel.setIgnoreRepaint(true);
    panel.setPreferredSize(new Dimension(DISPLAY_WIDTH, DISPLAY_HEIGHT));
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setResizable(false);
    pack();    
    setLocationRelativeTo(null);
    setVisible(true);

      BufferedImage image = new BufferedImage(
        WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
    image.setAccelerationPriority(1f);
      Graphics panelGraphics = panel.getGraphics();

    float ox = (float) 0;
    float oy = (float) 0;
    float oz = (float) 0;

    float phi = (float) 0;
    float theta = (float) 0;

      int frame = 0;
      long startTime = System.currentTimeMillis() - 1L;
      int[] pixels = new int[PIXELS];
      while(true) {

          float cosPhi = (float)Math.cos((double) phi);
          float sinPhi = (float)Math.sin((double) phi);
          float cosTheta = (float)Math.cos((double) theta);
          float sinTheta = (float)Math.sin((double) theta);

          float ux = cosPhi * sinTheta;
          float uy = sinPhi * sinTheta;
          float uz = cosTheta;

          float vx = cosPhi * cosTheta;
          float vy = sinPhi * cosTheta;
          float vz = -sinTheta;

          float wx = uy * vz - uz * vy;
          float wy = uz * vx - ux * vz;
          float wz = ux * vy - uy * vx;

          float LIGHT_DIRECTION_X = wx;
          float LIGHT_DIRECTION_Y = wy;
          float LIGHT_DIRECTION_Z = wz;

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
        setTitle((long) (1000 * frame)
            / (System.currentTimeMillis() - startTime) + " fps");
      }

          int GX = (int)Math.floor((double) (ox * INVERSE_GRID_SIZE));
          int GY = (int)Math.floor((double) (oy * INVERSE_GRID_SIZE));
          int GZ = (int)Math.floor((double) (oz * INVERSE_GRID_SIZE));

      for(int k = 0; k < PIXELS; k++) {

          int gx = GX;

          float[] ray = rays[k];
          float Rx = ray[0];
          float Ry = ray[1];
          float Rz = ray[2];

          float rx = ux * Rx + vx * Ry + wx * Rz;

          float irx = 1f / rx;

          int dgx = 0;

          float tx = (float) 0;

          if (rx > (float) 0) {
          dgx = 1;
          tx = ((GRID_SIZE * (float) (gx + 1)) - ox) * irx;
        } else {
          dgx = -1;
          tx = ((GRID_SIZE * (float) gx) - ox) * irx;
        }
          float ty = (float) 0;
          int dgy = 0;
          int gy = GY;
          float ry = uy * Rx + vy * Ry + wy * Rz;
          float iry = 1f / ry;
        if (ry > (float) 0) {
          dgy = 1;
          ty = ((GRID_SIZE * (float) (gy + 1)) - oy) * iry;
        } else {
          dgy = -1;
          ty = (GRID_SIZE * (float) gy - oy) * iry;
        }
          float tz = (float) 0;
          int dgz = 0;
          int gz = GZ;
          float rz = uz * Rx + vz * Ry + wz * Rz;
          float irz = 1f / rz;
        if (rz > (float) 0) {
          dgz = 1;
          tz = ((GRID_SIZE * (float) (gz + 1)) - oz) * irz;
        } else {
          dgz = -1;
          tz = ((GRID_SIZE * (float) gz) - oz) * irz;
        }

          float dtz = Math.abs(GRID_SIZE * irz);
          float dty = Math.abs(GRID_SIZE * iry);
          float dtx = Math.abs(GRID_SIZE * irx);
          int blue = 0;
          int green = 0;
          int red = 0;
          for(int i = 0; i < ITERATIONS; i++) {

              float minT = Float.MAX_VALUE;

              float minY = GRID_SIZE * (float) gy;

              float j = (float) gx * GRID_SIZE + HALF_GRID_SIZE;
              float l = (float) gz * GRID_SIZE + HALF_GRID_SIZE;
              float P = ox - j;
              float Q = oz - l;
              float A = rx * rx + rz * rz;
              float B = 2.0F * (P * rx + Q * rz);
              float C = P * P + Q * Q - CYLINDER_RADIUS_2;
              float D = B * B - 4.0F * A * C;
          if (D > (float) 0) {
              float t = (-B - (float)Math.sqrt((double) D)) / (2.0F * A);
            if (t > (float) 0) {
                float y = oy + ry * t;
                float maxY = minY + GRID_SIZE;
                if (y >= minY && y <= maxY) {
                minT = t;
                i = ITERATIONS;
                    float nx = ox + rx * t - j;
                    float nz = oz + rz * t - l;
                    float diffuse =  (nx * LIGHT_DIRECTION_X
                    + nz * LIGHT_DIRECTION_Z);
                green = 128;
                if (diffuse > (float) 0) {
                  green += (int)(127f * diffuse * INVERSE_CYLINDER_RADIUS);
                }
                red = 0;
                blue = 0;
              }
            }
          }

              float minX = GRID_SIZE * (float) gx;

              j = (float) gy * GRID_SIZE + HALF_GRID_SIZE;
          l = (float) gz * GRID_SIZE + HALF_GRID_SIZE;
          P = oy - j;
          Q = oz - l;
          A = ry * ry + rz * rz;
          B = 2.0F * (P * ry + Q * rz);
          C = P * P + Q * Q - CYLINDER_RADIUS_2;
          D = B * B - 4.0F * A * C;
          if (D > (float) 0) {
              float t = (-B - (float)Math.sqrt((double) D)) / (2.0F * A);
            if (t > (float) 0 && t < minT) {
                float x = ox + rx * t;
                float maxX = minX + GRID_SIZE;
                if (x >= minX && x <= maxX) {
                minT = t;
                i = ITERATIONS;
                    float ny = oy + ry * t - j;
                    float nz = oz + rz * t - l;
                    float diffuse =  (ny * LIGHT_DIRECTION_Y
                    + nz * LIGHT_DIRECTION_Z);
                red = 128;
                if (diffuse > (float) 0) {
                  red += (int)(127f * diffuse * INVERSE_CYLINDER_RADIUS);
                }
                green = red;
                blue = 0;
              }
            }
          }

              float minZ = GRID_SIZE * (float) gz;

              j = (float) gy * GRID_SIZE + HALF_GRID_SIZE;
          l = (float) gx * GRID_SIZE + HALF_GRID_SIZE;
          P = oy - j;
          Q = ox - l;
          A = ry * ry + rx * rx;
          B = 2.0F * (P * ry + Q * rx);
          C = P * P + Q * Q - CYLINDER_RADIUS_2;
          D = B * B - 4.0F * A * C;
          if (D > (float) 0) {
              float t = (-B - (float)Math.sqrt((double) D)) / (2.0F * A);
            if (t > (float) 0 && t < minT) {
                float z = oz + rz * t;
                float maxZ = minZ + GRID_SIZE;
                if (z >= minZ && z <= maxZ) {
                minT = t;
                i = ITERATIONS;
                    float ny = oy + ry * t - j;
                    float nx = ox + rx * t - l;
                    float diffuse =  (ny * LIGHT_DIRECTION_Y
                    + nx * LIGHT_DIRECTION_X);
                blue = 128;
                if (diffuse > (float) 0) {
                  blue += (int)(127f * diffuse * INVERSE_CYLINDER_RADIUS);
                }
                red = 0;
                green = 0;
              }
            }
          }

              float sx = (float) gx * GRID_SIZE + HALF_GRID_SIZE;
              float sy = (float) gy * GRID_SIZE + HALF_GRID_SIZE;
              float sz = (float) gz * GRID_SIZE + HALF_GRID_SIZE;

              float dx = ox - sx;
              float dy = oy - sy;
              float dz = oz - sz;

          B = dx * rx + dy * ry + dz * rz;
          C = dx * dx + dy * dy + dz * dz - SPHERE_RADIUS_2;
          D = B * B - C;
          if (D > (float) 0) {
              float t = -B - (float)Math.sqrt((double) D);
            if (t > (float) 0 && t < minT) {
              i = ITERATIONS;
                float nx = ox + rx * t - sx;
                float ny = oy + ry * t - sy;
                float nz = oz + rz * t - sz;
                float diffuse =  (nx * LIGHT_DIRECTION_X
                  + ny * LIGHT_DIRECTION_Y
                  + nz * LIGHT_DIRECTION_Z);
              red = 128;
              if (diffuse > (float) 0) {
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
      Main main = new Main();
    main.launch();
  }

}
