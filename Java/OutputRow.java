package NetworkBuilder;

import org.gephi.graph.api.*;
import org.gephi.graph.spi.LayoutData;

import java.awt.*;
import java.lang.String;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class OutputRow {

    public String providerName;
    public String providerID;
    public float providerX;
    public float providerY;
    public String consumerName;
    public String consumerID;
    public float consumerX;
    public float consumerY;

    public OutputRow()
    {

    }
    public OutputRow(String pName, String pId,String cName,String cId//, float pX, float pY, float cX
    ) {
        this.providerName = pName;
        this.providerID = pId;
        this.consumerID = cId;
        this.consumerName = cName;
        this.providerX = 0;
        this.providerY = 0;
        this.consumerX = 0;
        this.consumerY = 0;
    }

    @Override
    public String toString() {
        return "OutputRow{" +
                "providerName='" + providerName + '\'' +
                ", providerID=" + providerID +
                ", providerX=" + providerX +
                ", providerY=" + providerY +
                ", consumerName='" + consumerName + '\'' +
                ", consumerID=" + consumerID +
                ", consumerX=" + consumerX +
                ", consumerY=" + consumerY +
                '}';
    }
}
