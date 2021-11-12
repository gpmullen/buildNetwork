package NetworkBuilder;

import org.gephi.appearance.api.AppearanceController;
import org.gephi.appearance.api.AppearanceModel;
import org.gephi.appearance.api.Function;
import org.gephi.appearance.plugin.RankingElementColorTransformer;
import org.gephi.appearance.plugin.RankingLabelSizeTransformer;
import org.gephi.appearance.plugin.RankingNodeSizeTransformer;
import org.gephi.graph.api.*;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.io.importer.api.ImportController;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.layout.plugin.random.RandomLayout;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.statistics.plugin.GraphDistance;
import org.openide.util.Lookup;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;


public class buildNetwork {
    private GraphModel graphModel;

    private DirectedGraph DirectedGraph;
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
            test.formatting();
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
        DirectedGraph = graphModel.getDirectedGraph();
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
                Node n0 = DirectedGraph.getNode(pId);
                if (n0 == null) {
                    n0 = graphModel.factory().newNode(pId);
                    n0.setLabel(pName);
                    DirectedGraph.addNode(n0);
                }

                Node n1 = DirectedGraph.getNode(cId);
                if (n1 == null) {
                    n1 = graphModel.factory().newNode(cId);
                    n1.setLabel(cName);
                    DirectedGraph.addNode(n1);
                }
                //Create an edge - directed and weight 1
                Edge e1 = graphModel.factory().newEdge(n0, n1, 1, true);

                //Append as a Directed Graph
                DirectedGraph.addEdge(e1);
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
        //Need this because the YifanHu alone is broken without providing a starting point
        //So we generate some random x/y values with the Random Layout
        RandomLayout randLayout = new RandomLayout(null,5d);
        randLayout.setGraphModel(graphModel);
        randLayout.initAlgo();
        randLayout.goAlgo();
        randLayout.endAlgo();

        //set YifanHu defaults
        layout.setGraphModel(graphModel);
        layout.resetPropertiesValues();
        layout.setStepRatio(0.99f);
        layout.setOptimalDistance(200f);
        layout.setBarnesHutTheta(1.0f);

        // 5 passes seems to give good separation
       for(int i=0; i < 5 && layout.canAlgo(); i++){
            layout.goAlgo();
        }

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

        layout.endAlgo();
        return ret.stream();
    }

    private void formatting()
    {


        //Rank color by Degree

        Function degreeRanking = appearanceModel.getNodeFunction(DirectedGraph, AppearanceModel.GraphFunction.NODE_DEGREE, RankingElementColorTransformer.class);
        RankingElementColorTransformer degreeTransformer = (RankingElementColorTransformer) degreeRanking.getTransformer();
        degreeTransformer.setColors(new Color[]{new Color(0xFEF0D9), new Color(0xB30000)});
        degreeTransformer.setColorPositions(new float[]{0f, 1f});
        appearanceController.transform(degreeRanking);

        //Get Centrality

        GraphDistance distance = new GraphDistance();
        distance.setDirected(false);
        distance.execute(graphModel);

        //Rank size by centrality

        Column centralityColumn = graphModel.getNodeTable().getColumn(GraphDistance.BETWEENNESS);
        Function centralityRanking = appearanceModel.getNodeFunction(DirectedGraph, centralityColumn, RankingNodeSizeTransformer.class);
        RankingNodeSizeTransformer centralityTransformer = (RankingNodeSizeTransformer) centralityRanking.getTransformer();
        centralityTransformer.setMinSize(1);
        centralityTransformer.setMaxSize(30);
        appearanceController.transform(centralityRanking);

        //Rank label size - set a multiplier size

        Function centralityRanking2 = appearanceModel.getNodeFunction(DirectedGraph, centralityColumn, RankingLabelSizeTransformer.class);
        RankingLabelSizeTransformer labelSizeTransformer = (RankingLabelSizeTransformer) centralityRanking2.getTransformer();
        labelSizeTransformer.setMinSize(1);
        labelSizeTransformer.setMaxSize(50);
        appearanceController.transform(centralityRanking2);

        //Set 'show labels' option in Preview - and disable node size influence on text size
        PreviewModel previewModel = Lookup.getDefault().lookup(PreviewController.class).getModel();
        previewModel.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, Boolean.TRUE);
        previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_PROPORTIONAL_SIZE, Boolean.FALSE);

        //Export
        ExportController ec = Lookup.getDefault().lookup(ExportController.class);
        try {
            ec.exportFile(new File("/Users/gmullen/Downloads/ranking.pdf"));
        } catch (IOException ex) {
            ex.printStackTrace();

        }
    }
}
