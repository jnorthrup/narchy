package nars;

import fr.inria.diversify.utils.sosiefier.InputConfiguration;
import fr.inria.diversify.utils.sosiefier.InputProgram;
import spoon.reflect.declaration.CtType;

import java.util.List;
import java.util.Properties;

class DSpot {

    public static void main(String[] args) throws Exception {
        /*
        #relative path to the project root from dspot project
        project=src/test/resources/test-projects/
        #relative path to the source project from the project properties
        src=src/main/java/
        #relative path to the test source project from the project properties
        testSrc=src/test/java
        #java version used
        javaVersion=8
        #path to the output folder
        outputDirectory=target/trash/
        #Argument string to use when PIT launches child processes. This is most commonly used
        # to increase the amount of memory available to the process,
        # but may be used to pass any valid JVM argument.
        # Use commas to separate multiple arguments, and put them within brackets
        jvmArgs=['-Xmx2048m','-Xms1024m']
         */
//        File project = new File("/home/me/n/nal");
//        File srcDir = new File("src/main/java");
//        File testDir = new File("src/test/java");
//        File classesDir = new File("src/test/java");
        InputConfiguration inputConfiguration = new InputConfiguration();
        Properties prop = inputConfiguration.getProperties();
        prop.setProperty("stat", "true");
        prop.setProperty("project", "/home/me/n");
        //prop.setProperty("targetModule", "nal");
        prop.setProperty("classes", "build/classes");

        prop.setProperty("builder", "gradle");
        prop.setProperty("automaticBuilderName", "gradle");

        System.setProperty("java.version", "1.9"); //HACK
        prop.setProperty("javaVersion", "10");
                //new InputConfiguration(propertiesFilePath);
//            new InputConfiguration(project, srcDir, testDir, classesDir, testClassesDir,
//                    tempDir, filter, mavenHome);

        InputProgram program = new InputProgram();
        inputConfiguration.setInputProgram(program);

        fr.inria.diversify.dspot.DSpot d = new fr.inria.diversify.dspot.DSpot(inputConfiguration);

//        DSpot dspot = new DSpot(
//                InputConfiguration inputConfiguration, // input configuration built at step 1
//        int numberOfIterations, // number of time that the main loop will be applied (-i | --iteration option of the CLI)
//        List<Amplifier> amplifiers, // list of the amplifiers to be used (-a | --amplifiers option of the CLI)
//        TestSelector testSelector // test selector criterion (-s | --test-selector option of the CLI)
//);


//        d.amplifyTest(String regex); // will amplify all test classes that match the given regex
        List<CtType> x = d.amplifyTest("*AnonTest*");
        System.out.println("amp: " + x);
//        d.amplifyTest(String fulQualifiedName, List<String> testCasesName); // will amplify test cases that have their name in testCasesName in the test class fulQualifiedName
        //d.amplifyAllTests(); // will amplify all test in the test suite.
    }





}
