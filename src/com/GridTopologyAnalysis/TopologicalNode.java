package com.GridTopologyAnalysis;

import com.CIMModel.ConnectivityNode;

import java.util.ArrayList;
import java.util.List;

public class TopologicalNode {

    String id;
    String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    protected List<ConnectivityNode> connectivityNodes = new ArrayList<>();

    protected TopologicalIsland topologicalIsland = null;

    public List<ConnectivityNode> getConnectivityNodes() {
        return this.connectivityNodes;
    }

    public TopologicalIsland getTopologicalIsland() {
        return this.topologicalIsland;
    }

    public void setTopologicalIsland(TopologicalIsland paramTopologicalIsland) {
        this.topologicalIsland = paramTopologicalIsland;
    }
}
