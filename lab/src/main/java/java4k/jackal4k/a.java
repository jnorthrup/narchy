package java4k.jackal4k;
/*
 * Jackal 4K
 * Copyright (C) 2011 meatfighter.com
 *
 * This file is part of Jackal 4K.
 *
 * Jackal 4K is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Jackal 4K is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http:
 *
 */

import java4k.GamePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

public class a extends GamePanel {

  
  private final boolean[] a = new boolean[32768];

  @Override
  public void start() {
    enableEvents(8L);
    new Thread(this).start();
  }

  
  
  
  
  
  

  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  

  
  
  
  
  

  
  
  
  

  
  

  @Override
  public void run() {

      int x = 0;
      int y = 0;
      int value = 0;
      int i = 0;
      int j = 0;
      Random random = new Random();
      int color0 = 0;
      int color1 = 0;


      String s = "\u10f0\u262a\uc8c4\u3214\uc8a1\u152a\u2529\u5116\ua8d1\u262a\uc8c4\u3214\uc8a1\u152a\ua9a5\u1651\u5128\u1088\u0240\u0fb8\u0f7c\u38ff\u788f\u788e\u709e\u715e\u6e2f\uadb7\u4c73\ufe9e\u3f0c\u1f5e\u0ffc\u0f38\u106d\uff0c\ufffe\u3802\u2002\u2001\u2039\u2001\ue781\uff41\u9041\u0c41\u037f\u017f\u0141\u017f\u8201\u1810\ufffc\uc36f\u07c3\u7b7a\u7a7a\uf646\ufefe\u0ef2\u0bfb\u0a0b\u0a0b\u00f4\u18b6\uffee\u6f23\u3fa3\u2727\u233f\ue323\u6763\u7c7e\u4040\u4040\u4040\uc040\u14a0\uffde\ue50d\u1d35\u9d9d\u1d1d\u65f5\u4545\u794d\u11f1\u1ff3\u0eb4\ub468\u0284\u0202\uce02\u6428\u1c4c\uf01a\u06ff\u2418\u4242\u1824\u1053\u271b\u273f\u837f\ubea3\u5a42\u3a5a\u3b1a\u007e\u1053\u4e36\u4e7e\u86f6\u7cc6\ub484\ub8b4\ubebc\u007c\u10e0\u0604\u1903\u2925\ue67c\ubbae\u7979\u8cd8\u44c8\u08b4\u423c\ua581\u8199\u7cc2\u106d\u18e0\u0204\u0102\u0101\u0101\u0201\u0402\ue018\u106d\u18e0\ue204\udf3a\u172f\u1713\u02ff\u0402\ue018\u106d\u38e0\u2edc\u1316\u2515\ua325\uaee7\ufc7e\ue0f8\u5555\u4001\u4001\u4041\u4401\u4001\u4101\u4001\u5415\u5695\u0000\u0000\u000c\u0000\u0000\u4155\u0000\u0800\u0000\u5030\u5400\u5500\u5540\u5550\u0000\u00c0\u0555\u0955\u0015\u0c05\u0000\u0000\u5541\u0000\u0308\u0000\u0550\u0000\u0000\u0000\u5555\u4821\u4001\u0000\u0280\u0000\u0000\u5415\u0800\u0000\u0080\u1400\u0008\u3000\u0014\u0000\u0000\u0040\u0000\u0000\u0200\u0550\u0000\u0000\u4155\u0c15\u0015\u0015\u5355\u0000\u2000\u0000\u5550\u400c\u4800\u4005\u4141\u4001\u4001\u5005\u5555\u0001\u0001\u5005\u5005\u5005\u8005\u00c1\u2000\u0008\u1050\u1040\u0140\u0580\u0000\u0000\u5451\u5451\u5451\u545d\u5751\u5451\u5451\u5451\u0000\u0000\u1554\u0200\u4000\u4000\u0005\u0005\u0c31\u4000\u400c\u0000\u0155\u4000\u0000\u0000\u5555\u4551\u4141\u4001\u4001\u4821\u4001\u4281\u5031\u5401\u5401\u5401\u5305\u4015\u0055\ua555\u0000\u3000\u0000\u0000\u5559\u0000\u0140\u0550\u1554\u0550\u2148\u0000\u3000\u0000\u0040\u1750\u1454\u044c\u0000\u1050\u1414\u0000\u4001\u5005\u5555\u9966\u0000\u0000\u0000\u0000\u0000\u0000\u0000\ua955\u0155\u0c00\u0000\u0140\u5825\u5005\u3000\u0000\u1445\u1445\u1445\u0000\u6559\u0000\u0000\u0010\u1400\u2400\u0510\u001c\u00d0\u001c\u0015\u5155\u0030\u5155\u5155\u4001\u4001\u5555\u0000\u0000\uaaaa\u0000\u0000\u0000\u0000\u0000\u300c\u0140\u0550\u1554\u0000\u5415\u5825\u5415\u5415\u4001\u4001\u4002\u4001\u4002\u4001\u4001\u0000\u4551\u0300\u0000\u155c\u1990\u0000\u0000\u0555\u3002\u0002\u0002\u5415\u4001\u4001\u4001";
      int[] pixels = new int[1024];
      BufferedImage[] sprites = new BufferedImage[21];
    for(i = 0; i < 21; i++) {
        int width = (i == 2) ? 32 : (i < 7 || i > 12 ? 16 : 8);
      color1 = i == 0  ? 0xFF994E00             
             : i == 16 ? 0xFFADADAD
             : i == 17 ? 0xFFABABAB
             : i == 18 ? 0xFF008B00
             : i == 19 ? 0xFF626262
             : 0xFF000000;
      if (i == 12) {
        j -= 5;
      } else if (i > 15) {
        j = 0;
        width = 16;
      }
      value = (int) s.charAt(j++);
        int height = value >> 8;
        int color = value & 0xFF;
      color0 = i == 12 ? 0xFF666666
             : i == 16 ? 0xFF6B6D00
             : i == 17 ? 0xFF626262
             : i == 18 ? 0xFF004545
             : i == 19 ? 0xFF562F00
             : i == 20 ? 0xFF626262
             : 0xFF000000 | ((color & 0xE0) << 16)
                  | ((color & 0x1C) << 11) | ((color & 0x03) << 6);

      sprites[i] = new BufferedImage(width, i == 2 ? 32 : height, 2);

      for(y = 0; y < height; y++) {
        value = (int) s.charAt(j);
        if (i < 3 || i > 15) {
          j++;
        } else if ((y&1) == 1) {
          j++;
          value >>= 8;
        } else {
          value &= 0xFF;
        }
          boolean inside = i == 0 || i > 15;
        for(x = 0; x < ((i < 3 || i > 15) ? 16 : 8); x++, value >>= 1) {
          if (i > 0 && i < 16) {
            if (((value&1) == 1)) {
              inside = true;
            }
            if (value == 0 && ((i > 6 && i < 13) || i == 1)) {
              inside = false;
            }
          }
            int p = pixels[x + y * width]
              = inside ? (((value&1) == 0) ? color0 : color1) : 0;
          if (width > 8 && i > 1 && i < 16) {
            pixels[i == 6 ? (height - y) * width - x - 1
                : (y + 1) * width - x - 1] = p;
          }
          if (i == 2) {
            pixels[(32 - y) * 32 - x - 1] = pixels[x + (31 - y) * 32] = p;
          }
        }
      }
      sprites[i].setRGB(0, 0, width, i == 2 ? 32 : height, pixels, 0, width);
    }


      int[][][] stages = new int[6][80][16];
      for(i = 0, j = 159; i < 6; i++) {
          int[][] map = stages[i];
      for(y = 0; y < 40; y++) {
        value = (int) s.charAt(j++);
        for(x = 0; x < 8; x++, value >>= 2) {
            int m = value & 3;
          if (m == 1) {
            
            map[y*2][x*2 + 1] = map[y*2 + 1][x*2 + 1]
                = map[y*2 + 1][x*2] = map[y*2][x*2] = 1;
          } else if (m == 2 && y >= 16 && y < 24) {
            map[y*2][x*2] = 4; 
          } else {
            map[y*2][x*2] = m;
          } 
        }
      }
    }


      BufferedImage image = new BufferedImage(256, 256, 1);
      Graphics2D g = (Graphics2D)image.getGraphics();
    Graphics2D g2 = null;
      AffineTransform defaultTransform = g.getTransform();

      long nextFrameStartTime = System.nanoTime();
      int bossSpawn = 0;
      boolean fireworks = false;
      float[] player = null;
      int playerDead = 1;
      int[][] stage = null;
      int stageIndex = 0;
      int spawnY = 0;
      int cameraY = 0;
      ArrayList<float[]> queue = new ArrayList<float[]>();
      while(true) {

      do {
        nextFrameStartTime += 16666667L;

        

        if (playerDead > 0 && --playerDead == 0) {
          
          cameraY = 1008;
          spawnY = 79;
          stage = stages[stageIndex];
          sprites[0] = sprites[stageIndex > 0 ? 15 + stageIndex : 0];
          fireworks = false;
          player = new float[]
              
              
              {(float) 0, (float) 0, 3.0F, 128.0F, 1216.0F, 1.0F, 8.0F, 12.0F, 3.14f, 3.14f, 1.0F, (float) 0};
          queue.clear();
          queue.add(player);
        } else if (fireworks) {
          
          queue.add(new float[] {(float) 0, 6.0F, 6.0F,
                  (float) random.nextInt(256), (float) random.nextInt(256),
                  1.0F, 8.0F, 7.0F, (float) 0, 16.0F, 0.1f});
        }


          int nextSpawnY = (cameraY >> 4) - 2;
        while(nextSpawnY < spawnY && spawnY > 0) {
          for(x = 0; x < 16; x++) {
              int enemy = stage[spawnY][x];
            if (enemy == 0) {
              
              if (spawnY > 16 && x > 0 && x < 15
                  && random.nextInt(64) == 0 && spawnY < 64) {
                if (random.nextInt(8) == 0) {
                  
                  queue.add(new float[] {(float) 0, 11.0F, 11.0F,
                          (float) (x * 16 + 9), (float) (spawnY * 16 + 9),
                          (float) 0, 4.0F, 4.0F, (float) 0, (float) 0, 1.0F, (float) 0, 1.0F, (float) 0, (float) 0, (float) 0});
                } else {
                  
                  queue.add(new float[] {(float) 0, 3.0F, 8.0F,
                          (float) (x * 16 + 9), (float) (spawnY * 16 + 9),
                          (float) 0, 4.0F, 7.0F, (float) 0, (float) 0, 1.0F, (float) 0, 1.0F, (float) 0, (float) 0, (float) 0});
                }
              }
            } else if (enemy == 2) {
              
              queue.add(new float[] {(float) 0, 4.0F, 4.0F,
                      (float) ((x + 1) * 16), (float) ((spawnY + 1) * 16), 1.0F, 8.0F, 8.0F, (float) 0, (float) 0, 1.0F, (float) 0, 1.0F,
                      (float) 0, (float) 0, (float) ((x + 1) * 16), (float) ((spawnY + 1) * 16)});
            } else if (enemy == 3) {
              
              queue.add(new float[] {(float) 0, 5.0F, 5.0F,
                      (float) (x * 16 + 9), (float) (spawnY * 16 + 9),
                      1.0F, 8.0F, 10.0F, (float) 0, (float) 0, 1.0F, (float) 0, 1.0F, (float) 0, (float) 0, (float) 0});
            } else if (enemy == 4) { 
              
              queue.add(new float[] {(float) 0, 10.0F, 13.0F,
                      (float) ((x + 1) * 16), (float) ((spawnY + 1) * 16), 1.0F, 8.0F, 8.0F, (float) 0, (float) 0, 1.0F, 180.0F, 1.0F,
                      (float) 0, (float) 0, (float) 0, (float) 0});
            } else if (spawnY > 16 && spawnY < 64
                && (x&1) == 0 && (spawnY&1) == 0
                && random.nextInt(20 - stageIndex * 2) == 0) {
              
              queue.add(new float[] {(float) 0, 9.0F, 1.0F,
                      (float) (x * 16 + 16), (float) (spawnY * 16 + 16),
                      1.0F, 8.0F, 8.0F, (float) 0, (float) 0, 1.0F, (float) 0, 1.0F, (float) 0, (float) 0, (float) 0});
            }
          }
          spawnY--;
        }

        
        for(i = queue.size() - 1; i >= 0; i--) {
            float[] object = queue.get(i);
          if (object[0] != (float) 0) {
            queue.remove(i);
            continue;
          }
          switch((int)object[1]) {
            case 0: 
              if (a[0x26] && stage[((int)object[4] - 12 >> 4)]
                    [((int)object[3] >> 4)] != 1
                    && (int)object[4] - 12 > 0) { 
                object[9] = 3.14f;
                object[4] -= 0.75f;
              } else if (a[0x28] && stage[((int)object[4] + 11 >> 4)]
                  [((int)object[3] >> 4)] != 1
                  && (int)object[4] < cameraY + 245) { 
                object[9] = (float) 0;
                object[4] += 0.75f;
              } else if (a[0x25] && stage[((int)object[4] >> 4)]
                  [((int)object[3] - 12 >> 4)] != 1
                  && (int)object[3] - 12 > 0) { 
                object[9] = 1.57f;
                object[3] -= 0.75f;
              } else if (a[0x27] && stage[((int)object[4] >> 4)]
                  [((int)object[3] + 11 >> 4)] != 1
                  && (int)object[3] + 11 < 255) { 
                object[9] = 4.71f;
                object[3] += 0.75f;
              }
              if (a[0x20] && object[11] < 1.0F) {
                queue.add(new float[] {(float) 0, 1.0F, 7.0F,
                    object[3], object[4], (float) 0, 4.0F, 3.0F,
                        -2.0F * (float)Math.sin((double) object[9]),
                        2.0F * (float)Math.cos((double) object[9]), 60.0F, (float) 0, 1.0F});
                object[11] = 1.0F;
              }
              break;
            case 1: 
            case 2: 
              object[3] += object[8];
              object[4] += object[9];
              if (object[10] > 2.0F && object[3] > 2.0F && object[3] < 253.0F
                  && object[4] < 1277.0F && object[4] > (float) 0) {
                object[10]--;
                if (stage[((int)object[4] >> 4)]
                    [((int)object[3] >> 4)] == 1) {
                  object[0] = 1.0F;
                  if (object[1] == 1.0F) {
                    player[11] = (float) 0;
                  }
                }

                if (object[1] == 1.0F) {
                  
                  for(j = queue.size() - 1; j >= 0; j--) {
                      float[] obj = queue.get(j);
                    if (obj[0] == (float) 0) {
                      if (object[1] == 1.0F && (
                          (obj[1] == 11.0F && obj[2] > (float) 0)
                          || obj[1] == 10.0F || obj[1] == 9.0F
                          || obj[1] == 7.0F
                          || (obj[1] >= 3.0F && obj[1] <= 5.0F))) {
                          float dx = obj[3] - object[3];
                          float dy = obj[4] - object[4];
                        float radius = (float) (obj[1] == 3.0F ? (int) obj[10] * 4 + 4
                                : (int) obj[10] * 8 + 8);
                        if (dx * dx + dy * dy < radius * radius) {
                          object[0] = 1.0F;
                          player[11] = (float) 0;
                          if (obj[1] != 10.0F
                              || (obj[11] > 15.0F && obj[11] < 90.0F)) {
                            if (--obj[12] < 1.0F) {

                              if (obj[1] == 3.0F) {
                                
                                obj[1] = 8.0F;
                                obj[2] = 10.0F;
                              } else {
                                obj[0] = 1.0F;

                                if (obj[1] == 7.0F) {
                                  if (stageIndex < 5) {
                                    
                                    stageIndex++;
                                    playerDead = 300;
                                  } else {
                                    playerDead = 32768;
                                  }
                                  for(int k = queue.size() - 1; k >= 0; k--) {
                                    queue.get(k)[0] = 1.0F;
                                  }
                                  player[0] = (float) 0;
                                  fireworks = true;
                                }

                                
                                queue.add(new float[] {(float) 0, 6.0F, 6.0F, obj[3], obj[4],
                                        1.0F, 8.0F, 7.0F, (float) 0, 16.0F, 0.1f});
                              }
                            } else {
                              
                              queue.add(new float[] {(float) 0, 6.0F, 6.0F,
                                  object[3], object[4], 1.0F, 8.0F, 7.0F, (float) 0, 16.0F, 0.1f});
                            }
                          }
                        }
                      }
                    }
                  }
                } else if (player[0] == (float) 0) {
                    float vx = player[3] - object[3];
                    float vy = player[4] - object[4];
                  if (vx * vx + vy * vy < 128.0F) {
                    object[0] = 1.0F;
                    player[0] = 1.0F;
                    playerDead = 120;
                    
                    queue.add(new float[] {(float) 0, 6.0F, 6.0F, player[3], player[4],
                            1.0F, 8.0F, 7.0F, (float) 0, 16.0F, 0.1f});
                  }
                }
              } else {
                object[0] = 1.0F;
                if (object[1] == 1.0F) {
                  player[11] = (float) 0;
                }
              }
              break;
            case 4: 
              if (object[11] < 165.0F) {
                object[9] = (float)Math.atan2((double) (player[4] - object[4]),
                        (double) (player[3] - object[3])) - 1.57f;
                object[3] = object[15];
                object[4] = object[16];
              } else {
                  float deflection = 4.0F * (float)Math.sin(
                          (double) (0.209f * (object[11] - 165.0F)));
                object[3] = object[15] - object[13] * deflection;
                object[4] = object[16] - object[14] * deflection;
              }
              if (object[11] > (float) 0) {
                object[11]--;
              } else {
                object[11] = 180.0F;
                  float vx = player[3] - object[3];
                  float vy = player[4] - object[4];
                  float mag = (float)Math.sqrt((double) (vx * vx + vy * vy));

                
                queue.add(new float[] {(float) 0, 2.0F, 7.0F,
                    object[3], object[4], (float) 0, 4.0F, 3.0F,
                    object[13] = vx / mag,
                    object[14] = vy / mag, 120.0F, (float) 0, 1.0F});
                object[15] = object[3];
                object[16] = object[4];
              }
              break;
            case 10: 
              if (object[11] >= 15.0F && object[11] < 75.0F) {
                if (object[11] == 74.0F) {
                  object[9] = (float)Math.atan2(
                          (double) (player[4] - object[4]),
                          (double) (player[3] - object[3])) - 1.57f;
                } else if (object[11] == 60.0F) {

                    float angle = object[9] + 0.785f;
                  for(int z = 0; z < 5; z++, angle += 0.314f) {
                    queue.add(new float[] {(float) 0, 2.0F, 7.0F,
                        object[3], object[4], (float) 0, 4.0F, 3.0F,
                        (float)Math.cos((double) angle),
                        (float)Math.sin((double) angle), 120.0F, (float) 0, 1.0F});
                  }
                }
                object[2] = 15.0F;
              } else if (object[11] < 15.0F
                  || (object[11] >= 75.0F && object[11] < 90.0F)) {
                object[2] = 14.0F;
                object[8] = (float) 0;
                object[9] = (float) 0;
              } else {
                object[8] = (float) 0;
                object[9] = (float) 0;
                object[2] = 13.0F;
              }
              if (--object[11] == (float) 0) {
                object[11] = 240.0F;
              }
              break;
            case 3: 
            case 5: 
            case 7: 
              if (object[13] < 1.0F) {


                  float nextX = object[3] + object[14];
                  float nextY = object[4] + object[15];

                  boolean safeMove = true;
                for(int z = -1; z <= 1; z++) {
                  for(int b = -1; b <= 1; b++) {
                      int X = ((int)nextX + z * 8) >> 4;
                      int Y = ((int)nextY + b * 8) >> 4;
                    if (X < 0 || X > 15 || Y < 0 || Y > 78
                        || stage[Y][X] == 1) {
                      safeMove = false;
                      break;
                    }
                  }
                }

                if (--object[11] > (float) 0 && (object[1] == 7.0F || safeMove)) {
                  
                  object[3] = nextX;
                  object[4] = nextY;
                  if ((object[2] == 8.0F || object[2] == 9.0F)
                     && ((int)object[11] & 7) == 0) {
                    
                    object[2] = (float) (object[2] > 8.0F ? 8 : 9);
                  }
                } else {
                  
                  object[13] = 1.0F;
                  object[11] = (float) (object[1] == 7.0F ? 2 : 120);
                    float vx = player[3] - object[3];
                    float vy = player[4] - object[4];
                    float mag = (float)Math.sqrt((double) (vx * vx + vy * vy));
                  vx /= mag;
                  vy /= mag;
                  
                  queue.add(new float[] {(float) 0, 2.0F, 7.0F,
                      object[3], object[4], (float) 0,
                          4.0F, 3.0F, vx, vy, 120.0F, (float) 0, 1.0F});
                }
              } else if (--object[11] < 1.0F) {


                  float vx = player[3] - object[3];
                  float vy = player[4] - object[4];
                  float mag = (float)Math.sqrt((double) (vx * vx + vy * vy));
                vx /= mag;
                vy /= mag;

                object[13] = (float) 0;
                object[11] = (float) (object[1] == 7.0F ? 60 : 120);
                if (random.nextBoolean()) {
                  object[14] = vx * 0.5f;
                  object[15] = vy * 0.5f;
                } else {
                  mag = 6.28f * random.nextFloat();
                  object[14] = 0.5f * (float)Math.cos((double) mag);
                  object[15] = 0.5f * (float)Math.sin((double) mag);
                }
                if (object[1] == 3.0F) {
                  object[14] *= 0.5f;
                  object[15] *= 0.5f;
                  object[11] *= 2.0F;
                }
                object[9] = (float)Math.atan2((double) object[15], (double) object[14]) - 1.57f;
              }
              break;
            case 6: 
              object[10] += 0.5f;
              object[3]-= 2.0F;
              object[4]-= 2.0F;
              if (object[10] > 3.0F) {
                object[0] = 1.0F;
              }
              break;
            case 9: {
                float vx = player[3] - object[3];
                float vy = player[4] - object[4];
                if (object[11] == (float) 0) {
                    float mag = vx * vx + vy * vy;
                    if (mag < 8192.0F) {
                  object[9] = 128.0F;
                  object[11] = 128.0F;
                  mag = (float)Math.sqrt((double) mag);
                  object[14] = vx * 0.5f / mag;
                  object[15] = vy * 0.5f / mag;                  
                }
              } else {
                if (object[11] > 1.0F) {
                  object[11]--;
                  object[3] += object[14];
                  object[4] += object[15];
                } else {
                  object[9] = object[8];
                }
              }
              
              break;
            }
            case 11:
                float vx = player[3] - object[3];
                float vy = player[4] - object[4];
                if (object[11]-- == (float) 0) {
                  object[11] = 2.0F;
                    float mag = vx * vx + vy * vy;
                    object[2] = (float) (mag > 8192.0F ? -1 : object[2] == 11.0F ? 12 : 11);
                }
                break;
          }

          
          if (object[1] == 7.0F) {
            if (stageIndex > 1 && --bossSpawn < 0) {
              bossSpawn = 360 + random.nextInt(512);

              if ((stageIndex == 5 && random.nextBoolean())
                  || stageIndex == 2 || stageIndex == 4) {
                
                queue.add(new float[] {(float) 0, 5.0F, 5.0F,
                        (float) (64 + random.nextInt(128)), 255.0F,
                        1.0F, 8.0F, 10.0F, (float) 0, (float) 0, 1.0F, 60.0F, 1.0F, (float) 0, (float) 0, -0.5f });
              } else {
                
                queue.add(new float[] {(float) 0, 3.0F, 8.0F,
                        (float) (64 + random.nextInt(128)), 255.0F, (float) 0, 4.0F, 7.0F, (float) 0, (float) 0,
                        1.0F, 180.0F, 1.0F, (float) 0, (float) 0, -0.25f });
              }
            }
          }

          
          if ((object[1] >= 3.0F && object[1] <= 5.0F) || object[1] == 7.0F
              || object[1] == 9.0F || object[1] == 10.0F || object[1] == 11.0F) {
              float vx = player[3] - object[3];
              float vy = player[4] - object[4];
            vx = vx * vx + vy * vy;
            vy = 12.0F + 10.0F * object[10];
            if (object[1] == 3.0F && vx < 64.0F) {
              
              object[1] = 8.0F;
              object[2] = 10.0F;
            } else if (object[1] != 3.0F && vx < (object[1] == 11.0F ? 128.0F :
                vy * vy)
                && player[0] < 1.0F) {
              
              player[0] = 1.0F;
              playerDead = 120;
              queue.add(new float[] {(float) 0, 6.0F, 6.0F, player[3], player[4],
                      1.0F, 8.0F, 7.0F, (float) 0, 16.0F, 0.1f});

              
              if (object[1] != 7.0F) {
                object[0] = 1.0F;
                queue.add(new float[] {(float) 0, 6.0F, 6.0F, object[3], object[4],
                        1.0F, 8.0F, 7.0F, (float) 0, 16.0F, 0.1f});
              }
            }
          }

          
          if (object[5] != (float) 0) {
            if (Math.abs(object[8] - object[9]) > 0.1f) {
              if (object[8] < object[9]) {
                object[8] += 0.1f;
              } else {
                object[8] -= 0.1f;
              }
            } else {
              object[8] = object[9];
            }
          }
        }


          int nextCameraY = (int)player[4] - 128;
        if (nextCameraY < 0) {
          nextCameraY = 0;
        }
        if (nextCameraY < cameraY) {
          cameraY = nextCameraY;
          if (cameraY == 0) {
            

            bossSpawn = 0;
              boolean b = stageIndex != 1;
              float scale = 2.0F + (b ? 0.5f * (float) stageIndex : (float) stageIndex);
            queue.add(new float[] {(float) 0, 7.0F,
                    (float) (stageIndex == 5 ? 15 :
                            stageIndex == 4 ? 6 : stageIndex == 3 ? 4
                                    : (b ? (stageIndex == 2 ? 1 : 5) : 8)),
                    128.0F, -96.0F,
                    (float) (b ? 1 : 0),
                    (float) (b ? 8 : 4) * scale,
                    (float) (b ? (stageIndex == 5 ? 8 : stageIndex == 4 ? 7 : 10) : 7)
                    * scale, (float) 0, (float) 0,
                scale,
                    256.0F, 5.0F, (float) 0, (float) 0, 0.5f });
          }
        }
        
        
      } while(nextFrameStartTime < System.nanoTime());

      

      
      for(y = 0; y < 17; y++) {
        for(x = 0; x < 16; x++) {
          g.drawImage(sprites[0], x * 16, y * 16 - (cameraY & 15), null);          
        }
      }
      
      
      for(y = -1; y < 17; y++) {
        for(x = 0; x < 16; x++) {
          j = y + (cameraY >> 4);
          if (j >= 0) {
            j = stage[j][x];
            if (j == 1 || j == 2) {
              g.drawImage(sprites[j], x * 16, y * 16 - (cameraY & 15), null);
            }
          }
        }
      }

      
      for(i = queue.size() - 1; i >= 0; i--) {
          float[] object = queue.get(i);
        if (object[2] >= (float) 0) {
          g.translate((int)(object[3] - object[6]),
              (int)(object[4] - object[7]) - cameraY);
          if (object[5] != (float) 0 || object[1] == 7.0F) {
            g.rotate((double) object[8], (double) object[6], (double) object[7]);
            g.scale((double) object[10], (double) object[10]);
          }
          g.drawImage(sprites[(int)object[2]], 0, 0, null);
          g.setTransform(defaultTransform);
        }

        
        
        
      }

      

      
      if (g2 == null) {
        g2 = (Graphics2D)getGraphics();
        requestFocus();
      } else {
        g2.drawImage(image, 0, 0, 512, 512, null);
      }

      
      while(nextFrameStartTime - System.nanoTime() > 0L) {
        Thread.yield();
      }      
    }
  }

  @Override
  public void processKeyEvent(KeyEvent keyEvent) {
      int k = keyEvent.getKeyCode();
    if (k > 0) {
        switch (k) {
            case 0x57:
                k = 0x26;
                break;
            case 0x53:
                k = 0x28;
                break;
            case 0x41:
                k = 0x25;
                break;
            case 0x44:
                k = 0x27;
                break;
        }
      a[k >= 0x25 && k <= 0x28 ? k : 0x20] = keyEvent.getID() != 402;
    }
  }

  
  public static void main(String[] args) throws Throwable {
      JFrame frame = new javax.swing.JFrame("Jackal 4K");
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
      java4k.jackal4k.a applet = new a();
    applet.setPreferredSize(new java.awt.Dimension(512, 512));
    frame.add(applet, java.awt.BorderLayout.CENTER);
    frame.setResizable(false);
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
    Thread.sleep(250L);
    applet.start();
  }
}
