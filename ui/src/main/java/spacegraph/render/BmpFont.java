package spacegraph.render;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import spacegraph.Surface;

import java.io.InputStream;
import java.nio.ByteBuffer;

import static spacegraph.SpaceGraph.window;

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
        window(new Surface() {

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
