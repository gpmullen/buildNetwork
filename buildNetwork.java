package NetworkBuilder;

import org.gephi.appearance.api.AppearanceController;
import org.gephi.appearance.api.AppearanceModel;
import org.gephi.graph.api.*;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.generator.plugin.RandomGraph;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.layout.plugin.AutoLayout;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.layout.plugin.forceAtlas.ForceAtlasLayout;
import org.gephi.layout.plugin.random.RandomLayout;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.openide.util.Lookup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;


public class buildNetwork {
    private GraphModel graphModel;

    private DirectedGraph directedGraph;
    private AppearanceController appearanceController;
    private AppearanceModel appearanceModel;
    private ImportController importController;
    private ProjectController pc;
    private Workspace workspace;
    private YifanHuLayout layout;

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
        pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.newProject();
        workspace = pc.getCurrentWorkspace();

        //Get controllers and models
        importController = Lookup.getDefault().lookup(ImportController.class);
        graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
        appearanceController = Lookup.getDefault().lookup(AppearanceController.class);
        appearanceModel = appearanceController.getModel();
        directedGraph = graphModel.getDirectedGraph();
        layout = new YifanHuLayout(null,new StepDisplacement(1f));
        layout.initAlgo();

    }
    public static Class getOutputClass() {
        return OutputRow.class;
    }

    public Stream<OutputRow> process(String pName, String pId, String cName, String cId) throws Exception {
        try {
            if (pId != null && cId != null && pName != null && cId != null) {
                //Create two nodes
                Node n0 = directedGraph.getNode(pId);
                if (n0 == null) {
                    n0 = graphModel.factory().newNode(pId);
                    n0.setLabel(pName);
                    //VERY IMPORTANT: initialize with random position. Otherwise, if all nodes get x=0,y=0 coordinates, layouts won't work (known bug) https://github.com/gephi/gephi/issues/1698
                    float x = (float) (((0.01 + Math.random()) * 1000) - 500);
                    float y = (float) (((0.01 + Math.random()) * 1000) - 500);
                    n0.setPosition(x, y);
                    System.out.println(n0.x()+","+n0.y());
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

        return Stream.of();
    }

    public Stream<OutputRow> endPartition() {
        List<OutputRow> ret = new ArrayList<OutputRow>();

        //set YifanHu defaults
        layout.setGraphModel(graphModel);
        layout.resetPropertiesValues();
        layout.setStepRatio(0.99f);
        layout.setOptimalDistance(200f);
        layout.setBarnesHutTheta(1.0f);

        AutoLayout autoLayout = new AutoLayout(1, TimeUnit.SECONDS);
        autoLayout.setGraphModel(graphModel);
        YifanHuLayout firstLayout = new YifanHuLayout(null, new StepDisplacement(1f));
        AutoLayout.DynamicProperty adjustBySizeProperty = AutoLayout.createDynamicProperty("forceAtlas.adjustSizes.name", Boolean.TRUE, 0.1f);//True after 10% of layout time
        AutoLayout.DynamicProperty repulsionProperty = AutoLayout.createDynamicProperty("forceAtlas.repulsionStrength.name", 500., 0f);//500 for the complete period
        autoLayout.addLayout(firstLayout, 1f);
        autoLayout.execute();

        for(Edge e: graphModel.getGraph().getEdges())
        {
            OutputRow o = new OutputRow( e.getSource().getLabel(), (String)e.getSource().getId()
                    ,e.getTarget().getLabel(),(String) e.getTarget().getId());
            o.providerX = e.getSource().x();
            o.providerY = e.getSource().y();
            o.consumerX = e.getTarget().x();
            o.consumerY = e.getTarget().y();
            ret.add(o);
        }

        return ret.stream();
    }
}
