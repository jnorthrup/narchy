package spacegraph.video;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;

import java.io.InputStream;
import java.nio.ByteBuffer;

public class BmpFont {


    private TextureData texture;


    private int base;  // Base Display List For The Font
    private final int[] textures = new int[2];  // Storage For Our Font Texture


    private ByteBuffer stringBuffer = ByteBuffer.allocate(256);

    private static final ThreadLocal<BmpFont> fonts = ThreadLocal.withInitial(BmpFont::new);

    private GL2 gl;

    public static BmpFont the(GL2 g) {
        BmpFont f = fonts.get();
        if (!f.init)
            f.init(g);
        return f;
    }



    void loadGLTextures() {

        String tileNames[] =
                {"font2.png"/*, "bumps.png"*/};


        gl.glGenTextures(2, textures, 0);

        for (int i = 0; i < 1; i++) {

            InputStream r = Draw.class.getClassLoader().getResourceAsStream(tileNames[i]);
            try {
                //File r = new File("/home/me/n/lab/src/main/resources/" + tileNames[i]);
                boolean mipmap = false;
                texture = TextureIO.newTextureData(gl.getGLProfile(), r,
                        mipmap, "png");
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(1);
            }
            //Create Nearest Filtered Texture
            gl.glBindTexture(GL2.GL_TEXTURE_2D, textures[i]);

            gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
            gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);

            gl.glTexImage2D(GL2.GL_TEXTURE_2D,
                    0,

                    3,
                    texture.getWidth(),
                    texture.getHeight(),
                    0,
                    GL2.GL_RGBA,
                    GL2.GL_UNSIGNED_BYTE,
                    texture.getBuffer());


        }
    }

    private void buildFont()  // Build Our Font Display List
    {
        float cx;      // Holds Our X Character Coord
        float cy;      // Holds Our Y Character Coord

        base = gl.glGenLists(256);  // Creating 256 Display Lists
        gl.glBindTexture(GL2.GL_TEXTURE_2D, textures[0]);  // Select Our Font Texture
        for (int loop = 0; loop < 256; loop++)      // Loop Through All 256 Lists
        {
            cx = (float) (loop % 16) / 16.0f;  // X Position Of Current Character
            cy = (float) (loop / 16) / 16.0f;  // Y Position Of Current Character

            gl.glNewList(base + loop, GL2.GL_COMPILE);  // Start Building A List
            gl.glBegin(GL2.GL_QUADS);      // Use A Quad For Each Character
            gl.glTexCoord2f(cx, 1 - cy - 0.0625f);  // Texture Coord (Bottom Left)
            gl.glVertex2i(0, 0);      // Vertex Coord (Bottom Left)
            gl.glTexCoord2f(cx + 0.0625f, 1 - cy - 0.0625f);  // Texture Coord (Bottom Right)
            gl.glVertex2i(10, 0);      // Vertex Coord (Bottom Right)
            gl.glTexCoord2f(cx + 0.0625f, 1 - cy);  // Texture Coord (Top Right)
            gl.glVertex2i(10, 16);      // Vertex Coord (Top Right)
            gl.glTexCoord2f(cx, 1 - cy);    // Texture Coord (Top Left)
            gl.glVertex2i(0, 16);      // Vertex Coord (Top Left)
            gl.glEnd();          // Done Building Our Quad (Character)
            gl.glTranslated(10, 0, 0);      // Move To The Right Of The Character
            gl.glEndList();        // Done Building The Display List
        }            // Loop Until All 256 Are Built
    }

    // Where The Printing Happens
    public void write(int x, int y, String string, int set) {

        if (set > 1) {
            set = 1;
        }
        gl.glBindTexture(GL2.GL_TEXTURE_2D, textures[0]); // Select Our Font Texture
        //gl.glDisable(GL2.GL_DEPTH_TEST);       // Disables Depth Testing
        //gl.glMatrixMode(GL2.GL_PROJECTION);     // Select The Projection Matrix
//        gl.glPushMatrix();         // Store The Projection Matrix
//        gl.glLoadIdentity();         // Reset The Projection Matrix
        //gl.glOrtho(0, 640, 0, 480, -1, 1);     // Set Up An Ortho Screen
        //gl.glMatrixMode(GL2.GL_MODELVIEW);     // Select The Modelview Matrix
//        gl.glPushMatrix();         // Store The Modelview Matrix
//        gl.glLoadIdentity();         // Reset The Modelview Matrix
        //gl.glTranslated(x, y, 0);       // Position The Text (0,0 - Bottom Left)
        gl.glListBase(base - 32 + (128 * set));     // Choose The Font Set (0 or 1)

        if (stringBuffer.capacity() < string.length()) {
            stringBuffer = ByteBuffer.allocate(string.length());
        }

        stringBuffer.clear();
        stringBuffer.put(string.getBytes());
        stringBuffer.flip();

        // Write The Text To The Screen
        gl.glCallLists(string.length(), GL2.GL_BYTE, stringBuffer);

        //gl.glMatrixMode(GL2.GL_PROJECTION);  // Select The Projection Matrix
//        gl.glPopMatrix();      // Restore The Old Projection Matrix
        //gl.glMatrixMode(GL2.GL_MODELVIEW);  // Select The Modelview Matrix
        //      gl.glPopMatrix();      // Restore The Old Projection Matrix
        //gl.glEnable(GL2.GL_DEPTH_TEST);    // Enables Depth Testing

        gl.glDisable(textures[0]);
    }

    boolean init;

    public synchronized void init(GL2 gl) {

        if (this.init)
            return; //already init

        this.gl = gl;

        loadGLTextures();

        buildFont();

        this.init = true;

        //gl.glShadeModel(GL2.GL_SMOOTH);                 // Enables Smooth Color Shading

        // This Will Clear The Background Color To Black
        //gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        // Enables Clearing Of The Depth Buffer
        //gl.glClearDepth(1.0);

//        gl.glEnable(GL2.GL_DEPTH_TEST);                 // Enables Depth Testing
//        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE);    // Select The Type Of Blending
//        gl.glDepthFunc(GL2.GL_LEQUAL);                  // The Type Of Depth Test To Do
//
//        // Really Nice Perspective Calculations
//        gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);
        gl.glEnable(GL2.GL_TEXTURE_2D);      // Enable 2D Texture Mapping
    }

    public static void main(String[] args) {
        SpaceGraph.window(new Surface() {

            public BmpFont f;
            private float cnt1;    // 1st Counter Used To Move Text & For Coloring
            private float cnt2;    // 2nd Counter Used To Move Text & For Coloring


            @Override
            protected void paint(GL2 gl, int dtMS) {
                if (f == null)
                    f = BmpFont.the(gl);

                gl.glScalef(0.05f, 0.08f, 1f);
                // Pulsing Colors Based On Text Position
                gl.glColor3f((float) (Math.cos(cnt1)), (float)
                        (Math.sin(cnt2)), 1.0f - 0.5f * (float) (Math.cos(cnt1 + cnt2)));

                // Print GL Text To The Screen
                f.write( (int) ((280 + 250 * Math.cos(cnt1))),
                        (int) (235 + 200 * Math.sin(cnt2)), "NeHe", 0);

                gl.glColor3f((float) (Math.sin(cnt2)), 1.0f - 0.5f *
                        (float) (Math.cos(cnt1 + cnt2)), (float) (Math.cos(cnt1)));

                // Print GL Text To The Screen
                f.write((int) ((280 + 230 * Math.cos(cnt2))),
                        (int) (235 + 200 * Math.sin(cnt1)), "OpenGL", 1);

                gl.glColor3f(0.0f, 0.0f, 1.0f);
                f.write( (int) (240 + 200 * Math.cos((cnt2 + cnt1) / 5)),
                        2, "Giuseppe D'Agata", 0);

                gl.glColor3f(1.0f, 1.0f, 1.0f);
                f.write( (int) (242 + 200 * Math.cos((cnt2 + cnt1) / 5)),
                        2, "Giuseppe D'Agata", 0);

                cnt1 += 0.01f;      // Increase The First Counter
                cnt2 += 0.0081f;    // Increase The Second Counter
            }
        }, 800, 600);
    }


}


