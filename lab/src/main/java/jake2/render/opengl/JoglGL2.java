package jake2.render.opengl;


import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;


public final class JoglGL2 implements QGL {
    
    private GL2 gl;
    
    JoglGL2() {
        
    }
    
    void setGL(GL2 gl) {
        this.gl = gl;
    }
    
    @Override
    public void glAlphaFunc(int func, float ref) {
        gl.glAlphaFunc(func, ref);
    }

    @Override
    public void glBegin(int mode) {
        gl.glBegin(mode);
    }

    @Override
    public void glBindTexture(int target, int texture) {
        gl.glBindTexture(target, texture);
    }

    @Override
    public void glBlendFunc(int sfactor, int dfactor) {
        gl.glBlendFunc(sfactor, dfactor);
    }

    @Override
    public void glClear(int mask) {
        gl.glClear(mask);
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha) {
        gl.glClearColor(red, green, blue, alpha);
    }

    @Override
    public void glColor3f(float red, float green, float blue) {
        gl.glColor3f(red, green, blue);
    }

    @Override
    public void glColor3ub(byte red, byte green, byte blue) {
        gl.glColor3ub(red, green, blue);
    }

    @Override
    public void glColor4f(float red, float green, float blue, float alpha) {
        gl.glColor4f(red, green, blue, alpha);
    }

    @Override
    public void glColor4ub(byte red, byte green, byte blue, byte alpha) {
        gl.glColor4ub(red, green, blue, alpha);
    }

    @Override
    public void glColorPointer(int size, boolean unsigned, int stride,
                               ByteBuffer pointer) {
        gl.glColorPointer(size, GL_UNSIGNED_BYTE, stride, pointer);
    }
    
    @Override
    public void glColorPointer(int size, int stride, FloatBuffer pointer) {
        gl.glColorPointer(size, GL_FLOAT, stride, pointer);
    }

    @Override
    public void glCullFace(int mode) {
        gl.glCullFace(mode);
    }

    @Override
    public void glDeleteTextures(IntBuffer textures) {
        gl.glDeleteTextures(textures.limit(), textures);
    }

    @Override
    public void glDepthFunc(int func) {
        gl.glDepthFunc(func);
    }

    @Override
    public void glDepthMask(boolean flag) {
        gl.glDepthMask(flag);
    }

    @Override
    public void glDepthRange(double zNear, double zFar) {
        gl.glDepthRange(zNear, zFar);
    }

    @Override
    public void glDisable(int cap) {
        gl.glDisable(cap);
    }

    @Override
    public void glDisableClientState(int cap) {
        gl.glDisableClientState(cap);
    }

    @Override
    public void glDrawArrays(int mode, int first, int count) {
        gl.glDrawArrays(mode, first, count);
    }

    @Override
    public void glDrawBuffer(int mode) {
        gl.glDrawBuffer(mode);
    }

    @Override
    public void glDrawElements(int mode, ShortBuffer indices) {
        gl.glDrawElements(mode, indices.remaining(), GL.GL_UNSIGNED_SHORT, indices);
    }

    @Override
    public void glEnable(int cap) {
        gl.glEnable(cap);
    }

    @Override
    public void glEnableClientState(int cap) {
        gl.glEnableClientState(cap);
    }

    @Override
    public void glEnd() {
        gl.glEnd();
    }

    @Override
    public void glFinish() {
        gl.glFinish();
    }

    @Override
    public void glFlush() {
        gl.glFlush();
    }

    @Override
    public void glFrustum(double left, double right, double bottom,
                          double top, double zNear, double zFar) {
        gl.glFrustum(left, right, bottom, top, zNear, zFar);
    }

    @Override
    public int glGetError() {
        return gl.glGetError();
    }

    @Override
    public void glGetFloat(int pname, FloatBuffer params) {
        gl.glGetFloatv(pname, params);
    }

    @Override
    public String glGetString(int name) {
        return gl.glGetString(name);
    }
    
    @Override
    public void glHint(int target, int mode) {
	gl.glHint(target, mode);
    }

    @Override
    public void glInterleavedArrays(int format, int stride,
                                    FloatBuffer pointer) {
        gl.glInterleavedArrays(format, stride, pointer);
    }

    @Override
    public void glLoadIdentity() {
        gl.glLoadIdentity();
    }

    @Override
    public void glLoadMatrix(FloatBuffer m) {
        gl.glLoadMatrixf(m);
    }

    @Override
    public void glMatrixMode(int mode) {
        gl.glMatrixMode(mode);
    }

