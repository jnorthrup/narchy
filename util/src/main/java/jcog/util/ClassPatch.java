package jcog.util;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.StubMethod;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;

/** an patching/mocking/extending classloader which can be bootstrapped into
 *  by invoking the resulting classes in embedded context
 *  http://bytebuddy.net/#/tutorial
 *  */
public class ClassPatch {

    public static void main(String[] args) throws Exception {

        //ClassPath.from(Thread.currentThread().getContextClassLoader()).getAllClasses()

        ClassLoader parentClassLoader =
                //ClassLoader.getSystemClassLoader().getParent();
                Thread.currentThread().getContextClassLoader();

//               String className = generatedClassName(conventionBasedTest);
//        String methodName = generatedMethodName(conventionBasedTest);
//        TestAnnotationImpl testAnnotationImpl = new TestAnnotationImpl(conventionBasedTest);


        String entryClass = ClassPatch.class.getName();
        Map<String, byte[]> overrides = Map.of(
            entryClass,
                new ByteBuddy().with(TypeValidation.DISABLED)
                //.subclass(Virtual.class)
                .redefine(ClassPatch.class)
                .name(ClassPatch.class.getName())
                .defineMethod("main2", void.class, Modifier.STATIC | Modifier.PUBLIC)
                        .withParameters(new Type[] { String[].class })
                        .intercept(StubMethod.INSTANCE)

                        //.withoutCode()
                        //.intercept(MethodCall.invoke(ConventionBasedTestProxy.class.getMethod("test")))
                        //.annotateMethod(testAnnotationImpl)
                .make().getBytes()
        );

        ClassLoader classloader =
                new ByteArrayClassLoader.ChildFirst(parentClassLoader, overrides);

        {
            Thread.currentThread().setContextClassLoader(classloader);

            Class mainClass = classloader.loadClass(entryClass);

            System.out.println(Arrays.toString(mainClass.getMethods()));

            Method main = mainClass.getMethod("main2", String[].class);

            main.invoke(null, new Object[] {ArrayUtils.EMPTY_STRING_ARRAY});
        }

    }

}