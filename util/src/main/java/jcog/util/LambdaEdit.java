package jcog.util;

import net.bytebuddy.jar.asm.ClassReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.function.Supplier;

import static jdk.nashorn.internal.objects.Global.print;

/**
 * experiments for dynamic lambda mutation
 * https://stackoverflow.com/questions/23861619/how-to-read-lambda-expression-bytecode-using-asm
 */
public class LambdaEdit {
    static SerializedLambda getSerializedLambda(Serializable lambda) throws Exception {
        final Method method = lambda.getClass().getDeclaredMethod("writeReplace");
        method.setAccessible(true);
        return (SerializedLambda) method.invoke(lambda);
    }

    static byte[] classByteCode(Class<?> c) {
        //in the following - c.getResourceAsStream will return null..
        String n = c.getName();
        return classByteCode(n);
    }

    static byte[] classByteCode(String n) {
        String name =  "./jcog/util/" + n.replace('.', '/') + ".class";
        try {
            File[] l = new File(LambdaEdit.class.getResource(".").toURI()).listFiles();
            System.out.println(Arrays.toString(l));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        try (InputStream input = LambdaEdit.class.getResourceAsStream(name)) {
            if (input == null)
                return null;

            byte[] result = new byte[input.available()];
            input.read(result);
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    protected final byte[] loadOriginalBytecode(String originalResources, String name) throws IOException {
        try (InputStream is = getResourceStream(originalResources + name + ".class")) {
            return is.readAllBytes();
        }
    }
    private InputStream getResourceStream(String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }

    final Supplier<Integer> MY_LAMBDA = (Supplier<Integer>&Serializable)() -> 1;

    {
        SerializedLambda sl = null;
        try {
            sl = getSerializedLambda((Serializable) MY_LAMBDA);
            byte[] bc1 = classByteCode(sl.getImplMethodName());
            byte[] bc2 = classByteCode(sl.getImplClass());

            byte[] bytecode = classByteCode(MY_LAMBDA.getClass()); //this is the method that we need to create.
            ClassReader reader = new ClassReader(bytecode);
            print(reader);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) throws Exception {

        new LambdaEdit();
//        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
//        ObjectOutputStream o = new ObjectOutputStream(baos);
//        o.writeObject(sl);
//        o.close();
//        byte[] cl = baos.toByteArray();

    }

//    private static final Instrumentation instrumentation = new ByteBuddyAgent...;
//
//    byte[] getByteCodeOf(Class<?> c) throws IOException {
//        ClassFileLocator locator = ClassFileLocator.AgentBased.of(instrumentation, c);
//        TypeDescription.ForLoadedType desc = new TypeDescription.ForLoadedType(c);
//        ClassFileLocator.Resolution resolution = locator.locate(desc.getName());
//        return resolution.resolve();
//    }

//    public static class Sample {
//        public static void main(String... args) {
//            SerializableRunnable oneWay = () -> System.out.println("I am a serializable lambda");
//
//            Runnable anotherWay = (Serializable & Runnable) () -> System.out.println("I am a serializable lambda too!");
//        }
//
//        interface SerializableRunnable extends Runnable, Serializable {
//        }
//    }

    public static Method getLambdaMethod(SerializedLambda lambda) {
        try {
            String implClassName = lambda.getImplClass().replace('/', '.');
            Class<?> implClass = Class.forName(implClassName);

            String lambdaName = lambda.getImplMethodName();

            for (Method m : implClass.getDeclaredMethods()) {
                if (m.getName().equals(lambdaName)) {
                    return m;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        throw new RuntimeException("Lambda Method not found");
    }
}
