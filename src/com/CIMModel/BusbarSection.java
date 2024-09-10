package com.CIMModel;

public class BusbarSection extends ConductingEquipment{
    private Terminal terminal0;

    public BusbarSection(String mrid) {
        super(mrid);
        this.terminal0 = null;
    }

    public Terminal getTerminal0() {
        return terminal0;
    }

    public void setTerminal0(Terminal terminal0) {
        this.terminal0 = terminal0;
    }

}
