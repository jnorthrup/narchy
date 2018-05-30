package jake2.render.opengl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class DummyGL implements QGL {
    
    private static final QGL self = new DummyGL();
    
    protected DummyGL() {
        
    }
    
    public static QGL getInstance() {
        return self;
    }
    
    @Override
    public void glAlphaFunc(int func, float ref) {
        
    }

    @Override
    public void glBegin(int mode) {
        
    }

    @Override
    public void glBindTexture(int target, int texture) {
        
    }

    @Override
    public void glBlendFunc(int sfactor, int dfactor) {
        
    }

    @Override
    public void glClear(int mask) {
        
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha) {
        
    }

    @Override
    public void glColor3f(float red, float green, float blue) {
        
    }

    @Override
    public void glColor3ub(byte red, byte green, byte blue) {
        
    }

    @Override
    public void glColor4f(float red, float green, float blue, float alpha) {
        
    }

    @Override
    public void glColor4ub(byte red, byte green, byte blue, byte alpha) {
        
    }

    @Override
    public void glColorPointer(int size, boolean unsigned, int stride,
                               ByteBuffer pointer) {
        
    }
    
    @Override
    public void glColorPointer(int size, int stride, FloatBuffer pointer) {
        
    }

    @Override
    public void glCullFace(int mode) {
        
    }

    @Override
    public void glDeleteTextures(IntBuffer textures) {
        
    }

    @Override
    public void glDepthFunc(int func) {
        
    }

    @Override
    public void glDepthMask(boolean flag) {
        
    }

    @Override
    public void glDepthRange(double zNear, double zFar) {
        
    }

    @Override
    public void glDisable(int cap) {
        
    }

    @Override
    public void glDisableClientState(int cap) {
        
    }

    @Override
    public void glDrawArrays(int mode, int first, int count) {
        
    }

    @Override
    public void glDrawBuffer(int mode) {
        
    }

    @Override
    public void glDrawElements(int mode, ShortBuffer indices) {
        
    }

    @Override
    public void glEnable(int cap) {
        
    }

    @Override
    public void glEnableClientState(int cap) {
        
    }

    @Override
    public void glEnd() {
        
    }

    @Override
    public void glFinish() {
        
    }

    @Override
    public void glFlush() {
        
    }

    @Override
    public void glFrustum(double left, double right, double bottom,
                          double top, double zNear, double zFar) {
        
    }

    @Override
    public int glGetError() {
        return GL_NO_ERROR;
    }

    @Override
    public void glGetFloat(int pname, FloatBuffer params) {
        
    }

    @Override
    public String glGetString(int name) {
        switch (name) {
        case GL_EXTENSIONS:
            return "GL_ARB_multitexture GL_EXT_point_parameters";
        case GL_VERSION:
            return "2.0.0 Dummy";
        case GL_VENDOR:
            return "Dummy Cooperation";
        case GL_RENDERER:
            return "Dummy Renderer";
        default:
            return "";
        }
    }
    
    @Override
    public void glHint(int target, int mode) {
        
    }

    @Override
    public void glInterleavedArrays(int format, int stride,
                                    FloatBuffer pointer) {
        
    }

    @Override
    public void glLoadIdentity() {
        
    }

    @Override
    public void glLoadMatrix(FloatBuffer m) {
        
    }

    @Override
    public void glMatrixMode(int mode) {
        
    }

    @Override
    public void glOrtho(double left, double right, double bottom,
                        double top, double zNear, double zFar) {
        
    }

    @Override
    public void glPixelStorei(int pname, int param) {
        
    }

    @Override
    public void glPointSize(float size) {
        
    }

    @Override
    public void glPolygonMode(int face, int mode) {
        
    }

    @Override
    public void glPopMatrix() {
        
    }

    @Override
    public void glPushMatrix() {
        
    }

    @Override
    public void glReadPixels(int x, int y, int width, int height,
                             int format, int type, ByteBuffer pixels) {
        
    }

    @Override
    public void glRotatef(float angle, float x, float y, float z) {
        
    }

    @Override
    public void glScalef(float x, float y, float z) {
        
    }

    @Override
    public void glScissor(int x, int y, int width, int height) {
        
    }

    @Override
    public void glShadeModel(int mode) {
        
    }

    @Override
    public void glTexCoord2f(float s, float t) {
        
    }

    @Override
    public void glTexCoordPointer(int size, int stride, FloatBuffer pointer) {
        
    }

    @Override
    public void glTexEnvi(int target, int pname, int param) {
        
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat,
                             int width, int height, int border, int format, int type,
                             ByteBuffer pixels) {
        
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat,
                             int width, int height, int border, int format, int type,
                             IntBuffer pixels) {
        
    }

    @Override
    public void glTexParameterf(int target, int pname, float param) {
        
    }

    @Override
    public void glTexParameteri(int target, int pname, int param) {
        
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset,
                                int yoffset, int width, int height, int format, int type,
                                IntBuffer pixels) {
        
    }

    @Override
    public void glTranslatef(float x, float y, float z) {
        
    }

    @Override
    public void glVertex2f(float x, float y) {
        
    }

    @Override
    public void glVertex3f(float x, float y, float z) {
        
    }

    @Override
    public void glVertexPointer(int size, int stride, FloatBuffer pointer) {
        
    }

    @Override
    public void glViewport(int x, int y, int width, int height) {
        
    }

    @Override
    public void glColorTable(int target, int internalFormat, int width,
                             int format, int type, ByteBuffer data) {
        
    }

    @Override
    public void glActiveTextureARB(int texture) {
        
    }

    @Override
    public void glClientActiveTextureARB(int texture) {
        
    }

    @Override
    public void glPointParameterEXT(int pname, FloatBuffer pfParams) {
        
    }

    @Override
    public void glPointParameterfEXT(int pname, float param) {
        
    }

    @Override
    public void glLockArraysEXT(int first, int count) {
        
    }

    @Override
    public void glArrayElement(int index) {
        
    }

    @Override
    public void glUnlockArraysEXT() {
        
    }

    @Override
    public void glMultiTexCoord2f(int target, float s, float t) {
        
    }

    /*
     * util extensions
     */
    @Override
    public void setSwapInterval(int interval) {
        
    }

}
