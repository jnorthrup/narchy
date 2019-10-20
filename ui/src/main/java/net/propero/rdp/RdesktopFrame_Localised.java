/* RdesktopFrame_Localised.java
 * Component: ProperJavaRDP
 *
 * Revision: $Revision: #2 $
 * Author: $Author: tvkelley $
 * Date: $Date: 2009/09/15 $
 *
 * Copyright (c) 2005 Propero Limited
 *
 * Purpose: Java 1.4 specific extension of RdesktopFrame class
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA
 *
 * (See gpl.txt for details of the GNU General Public License.)
 *
 */

package net.propero.rdp;

import java.awt.*;

public class RdesktopFrame_Localised extends RdesktopFrame {

    private static final long serialVersionUID = 8966796397221900303L;

    public RdesktopFrame_Localised() {
    }

    protected void fullscreen() {
        setUndecorated(true);
        setExtendedState(Frame.MAXIMIZED_BOTH);
    }

    @Override
    public void goFullScreen() {
        if (!Options.fullscreen)
            return;

        inFullscreen = true;

        if (this.isDisplayable())
            this.dispose();
        this.setVisible(false);
        this.setLocation(0, 0);
        this.setUndecorated(true);
        this.setVisible(true);
        
        
        
        
        
        

        this.pack();
    }

    @Override
    public void leaveFullScreen() {
        if (!Options.fullscreen)
            return;

        inFullscreen = false;

        if (this.isDisplayable())
            this.dispose();

        var env = GraphicsEnvironment
                .getLocalGraphicsEnvironment();
        var myDevice = env.getDefaultScreenDevice();
        if (myDevice.isFullScreenSupported())
            myDevice.setFullScreenWindow(null);

        this.setLocation(10, 10);
        this.setUndecorated(false);
        this.setVisible(true);
        
        this.pack();
    }
}
