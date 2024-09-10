package com.CIMModel;

public class ACLineSegment extends ConductingEquipment{

    private Terminal terminal0;
    private Terminal terminal1;

    public ACLineSegment(String id) {
        super(id);
    }

    public Terminal getTerminal0() {
        return terminal0;
    }

    public void setTerminal0(Terminal terminal0) {
        this.terminal0 = terminal0;
    }

    public Terminal getTerminal1() {
        return terminal1;
    }

    public void setTerminal1(Terminal terminal1) {
        this.terminal1 = terminal1;
    }
}
