package raytracer.main;

import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.Animator;
import raytracer.basic.BowlingScene;

import javax.vecmath.Matrix3d;
import javax.vecmath.Vector3d;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

class PreviewMain
{
    
    public final static double MOUSE_TRANSLATION_FACTOR = 5.0;
    public final static double MOUSE_ROTATION_FACTOR = 1.0;
    
    
    public final static double KEY_TRANSLATION_FACTOR = 1.0;
    public final static double KEY_ROTATION_FACTOR = 10.0;
    public final static double KEY_ANGLE_INCREMENT = 10.0;
        

    
    
    public static void main(String... argv) throws Exception
    {
        
        /*
        GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
        caps.setStencilBits(1);
        GLJPanel canvas = new GLJPanel(caps);
        */

        
        GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
        
        caps.setBackgroundOpaque(false);
        caps.setStencilBits(1);
        caps.setHardwareAccelerated(true);
        GLWindow glWindow = GLWindow.create(caps);



        

        glWindow.addGLEventListener(new OpenGLBox());


        
        final Animator animator = new Animator();
        animator.add(glWindow);
        

        glWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyNotify(com.jogamp.newt.event.WindowEvent windowEvent) {
                animator.stop();
                System.exit(0);
            }

            @Override
            public void windowGainedFocus(com.jogamp.newt.event.WindowEvent windowEvent) {
                animator.start();
            }

            @Override
            public void windowLostFocus(com.jogamp.newt.event.WindowEvent windowEvent) {
                animator.stop();
            }
        });

        
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        glWindow.setSize(d.width, d.height);
        
        glWindow.setVisible(true);
    }

    static class OpenGLBox implements GLEventListener, MouseListener, KeyListener {
        
        protected final static double DEC_TO_RAD = Math.PI/180.0;
        protected final static double RAD_TO_DEC = 180.0/Math.PI;

        
        protected final Vector3d EYE = new Vector3d(0.0, 2.0, 10.0);
        protected final Vector3d VIEW = new Vector3d(0.0, 0.0, -1.0);
        protected final Vector3d UP = new Vector3d(0.0, 1.0, 0.0);
        protected double ANGLE = 60.0;
        
        protected boolean bDragging, bDisplayed;
        protected int width, height;
        protected int nDragButton, nDisplayList, texnames[];
        protected double matrix[];
        protected Vector3d mouse, last_mouse, saved_mouse;
        protected Vector3d saved_view, saved_up;
        protected Matrix3dEx rotation, saved_rotation;
        protected GL2 gl;
        protected GLU glu;
        protected GLDrawable gld;
        
        
        protected final double[] mmatrix = new double[16];
        protected final double[] pmatrix = new double[16];
        protected final int[] viewport = new int[4];

        
        @Override
        public void init(GLAutoDrawable gld)
        {
            GLWindow j = (GLWindow)gld;
            
            gl = (GL2) gld.getGL();
            glu = GLU.createGLU(gl);
            this.gld = gld;
            
            
            gl.glEnable(GL.GL_DEPTH_TEST);
            
            
            
            
            gl.glEnable(GL2.GL_NORMALIZE);
            
            
            gl.glEnable(GL2.GL_COLOR_MATERIAL);
            
            
            gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
            
            
            gl.glShadeModel(GL2.GL_SMOOTH);
            
            
            
            gl.glFrontFace(GL.GL_CCW);
            
            
            
            j.addMouseListener(this);
            
            j.addKeyListener(this);
            
            
            nDisplayList = gl.glGenLists(1);
            if (!gl.glIsList(nDisplayList))
                nDisplayList = -1;      
            bDisplayed = false; 
            
            
            rotation = new Matrix3dEx();     
            matrix = new double[16];
            mouse = new Vector3d();
            last_mouse = new Vector3d(); 
            saved_mouse = new Vector3d();
            saved_rotation = new Matrix3dEx();   
            saved_view = new Vector3d();         
            saved_up = new Vector3d();           
           
            
            getLookAtRotationMatrix(rotation);
            matrix[3] = 0.0;
            matrix[7] = 0.0;
            matrix[11] = 0.0;
            matrix[12] = 0.0;
            matrix[13] = 0.0;
            matrix[14] = 0.0;
            matrix[15] = 1.0;
            
            
            bDragging = false;
            
            initLighting();
        }

        @Override
        public void dispose(GLAutoDrawable glAutoDrawable) {

        }

        
        protected void initLighting()
        {
            
            gl.glEnable(GL2.GL_LIGHTING);
            
            
            
            
            gl.glLightModeli(GL2.GL_LIGHT_MODEL_TWO_SIDE, 1);
            
            
            
            
            
            
            
            
            
            
            
            
            
            gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, getFloatv4(0.0f, 0.0f, 0.0f, 1.0f));
            
            
            
            
            initLights();
            resetMaterial();
        }
        
        
        protected void initLights()
        {
            
            gl.glLightf(GL2.GL_LIGHT0, GL2.GL_CONSTANT_ATTENUATION, 0.4f);
            gl.glLightf(GL2.GL_LIGHT0, GL2.GL_LINEAR_ATTENUATION, 0.05f);
            gl.glLightf(GL2.GL_LIGHT0, GL2.GL_QUADRATIC_ATTENUATION, 0.005f);

            
            gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, getFloatv4(0.0f, 0.0f, 0.0f, 1.0f));
            gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, getFloatv4(0.5f, 0.5f, 0.5f, 1.0f));
            gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, getFloatv4(0.5f, 0.5f, 0.5f, 1.0f));
            
            
            
            
            
            
        }
        
        
        @Override
        public void display(GLAutoDrawable drawable)
        {
            
            gl.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT | GL.GL_STENCIL_BUFFER_BIT);
            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glLoadIdentity();

            
            gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, DoubleBuffer.wrap(mmatrix));
            gl.glGetDoublev(GL2.GL_PROJECTION_MATRIX, DoubleBuffer.wrap(pmatrix));
            gl.glGetIntegerv(GL.GL_VIEWPORT, IntBuffer.wrap(viewport));


            
            gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, getFloatv4(0.0f, 0.0f, 0.0f, 1.0f));
            gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPOT_DIRECTION, getFloatv4(0.0f, 0.0f, -1.0f, 1.0f));
            gl.glEnable(GL2.GL_LIGHT0);
            
            
            
            matrix[0] = rotation.m00;
            matrix[1] = rotation.m10;
            matrix[2] = rotation.m20;
            matrix[4] = rotation.m01;
            matrix[5] = rotation.m11;
            matrix[6] = rotation.m21;
            matrix[8] = rotation.m02;
            matrix[9] = rotation.m12;
            matrix[10] = rotation.m22;
            gl.glMultMatrixd(DoubleBuffer.wrap(matrix));
            gl.glTranslated(-EYE.x, -EYE.y, -EYE.z);
            
            displayStaticScene(drawable);
        }


        
        protected void displayStaticScene(GLAutoDrawable drawable)
        {
            if (bDisplayed)
            {
                gl.glCallList(nDisplayList);    
                return;
            }

            
            
            
            if (nDisplayList >= 0)
                gl.glNewList(nDisplayList, GL2.GL_COMPILE_AND_EXECUTE);
            
            
            
            BowlingScene scene = new BowlingScene();
            scene.display(drawable);
            
            
            
            
            if (nDisplayList >= 0)
            {
                gl.glEndList();
                bDisplayed = true;
            }
        }
        
        
        
        protected void getLookAtRotationMatrix(Matrix3d matrix)
        {
            Vector3d x = new Vector3d(), y = new Vector3d(), z = new Vector3d();
            
            
            x.cross(VIEW, UP);
            y.cross(x, VIEW);
            z.set(VIEW);
            z.scale(-1.0);
            
            
            x.normalize();
            y.normalize();
            z.normalize();
            
            
            matrix.setRow(0, x);
            matrix.setRow(1, y);
            matrix.setRow(2, z);
        }

        
        
        
        
        
        
        
        protected void getRotationMatrix(Matrix3d matrix, double fAngle, Vector3d v)
        {
            double x = v.x, y = v.y, z = v.z;
            double fSin = Math.sin(fAngle*DEC_TO_RAD);
            double fCos = Math.cos(fAngle*DEC_TO_RAD);

            
            double length = Math.sqrt(x * x + y * y + z * z);

            
            
            x /= length;
            y /= length;
            z /= length;
            
            
            matrix.m00 = (y*y + z*z)*fCos + x*x;
            matrix.m10 = -(x*y*fCos - z*fSin - x*y);
            matrix.m20 = -(x*z*fCos + y*fSin - x*z);
            
            
            matrix.m01 = -(x*y*fCos + z*fSin - x*y);
            matrix.m11 = (x*x + z*z)*fCos + y*y;
            matrix.m21 = -(y*z*fCos - x*fSin - y*z);

            
            matrix.m02 = -(x*z*fCos - y*fSin - x*z);
            matrix.m12 = -(y*z*fCos + x*fSin - y*z);
            matrix.m22 = (x*x + y*y)*fCos + z*z;
        }

        
        
        protected boolean getVirtualCoordinates(int x, int y, int z, Vector3d v)
        {
            double resultXYZ[] = new double[3];

            
            
            if (!glu.gluUnProject((double) x, (double) y, (double) z, mmatrix, 0, pmatrix, 0, viewport, 0, resultXYZ, 0))
                return false;

            v.x = resultXYZ[0];
            v.y = resultXYZ[1];
            v.z = resultXYZ[2];

            return true;
        }
        
        
        @Override
        public void reshape(GLAutoDrawable gld, int x, int y, int w, int h)
        {
            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glLoadIdentity();

            width = w;
            height = h;
            if (w <= h)
            {
                glu.gluPerspective(ANGLE, 1.0, 1.0, 100.0);
                
            }
            else
            {
                glu.gluPerspective(ANGLE, 1.0, 1.0, 100.0);
                
            }
        }

        @Override
        public void mouseClicked(com.jogamp.newt.event.MouseEvent mouseEvent) {

        }

        @Override
        public void mouseEntered(com.jogamp.newt.event.MouseEvent mouseEvent) {

        }

        @Override
        public void mouseExited(com.jogamp.newt.event.MouseEvent mouseEvent) {

        }

        @Override
        public void mousePressed(com.jogamp.newt.event.MouseEvent event) {
            
            if (bDragging)
            {
                mouseReleased(event);
                return;
            }
            
            
            if (!getVirtualCoordinates(event.getX(), height-event.getY()-1, 0, last_mouse))
                return;

            
            nDragButton = event.getButton();
            
            
            saved_rotation.set(rotation);
            saved_mouse.set(last_mouse);
            saved_view.set(VIEW);
            saved_up.set(UP);
            
            
            bDragging = true;
        }

        @Override
        public void mouseReleased(com.jogamp.newt.event.MouseEvent event) {

            bDragging = false;
        }

        @Override
        public void mouseMoved(com.jogamp.newt.event.MouseEvent mouseEvent) {

        }

        @Override
        public void mouseDragged(com.jogamp.newt.event.MouseEvent event) {

            if (!bDragging)
                return;     
            
            Matrix3d matrix = new Matrix3d();
            Vector3d v = new Vector3d();
            
            
            
            Vector3d x = new Vector3d(), y = new Vector3d(), z = new Vector3d();
            z.set(VIEW);        z.normalize();
            x.cross(z, UP);     x.normalize();
            y.cross(x, z);      y.normalize();
            
            
            getVirtualCoordinates(event.getX(), height-event.getY()-1, 0, mouse);         
            
            switch (nDragButton)
            {
            case MouseEvent.BUTTON1:        
                
                
                v.set(z);
                v.scale(MOUSE_TRANSLATION_FACTOR*(mouse.y-last_mouse.y));
                EYE.add(v);
                
                
                getRotationMatrix(matrix, MOUSE_ROTATION_FACTOR*(last_mouse.x-mouse.x)*RAD_TO_DEC, y);
                matrix.transform(VIEW);
                matrix.transform(UP);
                
                break;
                
            case MouseEvent.BUTTON2:        
                
                
                v.set(x);
                v.scale(MOUSE_TRANSLATION_FACTOR*(mouse.x-last_mouse.x));
                EYE.add(v);
                
                
                v.set(y);
                v.scale(MOUSE_TRANSLATION_FACTOR*(mouse.y-last_mouse.y));
                EYE.add(v);
                
                break;
                
            case MouseEvent.BUTTON3:        
                Vector3d u = new Vector3d();
               
                
                
                mouse.z = 0.0;
                saved_mouse.z = 0.0;

                
                if (mouse.dot(mouse) > 1.0)
                    mouse.normalize();
                if (saved_mouse.dot(saved_mouse) > 1.0)
                    saved_mouse.normalize();
                
                
                
                v.x = mouse.x;
                v.y = mouse.y;
                v.z = Math.sqrt(Math.max(0.0, 1.0-mouse.dot(mouse)));
                u.x = saved_mouse.x;
                u.y = saved_mouse.y;
                u.z = Math.sqrt(Math.max(0.0, 1.0-saved_mouse.dot(saved_mouse)));

                
                double fAngle = Math.acos(v.dot(u)/(v.length()*u.length()));
                
                
                
                v.cross(v, u);

                
                getRotationMatrix(matrix, MOUSE_ROTATION_FACTOR*fAngle*RAD_TO_DEC, v);

                
                
                
                
                
                
                
                
                
                
                
                u.set(saved_view);
                saved_rotation.transform(u);
                matrix.transform(u);
                saved_rotation.transformTransposed(u);
                VIEW.set(u);
                
                
                u.set(saved_up);
                saved_rotation.transform(u);
                matrix.transform(u);
                saved_rotation.transformTransposed(u);
                UP.set(u);

                break;
            }
            last_mouse.set(mouse);
            
            
            getLookAtRotationMatrix(rotation);
        }

        @Override
        public void mouseWheelMoved(com.jogamp.newt.event.MouseEvent mouseEvent) {

        }

        @Override
        public void keyPressed(com.jogamp.newt.event.KeyEvent event) {

            Matrix3d matrix = new Matrix3d();
            Vector3d v = new Vector3d();
            
            
            
            Vector3d x = new Vector3d(), y = new Vector3d(), z = new Vector3d();
            z.set(VIEW);        z.normalize();
            x.cross(z, UP);     x.normalize();
            y.cross(x, z);      y.normalize();
            
            switch (event.getKeyCode())
            {
            case KeyEvent.VK_UP:            
                v.set(z);
                v.scale(KEY_TRANSLATION_FACTOR);
                EYE.add(v);
                break;
                
            case KeyEvent.VK_DOWN:          
                v.set(z);
                v.scale(KEY_TRANSLATION_FACTOR);
                EYE.sub(v);
                break;
                
            case KeyEvent.VK_LEFT:          
            case KeyEvent.VK_NUMPAD4:       
                getRotationMatrix(matrix, KEY_ROTATION_FACTOR, y);
                matrix.transform(VIEW);
                matrix.transform(UP);
                break;
                
            case KeyEvent.VK_RIGHT:         
            case KeyEvent.VK_NUMPAD6:       
                getRotationMatrix(matrix, -KEY_ROTATION_FACTOR, y);
                matrix.transform(VIEW);
                matrix.transform(UP);
                break;
            
            case KeyEvent.VK_NUMPAD8:       
                getRotationMatrix(matrix, KEY_ROTATION_FACTOR, x);
                matrix.transform(VIEW);
                matrix.transform(UP);
                break;
                
            case KeyEvent.VK_NUMPAD2:       
                getRotationMatrix(matrix, -KEY_ROTATION_FACTOR, x);
                matrix.transform(VIEW);
                matrix.transform(UP);
                break;
                
            case KeyEvent.VK_PLUS:       
                ANGLE += KEY_ANGLE_INCREMENT;
                ((GLJPanel)gld).setSize(width, height);
                break;
                
            case KeyEvent.VK_MINUS:       
                ANGLE -= KEY_ANGLE_INCREMENT;
                ((GLJPanel)gld).setSize(width, height);
                break;
                
            case KeyEvent.VK_SPACE:       
                System.out.println();
                System.out.println("EYE:   " + EYE);
                System.out.println("VIEW:  " + VIEW);
                System.out.println("UP:    " + UP);
                System.out.println("ANGLE: " + ANGLE);
                break;
            }
            
            
            getLookAtRotationMatrix(rotation);
        }

        @Override
        public void keyReleased(com.jogamp.newt.event.KeyEvent keyEvent) {

        }

        
        protected void resetMaterial()
        {
            setAmbient(0.2f, 0.2f, 0.2f);
            setDiffuse(0.8f, 0.8f, 0.8f);
            setSpecular(0.0f, 0.0f, 0.0f);
            setEmission(0.0f, 0.0f, 0.0f);
            setShininess(0.0f);
        }
        
        
        protected void setAmbient(float red, float green, float blue)
        {
            gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_AMBIENT, getFloatv4(red, green, blue, 1.0f));
        }
        
        
        protected void setDiffuse(float red, float green, float blue)
        {
            gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_DIFFUSE, getFloatv4(red, green, blue, 1.0f));
        }
        
        
        protected void setSpecular(float red, float green, float blue)
        {
            gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_SPECULAR, getFloatv4(red, green, blue, 1.0f));
        }
        
        
        protected void setEmission(float red, float green, float blue)
        {
            gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL2.GL_EMISSION, getFloatv4(red, green, blue, 1.0f));
        }
        
        
        protected void setShininess(float shininess)
        {
            gl.glMaterialf(GL.GL_FRONT_AND_BACK, GL2.GL_SHININESS, shininess);
        }
        
        
        protected static FloatBuffer getFloatv4(float a, float b, float c, float d)
        {
            return FloatBuffer.wrap( new float[] { a, b, c, d } );
        }

        public void displayChanged(GLDrawable drawable, boolean modeChanged, boolean deviceChanged) {}

    }
}