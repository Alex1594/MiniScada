package com.CIMModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//子类：端子类
//Terminal类表示设备的端子，是设备与其他设备的逻辑连接点。
public class Terminal  {
    private String id;
    private String name;
    private ConductingEquipment conductingEquipment;
    private ConnectivityNode connectivityNode;
    private List<Measurement> measurements;

    public Terminal(String mrid) {
        this.id = mrid;
        this.measurements = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public ConductingEquipment getConductingEquipment() {
        return conductingEquipment;
    }

    public void setConductingEquipment(ConductingEquipment conductingEquipment) {
        this.conductingEquipment = conductingEquipment;
        if (!conductingEquipment.getTerminals().contains(this)) {
            conductingEquipment.addTerminal(this);
        }
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public ConnectivityNode getConnectivityNode() {
        return connectivityNode;
    }

    public void setConnectivityNode(ConnectivityNode connectivityNode) {
        this.connectivityNode = connectivityNode;
        if (!connectivityNode.getTerminals().contains(this)) {
            connectivityNode.addTerminal(this);
        }
    }

    public List<Measurement> getMeasurements() {
        return measurements;
    }

    public void addMeasurement(Measurement measurement) {
        this.measurements.add(measurement);
    }


}