// Clear The Screen And The Depth Buffer
//gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
//gl.glLoadIdentity();  // Reset The View

//        // Select Our Second Texture
//        gl.glBindTexture(GL2.GL_TEXTURE_2D, textures[1]);
//        ///gl.glTranslatef(0.0f, 0.0f, -5.0f);  // Move Into The Screen 5 Units
//
//        // Rotate On The Z Axis 45 Degrees (Clockwise)
//        gl.glRotatef(45.0f, 0.0f, 0.0f, 1.0f);
//
//        // Rotate On The X & Y Axis By cnt1 (Left To Right)
//        gl.glRotatef(cnt1 * 30.0f, 1.0f, 1.0f, 0.0f);
//        //gl.glDisable(GL2.GL_BLEND);          // Disable Blending Before We Draw In 3D
//        gl.glColor3f(1.0f, 1.0f, 1.0f);     // Bright White
//        gl.glBegin(GL2.GL_QUADS);            // Draw Our First Texture Mapped Quad
//        gl.glTexCoord2d(0.0f, 0.0f);        // First Texture Coord
//        gl.glVertex2f(-1.0f, 1.0f);         // First Vertex
//        gl.glTexCoord2d(1.0f, 0.0f);        // Second Texture Coord
//        gl.glVertex2f(1.0f, 1.0f);          // Second Vertex
//        gl.glTexCoord2d(1.0f, 1.0f);        // Third Texture Coord
//        gl.glVertex2f(1.0f, -1.0f);         // Third Vertex
//        gl.glTexCoord2d(0.0f, 1.0f);        // Fourth Texture Coord
//        gl.glVertex2f(-1.0f, -1.0f);        // Fourth Vertex
//        gl.glEnd();                         // Done Drawing The First Quad
//
//        // Rotate On The X & Y Axis By 90 Degrees (Left To Right)
//        gl.glRotatef(90.0f, 1.0f, 1.0f, 0.0f);
//        gl.glBegin(GL2.GL_QUADS);            // Draw Our Second Texture Mapped Quad
//        gl.glTexCoord2d(0.0f, 0.0f);        // First Texture Coord
//        gl.glVertex2f(-1.0f, 1.0f);         // First Vertex
//        gl.glTexCoord2d(1.0f, 0.0f);        // Second Texture Coord
//        gl.glVertex2f(1.0f, 1.0f);          // Second Vertex
//        gl.glTexCoord2d(1.0f, 1.0f);        // Third Texture Coord
//        gl.glVertex2f(1.0f, -1.0f);         // Third Vertex
//        gl.glTexCoord2d(0.0f, 1.0f);        // Fourth Texture Coord
//        gl.glVertex2f(-1.0f, -1.0f);        // Fourth Vertex
//        gl.glEnd();                         // Done Drawing Our Second Quad
//        gl.glEnable(GL2.GL_BLEND);           // Enable Blending

