package spacegraph.video;

import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.curve.opengl.TextRegionUtil;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.geom.SVertex;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.util.PMVMatrix;
import jogamp.graph.font.typecast.TypecastFontConstructor;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.hud.Ortho;
import spacegraph.util.math.v2;

import java.io.File;
import java.io.IOException;

/**
 * http:
 * <p>
 * __ __|_  ___________________________________________________________________________  ___|__ __
 * 
 * 
 * \    \  / /  __|  |     |   __|  _  |     |  _  | | |  __|  |     |   __|  |      /\ \  /    /
 * \____\/_/  |  |  |  |  |  |  |     | | | |   __| | | |  |  |  |  |  |  |  |__   "  \_\/____/
 * /\    \     |_____|_____|_____|__|__|_|_|_|__|    | | |_____|_____|_____|_____|  _  /    /\
 * /  \____\                       http:
 * \  /   "' _________________________________________________________________________ `"   \  /
 * \/____.                                                                             .____\/
 * </pre>
 *
 * <p>
 * JogAmp JOGL OpenGL ES 2 graph text demo to expose and learn how to use the graph API to draw text.
 * <p>
 * Inside the main JOGL source tree we have the "Graph" API that is what we consider
 * the *best* way to render text using nurbs on all GPU's.
 * Graph is using a patent free shaders implementation.
 * Graph is suitable for both desktop and mobile GPU processors.
 * <p>
 * NOTE: This demo is using jogamp.graph.font.fonts.ubuntu is found inside jogl-fonts-p0.jar
 * you may need to add this jar to your classpath
 * http:
 * <p>
 * In a nutshell the JogAmp Graph API enable you to define nurbs shapes
 * Outline → OutlineShapes → GLRegion
 * and then render the shapes using a Renderer
 * RegionRenderer
 * TextRegionUtil (same as RegionRender with Helper methods for texts and fonts.)
 * <p>
 * To load a Font you need to implement your own FontSet
 * The JogAmp JOGL source tree contains two FontSet's
 * One for loading Ubuntu true type fonts bundled with JogAmp
 * One for loading "Java" true type fonts bundled with the JRE
 * http:
 * http:
 * The FontFactory class can be used to load a default JogAmp FontSet Font.
 * <p>
 * The graph API is using the math by Rami Santina introduced in 2011
 * https:
 * https:
 * <p>
 * The best documentation for the graph API is found in the JOGL junit tests
 * http:
 * <p>
 * and javadoc for Outline and OutlineShape .. and all classes i mentioned above..
 * https:
 * https:
 * </p>
 *
 * @author Xerxes Rånby (xranby)
 */

public class JogAmpGraphAPITextDemo {


    public static void main(String[] args) {
        SpaceGraph.window(new GraphText(), 800, 800);
    }

    private static class GraphText extends Surface {

        final float fontSize = 0.1f; 
        final float zNear = 0.1f, zFar = 1f;
        /* 2nd pass texture size antialias SampleCount
           4 is usually enough */
        private final int[] sampleCount = new int[]{
                2
                
        };
        private final int renderModes =
                0;



        TextRegionUtil textRegionUtil;
        RenderState renderState;
        RegionRenderer regionRenderer;







        Font font;
        
        private GL2 gl = null;

        void init() {










            /* load a ttf font */
            try {
                /* JogAmp FontFactory will load a true type font
                 *
                 * fontSet == 0 loads
                 * jogamp.graph.font.fonts.ubuntu found inside jogl-fonts-p0.jar
                 * http:
                 *
                 * fontSet == 1 tries loads LucidaBrightRegular from the JRE.
                 */

                

                File fontfile = new File(
                        
                        
                        "/usr/share/fonts/truetype/hack/Hack-Regular.ttf"
                );

                this.font = new TypecastFontConstructor().create(fontfile);

            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }


            /* initialize OpenGL specific classes that know how to render the graph API shapes */
            renderState = RenderState.createRenderState(SVertex.factory());
            
            renderState.setColorStatic(1.0f, 0.0f, 0.0f, 1.0f);
            renderState.setHintMask(RenderState.BITHINT_GLOBAL_DEPTH_TEST_ENABLED);

            regionRenderer = RegionRenderer.create(renderState, /* GLCallback */ RegionRenderer.defaultBlendEnable, /* GLCallback */ RegionRenderer.defaultBlendDisable);

            textRegionUtil = new TextRegionUtil(renderModes);

            regionRenderer.init(gl, renderModes);
            regionRenderer.enable(gl, false);


        }

        @Override
        public boolean stop() {
            if (super.stop()) {
                
                
                
                renderState.destroy(gl);
                gl = null;
                return true;
            }
            return false;
        }

        @Override
        protected void paint(GL2 gl, SurfaceRender surfaceRender) {

            gl.glColor3f(0.25f, 0.25f, 0.25f); 
            Draw.rect(bounds, gl);

            if (this.gl == null) {
                this.gl = gl;
                init();
                return;
            }
            if (this.font == null)
                return;



            String text = "JogAmp GRAPH API Text demo\nFPS:\nFPS human readable:";

            
            
            











            int pixelGranularity = 1024;
            int iw = pixelGranularity;
            int ih = (int) Math.ceil(h() / w() * pixelGranularity); 
            final PMVMatrix Pmv = regionRenderer.getMatrix();
            

            

            

            

            Pmv.glLoadIdentity();
            Pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            Pmv.glOrthof(0, iw, 0, ih, zNear, zFar);



            

            
            

            v2 scale = ((Ortho) root()).scale; 
            float sx = scale.x * (pixelGranularity);
            float sy = scale.y * (pixelGranularity);
            Pmv.glTranslatef(cx(), cy(), -0.2f);
            Pmv.glScalef(sx, sy, 1f);
            





            
            regionRenderer.enable(gl, true);
            textRegionUtil.drawString3D(gl, regionRenderer, font, fontSize, text, null, sampleCount);
            regionRenderer.enable(gl, false);
            
        }


    }
}