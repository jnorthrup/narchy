package jake2.render.opengl;


import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class CountGL implements QGL {
    
    private static int count;
    
    private static final QGL self = new CountGL();
    
    private CountGL() {
        
    }
    
    public static QGL getInstance() {
        return self;
    }
    
    @Override
    public void glAlphaFunc(int func, float ref) {
        ++count;
    }

    @Override
    public void glBegin(int mode) {
        ++count;
    }

    @Override
    public void glBindTexture(int target, int texture) {
        ++count;
    }

    @Override
    public void glBlendFunc(int sfactor, int dfactor) {
        ++count;
    }

    @Override
    public void glClear(int mask) {
        ++count;
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha) {
        ++count;
    }

    @Override
    public void glColor3f(float red, float green, float blue) {
        ++count;
    }

    @Override
    public void glColor3ub(byte red, byte green, byte blue) {
        ++count;
    }

    @Override
    public void glColor4f(float red, float green, float blue, float alpha) {
        ++count;
    }

    @Override
    public void glColor4ub(byte red, byte green, byte blue, byte alpha) {
        ++count;
    }

    @Override
    public void glColorPointer(int size, boolean unsigned, int stride,
                               ByteBuffer pointer) {
        ++count;
    }
    
    @Override
    public void glColorPointer(int size, int stride, FloatBuffer pointer) {
        ++count;
    }

    @Override
    public void glCullFace(int mode) {
        ++count;
    }

    @Override
    public void glDeleteTextures(IntBuffer textures) {
        ++count;
    }

    @Override
    public void glDepthFunc(int func) {
        ++count;
    }

    @Override
    public void glDepthMask(boolean flag) {
        ++count;
    }

    @Override
    public void glDepthRange(double zNear, double zFar) {
        ++count;
    }

    @Override
    public void glDisable(int cap) {
        ++count;
    }

    @Override
    public void glDisableClientState(int cap) {
        ++count;
    }

    @Override
    public void glDrawArrays(int mode, int first, int count) {
        ++count;
    }

    @Override
    public void glDrawBuffer(int mode) {
        ++count;
    }

    @Override
    public void glDrawElements(int mode, ShortBuffer indices) {
        ++count;
    }

    @Override
    public void glEnable(int cap) {
        ++count;
    }

    @Override
    public void glEnableClientState(int cap) {
        ++count;
    }

    @Override
    public void glEnd() {
        ++count;
    }

    @Override
    public void glFinish() {
        ++count;
    }

    @Override
    public void glFlush() {
        System.err.println("GL calls/frame: " + (++count));
        count = 0;
    }

    @Override
    public void glFrustum(double left, double right, double bottom,
                          double top, double zNear, double zFar) {
        ++count;
    }

    @Override
    public int glGetError() {
        return GL_NO_ERROR;
    }

    @Override
    public void glGetFloat(int pname, FloatBuffer params) {
        ++count;
    }

    @Override
    public String glGetString(int name) {
        switch (name) {
        case GL_EXTENSIONS:
            return "GL_ARB_multitexture";
        default:
            return "";
        }
    }

    @Override
    public void glHint(int target, int mode) {
	++count;
    }

    @Override
    public void glInterleavedArrays(int format, int stride,
                                    FloatBuffer pointer) {
        ++count;
    }

    @Override
    public void glLoadIdentity() {
        ++count;
    }

    @Override
    public void glLoadMatrix(FloatBuffer m) {
        ++count;
    }

    @Override
    public void glMatrixMode(int mode) {
        ++count;
    }

    @Override
    public void glOrtho(double left, double right, double bottom,
                        double top, double zNear, double zFar) {
        ++count;
    }

    @Override
    public void glPixelStorei(int pname, int param) {
        ++count;
    }

    @Override
    public void glPointSize(float size) {
        ++count;
    }

    @Override
    public void glPolygonMode(int face, int mode) {
        ++count;
    }

    @Override
    public void glPopMatrix() {
        ++count;
    }

    @Override
    public void glPushMatrix() {
        ++count;
    }

    @Override
    public void glReadPixels(int x, int y, int width, int height,
                             int format, int type, ByteBuffer pixels) {
        ++count;
    }

    @Override
    public void glRotatef(float angle, float x, float y, float z) {
        ++count;
    }

    @Override
    public void glScalef(float x, float y, float z) {
        ++count;
    }

    @Override
    public void glScissor(int x, int y, int width, int height) {
        ++count;
    }

    @Override
    public void glShadeModel(int mode) {
        ++count;
    }

    @Override
    public void glTexCoord2f(float s, float t) {
        ++count;
    }

    @Override
    public void glTexCoordPointer(int size, int stride, FloatBuffer pointer) {
        ++count;
    }

    @Override
    public void glTexEnvi(int target, int pname, int param) {
        ++count;
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat,
                             int width, int height, int border, int format, int type,
                             ByteBuffer pixels) {
        ++count;
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat,
                             int width, int height, int border, int format, int type,
                             IntBuffer pixels) {
        ++count;
    }

    @Override
    public void glTexParameterf(int target, int pname, float param) {
        ++count;
    }

    @Override
    public void glTexParameteri(int target, int pname, int param) {
        ++count;
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset,
                                int yoffset, int width, int height, int format, int type,
                                IntBuffer pixels) {
        ++count;
    }

    @Override
    public void glTranslatef(float x, float y, float z) {
        ++count;
    }

    @Override
    public void glVertex2f(float x, float y) {
        ++count;
    }

    @Override
    public void glVertex3f(float x, float y, float z) {
        ++count;
    }

    @Override
    public void glVertexPointer(int size, int stride, FloatBuffer pointer) {
        ++count;
    }

    @Override
    public void glViewport(int x, int y, int width, int height) {
        ++count;
    }

    @Override
    public void glColorTable(int target, int internalFormat, int width,
                             int format, int type, ByteBuffer data) {
        ++count;
    }

    @Override
    public void glActiveTextureARB(int texture) {
        ++count;
    }

    @Override
    public void glClientActiveTextureARB(int texture) {
        ++count;
    }

    @Override
    public void glPointParameterEXT(int pname, FloatBuffer pfParams) {
        ++count;
    }

    @Override
    public void glPointParameterfEXT(int pname, float param) {
        ++count;
    }

    @Override
    public void glLockArraysEXT(int first, int count) {
        ++count;
    }

    @Override
    public void glArrayElement(int index) {
        ++count;
    }

    @Override
    public void glUnlockArraysEXT() {
        ++count;
    }

    @Override
    public void glMultiTexCoord2f(int target, float s, float t) {
        ++count;
    }

    /*
     * util extensions
     */
    @Override
    public void setSwapInterval(int interval) {
	++count;
    }

}
