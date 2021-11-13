package NetworkBuilder;

import org.gephi.appearance.api.AppearanceController;
import org.gephi.appearance.api.AppearanceModel;
import org.gephi.graph.api.*;
import org.gephi.io.importer.api.ImportController;
import org.gephi.layout.plugin.AutoLayout;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;


public class buildNetwork {
    private GraphModel graphModel;
    private DirectedGraph directedGraph;
    private ProjectController pc;
    private Workspace workspace;

    public static void main(String[] args) {
        try {
            buildNetwork test = new buildNetwork();
            test.process("test", "1", "test2", "2");
            test.process("3", "3", "40", "40");
            test.process("40", "40", "5", "5");
            test.process("Dept", "0010A0", "Finance Inc", "001J7AAK");
            test.process("Dept", "0010A0", "Healthcare Services", "0013AAE");
            test.process("Dept", "0010A0", "Healthcare Services", "0013AAE");
            Stream output = test.endPartition();

            output.forEach(s -> System.out.println(s));
        }
        catch   (Exception ex)
        {
            System.out.println(ex);
        }

    }
    public buildNetwork()
    {
        //generate the workspace which is necessary
        pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.newProject();
        workspace = pc.getCurrentWorkspace();

        //Get controllers and models
        graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
        directedGraph = graphModel.getDirectedGraph();

    }
    public static Class getOutputClass() {
        return OutputRow.class;
    }

    public Stream<OutputRow> process(String pName, String pId, String cName, String cId) throws Exception {
        try {
            //check that we have data
            if (pId != null && cId != null && pName != null && cId != null) {
                //check if we have the node already
                Node n0 = directedGraph.getNode(pId);
                if (n0 == null) { //if not create a new node
                    n0 = graphModel.factory().newNode(pId);
                    n0.setLabel(pName);
                    //VERY IMPORTANT: initialize with random position. Otherwise, if all nodes get x=0,y=0 coordinates, layouts won't work (known bug) https://github.com/gephi/gephi/issues/1698
                    float x = (float) (((0.01 + Math.random()) * 1000) - 500);
                    float y = (float) (((0.01 + Math.random()) * 1000) - 500);
                    n0.setPosition(x, y);
                    directedGraph.addNode(n0);
                }

                Node n1 = directedGraph.getNode(cId);
                if (n1 == null) {
                    n1 = graphModel.factory().newNode(cId);
                    n1.setLabel(cName);
                    float x = (float) (((0.01 + Math.random()) * 1000) - 500);
                    float y = (float) (((0.01 + Math.random()) * 1000) - 500);
                    n1.setPosition(x, y);
                    directedGraph.addNode(n1);
                }
                //Create an edge - undirected and weight 1
                Edge e1 = graphModel.factory().newEdge(n0, n1, 0, true);

                //Append as a Directed Graph
                directedGraph.addEdge(e1);
            }
        }
        catch (Exception ex)
        {
            Exception nex = new Exception("pName="+pName+",pId="+pId+"cName="+cName+"cID="+cId);
            nex.setStackTrace(ex.getStackTrace());
            throw nex;
        }
        //return nothing. We only load up the nodes into the graph
        return Stream.of();
    }

    //This executes after processing all rows
    public Stream<OutputRow> endPartition() {
        List<OutputRow> ret = new ArrayList<OutputRow>();

        AutoLayout autoLayout = new AutoLayout(1, TimeUnit.SECONDS);
        autoLayout.setGraphModel(graphModel);
        YifanHuLayout firstLayout = new YifanHuLayout(null, new StepDisplacement(1f));
        firstLayout.setStepRatio(0.99f);
        firstLayout.setOptimalDistance(200f);
        firstLayout.setBarnesHutTheta(1.0f);
        autoLayout.addLayout(firstLayout, 1f);
        autoLayout.execute();

        for(Edge e: graphModel.getGraph().getEdges())
        {
            OutputRow o = new OutputRow(
                    e.getSource().getLabel()
                    , (String)e.getSource().getId()
                    ,e.getTarget().getLabel()
                    ,(String) e.getTarget().getId());
            
            o.providerX = e.getSource().x();
            o.providerY = e.getSource().y();
            o.consumerX = e.getTarget().x();
            o.consumerY = e.getTarget().y();
            ret.add(o);
        }

        return ret.stream();
    }
}
