package jake2.render.opengl;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.util.ImmModeSink;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class JoglGL2ES1 implements QGL {

    public GL2ES1 gl;
    protected ImmModeSink ims;
    private boolean inBlock; 

    JoglGL2ES1() {
    }

    void setGL(GL2ES1 gl) {
        this.gl = gl;
        ims = ImmModeSink.createFixed(4,
                3, GL.GL_FLOAT,  
                4, GL.GL_FLOAT,  
                0, 0,            
                2, GL.GL_FLOAT,  
                GL.GL_STATIC_DRAW);
    }

    @Override
    public void glBegin(int mode) {
        if(inBlock) {
            throw new GLException("glBegin already called");
        }
        ims.glBegin(mode);
        inBlock = true;
    }

    @Override
    public void glEnd() {
        if(!inBlock) {
            throw new GLException("glBegin not called");
        }        
        ims.glEnd(gl, true);
        inBlock = false;
    }

    @Override
    public void glColor3f(float red, float green, float blue) {
        glColor4f(red, green, blue, 1f);
    }

    @Override
    public void glColor3ub(byte red, byte green, byte blue) {
        if(inBlock) {
            ims.glColor3ub(red, green, blue);
        } else {
            gl.glColor4ub(red, green, blue, (byte)255);
        }
    }
    
    @Override
    public void glColor4f(float red, float green, float blue, float alpha) {
        if(inBlock) {
            ims.glColor4f(red, green, blue, alpha);
        } else {
            gl.glColor4f(red, green, blue, alpha);
        }
    }

    @Override
    public void glColor4ub(byte red, byte green, byte blue, byte alpha) {
        if(inBlock) {
            ims.glColor4ub(red, green, blue, alpha);
        } else {
            gl.glColor4ub(red, green, blue, alpha);
        }
    }

    @Override
    public void glTexCoord2f(float s, float t) {
        if(inBlock) {
            ims.glTexCoord2f(s, t);
        } else {
            throw new GLException("glBegin missing");
        }
    }

    @Override
    public void glVertex2f(float x, float y) {
        if(inBlock) {
            ims.glVertex2f(x, y);
        } else {
            throw new GLException("glBegin missing");
        }
    }

    @Override
    public void glVertex3f(float x, float y, float z) {
        if(inBlock) {
            ims.glVertex3f(x, y, z);
        } else {
            throw new GLException("glBegin missing");
        }
    }

    @Override
    public void glAlphaFunc(int func, float ref) {
        gl.glAlphaFunc(func, ref);
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
    public void glColorPointer(int size, boolean unsigned, int stride, ByteBuffer pointer) {
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
        gl.glDepthRangef((float)zNear, (float)zFar);
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
        switch(mode) {
            case GL_QUAD_STRIP:
                mode=GL.GL_TRIANGLE_STRIP;
                break;
            case GL_POLYGON:
                mode=GL.GL_TRIANGLE_FAN;
                break;
        }
        /** No GL_QUADS used
        if ( GL_QUADS == mode && !gl.isGL2() ) {
            for (int j = first; j < count - 3; j += 4) {
                gl.glDrawArrays(GL.GL_TRIANGLE_FAN, j, 4);
            }
        } else { */
            gl.glDrawArrays(mode, first, count);
        
    }

    @Override
    public void glDrawBuffer(int mode) {
        
        if(GL.GL_BACK != mode) {
            System.err.println("IGNORED: glDrawBuffer(0x"+Integer.toHexString(mode)+ ')');
        }
    }

    @Override
    public void glDrawElements(int mode, ShortBuffer indices) {
        switch(mode) {
            case GL_QUAD_STRIP:
                mode=GL.GL_TRIANGLE_STRIP;
                break;
            case GL_POLYGON:
                mode=GL.GL_TRIANGLE_FAN;
                break;
        }
        final int idxLen = indices.remaining();
        /** No GL_QUADS used
        if ( GL_QUADS == mode && !gl.isGL2() ) {
            final int idx0 = indices.position();
            final ShortBuffer b = (ShortBuffer) indices;
            for (int j = 0; j < idxLen; j++) {
                gl.glDrawArrays(GL.GL_TRIANGLE_FAN, (int)(0x0000ffff & b.get(idx0+j)), 4);
            }
        } else { */
            gl.glDrawElements(mode, idxLen, GL.GL_UNSIGNED_SHORT, indices);
        
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

    private static final String GL_EXT_point_parameters = "GL_EXT_point_parameters";
    
    @Override
    public String glGetString(int name) {
        if( GL.GL_EXTENSIONS == name ) {
            StringBuilder sb = new StringBuilder();
            sb.append(gl.glGetString(GL.GL_EXTENSIONS));
            sb.append(" GL_ARB_multitexture");
            sb.append(' ').append(GL_EXT_point_parameters);
            return sb.toString();
        }
        return gl.glGetString(name);
    }

    @Override
    public void glHint(int target, int mode) {
        gl.glHint(target, mode);
    }

    @Override
    public void glInterleavedArrays(int format, int stride, FloatBuffer pointer) {
        
        
        if(GL_T2F_V3F == format) {
            glInterleavedArraysT2F_V3F(stride, pointer);
            return;
        }
        throw new GLException("Type not supported: 0x"+Integer.toHexString(format));
    }

    private void glInterleavedArraysT2F_V3F(int byteStride, FloatBuffer buf) {
        int pos = buf.position();
        gl.glTexCoordPointer(2, GL.GL_FLOAT, byteStride, buf);
        gl.glEnableClientState(GL2ES1.GL_TEXTURE_COORD_ARRAY);

        buf.position(pos + 2);
        gl.glVertexPointer(3, GL.GL_FLOAT, byteStride, buf);
        gl.glEnableClientState(GL2ES1.GL_VERTEX_ARRAY);

        buf.position(pos);
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
    public void glOrtho(double left, double right, double bottom, double top, double zNear, double zFar) {
        gl.glOrtho(left, right, bottom, top, zNear, zFar);
    }

    @Override
    public void glPixelStorei(int pname, int param) {
        gl.glPixelStorei(pname, param);
    }

    @Override
    public void glPolygonMode(int face, int mode) {
        if( GL_FRONT_AND_BACK != face || GL_FILL != mode ) { 
            System.err.println("IGNORED: glPolygonMode(0x"+Integer.toHexString(face)+", 0x"+Integer.toHexString(mode)+ ')');
        }
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
    public void glReadPixels(int x, int y, int width, int height, int format, int type, ByteBuffer pixels) {
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
    public void glTexCoordPointer(int size, int stride, FloatBuffer pointer) {
        gl.glTexCoordPointer(size, GL_FLOAT, stride, pointer);
    }

    @Override
    public void glTexEnvi(int target, int pname, int param) {
        gl.glTexEnvi(target, pname, param);
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                             int format, int type, ByteBuffer pixels) {
        switch(internalformat) {
            case 3: internalformat= ( GL.GL_RGBA == format ) ? GL.GL_RGBA : GL.GL_RGB; break;
            case 4: internalformat= ( GL.GL_RGB  == format ) ? GL.GL_RGB  : GL.GL_RGBA; break;
        }
        gl.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                             int format, int type, IntBuffer pixels) {
        switch(internalformat) {
            case 3: internalformat= ( GL.GL_RGBA == format ) ? GL.GL_RGBA : GL.GL_RGB; break;
            case 4: internalformat= ( GL.GL_RGB  == format ) ? GL.GL_RGB  : GL.GL_RGBA; break;
        }
        gl.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
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
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height,
                                int format, int type, IntBuffer pixels) {
        gl.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels);
    }

    @Override
    public void glTranslatef(float x, float y, float z) {
        gl.glTranslatef(x, y, z);
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
    public void glColorTable(int target, int internalFormat, int width, int format, int type, ByteBuffer data) {
        
        System.err.println("IGNORED: glColorTable(0x"+Integer.toHexString(target)+", 0x"+Integer.toHexString(internalFormat)+", ..)");
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
    public void glPointSize(float size) {
        gl.glPointSize(size);
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
        
        System.err.println("IGNORED: glLockArraysEXT(0x"+Integer.toHexString(first)+", 0x"+Integer.toHexString(count)+", ..)");
    }

    @Override
    public void glArrayElement(int index) {
        
        System.err.println("IGNORED: glArrayElement(0x"+Integer.toHexString(index)+ ')');
    }

    @Override
    public void glUnlockArraysEXT() {
        
        System.err.println("IGNORED: glUnlockArraysEXT()");
    }

    @Override
    public void glMultiTexCoord2f(int target, float s, float t) {
        
        System.err.println("IGNORED: glMultiTexCoord2f(0x"+Integer.toHexString(target)+", "+s+", "+t+ ')');
    }

    /*
     * util extensions
     */
    @Override
    public void setSwapInterval(int interval) {
        gl.setSwapInterval(interval);
    }

}
