/*******************************************************************************
 * Copyright (c) 2016 comtel inc.
 *
 * Licensed under the Apache License, version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at:
 *
 * http:
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package nars.video;

import nars.GameX;
import nars.NAR;
import nars.game.Game;
import nars.game.GameTime;
import net.propero.rdp.Rdesktop;
import net.propero.rdp.RdesktopException;
import net.propero.rdp.RdesktopFrame;
import org.slf4j.LoggerFactory;

import static nars.$.$$;


/**
 * Remote Desktop Protocol
 */
public class RDP extends Game {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(RDP.class);

    public RDP(NAR n, String host, int port) throws RdesktopException {
        super($$("rdp(\"" + host + "\", " + port + ')'), GameTime.durs(1.0F));
        RdesktopFrame w = Rdesktop.RDPwindow(host + ':' + port);

        //senseCameraRetina(("video"), ()->w.canvas.backstore.getBufferedImage(), 64, 64);

    }

    public static void main(String[] args) {
        GameX.Companion.initFn(16f, (n)->{
            try {
                return new RDP(n,
                        //"localhost"
                        "10.0.0.249"
                        , 3389) {


                };
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }



































































































































}
