package ptrman.difficultyEnvironment.helper;

import ptrman.difficultyEnvironment.JavascriptDescriptor;

/**
 *
 */
public class JavascriptAccessorHelper {
    public static void resetAndAccessAccessor(JavascriptDescriptor javascriptDescriptor) {
        javascriptDescriptor.engine.resetBindings();
        
        javascriptDescriptor.engine.addBinding("environmentScriptingAccessor", javascriptDescriptor.environmentScriptingAccessor);
        javascriptDescriptor.engine.addBinding("helperScriptingAccessor", javascriptDescriptor.helperScriptingAccessor);
        javascriptDescriptor.engine.addBinding("componentManipulationScriptingAccessor", javascriptDescriptor.componentManipulationScriptingAccessor);
    }
}
