package com.CIMModel;

import com.GridTopologyAnalysis.TopologicalIsland;
import com.GridTopologyAnalysis.TopologicalNode;

import java.util.ArrayList;
import java.util.List;

//子类：连接点类
//ConnectivityNode类表示连接点，一个连接点可以关联多个端子。
public class ConnectivityNode {
    private String id;
    private String name;
    private List<Terminal> terminals;
    protected TopologicalNode topologicalNode = null;

    public ConnectivityNode(String id) {
        this.id = id;
        this.terminals = new ArrayList<>();
    }

    public TopologicalNode getTopologicalNode() {
        return this.topologicalNode;
    }

    //重要
    public void setTopologicalNode(TopologicalNode paramTopologicalNode) {
        this.topologicalNode = paramTopologicalNode;
    }

    public List<Terminal> getTerminals() {
        return terminals;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addTerminal(Terminal terminal) {
        this.terminals.add(terminal);
        if (terminal.getConnectivityNode() != this) {
            terminal.setConnectivityNode(this);
        }
    }
}
