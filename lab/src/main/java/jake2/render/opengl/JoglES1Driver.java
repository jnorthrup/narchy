/*
 * JoglDriver.java
 * Copyright (C) 2004
 * 
 */
/*
Copyright (C) 1997-2001 Id Software, Inc.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

 */

package jake2.render.opengl;

import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.newt.MonitorMode;
import com.jogamp.opengl.GLProfile;
import jake2.game.cvar_t;
import jake2.qcommon.Cvar;
import jake2.qcommon.xcommand_t;
import jake2.render.Base;

import java.util.List;

/**
 * JoglCommon
 */
public abstract class JoglES1Driver extends JoglGL2ES1 implements GLDriver {

    protected static final GLProfile glp;    
    static {

        var v = Cvar.Get("jogl_gl2es1", "0", 0);
        if( v.value != 0f ) {
            glp = GLProfile.getGL2ES1();
        } else {
            glp = GLProfile.get(GLProfile.GLES1);
        }
    }
    
    protected JoglES1Driver() {
        super();
    }

    private NEWTWin newtWin;

    public abstract String getName();
    
    @Override
    public List<MonitorMode> getModeList() {
        if(null == newtWin) {
            throw new RuntimeException("NEWTWin not yet initialized.");
        }
        return newtWin.getModeList();        
    }
    
    @Override
    public int setMode(Dimension dim, int mode, boolean fullscreen) {
        if(null == newtWin) {
            newtWin = new NEWTWin();
        }
        var res = newtWin.setMode(glp, dim, mode, fullscreen, getName());
        if( Base.rserr_ok == res ) {
            setGL(newtWin.window.getGL().getGL2ES1());
            init(0, 0);
            
            return Base.rserr_ok;
        }
        return res;
    }

    @Override
    public void shutdown() {
        if(null != newtWin) {
            newtWin.shutdown();
        }
    }

    /**
     * @return true
     */
    @Override
    public boolean init(int xpos, int ypos) {
        
        
        beginFrame(0.0f);
        glViewport(0, 0, newtWin.window.getWidth(), newtWin.window.getHeight());
        glClearColor(0, 0, 0, 0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        endFrame();
        
        beginFrame(0.0f);
        glViewport(0, 0, newtWin.window.getWidth(), newtWin.window.getHeight());
        glClearColor(0, 0, 0, 0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        endFrame();
        return true;
    }

    @Override
    public boolean beginFrame(float camera_separation) {
        return activateGLContext(false);
    }

    @Override
    public void endFrame() {
        newtWin.endFrame();
        
    }

    @Override
    public void appActivate(boolean activate) {
        
    }

    @Override
    public void enableLogging(boolean enable) {
        
    }

    @Override
    public void logNewFrame() {
        
    }

    /*
     * @see jake2.client.refexport_t#updateScreen()
     */
    @Override
    public void updateScreen(xcommand_t callback) {
        callback.execute();
    }

    protected final boolean activateGLContext(boolean force) {
        return newtWin.activateGLContext(force);        
    }

    protected final void deactivateGLContext() {
        newtWin.deactivateGLContext();        
    }
    
    
}
