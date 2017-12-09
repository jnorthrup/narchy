/*******************************************************************************
 * Copyright (c) 2016 comtel inc.
 *
 * Licensed under the Apache License, version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package nars.op.video;

import nars.NAR;
import nars.NAgentX;
import nars.Narsese;
import net.propero.rdp.Rdesktop;
import net.propero.rdp.RdesktopException;
import net.propero.rdp.RdesktopFrame;
import org.slf4j.LoggerFactory;


/**
 * Remote Desktop Protocol
 */
public class RDP extends NAgentX {

    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(RDP.class);

    public RDP(NAR n, String host, int port) throws RdesktopException, Narsese.NarseseException {
        super("rdp(\"" + host + "\", " + port + ")", n);
        RdesktopFrame w = Rdesktop.RDPwindow(host + ":" + port);

        senseCameraRetina(("video"), ()->w.canvas.backstore.getBufferedImage(), 64, 64);

    }

    public static void main(String[] args) {
        NAgentX.runRT((n)->{
            try {
                return new RDP(n, "localhost", 3389);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }, 16f);
    }

//    public void newFXWindow() {
//        FX.run(()->{
//            final VncImageView imageView = new VncImageView() {
//
//                @Override
//                protected void onFired(KeyButtonEvent msg) {
//                    System.out.println("KEY: " + msg);
//                }
//            };
//
//            //vncService.inputEventListenerProperty().addListener(l -> );
//
//
//            renderer.vncImage.addListener((images, prev, next)->{
//                if (next!=null)
//                    imageView.setImage(next);
//            });
//
//            imageView.setImage(image);
//
////            imageView.setScaleX(2);
////            imageView.setScaleY(2);
//
//            FX.newWindow("x", new BorderPane(scrolled((imageView))));
//
//            //imageView.registerInputEventListener(vncService.inputEventListenerProperty().get());
//
//        });
//    }


////        //TODO
////        int pw = 200;
////        int ph = 200;
////        return new MatrixView(pw, ph, (x, y, gl) -> {
////            final PixelReader reader = this.vncReader;
////            if (reader !=null) {
////                int argb = reader.getArgb(x, y);
////                gl.glColor3f(decodeRed(argb), decodeGreen(argb), decodeBlue(argb));
////            }
////            return 0;
////        });
//
//        return new Surface() {
//
//            Texture texture;
//
//            @Override
//            protected void paint(GL2 gl) {
//
//                //VideoBag.Frame f = ff.get();
//
//                Texture tt = texture;
//                if (tt!=null) {
//                    tt.destroy(gl);
//                    tt = null;
//                }
//
//                if (vncReader!=null) {
//
//
//
//                    int ww = (int)renderer.vncImage.get().getWidth();
//                    int hh = (int)renderer.vncImage.get().getHeight();
//                    byte[] data = new byte[ww * hh * 4];
//                    vncReader.getPixels(0,0,ww,hh, PixelFormat.getByteBgraInstance(), data, 0, ww*4);
//
//                    tt = tgaTexture(ww, hh, true, data);
//
//                    texture = tt;
//
//                }
//
//                float xx = 0, yy = 0;
//                float sc = 1f;
//                if (tt!=null) {
//                    Draw.rectTex(gl, tt, xx, yy, sc, sc, 0);
//                } else {
//                    Draw.colorBipolar(gl, 0.5f);
//                    Draw.rect(gl, xx, yy, sc, sc);
//                }
//            }
//        };
//    }
//
//    public PixelBag newSensor(int pw, int ph) {
//        return new PixelBag(pw, ph) {
//
//            @Override public int sw() { return image==null ? 0 : (int) image.getWidth(); }
//
//            @Override public int sh() { return image==null ? 0 : (int) image.getHeight(); }
//
//            @Override public int rgb(int sx, int sy) { return vncReader.getArgb(sx, sy); }
//        };
//    }

    @Override
    protected float act() {
        return 0;
    }

//    public static void main(String[] args) throws RdesktopException {
//        RDP v = new RDP("localhost", 3389);
//
//        //1.
//        //v.newFXWindow();
//
//        //2.
//        //SpaceGraph.window(v.newSurface(), 800, 800);
//
////        Default nar = NAgents.newMultiThreadNAR(2, new RealTime.CS(true).dur(0.25f));
////
////        NAgents a = new NAgents(nar) {
////
////            {
////                PixelBag pb = v.newSensor(64, 64);
////                pb.addActions("vnc", this);
////
////                addCamera("vnc", pb, (v) -> t(v, alpha));
////            }
////
////            @Override
////            protected float act() {
////                return 0;
////            }
////
////
////        };
////        NAgents.chart(a);
////
////        a.runRT(55f);
//
//
//    }



}
