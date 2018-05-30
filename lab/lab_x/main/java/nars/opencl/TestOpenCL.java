package nars.opencl;

import com.jogamp.opencl.*;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Random;

import static com.jogamp.opencl.CLMemory.Mem.READ_ONLY;
import static com.jogamp.opencl.CLMemory.Mem.WRITE_ONLY;
import static java.lang.System.nanoTime;
import static java.lang.System.out;

/**
 * Created by me on 5/29/15.
 */
public class TestOpenCL {
    public static void main(String[] args) throws IOException {

        
        CLContext context = CLContext.create();
        out.println("created " + context);

        
        


        
        CLDevice device = context.getMaxFlopsDevice();
        out.println("using " + device);

        
        CLCommandQueue queue = device.createCommandQueue();

        int elementCount = 1444477;                                  
        int localWorkSize = min(device.getMaxWorkGroupSize(), 256);  
        int globalWorkSize = roundUp(localWorkSize, elementCount);   

        
        CLProgram program = context.createProgram(TestOpenCL.class.getResourceAsStream("VectorAdd.cl")).build();
        /*CLProgram program = context.createProgram(
                
            "    kernel void VectorAdd(global const float* a, global const float* b, global float* c, int numElements) {\n" +
            "        
            "        int iGID = get_global_id(0);\n" +
            "        
            "        if (iGID >= numElements)  {\n" +
            "            return;\n" +
            "        }\n" +
            "        
            "        c[iGID] = a[iGID] + b[iGID];\n" +
            "    }\n"
        ).builder();*/

        
        CLBuffer<FloatBuffer> clBufferA = context.createFloatBuffer(globalWorkSize, READ_ONLY);
        CLBuffer<FloatBuffer> clBufferB = context.createFloatBuffer(globalWorkSize, READ_ONLY);
        CLBuffer<FloatBuffer> clBufferC = context.createFloatBuffer(globalWorkSize, WRITE_ONLY);

        out.println("used device memory: "
                + (clBufferA.getCLSize() + clBufferB.getCLSize() + clBufferC.getCLSize()) / 1000000 + "MB");

        
        
        fillBuffer(clBufferA.getBuffer(), 12345);
        fillBuffer(clBufferB.getBuffer(), 67890);

        
        
        CLKernel kernel = program.createCLKernel("VectorAdd");
        kernel.putArgs(clBufferA, clBufferB, clBufferC).putArg(elementCount);

        
        
        long time = nanoTime();
        queue.putWriteBuffer(clBufferA, false)
                .putWriteBuffer(clBufferB, false)
                .put1DRangeKernel(kernel, 0, globalWorkSize, localWorkSize)
                .putReadBuffer(clBufferC, true);
        time = nanoTime() - time;

        
        out.println("a+b=c results snapshot: ");
        for (int i = 0; i < 10; i++)
            out.print(clBufferC.getBuffer().get() + ", ");
        out.println("...; " + clBufferC.getBuffer().remaining() + " more");

        out.println("computation took: " + (time / 1000000.0) + "ms");


        
        context.release();


    }

    private static void fillBuffer(FloatBuffer buffer, int seed) {
        Random rnd = new Random(seed);
        while (buffer.remaining() != 0)
            buffer.put(rnd.nextFloat() * 100);
        buffer.rewind();
    }

    private static int roundUp(int groupSize, int globalSize) {
        int r = globalSize % groupSize;
        if (r == 0) {
            return globalSize;
        } else {
            return globalSize + groupSize - r;
        }
    }
}
