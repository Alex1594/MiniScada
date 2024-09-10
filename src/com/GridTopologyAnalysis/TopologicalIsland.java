package com.GridTopologyAnalysis;

import java.util.ArrayList;
import java.util.List;

public class TopologicalIsland {
    String id;
    String name;

    public TopologicalIsland(String id) {
        this.id = id;
    }

    public TopologicalIsland() {

    }

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
    protected List<TopologicalNode> topologicalNodes = new ArrayList<>();

    public List<TopologicalNode> getTopologicalNodes() {
        return this.topologicalNodes;
    }
}
