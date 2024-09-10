package com.CIMModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// 开关类
public class Switch extends ConductingEquipment {
    public String normalOpen;
    List<Measurement> measurements = new ArrayList<>();

    public Switch(String id) {
        super(id);
    }

    public Switch(String id, String name) {
        super(id, name);
    }
    public String getNormalOpen() {
        return this.normalOpen;
    }

    public void setNormalOpen(String paramString) {
        this.normalOpen = paramString;
    }

    public List<Measurement> getMeasurements() {
        return this.measurements;
    }

}
