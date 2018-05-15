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
import spacegraph.space2d.hud.Ortho;
import spacegraph.util.math.v2;

import java.io.File;
import java.io.IOException;

/**
 * http://forum.jogamp.org/Text-without-AWT-td4037684.html
 * <p>
 * __ __|_  ___________________________________________________________________________  ___|__ __
 * //    /\                                           _                                  /\    \\
 * //____/  \__     __ _____ _____ _____ _____ _____  | |     __ _____ _____ __        __/  \____\\
 * \    \  / /  __|  |     |   __|  _  |     |  _  | | |  __|  |     |   __|  |      /\ \  /    /
 * \____\/_/  |  |  |  |  |  |  |     | | | |   __| | | |  |  |  |  |  |  |  |__   "  \_\/____/
 * /\    \     |_____|_____|_____|__|__|_|_|_|__|    | | |_____|_____|_____|_____|  _  /    /\
 * /  \____\                       http://jogamp.org  |_|                              /____/  \
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
 * http://jogamp.org/deployment/jogamp-current/jar/atomic/jogl-fonts-p0.jar
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
 * http://jogamp.org/git/?p=jogl.git;a=blob;f=src/jogl/classes/jogamp/graph/font/UbuntuFontLoader.java;hb=HEAD
 * http://jogamp.org/git/?p=jogl.git;a=blob;f=src/jogl/classes/jogamp/graph/font/JavaFontLoader.java;hb=HEAD
 * The FontFactory class can be used to load a default JogAmp FontSet Font.
 * <p>
 * The graph API is using the math by Rami Santina introduced in 2011
 * https://jogamp.org/doc/gpunurbs2011/p70-santina.pdf
 * https://jogamp.org/doc/gpunurbs2011/graphicon2011-slides.pdf
 * <p>
 * The best documentation for the graph API is found in the JOGL junit tests
 * http://jogamp.org/git/?p=jogl.git;a=tree;f=src/test/com/jogamp/opengl/test/junit/graph;hb=HEAD
 * <p>
 * and javadoc for Outline and OutlineShape .. and all classes i mentioned above..
 * https://www.jogamp.org/deployment/jogamp-next/javadoc/jogl/javadoc/com/jogamp/graph/geom/Outline.html
 * https://www.jogamp.org/deployment/jogamp-next/javadoc/jogl/javadoc/com/jogamp/graph/curve/OutlineShape.html
 * </p>
 *
 * @author Xerxes Rånby (xranby)
 */

public class JogAmpGraphAPITextDemo {


    public static void main(String[] args) {
        SpaceGraph.window(new GraphText(), 800, 800);
    }

    private static class GraphText extends Surface {

        final float fontSize = 0.1f; //1.0f;
        final float zNear = 0.1f, zFar = 1f;
        /* 2nd pass texture size antialias SampleCount
           4 is usually enough */
        private final int[] sampleCount = new int[]{
                2
                //4
        };
        private final int renderModes =
                0;


//        volatile float weight = 1.0f;
        TextRegionUtil textRegionUtil;
        RenderState renderState;
        RegionRenderer regionRenderer;

//        /* variables used to update the PMVMatrix before rendering */
//        private final float xTranslate = 0f;
//        private final float yTranslate = 0f;
//        private final float zTranslate =
//                //-5;
//                -1f;
        Font font;
        //Region.VARWEIGHT_RENDERING_BIT;
        private GL2 gl = null;

