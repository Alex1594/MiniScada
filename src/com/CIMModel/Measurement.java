package com.CIMModel;

public class Measurement {
    private String id;
    private String name;
    private Terminal ter;
    PowerSystemResource member = null;
    int meastype;
    float value;
    boolean status;
    public void setMeastype(int meastype) {
        this.meastype = meastype;
    }

    public PowerSystemResource getMember() {
        return member;
    }

    public void setMember(PowerSystemResource member) {
        this.member = member;
    }

    public Measurement(String id) {
        this.id = id;
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

    public Terminal getTerminal() {
        return ter;
    }

    public void setTerminal(Terminal paramTerminal) {
        this.ter = paramTerminal;
        paramTerminal.getMeasurements().add(this);
    }

    public void setDiscreteValue(boolean status) {
        this.status = status;
    }

    public boolean getDiscreteValue() {
        return this.status;
    }

    public void setAnalogValue(float value) {
        this.value = value;
    }
    public float getAnalogValue() {
        return this.value;
    }
}
