package ptrman.causal;

import java.util.ArrayList;

public class ConvertInputGraphToCausalGraph
{
    public static DecoratedCausalGraph convert(InputGraph input)
    {
        DecoratedCausalGraph resultGraph;
        int[] numberOfInputEdges;
        int[] numberOfOutputEdges;
        int nodeIndex;
        
        
        int[] incommingEdgesIndices;
        int[] outgoingEdgesIndices;
        
        numberOfInputEdges = new int[input.numberOfNodes];
        numberOfOutputEdges = new int[input.numberOfNodes];
        
        resultGraph = new DecoratedCausalGraph();
        resultGraph.nodes = createNodes(input.numberOfNodes);
        
        
        for( InputGraph.Connection iterationConnection : input.connections )
        {
            numberOfOutputEdges[iterationConnection.sourceIndex]++;
            numberOfInputEdges[iterationConnection.destinationIndex]++;
        }
        
        
        nodeIndex = 0;
        for( DecoratedCausalGraph.Node iterationNode : resultGraph.nodes )
        {
            iterationNode.incommingEdgeElementIndices = new int[numberOfInputEdges[nodeIndex]];
            iterationNode.outgoingEdgeElementIndices = new int[numberOfOutputEdges[nodeIndex]];
            
            nodeIndex++;
        }
        
        
        incommingEdgesIndices = new int[input.numberOfNodes];
        outgoingEdgesIndices = new int[input.numberOfNodes];
        
        for( InputGraph.Connection iterationConnection : input.connections )
        {
            int incommingArrayIndex;
            int outgoingArrayIndex;
            
            incommingArrayIndex = incommingEdgesIndices[iterationConnection.destinationIndex];
            outgoingArrayIndex = outgoingEdgesIndices[iterationConnection.sourceIndex];
            
            resultGraph.nodes.get(iterationConnection.destinationIndex).incommingEdgeElementIndices[incommingArrayIndex] = iterationConnection.sourceIndex;
            resultGraph.nodes.get(iterationConnection.sourceIndex).outgoingEdgeElementIndices[outgoingArrayIndex] = iterationConnection.destinationIndex;
            
            incommingEdgesIndices[iterationConnection.destinationIndex]++;
            outgoingEdgesIndices[iterationConnection.sourceIndex]++;
        }
        
        return resultGraph;
    }
    
    private static ArrayList<DecoratedCausalGraph.Node> createNodes(int count)
    {
        ArrayList<DecoratedCausalGraph.Node> result;
        int i;
        
        result = new ArrayList<DecoratedCausalGraph.Node>();
        
        for( i = 0; i < count; i++ )
        {
            result.add(new DecoratedCausalGraph.Node());
        }
        
        return result;
    }
}
