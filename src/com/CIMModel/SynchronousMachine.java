package com.CIMModel;

public class SynchronousMachine extends ConductingEquipment{
    private Terminal terminal0;

    public SynchronousMachine(String id) {
        super(id);
        this.terminal0 = null;
    }

    public Terminal getTerminal0() {
        return terminal0;
    }

    public void setTerminal0(Terminal terminal0) {
        this.terminal0 = terminal0;
    }

}
