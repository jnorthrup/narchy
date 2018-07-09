package spacegraph.video;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import spacegraph.SpaceGraph;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;

/** TODO */
public class GLSL extends Surface {

    private ShaderState st;
    private ShaderCode /*vp0, */fp0;

    public static void main(String[] args) {
        SpaceGraph.window(new GLSL().pos(1, 1, 500, 500), 800, 600);
    }

    private final boolean updateUniformVars = true;
    private int vertexShaderProgram;
    private int fragmentShaderProgram;
    private int shaderprogram;

    private final float x = -2;
    private final float y = -2;
    private final float height = 4;
    private final float width = 4;
    private final int iterations = 1;


    private boolean init = false;


    private String[] loadShaderSrc(String name) {
        StringBuilder sb = new StringBuilder();
        try {
            InputStream is = getClass().getResourceAsStream(name);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Shader is " + sb);
        return new String[]{sb.toString()};
    }


    private void initShaders(GL2 gl) {
        if (!gl.hasGLSL()) {
            System.err.println("No GLSL available, no rendering.");
            return;
        }

        st = new ShaderState();
        





        CharSequence fsrc = null;
        try {
            fsrc = new StringBuilder(new String(GLSL.class.getClassLoader().getResourceAsStream(

                    "glsl/16seg.glsl"

            ).readAllBytes()));
        } catch (IOException e) {
            e.printStackTrace();
        }


        
        fp0 = new ShaderCode(GL_FRAGMENT_SHADER, 1, new CharSequence[][]{{fsrc}});
        
        fp0.defaultShaderCustomization(gl, true, true);
        final ShaderProgram sp0 = new ShaderProgram();
        
        sp0.add(gl, fp0, System.err);
        st.attachShaderProgram(gl, sp0, true);
        
        
        gl.glFinish(); 

    }




































    @Override
    public void paint(GL2 gl, SurfaceRender surfaceRender) {
        Draw.bounds(gl, this, this::doPaint);
    }

    private void doPaint(GL2 gl) {
        if (!gl.hasGLSL()) {
            return;
        }

        if (!init) {
            try {
                initShaders(gl);
            } catch (Exception e) {
                e.printStackTrace();
            }
            init = true;
        }





        gl.glEnable(GL2.GL_TEXTURE);

        gl.glColor3f(1f,1f,1f);
        Draw.rect(gl, -1, -1, 1, 1);

        st.useProgram(gl, true);



        gl.glBegin(GL2.GL_QUADS);
        {
            gl.glTexCoord2f(0.0f, 800.0f);
            gl.glVertex3f(0.0f, 1.0f, 1.0f);  
            gl.glTexCoord2f(800.0f, 800.0f);
            gl.glVertex3f(1.0f, 1.0f, 1.0f);   
            gl.glTexCoord2f(800.0f, 0.0f);
            gl.glVertex3f(1.0f, 0.0f, 1.0f);  
            gl.glTexCoord2f(0.0f, 0.0f);
            gl.glVertex3f(0.0f, 0.0f, 1.0f); 
        }
        gl.glEnd();
        











        


        st.useProgram(gl, false);

    }





















}