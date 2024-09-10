package com.CIMModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// 基类：导电设备
// ConductingEquipment类 继承自Equipment，表示所有可导电的设备。
public abstract class ConductingEquipment extends Equipment {
    private String id;
    private String name;
    private List<Terminal> terminals;
    private List<Measurement> measurements;


    public ConductingEquipment(String id) {
        this.id = id;
        this.terminals = new ArrayList<>();
    }

    public List<Measurement> getMeasurements() {
        return measurements;
    }

    public void setMeasurements(List<Measurement> measurements) {
        this.measurements = measurements;
    }

    public ConductingEquipment(String id, String name) {
        this.terminals = new ArrayList<>();
    }

    public List<Terminal> getTerminals() {
        return terminals;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void addTerminal(Terminal terminal) {
        this.terminals.add(terminal);
        if (terminal.getConductingEquipment() != this) {
            terminal.setConductingEquipment(this);
        }
    }
}