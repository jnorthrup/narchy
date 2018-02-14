package spacegraph.render;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import spacegraph.Surface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static spacegraph.render.JoglPhysics.window;

/** TODO */
public class GLSL extends Surface {

    private ShaderState st;
    private ShaderCode /*vp0, */fp0;

    public static void main(String[] args) {
        window(new GLSL().pos(1, 1, 500, 500), 800, 600);
    }

    private boolean updateUniformVars = true;
    private int vertexShaderProgram;
    private int fragmentShaderProgram;
    private int shaderprogram;

    private float x = -2;
    private float y = -2;
    private float height = 4;
    private float width = 4;
    private int iterations = 1;


    boolean init = false;


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
        //st.setVerbose(true);
//        final ShaderCode vp0 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, this.getClass(), "shader",
//                "shader/bin", "gears", true);
//        final ShaderCode fp0 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, this.getClass(), "shader",
//                "shader/bin", "gears", true);

        CharSequence fsrc = null;
        try {
            fsrc = new StringBuilder(new String(GLSL.class.getClassLoader().getResourceAsStream(
//                    "glsl/metablob.glsl"
                    "glsl/16seg.glsl"

            ).readAllBytes()));
        } catch (IOException e) {
            e.printStackTrace();
        }


        //vp0 = new ShaderCode(GL_VERTEX_SHADER, 1, new CharSequence[][]{{vsrc}});
        fp0 = new ShaderCode(GL_FRAGMENT_SHADER, 1, new CharSequence[][]{{fsrc}});
        //vp0.defaultShaderCustomization(gl, true, true);
        fp0.defaultShaderCustomization(gl, true, true);
        final ShaderProgram sp0 = new ShaderProgram();
        //sp0.add(gl, vp0, System.err);
        sp0.add(gl, fp0, System.err);
        st.attachShaderProgram(gl, sp0, true);
        // Use debug pipeline
        //gl.getContext().addGLDebugListener(new GLDebugMessageHandler.StdErrGLDebugListener(true));
        gl.glFinish(); // make sure .. for shared context (impacts OSX 10.9)

    }
//    private void attachShaders(GL2 gl) throws Exception {
//
//        vertexShaderProgram = gl.glCreateShader(GL_VERTEX_SHADER);
//        gl.glShaderSource(vertexShaderProgram, 1, vsrc, null, 0);
//        gl.glCompileShader(vertexShaderProgram);
//
//        fragmentShaderProgram = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);
//        gl.glShaderSource(fragmentShaderProgram, 1, fsrc, null, 0);
//        gl.glCompileShader(fragmentShaderProgram);
//
//        shaderprogram = gl.glCreateProgram();
//        gl.glAttachShader(shaderprogram, vertexShaderProgram);
//        gl.glAttachShader(shaderprogram, fragmentShaderProgram);
//        gl.glLinkProgram(shaderprogram);
//        gl.glValidateProgram(shaderprogram);
//        IntBuffer intBuffer = IntBuffer.allocate(1);
//        gl.glGetProgramiv(shaderprogram, gl.GL_LINK_STATUS, intBuffer);
//        if (intBuffer.get(0) != 1) {
//            gl.glGetProgramiv(shaderprogram, gl.GL_INFO_LOG_LENGTH, intBuffer);
//            int size = intBuffer.get(0);
//            System.err.println("Program link error: ");
//            if (size > 0) {
//                ByteBuffer byteBuffer = ByteBuffer.allocate(size);
//                gl.glGetProgramInfoLog(shaderprogram, size, intBuffer, byteBuffer);
//                for (byte b : byteBuffer.array()) {
//                    System.err.print((char) b);
//                }
//            } else {
//                System.out.println("Unknown");
//            }
//            System.exit(1);
//        }
//        gl.glUseProgram(shaderprogram);
//    }


    @Override
    public void paint(GL2 gl, int dtMS) {
        Draw.bounds(gl, this, this::doPaint);
    }

    public void doPaint(GL2 gl) {
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

//        if (updateUniformVars) {
//            updateUniformVars(gl);
//        }

        gl.glEnable(GL2.GL_TEXTURE);

        gl.glColor3f(1f,1f,1f);
        Draw.rect(gl, -1, -1, 1, 1);

        st.useProgram(gl, true);



        gl.glBegin(GL2.GL_QUADS);
        {
            gl.glTexCoord2f(0.0f, 800.0f);
            gl.glVertex3f(0.0f, 1.0f, 1.0f);  // Top Left
            gl.glTexCoord2f(800.0f, 800.0f);
            gl.glVertex3f(1.0f, 1.0f, 1.0f);   // Top Right
            gl.glTexCoord2f(800.0f, 0.0f);
            gl.glVertex3f(1.0f, 0.0f, 1.0f);  // Bottom Right
            gl.glTexCoord2f(0.0f, 0.0f);
            gl.glVertex3f(0.0f, 0.0f, 1.0f); // Bottom Left
        }
        gl.glEnd();
        // Draw A Quad
//        gl.glBegin(GL2.GL_QUADS);
//        {
//            gl.glTexCoord2f(0.0f, 1.0f);
//            gl.glVertex3f(0.0f, 1.0f, 1.0f);  // Top Left
//            gl.glTexCoord2f(1.0f, 1.0f);
//            gl.glVertex3f(1.0f, 1.0f, 1.0f);   // Top Right
//            gl.glTexCoord2f(1.0f, 0.0f);
//            gl.glVertex3f(1.0f, 0.0f, 1.0f);  // Bottom Right
//            gl.glTexCoord2f(0.0f, 0.0f);
//            gl.glVertex3f(0.0f, 0.0f, 1.0f); // Bottom Left
//        }
        // Done Drawing The Quad
//        gl.glEnd();

        st.useProgram(gl, false);

    }

//    private void updateUniformVars(GL2 gl) {
//        int mandel_x = gl.glGetUniformLocation(shaderprogram, "mandel_x");
//        int mandel_y = gl.glGetUniformLocation(shaderprogram, "mandel_y");
//        int mandel_width = gl.glGetUniformLocation(shaderprogram, "mandel_width");
//        int mandel_height = gl.glGetUniformLocation(shaderprogram, "mandel_height");
//        int mandel_iterations = gl.glGetUniformLocation(shaderprogram, "mandel_iterations");
//        assert (mandel_x != -1);
//        assert (mandel_y != -1);
//        assert (mandel_width != -1);
//        assert (mandel_height != -1);
//        assert (mandel_iterations != -1);
//
//        gl.glUniform1f(mandel_x, x);
//        gl.glUniform1f(mandel_y, y);
//        gl.glUniform1f(mandel_width, width);
//        gl.glUniform1f(mandel_height, height);
//        gl.glUniform1f(mandel_iterations, iterations);
//
//    }

}