    @Override
    public void glOrtho(double left, double right, double bottom,
                        double top, double zNear, double zFar) {
        gl.glOrtho(left, right, bottom, top, zNear, zFar);
    }

    @Override
    public void glPixelStorei(int pname, int param) {
        gl.glPixelStorei(pname, param);
    }

    @Override
    public void glPointSize(float size) {
        gl.glPointSize(size);
    }

    @Override
    public void glPolygonMode(int face, int mode) {
        gl.glPolygonMode(face, mode);
    }

    @Override
    public void glPopMatrix() {
        gl.glPopMatrix();
    }

    @Override
    public void glPushMatrix() {
        gl.glPushMatrix();
    }

    @Override
    public void glReadPixels(int x, int y, int width, int height,
                             int format, int type, ByteBuffer pixels) {
        gl.glReadPixels(x, y, width, height, format, type, pixels);
    }

    @Override
    public void glRotatef(float angle, float x, float y, float z) {
        gl.glRotatef(angle, x, y, z);
    }

    @Override
    public void glScalef(float x, float y, float z) {
        gl.glScalef(x, y, z);
    }

    @Override
    public void glScissor(int x, int y, int width, int height) {
        gl.glScissor(x, y, width, height);
    }

    @Override
    public void glShadeModel(int mode) {
        gl.glShadeModel(mode);
    }

    @Override
    public void glTexCoord2f(float s, float t) {
        gl.glTexCoord2f(s, t);
    }

    @Override
    public void glTexCoordPointer(int size, int stride, FloatBuffer pointer) {
        gl.glTexCoordPointer(size, GL_FLOAT, stride, pointer);
    }

    @Override
    public void glTexEnvi(int target, int pname, int param) {
        gl.glTexEnvi(target, pname, param);
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat,
                             int width, int height, int border, int format, int type,
                             ByteBuffer pixels) {
        gl.glTexImage2D(target, level, internalformat, width, height, border,
                format, type, pixels);
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat,
                             int width, int height, int border, int format, int type,
                             IntBuffer pixels) {
        gl.glTexImage2D(target, level, internalformat, width, height, border,
                format, type, pixels);
    }

    @Override
    public void glTexParameterf(int target, int pname, float param) {
        gl.glTexParameterf(target, pname, param);
    }

    @Override
    public void glTexParameteri(int target, int pname, int param) {
        gl.glTexParameteri(target, pname, param);
    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset,
                                int yoffset, int width, int height, int format, int type,
                                IntBuffer pixels) {
        gl.glTexSubImage2D(target, level, xoffset, yoffset, width, height,
                format, type, pixels);
    }

    @Override
    public void glTranslatef(float x, float y, float z) {
        gl.glTranslatef(x, y, z);
    }

    @Override
    public void glVertex2f(float x, float y) {
        gl.glVertex2f(x, y);
    }

    @Override
    public void glVertex3f(float x, float y, float z) {
        gl.glVertex3f(x, y, z);
    }

    @Override
    public void glVertexPointer(int size, int stride, FloatBuffer pointer) {
        gl.glVertexPointer(size, GL_FLOAT, stride, pointer);
    }

    @Override
    public void glViewport(int x, int y, int width, int height) {
        gl.glViewport(x, y, width, height);
    }

    @Override
    public void glColorTable(int target, int internalFormat, int width,
                             int format, int type, ByteBuffer data) {
        gl.glColorTable(target, internalFormat, width, format, type, data);
    }

    @Override
    public void glActiveTextureARB(int texture) {
        gl.glActiveTexture(texture);
    }

    @Override
    public void glClientActiveTextureARB(int texture) {
        gl.glClientActiveTexture(texture);
    }

    @Override
    public void glPointParameterEXT(int pname, FloatBuffer pfParams) {
        gl.glPointParameterfv(pname, pfParams);
    }

    @Override
    public void glPointParameterfEXT(int pname, float param) {
        gl.glPointParameterf(pname, param);
    }
    @Override
    public void glLockArraysEXT(int first, int count) {
        gl.glLockArraysEXT(first, count);
    }

    @Override
    public void glArrayElement(int index) {
        gl.glArrayElement(index);
    }

    @Override
    public void glUnlockArraysEXT() {
        gl.glUnlockArraysEXT();
    }
    
    @Override
    public void glMultiTexCoord2f(int target, float s, float t) {
        gl.glMultiTexCoord2f(target, s, t);
    }
    
    /*
     * util extensions
     */
    @Override
    public void setSwapInterval(int interval) {
	gl.setSwapInterval(interval);
    }

}