//gl.glLoadIdentity();                // Reset The View

//    public void reshape(GL2 glDrawable, int x, int y, int w, int h) {
//        if (h == 0) h = 1;
//        GL gl = glDrawable.getGL();
//
//        // Reset The Current Viewport And Perspective Transformation
//        gl.glViewport(0, 0, w, h);
//        gl.glMatrixMode(GL2.GL_PROJECTION);    // Select The Projection Matrix
//        gl.glLoadIdentity();                  // Reset The Projection Matrix
//
//        // Calculate The Aspect Ratio Of The Window
//        glu.gluPerspective(45.0f, (float) w / (float) h, 0.1f, 100.0f);
//        gl.glMatrixMode(GL2.GL_MODELVIEW);    // Select The Modelview Matrix
//        gl.glLoadIdentity();                 // Reset The ModalView Matrix
//    }

///* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */
//
///*
//  Part of the Processing project - http://processing.org
//
//  Copyright (c) 2012-15 The Processing Foundation
//  Copyright (c) 2004-12 Ben Fry and Casey Reas
//  Copyright (c) 2001-04 Massachusetts Institute of Technology
//
//  This library is free software; you can redistribute it and/or
//  modify it under the terms of the GNU Lesser General Public
//  License as published by the Free Software Foundation, version 2.1.
//
//  This library is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//  Lesser General Public License for more details.
//
//  You should have received a copy of the GNU Lesser General
//  Public License along with this library; if not, write to the
//  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
//  Boston, MA  02111-1307  USA
//*/
//
//package spacegraph.render;
//
//import com.jogamp.opengl.util.texture.Texture;
//import org.eclipse.collections.impl.map.mutable.primitive.CharObjectHashMap;
//
//import java.util.HashMap;
//
///**
// * All the infrastructure needed for optimized font rendering
// * in OpenGL. Basically, this special class is needed because
// * fonts in Processing are handled by a separate PImage for each
// * glyph. For performance reasons, all these glyphs should be
// * stored in a single OpenGL texture (otherwise, rendering a
// * string of text would involve binding and un-binding several
// * textures.
// * PFontTexture manages the correspondence between individual
// * glyphs and the large OpenGL texture containing them. Also,
// * in the case that the font size is very large, one single
// * OpenGL texture might not be enough to store all the glyphs,
// * so PFontTexture also takes care of spreading a single font
// * over several textures.
// * @author Andres Colubri
// */
//public class FontTexture  {
//  //protected PGL pgl;
//  protected boolean is3D;
//
//  protected int minSize;
//  protected int maxSize;
//  protected int offsetX;
//  protected int offsetY;
//  protected int lineHeight;
//  protected Texture[] textures = null;
////  protected PImage[] images = null;
//  protected int lastTex;
//  protected TextureInfo[] glyphTexinfos;
//  protected CharObjectHashMap<TextureInfo> texinfoMap;
//
//  public FontTexture(PGraphicsOpenGL pg, PFont font, boolean is3D) {
//    pgl = pg.pgl;
//    this.is3D = is3D;
//
//    initTexture(pg, font);
//  }
//
//
//  protected void allocate() {
//    // Nothing to do here: the font textures will allocate
//    // themselves.
//  }
//
//
//  protected void dispose() {
//    for (int i = 0; i < textures.length; i++) {
//      textures[i].dispose();
//    }
//  }
//
//
//  protected void initTexture(PGraphicsOpenGL pg, PFont font) {
//    lastTex = -1;
//
//    int spow = PGL.nextPowerOfTwo(font.getSize());
//    minSize = PApplet.min(PGraphicsOpenGL.maxTextureSize,
//                          PApplet.max(PGL.MIN_FONT_TEX_SIZE, spow));
//    maxSize = PApplet.min(PGraphicsOpenGL.maxTextureSize,
//                          PApplet.max(PGL.MAX_FONT_TEX_SIZE, 2 * spow));
//
//    if (maxSize < spow) {
//      PGraphics.showWarning("The font size is too large to be properly " +
//                            "displayed with OpenGL");
//    }
//
//    addTexture(pg);
//
//    offsetX = 0;
//    offsetY = 0;
//    lineHeight = 0;
//
//    texinfoMap = new HashMap<PFont.Glyph, TextureInfo>();
//    glyphTexinfos = new TextureInfo[font.getGlyphCount()];
//    addAllGlyphsToTexture(pg, font);
//  }
//
//
//  public boolean addTexture(PGraphicsOpenGL pg) {
//    int w, h;
//    boolean resize;
//
//    w = maxSize;
//    int th = textures[lastTex].getHeight();
//    if (-1 < lastTex && th < maxSize) {
//      // The height of the current texture is less than the maximum, this
//      // means we can replace it with a larger texture.
//      h = Math.min(2 * th, maxSize);
//      resize = true;
//    } else {
//      h = minSize;
//      resize = false;
//    }
//
//    Texture tex;
//    if (is3D) {
//      // Bilinear sampling ensures that the texture doesn't look pixelated
//      // either when it is magnified or minified...
//      tex = new Texture(pg, w, h,
//                        new Texture.Parameters(ARGB, Texture.BILINEAR, false));
//    } else {
//      // ...however, the effect of bilinear sampling is to add some blurriness
//      // to the text in its original size. In 2D, we assume that text will be
//      // shown at its original size, so linear sampling is chosen instead (which
//      // only affects minimized text).
//      tex = new Texture(pg, w, h,
//                        new Texture.Parameters(ARGB, Texture.LINEAR, false));
//    }
//
//    if (textures == null) {
//      textures = new Texture[1];
//      textures[0] = tex;
//      images = new PImage[1];
//      images[0] = pg.wrapTexture(tex);
//      lastTex = 0;
//    } else if (resize) {
//      // Replacing old smaller texture with larger one.
//      // But first we must copy the contents of the older
//      // texture into the new one.
//      Texture tex0 = textures[lastTex];
//      tex.put(tex0);
//      textures[lastTex] = tex;
//
//      pg.setCache(images[lastTex], tex);
//      images[lastTex].width = tex.width;
//      images[lastTex].height = tex.height;
//    } else {
//      // Adding new texture to the list.
//      lastTex = textures.length;
//      Texture[] tempTex = new Texture[lastTex + 1];
//      PApplet.arrayCopy(textures, tempTex, textures.length);
//      tempTex[lastTex] = tex;
//      textures = tempTex;
//
//      PImage[] tempImg = new PImage[textures.length];
//      PApplet.arrayCopy(images, tempImg, images.length);
//      tempImg[lastTex] = pg.wrapTexture(tex);
//      images = tempImg;
//    }
//
//    // Make sure that the current texture is bound.
//    tex.bind();
//
//    return resize;
//  }
//
//
//  public void begin() {
//  }
//
//
//  public void end() {
//    for (int i = 0; i < textures.length; i++) {
//      pgl.disableTexturing(textures[i].glTarget);
//    }
//  }
//
//
//  public PImage getTexture(TextureInfo info) {
//    return images[info.texIndex];
//  }
//
//
//  // Add all the current glyphs to opengl texture.
//  public void addAllGlyphsToTexture(PGraphicsOpenGL pg, PFont font) {
//    // loop over current glyphs.
//    for (int i = 0; i < font.getGlyphCount(); i++) {
//      addToTexture(pg, i, font.getGlyph(i));
//    }
//  }
//
//
//  public void updateGlyphsTexCoords() {
//    // loop over current glyphs.
//    for (int i = 0; i < glyphTexinfos.length; i++) {
//      TextureInfo tinfo = glyphTexinfos[i];
//      if (tinfo != null && tinfo.texIndex == lastTex) {
//        tinfo.updateUV();
//      }
//    }
//  }
//
//
//  public TextureInfo getTexInfo(PFont.Glyph glyph) {
//    TextureInfo info = texinfoMap.get(glyph);
//    return info;
//  }
//
//
//  public TextureInfo addToTexture(PGraphicsOpenGL pg, PFont.Glyph glyph) {
//    int n = glyphTexinfos.length;
//    if (n == 0) {
//      glyphTexinfos = new TextureInfo[1];
//    }
//    addToTexture(pg, n, glyph);
//    return glyphTexinfos[n];
//  }
//
//
//  public boolean contextIsOutdated() {
//    boolean outdated = false;
//    for (int i = 0; i < textures.length; i++) {
//      if (textures[i].contextIsOutdated())  {
//        outdated = true;
//      }
//    }
//    if (outdated) {
//      for (int i = 0; i < textures.length; i++) {
//        textures[i].dispose();
//      }
//    }
//    return outdated;
//  }
//
////  public void draw() {
////    Texture tex = textures[lastTex];
////    pgl.drawTexture(tex.glTarget, tex.glName,
////                    tex.glWidth, tex.glHeight,
////                    0, 0, tex.glWidth, tex.glHeight);
////  }
//
//
//  // Adds this glyph to the opengl texture in PFont.
//  protected void addToTexture(PGraphicsOpenGL pg, int idx, PFont.Glyph glyph) {
//    // We add one pixel to avoid issues when sampling the font texture at
//    // fractional screen positions. I.e.: the pixel on the screen only contains
//    // half of the font rectangle, so it would sample half of the color from the
//    // glyph area in the texture, and the other half from the contiguous pixel.
//    // If the later contains a portion of the neighbor glyph and the former
//    // doesn't, this would result in a shaded pixel when the correct output is
//    // blank. This is a consequence of putting all the glyphs in a common
//    // texture with bilinear sampling.
//    int w = 1 + glyph.width + 1;
//    int h = 1 + glyph.height + 1;
//
//    // Converting the pixels array from the PImage into a valid RGBA array for
//    // OpenGL.
//    int[] rgba = new int[w * h];
//    int t = 0;
//    int p = 0;
//    if (PGL.BIG_ENDIAN)  {
//      java.util.Arrays.fill(rgba, 0, w, 0xFFFFFF00); // Set the first row to blank pixels.
//      t = w;
//      for (int y = 0; y < glyph.height; y++) {
//        rgba[t++] = 0xFFFFFF00; // Set the leftmost pixel in this row as blank
//        for (int x = 0; x < glyph.width; x++) {
//          rgba[t++] = 0xFFFFFF00 | glyph.image.pixels[p++];
//        }
//        rgba[t++] = 0xFFFFFF00; // Set the rightmost pixel in this row as blank
//      }
//      java.util.Arrays.fill(rgba, (h - 1) * w, h * w, 0xFFFFFF00); // Set the last row to blank pixels.
//    } else {
//      java.util.Arrays.fill(rgba, 0, w, 0x00FFFFFF); // Set the first row to blank pixels.
//      t = w;
//      for (int y = 0; y < glyph.height; y++) {
//        rgba[t++] = 0x00FFFFFF; // Set the leftmost pixel in this row as blank
//        for (int x = 0; x < glyph.width; x++) {
//          rgba[t++] = (glyph.image.pixels[p++] << 24) | 0x00FFFFFF;
//        }
//        rgba[t++] = 0x00FFFFFF; // Set the rightmost pixel in this row as blank
//      }
//      java.util.Arrays.fill(rgba, (h - 1) * w, h * w, 0x00FFFFFF); // Set the last row to blank pixels.
//    }
//
//    // Is there room for this glyph in the current line?
//    if (offsetX + w > textures[lastTex].glWidth) {
//      // No room, go to the next line:
//      offsetX = 0;
//      offsetY += lineHeight;
//    }
//    lineHeight = Math.max(lineHeight, h);
//
//    boolean resized = false;
//    if (offsetY + lineHeight > textures[lastTex].glHeight) {
//      // We run out of space in the current texture, so we add a new texture:
//      resized = addTexture(pg);
//      if (resized) {
//        // Because the current texture has been resized, we need to
//        // update the UV coordinates of all the glyphs associated to it:
//        updateGlyphsTexCoords();
//      } else {
//        // A new texture has been created. Reseting texture coordinates
//        // and line.
//        offsetX = 0;
//        offsetY = 0;
//        lineHeight = 0;
//      }
//    }
//
//    TextureInfo tinfo = new TextureInfo(lastTex, offsetX, offsetY, w, h, rgba);
//    offsetX += w;
//
//    if (idx == glyphTexinfos.length) {
//      TextureInfo[] temp = new TextureInfo[glyphTexinfos.length + 1];
//      System.arraycopy(glyphTexinfos, 0, temp, 0, glyphTexinfos.length);
//      glyphTexinfos = temp;
//    }
//
//    glyphTexinfos[idx] = tinfo;
//    texinfoMap.put(glyph, tinfo);
//  }
//
//
//  class TextureInfo {
//    int texIndex;
//    int width;
//    int height;
//    int[] crop;
//    float u0, u1;
//    float v0, v1;
//    int[] pixels;
//
//    TextureInfo(int tidx, int cropX, int cropY, int cropW, int cropH,
//                int[] pix) {
//      texIndex = tidx;
//      crop = new int[4];
//      // The region of the texture corresponding to the glyph is surrounded by a
//      // 1-pixel wide border to avoid artifacts due to bilinear sampling. This
//      // is why the additions and subtractions to the crop values.
//      crop[0] = cropX + 1;
//      crop[1] = cropY + 1 + cropH - 2;
//      crop[2] = cropW - 2;
//      crop[3] = -cropH + 2;
//      pixels = pix;
//      updateUV();
//      updateTex();
//    }
//
//
//    void updateUV() {
//      width = textures[texIndex].glWidth;
//      height = textures[texIndex].glHeight;
//
//      u0 = (float)crop[0] / (float)width;
//      u1 = u0 + (float)crop[2] / (float)width;
//      v0 = (float)(crop[1] + crop[3]) / (float)height;
//      v1 = v0 - (float)crop[3] / (float)height;
//    }
//
//
//    void updateTex() {
//      textures[texIndex].setNative(pixels, crop[0] - 1, crop[1] + crop[3] - 1,
//                                           crop[2] + 2, -crop[3] + 2);
//    }
//  }
//}