        void init() {

//            /* SwapInterval 1 makes the demo run at 60 fps
//            use SwapInterval 0 here to get ... hundreds ... sometimes thousands of fps!
//            SwapInterval 0 can cause stuttering due to thermal throttling of your GPU */
//            gl.setSwapInterval(1);
//
//            gl.glEnable(GL.GL_DEPTH_TEST);
//            gl.glEnable(GL.GL_BLEND);
//            gl.glClearColor(1.0f, 0.0f, 1.0f, 1.0f);

            /* load a ttf font */
            try {
                /* JogAmp FontFactory will load a true type font
                 *
                 * fontSet == 0 loads
                 * jogamp.graph.font.fonts.ubuntu found inside jogl-fonts-p0.jar
                 * http://jogamp.org/deployment/jogamp-current/jar/atomic/jogl-fonts-p0.jar
                 *
                 * fontSet == 1 tries loads LucidaBrightRegular from the JRE.
                 */

                //font = FontFactory.get(fontSet).get(fontFamily, fontStyleBits);

                File fontfile = new File(
                        //"/usr/share/fonts/opentype/linux-libertine/LinLibertine_M.otf"
                        //"/usr/share/fonts/opentype/noto/NotoSerifCJK-Regular.ttc"
                        "/usr/share/fonts/truetype/hack/Hack-Regular.ttf"
                );

                this.font = new TypecastFontConstructor().create(fontfile);//FontFactory.getDefault(); //get(fontSet).getDefault();

            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }


            /* initialize OpenGL specific classes that know how to render the graph API shapes */
            renderState = RenderState.createRenderState(SVertex.factory());
            // define a RED colour to render our shape with
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
                //stop the animator thread when user close the window
                // it is important to free memory allocated no the GPU!
                // this memory cant be garbage collected by the JVM
                renderState.destroy(gl);
                gl = null;
                return true;
            }
            return false;
        }

        @Override
        protected void paint(GL2 gl, int dtMS) {

            gl.glColor3f(0.25f, 0.25f, 0.25f); //background
            Draw.rect(gl, bounds);

            if (this.gl == null) {
                this.gl = gl;
                init();
                return;
            }
            if (this.font == null)
                return;

//            float sinusAnimationRotate = (float) (Math.sin(time/1000000f));

            String text = "JogAmp GRAPH API Text demo\nFPS:\nFPS human readable:";

            // clear screen
            //gl.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
            //gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

//            float offsetX = 0;
//            float offsetY = 0;
//
//            // When rendering text we need to account for newlines inside the text.
//            final int newLineCount = TextRegionUtil.getCharCount(text, '\n');
//
//            final float lineHeight = font.getLineHeight(fontSize);
//            offsetX += font.getAdvanceWidth('X', fontSize);
//            offsetY -= lineHeight * newLineCount;

            int pixelGranularity = 1024;
            int iw = pixelGranularity;
            int ih = (int) Math.ceil(h() / w() * pixelGranularity); //aspect
            final PMVMatrix Pmv = regionRenderer.getMatrix();
            //Pmv.glPushMatrix();

            //regionRenderer.reshapePerspective(45.0f, iw, ih, zNear, zFar);

            //p.glMatrixMode(GLMatrixFunc.GL_PROJECTION);

            //regionRenderer.reshapeOrtho(iw, ih, zNear, zFar);

            Pmv.glLoadIdentity();
            Pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            Pmv.glOrthof(0, iw, 0, ih, zNear, zFar);



            // the RegionRenderer PMVMatrix define where we want to render our shape

            //Pmv.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            //Pmv.glLoadIdentity();

            v2 scale = ((Ortho) root()).scale; //HACK
            float sx = scale.x * (pixelGranularity);
            float sy = scale.y * (pixelGranularity);
            Pmv.glTranslatef(cx(), cy(), -0.2f);
            Pmv.glScalef(sx, sy, 1f);
            //Pmv.glRotatef(angleRotate+ 10f * sinusAnimationRotate, 0, 0, 1);

//            if( weight != regionRenderer.getRenderState().getWeight() ) {
//                regionRenderer.getRenderState().setWeight(weight);
//            }

            // Draw the  shape using RegionRenderer and TextRegionUtil
            regionRenderer.enable(gl, true);
            textRegionUtil.drawString3D(gl, regionRenderer, font, fontSize, text, null, sampleCount);
            regionRenderer.enable(gl, false);
            //Pmv.glPopMatrix();
        }


    }
}