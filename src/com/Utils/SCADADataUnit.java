package com.Utils;

import com.CIMModel.ConductingEquipment;
import com.CIMModel.Switch;

public class SCADADataUnit implements Comparable<SCADADataUnit> {
    public boolean dis11Status;

    public boolean dis12Status;

    public boolean dis2Status;

    public boolean brkStatus;

    public String dis11name;

    public String dis12name;

    public String dis2name;

    public String brkname;

    public String devicename;

    public ConductingEquipment device;

    public Switch dis11;

    public Switch dis12;

    public Switch dis2;

    public Switch breaker;

    public String P;

    public String Q;

    public String getP() {
        return this.P;
    }

    public void setP(String p) {
        this.P = p;
    }

    public String getQ() {
        return this.Q;
    }

    public void setQ(String q) {
        this.Q = q;
    }

    public boolean getBrkStatus() {
        return this.brkStatus;
    }

    public void setBrkStatus(boolean brkStatus) {
        this.brkStatus = brkStatus;
    }

    public String getBrkname() {
        return this.brkname;
    }

    public void setBrkname(String brkname) {
        this.brkname = brkname;
    }

    public void setBrk(Switch brk) {
        this.breaker = brk;
    }

    public Switch getBrk() {
        return this.breaker;
    }

    public boolean getDis11Status() {
        return this.dis11Status;
    }

    public void setDis11Status(boolean dis11) {
        this.dis11Status = dis11;
    }

    public String getDis11name() {
        return this.dis11name;
    }

    public void setDis11name(String dis11name) {
        this.dis11name = dis11name;
    }

    public Switch getDis11() {
        return this.dis11;
    }

    public void setDis11(Switch dis11) {
        this.dis11 = dis11;
    }

    public boolean getDis12Status() {
        return this.dis12Status;
    }

    public void setDis12Status(boolean dis12) {
        this.dis12Status = dis12;
    }

    public String getDis12name() {
        return this.dis12name;
    }

    public void setDis12name(String dis12name) {
        this.dis12name = dis12name;
    }

    public Switch getDis12() {
        return this.dis12;
    }

    public void setDis12(Switch dis12) {
        this.dis12 = dis12;
    }

    public boolean getDis2Status() {
        return this.dis2Status;
    }

    public void setDis2Status(boolean dis2) {
        this.dis2Status = dis2;
    }

    public String getDis2name() {
        return this.dis2name;
    }

    public void setDis2name(String dis2name) {
        this.dis2name = dis2name;
    }

    public Switch getDis2() {
        return this.dis2;
    }

    public void setDis2(Switch dis2) {
        this.dis2 = dis2;
    }

    public ConductingEquipment getDevice() {
        return this.device;
    }

    public void setDevice(ConductingEquipment device) {
        this.device = device;
    }

    public String getDevicename() {
        return this.devicename;
    }

    public void setDevicename(String devicename) {
        this.devicename = devicename;
    }

    public SCADADataUnit() {
        this.dis11Status = false;
        this.dis12Status = false;
        this.dis2Status = true;
        this.brkStatus = false;
    }

    public int compareTo(SCADADataUnit o) {
        return this.brkname.compareTo(o.getBrkname());
    }
}
