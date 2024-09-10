package com.Views;

import com.CIMModel.*;
import com.GridTopologyAnalysis.Analysis;
import com.GridTopologyAnalysis.TopologicalNode;
import com.Utils.SCADADataUnit;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DrawPanel extends JPanel implements MouseListener {
    private List<SCADADataUnit> data;
    private List<PaintUnit> paintData;
    private Set<ConductingEquipment> eqSet;
    private int stationType = 2;
    private int stationNo = -1;
    private int y0 = 350;
    private int y1;
    private int y2;
    private Image img;
    private int lastClickUnit = -1;
    private Analysis model;  // 添加字段 model 的定义

    public DrawPanel() {
        setBackground(Color.PINK);
        addMouseListener(this);
        try {
            img = ImageIO.read(getClass().getClassLoader().getResourceAsStream("resource/images/IEEE39.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setModel(Analysis model) {
        this.model = model;
    }

    public void setStationNo(int no) {
        this.stationNo = no;
    }

    public int getStationNo() {
        return this.stationNo;
    }

    public void setDeviceSet(Set<ConductingEquipment> set) {
        this.lastClickUnit = -1;
        this.eqSet = set;
        System.out.println("Size of EqSet=" + this.eqSet.size());
        generateData(this.eqSet);
        this.paintData = new ArrayList<>();
        this.data.forEach(d -> this.paintData.add(new PaintUnit()));
        List l = this.paintData;
    }

    public Set<ConductingEquipment> getDeviceSet() {
        return this.eqSet;
    }

    private Terminal getOtherTerminal(Terminal t) {
        ConductingEquipment e = t.getConductingEquipment();
        return e.getTerminals().stream().filter(tt -> tt != t).findFirst().orElse(null);
    }

    private Terminal getOtherTerminalOfCN(Terminal t) {
        ConnectivityNode cn = t.getConnectivityNode();
        return cn.getTerminals().stream().filter(tt -> tt != t).findFirst().orElse(null);
    }

    private boolean getSwitchStatus(Terminal t) {
        return getSwitchStatus((Switch) t.getConductingEquipment());
    }

    private boolean getSwitchStatus(Switch sw) {
        List<Measurement> meas = sw.getMeasurements();
        return meas.size() > 0 ? meas.get(0).getDiscreteValue() : !sw.getNormalOpen().equalsIgnoreCase("1");
    }

//    private void updateSwitchStatus() {
//        this.data.forEach(unit -> {
//            unit.setBrkStatus(getSwitchStatus(unit.getBrk()));
//            Optional.ofNullable(unit.getDis11()).ifPresent(sw -> unit.setDis11Status(getSwitchStatus(sw)));
//            Optional.ofNullable(unit.getDis12()).ifPresent(sw -> unit.setDis12Status(getSwitchStatus(sw)));
//            Optional.ofNullable(unit.getDis2()).ifPresent(sw -> unit.setDis2Status(getSwitchStatus(sw)));
//        });
//    }

    private void updateSwitchMeasurement(Switch sw, boolean value) {
        if (!sw.getMeasurements().isEmpty()) {
            sw.getMeasurements().get(0).setDiscreteValue(value);
        }
    }

    public void updateDataUnit(int no) {
        //updateSwitchStatus();
        SCADADataUnit unit = this.data.get(no);
        ConductingEquipment eq = unit.getDevice();
        if (eq == null || eq instanceof Energyconsumer) return;

        eq.getMeasurements().forEach(m -> {
            String station = m.getTerminal().getName().substring(1, 3);
            if (Double.parseDouble(station) == this.stationNo) {
                if (m.getName().contains("P")) {
                    unit.setP(String.valueOf(m.getAnalogValue()));
                } else if (m.getName().contains("Q")) {
                    unit.setQ(String.valueOf(m.getAnalogValue()));
                }
            }
        });
    }

    // 合并并简化 updateDataUnit 方法
    public void updateDataUnit() {
        //updateSwitchStatus();
        this.data.forEach(this::updateMeasurementsForUnit);
    }

    private void updateMeasurementsForUnit(SCADADataUnit unit) {
        ConductingEquipment eq = unit.getDevice();
        if (eq == null || eq instanceof Energyconsumer) return;

//        eq.getMeasurements().forEach(m -> {
//            String station = m.getTerminal().getName().substring(1, 3);
//            if (Double.parseDouble(station) == this.stationNo) {
//                if (m.getName().contains("P")) {
//                    unit.setP(String.valueOf(m.getAnalogValue()));
//                } else if (m.getName().contains("Q")) {
//                    unit.setQ(String.valueOf(m.getAnalogValue()));
//                }
//            }
//        });
    }

    // 简化 generateData 方法
    private void generateData(Set<ConductingEquipment> eqSet) {
        int nBusbar = (int) eqSet.stream().filter(eq -> eq instanceof BusbarSection).count();
        List<Breaker> breakers = eqSet.stream()
                .filter(eq -> eq instanceof Breaker)
                .map(eq -> (Breaker) eq)
                .collect(Collectors.toList());

        this.stationType = determineStationType(nBusbar, breakers.size());
        System.out.println("stationtype = " + this.stationType);
        this.data = new ArrayList<>();

        breakers.forEach(brk -> this.data.add(createDataUnit(brk)));
    }

    private int determineStationType(int nBusbar, int nBreaker) {
        if (nBusbar == 1) return 1;
        if (nBusbar > 2) {
            if (nBreaker < 6) return 2;
            if (nBreaker > 9) return 3;
        }
        return 0; // 未知类型
    }

    private SCADADataUnit createDataUnit(Breaker brk) {
        SCADADataUnit dataUnit = new SCADADataUnit();
        dataUnit.setBrkname(brk.getName());
        dataUnit.setBrk((Switch) brk);
        dataUnit.setBrkStatus(getSwitchStatus(brk.getTerminals().get(0)));

        List<Terminal> terminals = brk.getTerminals();
        Terminal t1 = terminals.get(0);
        Terminal t2 = getOtherTerminal(t1);

        if (this.stationType == 3) {
            processType3Unit(dataUnit, t1, t2);
        } else {
            processOtherTypesUnit(dataUnit, t1);
            processOtherTypesUnit(dataUnit, t2);
        }

        return dataUnit;
    }

    private void processType3Unit(SCADADataUnit dataUnit, Terminal t1, Terminal t2) {
        ConnectivityNode cn1 = t1.getConnectivityNode();
        ConnectivityNode cn2 = t2.getConnectivityNode();

        cn1.getTerminals().stream().filter(t -> t != t1).forEach(t -> processTerminalForType3(dataUnit, t));
        cn2.getTerminals().stream().filter(t -> t != t2).forEach(t -> processTerminalForType3(dataUnit, t));

        if (dataUnit.getDis12name() == null && dataUnit.getDis2name() != null) {
            moveDis2ToDis12(dataUnit);
        } else if (dataUnit.getDis11name() == null && dataUnit.getDis2name() != null) {
            moveDis2ToDis11(dataUnit);
        }
    }

    private void moveDis2ToDis12(SCADADataUnit dataUnit) {
        dataUnit.setDis12name(dataUnit.getDis2name());
        dataUnit.setDis12Status(dataUnit.getDis2Status());
        dataUnit.setDis12(dataUnit.getDis2());
        dataUnit.setDis2name(null);
        dataUnit.setDis2(null);
    }

    private void moveDis2ToDis11(SCADADataUnit dataUnit) {
        dataUnit.setDis11name(dataUnit.getDis2name());
        dataUnit.setDis11Status(dataUnit.getDis2Status());
        dataUnit.setDis11(dataUnit.getDis2());
        dataUnit.setDis2name(null);
        dataUnit.setDis2(null);
    }

    private void processOtherTypesUnit(SCADADataUnit dataUnit, Terminal t) {
        ConnectivityNode cn = t.getConnectivityNode();
        cn.getTerminals().stream().filter(term -> term != t).forEach(term -> processTerminalForOtherTypes(dataUnit, term));
    }

    private void processTerminalForType3(SCADADataUnit dataUnit, Terminal t) {
        Terminal t3 = getOtherTerminal(t);
        ConductingEquipment eq1 = t3.getConductingEquipment();
        for (Terminal t4 : t3.getConnectivityNode().getTerminals()) {
            if (t4 == t3) continue;
            ConductingEquipment eq2 = t4.getConductingEquipment();
            handleTerminalConnections(dataUnit, t3, eq1, eq2);
        }
    }

    private void processTerminalForOtherTypes(SCADADataUnit dataUnit, Terminal t) {
        Terminal t3 = getOtherTerminal(t);
        ConductingEquipment eq1 = t3.getConductingEquipment();
        for (Terminal t4 : t3.getConnectivityNode().getTerminals()) {
            if (t4 == t3) continue;
            ConductingEquipment eq2 = t4.getConductingEquipment();
            handleTerminalConnections(dataUnit, t3, eq1, eq2);
        }
    }

    private void handleTerminalConnections(SCADADataUnit dataUnit, Terminal t3, ConductingEquipment eq1, ConductingEquipment eq2) {
        if (eq2 instanceof BusbarSection) {
            handleBusbarSectionConnections(dataUnit, t3, eq1, eq2);
        } else if (eq2 instanceof ACLineSegment || eq2 instanceof SynchronousMachine ||
                eq2 instanceof TransformerWinding || eq2 instanceof Energyconsumer) {
            setDis2Details(dataUnit, t3, eq1, eq2);
        } else if (eq2 instanceof Disconnector) {
            handleDisconnectorConnections(dataUnit, t3, eq1, eq2);
        }
    }

    private void handleBusbarSectionConnections(SCADADataUnit dataUnit, Terminal t3, ConductingEquipment eq1, ConductingEquipment eq2) {
        if (eq2.getName().contains("-II")) {
            dataUnit.setDis12Status(getSwitchStatus(t3));
            dataUnit.setDis12name(eq1.getName());
            dataUnit.setDis12((Switch) eq1);
        } else if (eq2.getName().contains("-I")) {
            dataUnit.setDis11Status(getSwitchStatus(t3));
            dataUnit.setDis11name(eq1.getName());
            dataUnit.setDis11((Switch) eq1);
        }
    }

    private void setDis2Details(SCADADataUnit dataUnit, Terminal t3, ConductingEquipment eq1, ConductingEquipment eq2) {
        dataUnit.setDis2Status(getSwitchStatus(t3));
        dataUnit.setDis2name(eq1.getName());
        dataUnit.setDis2((Switch) eq1);
        dataUnit.setDevicename(eq2.getName());
        dataUnit.setDevice(eq2);
    }

    private void handleDisconnectorConnections(SCADADataUnit dataUnit, Terminal t3, ConductingEquipment eq1, ConductingEquipment eq2) {
        // 获取与当前终端 t3 连接的其他终端
        Terminal otherTerminal = findOtherTerminal(eq1.getTerminals(), t3);
        if (otherTerminal == null) return;  // 如果没有其他终端，则返回

        // 获取连接的设备 eq2 的类型
        ConductingEquipment connectedEquipment = eq2;
        if (connectedEquipment == null) return;  // 如果连接的设备为null，返回

        // 根据连接的设备类型，设置连接状态和名称
        if (connectedEquipment instanceof ACLineSegment ||
                connectedEquipment instanceof SynchronousMachine ||
                connectedEquipment instanceof TransformerWinding ||
                connectedEquipment instanceof Energyconsumer) {

            // 如果是线路段、同步机、变压器绕组或能源消费者，更新 DIS2 的状态和名称
            dataUnit.setDis2Status(getSwitchStatus(t3));
            dataUnit.setDis2name(eq1.getName());
            dataUnit.setDis2((Switch) eq1);
            dataUnit.setDevicename(connectedEquipment.getName());
            dataUnit.setDevice(connectedEquipment);

        } else if (connectedEquipment instanceof Disconnector) {
            // 如果是断路器，处理更复杂的连接情况
            handleNestedDisconnectorConnections(dataUnit, eq1, t3, connectedEquipment);
        }
    }

    /**
     * 处理嵌套的断路器连接情况。
     */
    private void handleNestedDisconnectorConnections(SCADADataUnit dataUnit, ConductingEquipment eq1, Terminal t3, ConductingEquipment eq2) {
        Terminal t5 = findOtherTerminal(eq2.getTerminals(), t3);
        if (t5 == null) return;

        Terminal t6 = findOtherTerminal(t5.getConductingEquipment().getTerminals(), t5);
        if (t6 == null) return;

        Terminal t7 = findOtherTerminal(t6.getConductingEquipment().getTerminals(), t6);
        if (t7 == null) return;

        Terminal t8 = findOtherTerminal(t7.getConductingEquipment().getTerminals(), t7);
        if (t8 == null) return;

        Terminal t9 = findOtherTerminal(t8.getConductingEquipment().getTerminals(), t8);
        if (t9 == null) return;

        Terminal t10 = findOtherTerminal(t9.getConductingEquipment().getTerminals(), t9);
        if (t10 == null) return;

        ConductingEquipment eq10 = t10.getConductingEquipment();

        if (eq10 != null && eq10.getName().contains("-II")) {
            dataUnit.setDis12name(eq1.getName());
            dataUnit.setDis12Status(getSwitchStatus(t3));
            dataUnit.setDis12((Switch) eq1);
        } else if (eq10 != null && eq10.getName().contains("-I")) {
            dataUnit.setDis11name(eq1.getName());
            dataUnit.setDis11Status(getSwitchStatus(t3));
            dataUnit.setDis11((Switch) eq1);
        } else {
            dataUnit.setDis2name(eq1.getName());
            dataUnit.setDis2Status(getSwitchStatus(t3));
            dataUnit.setDis2((Switch) eq1);
        }
    }

    /**
     * 在终端列表中查找与给定终端不同的终端。
     */
    private Terminal findOtherTerminal(List<Terminal> terminals, Terminal originalTerminal) {
        for (Terminal terminal : terminals) {
            if (!terminal.equals(originalTerminal)) {
                return terminal;
            }
        }
        return null;  // 如果没有找到其他终端，返回null
    }



    private void paintType1Unit(Graphics g, int id) {
        SCADADataUnit dataUnit = this.data.get(id - 1);
        PaintUnit pu = this.paintData.get(id - 1);

        int[] yCoordinates = calculateYCoordinatesForType1(id);
        int xbase = 350;
        int xleft = xbase - 10;

        updatePaintUnitCoordinates(pu, xleft, yCoordinates[0], yCoordinates[1], yCoordinates[2]);

        drawMainLine(g, xbase, this.y0, yCoordinates[4]);
        drawSwitch(g, xleft, yCoordinates[0], dataUnit.getDis11Status(), dataUnit.getDis11name());
        drawBreaker(g, xleft, yCoordinates[2], dataUnit.getBrkStatus(), dataUnit.getBrkname());
        drawSwitch(g, xleft, yCoordinates[1], dataUnit.getDis2Status(), dataUnit.getDis2name());

        paintDevice(g, id, xbase, yCoordinates[3], dataUnit.getDevice());
    }

    private void paintConUnit(Graphics g, int unitIndex) {
        SCADADataUnit dataUnit = this.data.get(unitIndex - 1);
        PaintUnit paintUnit = this.paintData.get(unitIndex - 1);

        int xBase = calculateXBaseForConUnit(unitIndex);
        int xLeft = xBase - 30;
        int xRight = xBase + 10;

        // 计算 y 坐标
        int yDis1 = this.y0 - 50;
        int yBreaker = this.y0 - 120;
        int yEnd = this.y0 - 160;

        // 调用 calculateYCoordinatesForType2 以便与更新后的方法保持一致
        int[] yCoordinates = {yDis1, yDis1, yBreaker, yBreaker, yEnd, yEnd}; // 构造 y 坐标数组

        // 更新 PaintUnit 的坐标
        updatePaintUnitCoordinates(paintUnit, xBase, yCoordinates);

        // 绘制连接线
        drawConnectionLines(g, xLeft, xRight, yEnd);
        drawSwitch(g, paintUnit.getX(PaintUnit.CoordinateType.DIS11), paintUnit.getY(PaintUnit.CoordinateType.DIS11), dataUnit.getDis11Status(), dataUnit.getDis11name());
        drawSwitch(g, paintUnit.getX(PaintUnit.CoordinateType.DIS12), paintUnit.getY(PaintUnit.CoordinateType.DIS12), dataUnit.getDis12Status(), dataUnit.getDis12name());
        drawBreaker(g, paintUnit.getX(PaintUnit.CoordinateType.BRK), paintUnit.getY(PaintUnit.CoordinateType.BRK), dataUnit.getBrkStatus(), dataUnit.getBrkname());
    }


    private int[] calculateYCoordinatesForType1(int id) {
        int y_d1, y_d2, y_brk, y_3, yend;
        if (id == 1) {
            y_d1 = this.y0 - 40;
            y_d2 = this.y0 - 140;
            y_brk = this.y0 - 100;
            y_3 = this.y0 - 200;
            yend = this.y0 - 250;
        } else {
            y_d1 = this.y0 + 20;
            y_d2 = this.y0 + 120;
            y_brk = this.y0 + 60;
            y_3 = this.y0 + 200;
            yend = this.y0 + 250;
        }
        return new int[]{y_d1, y_d2, y_brk, y_3, yend};
    }

    private int calculateXBaseForConUnit(int id) {
        int xbase = (id + 1) * 100;
        if (id % 2 == 0) {
            xbase += 100;
        }
        return xbase;
    }

    private void updatePaintUnitCoordinates(PaintUnit pu, int xleft, int y_d1, int y_brk, int y_d2) {
        pu.setX(PaintUnit.CoordinateType.DIS11,xleft);
        pu.setY(PaintUnit.CoordinateType.DIS11,y_d1);
        pu.setX(PaintUnit.CoordinateType.BRK,xleft);
        pu.setY(PaintUnit.CoordinateType.BRK,y_brk);
        pu.setX(PaintUnit.CoordinateType.DIS2,xleft);
        pu.setY(PaintUnit.CoordinateType.DIS2,y_d2);
    }

    private void updatePaintUnitCoordinates(PaintUnit paintUnit, int baseXCoordinate, int[] yCoordinates) {
        // 更清晰地设置 PaintUnit 的坐标
        paintUnit.setX(PaintUnit.CoordinateType.DIS11, baseXCoordinate - 30);
        paintUnit.setY(PaintUnit.CoordinateType.DIS11, yCoordinates[0]);  // yDis11
        paintUnit.setX(PaintUnit.CoordinateType.DIS12, baseXCoordinate + 10);
        paintUnit.setY(PaintUnit.CoordinateType.DIS12, yCoordinates[1]);  // yDis12
        paintUnit.setX(PaintUnit.CoordinateType.DIS2, baseXCoordinate - 10);
        paintUnit.setY(PaintUnit.CoordinateType.DIS2, yCoordinates[2]);   // yDis2
        paintUnit.setX(PaintUnit.CoordinateType.BRK, baseXCoordinate - 10);
        paintUnit.setY(PaintUnit.CoordinateType.BRK, yCoordinates[3]);    // yBreaker
    }


    private void drawMainLine(Graphics g, int xbase, int y0, int yend) {
        float lineWidth = 3.0F;
        ((Graphics2D) g).setStroke(new BasicStroke(lineWidth));
        g.setColor(Color.BLACK);
        g.drawLine(xbase, yend, xbase, y0);
    }

    private void drawConnectionLines(Graphics g, int xleft, int xright, int yend) {
        float lineWidth = 3.0F;
        ((Graphics2D) g).setStroke(new BasicStroke(lineWidth));
        g.setColor(Color.BLACK);
        g.drawLine(xleft + 10, this.y0 - 10, xleft + 10, yend);
        g.drawLine(xright + 10, this.y0 + 10, xright + 10, yend);
        g.drawLine(xleft + 10, yend, xright + 10, yend);
    }

    private void drawSwitch(Graphics g, int x, int y, boolean status, String name) {
        g.setColor(status ? Color.RED : Color.WHITE);
        g.fillOval(x, y, 20, 20);
        if (!status) {
            g.setColor(Color.BLACK);
            g.drawOval(x, y, 20, 20);
        }
        g.setColor(Color.BLACK);
        g.drawString(name, x + 30, y + 10);
    }

    private void drawBreaker(Graphics g, int x, int y, boolean status, String name) {
        g.setColor(status ? Color.RED : Color.WHITE);
        g.fillRect(x, y, 20, 40);
        if (!status) {
            g.setColor(Color.BLACK);
            g.drawRect(x, y, 20, 40);
        }
        g.setColor(Color.BLACK);
        g.drawString(name, x + 30, y + 20);
    }

    private void paintType2Unit(Graphics g, int id) {
        SCADADataUnit dataUnit = this.data.get(id - 1);
        PaintUnit pu = this.paintData.get(id - 1);

        int xbase = calculateXBaseForUnit(id);
        int[] yCoordinates = calculateYCoordinatesForType2(id);

        updatePaintUnitCoordinates(pu, xbase, yCoordinates);

        drawConnectionLinesForType2(g, xbase, yCoordinates);
        drawSwitch(g, pu.getX(PaintUnit.CoordinateType.DIS11), pu.getY(PaintUnit.CoordinateType.DIS11), dataUnit.getDis11Status(), dataUnit.getDis11name());
        drawSwitch(g, pu.getX(PaintUnit.CoordinateType.DIS12), pu.getY(PaintUnit.CoordinateType.DIS12), dataUnit.getDis12Status(), dataUnit.getDis12name());
        drawBreaker(g, pu.getX(PaintUnit.CoordinateType.BRK), pu.getY(PaintUnit.CoordinateType.BRK), dataUnit.getBrkStatus(), dataUnit.getBrkname());
        drawSwitch(g, pu.getX(PaintUnit.CoordinateType.DIS2), pu.getY(PaintUnit.CoordinateType.DIS2), dataUnit.getDis2Status(), dataUnit.getDis2name());

        paintDevice(g, id, xbase, yCoordinates[3], dataUnit.getDevice());
    }

    private void paintType3Unit(Graphics g) {
        float lineWidth = 3.0F;
        ((Graphics2D)g).setStroke(new BasicStroke(lineWidth));
        g.setColor(Color.BLACK);

        // Draw main lines for Type 3 Unit
        drawMainLinesForType3(g);

        for (int j = 0; j < 3; j++) {
            int xbase = 200 * (j + 1);
            for (int k = 0; k < 3; k++) {
                SCADADataUnit dataUnit = this.data.get(j * 3 + k);
                PaintUnit pu = this.paintData.get(j * 3 + k);
                int[] yCoordinates = calculateYCoordinatesForType3(k);

                updatePaintUnitCoordinates(pu, xbase, yCoordinates[0],yCoordinates[1],yCoordinates[2] );

                drawSwitch(g, pu.getX(PaintUnit.CoordinateType.DIS11), pu.getY(PaintUnit.CoordinateType.DIS11), dataUnit.getDis11Status(), dataUnit.getDis11name());
                drawBreaker(g, pu.getX(PaintUnit.CoordinateType.BRK), pu.getY(PaintUnit.CoordinateType.BRK), dataUnit.getBrkStatus(), dataUnit.getBrkname());
                drawSwitch(g, pu.getX(PaintUnit.CoordinateType.DIS12), pu.getY(PaintUnit.CoordinateType.DIS12), dataUnit.getDis12Status(), dataUnit.getDis12name());

                drawDeviceConnectionsForType3(g, xbase, k, yCoordinates, dataUnit.getDevice(), dataUnit.getDevicename());
            }
        }
    }

    private int calculateXBaseForUnit(int id) {
        int xbase = (id + 1) * 100;
        if (id % 2 == 0) {
            xbase += 100;
        }
        return xbase;
    }

    private int[] calculateYCoordinatesForType2(int unitIndex) {
        int yDis11, yDis12, yDis2, yBreaker, yDeviceStart, yDeviceEnd;
        this.y1 = this.y0 - 10;  // 上部基准Y坐标
        this.y2 = this.y0 + 10;  // 下部基准Y坐标

        if (unitIndex % 2 == 1) {  // 如果单元索引是奇数
            yDis11 = this.y1 - 40;
            yDis2 = this.y1 - 160;
            yBreaker = this.y1 - 120;
            yDeviceStart = this.y1 - 60;
            yDeviceEnd = this.y1 - 250;
            yDis12 = yDis11;  // DIS12 的 Y 坐标与 DIS11 相同
        } else {  // 如果单元索引是偶数
            yDis11 = this.y2 + 20;
            yDis2 = this.y2 + 140;
            yBreaker = this.y2 + 80;
            yDeviceStart = this.y2 + 60;
            yDeviceEnd = this.y2 + 250;
            yDis12 = yDis11;  // DIS12 的 Y 坐标与 DIS11 相同
        }

        return new int[]{yDis11, yDis12, yDis2, yBreaker, yDeviceStart, yDeviceEnd};
    }


    private int[] calculateYCoordinatesForType3(int k) {
        int y_d1 = 120 + k * 160;
        int y_brk = y_d1 + 40;
        int y_d2 = y_brk + 60;
        return new int[]{y_d1, y_brk, y_d2};
    }

    private void drawMainLinesForType3(Graphics g) {
        g.drawLine(200, 100, 200, 580);
        g.drawLine(400, 100, 400, 580);
        g.drawLine(600, 100, 600, 580);
    }

    private void drawConnectionLinesForType2(Graphics g, int xbase, int[] yCoordinates) {
        float lineWidth = 3.0F;
        ((Graphics2D)g).setStroke(new BasicStroke(lineWidth));
        g.setColor(Color.BLACK);

        int xleft = xbase - 30;
        int xright = xbase + 10;
        g.drawLine(xbase, yCoordinates[4], xbase, yCoordinates[5]);
        g.drawLine(xleft + 10, this.y1, xleft + 10, yCoordinates[4]);
        g.drawLine(xright + 10, this.y2, xright + 10, yCoordinates[4]);
        g.drawLine(xleft + 10, yCoordinates[4], xright + 10, yCoordinates[4]);
    }

    private void drawDeviceConnectionsForType3(Graphics g, int xbase, int k, int[] yCoordinates, ConductingEquipment device, String devicename) {
        if (k == 0) {
            g.drawLine(xbase - 90, yCoordinates[2] + 40, xbase, yCoordinates[2] + 40);
            g.drawLine(xbase - 90, yCoordinates[2] + 40, xbase - 90, yCoordinates[2] - 180);
            if (devicename != null) {
                paintDevice(g, 1, xbase - 90, yCoordinates[0] - 30, device);
            }
        } else if (k == 2) {
            if (devicename != null) {
                g.drawLine(xbase - 90, yCoordinates[2] + 45 - 160, xbase, yCoordinates[2] + 45 - 160);
                g.drawLine(xbase - 90, yCoordinates[2] + 45 - 160, xbase - 90, yCoordinates[2] + 260 - 160);
                paintDevice(g, 2, xbase - 90, yCoordinates[2] + 50, device);
            }
        }
    }

    private void paintDevice(Graphics g, int id, int xbase, int y, ConductingEquipment eq) {
        int direction = id % 2;
        String deviceName = eq.getName();
        String P = this.data.get(id - 1).getP();
        String Q = this.data.get(id - 1).getQ();

        if (eq instanceof ACLineSegment) {
            paintACLineSegment(g, xbase, y, direction, deviceName, P, Q);
        } else if (eq instanceof TransformerWinding) {
            paintTransformerWinding(g, xbase, y, direction, deviceName, P, Q);
        } else if (eq instanceof SynchronousMachine) {
            paintSynchronousMachine(g, xbase, y, direction, deviceName, P, Q);
        } else if (eq instanceof Energyconsumer) {
            paintEnergyConsumer(g, xbase, y, direction, deviceName, ((Energyconsumer) eq).getPfixed(), ((Energyconsumer) eq).getQfixed());
        }
    }

    private void paintACLineSegment(Graphics g, int xbase, int y, int direction, String deviceName, String P, String Q) {
        g.setColor(Color.BLACK);
        int y1 = y;
        for (int i = 0; i < 4; i++) {
            if (direction == 1) {
                g.drawLine(xbase - 5, y1, xbase, y1 - 10);
                g.drawLine(xbase + 5, y1, xbase, y1 - 10);
                y1 -= 15;
            } else {
                g.drawLine(xbase - 5, y1, xbase, y1 + 10);
                g.drawLine(xbase + 5, y1, xbase, y1 + 10);
                y1 += 15;
            }
        }
        drawDeviceText(g, xbase, y, direction, deviceName, P, Q);
    }

    private void paintTransformerWinding(Graphics g, int xbase, int y, int direction, String deviceName, String P, String Q) {
        int yOffset = (direction == 1) ? -30 : 30;
        int y_4 = (direction == 1) ? y - 50 : y + 50;
        g.setColor(Color.WHITE);
        g.fillOval(xbase - 15, y_4, 30, 30);
        g.fillOval(xbase - 15, y + yOffset, 30, 30);
        g.setColor(Color.BLACK);
        g.drawOval(xbase - 15, y_4, 30, 30);
        g.drawOval(xbase - 15, y + yOffset, 30, 30);
        drawDeviceText(g, xbase, y + yOffset, direction, deviceName, P, Q);
    }

    private void paintSynchronousMachine(Graphics g, int xbase, int y, int direction, String deviceName, String P, String Q) {
        y += (direction == 1) ? -60 : 30;
        g.setColor(Color.WHITE);
        g.fillOval(xbase - 15, y, 30, 30);
        g.setColor(Color.BLACK);
        g.drawOval(xbase - 15, y, 30, 30);
        g.drawString(" G ", xbase - 10, y + 20);
        drawDeviceText(g, xbase, y, direction, deviceName, P, Q);
    }

    private void paintEnergyConsumer(Graphics g, int xbase, int y, int direction, String deviceName, String Pfixed, String Qfixed) {
        int[] px = new int[3];
        int[] py = new int[3];
        y += (direction == 1) ? -30 : 30;
        px[0] = xbase - 15;
        px[1] = xbase + 15;
        px[2] = xbase;
        py[0] = y;
        py[1] = y;
        py[2] = (direction == 1) ? y - 26 : y + 26;
        g.setColor(Color.BLACK);
        g.fillPolygon(px, py, 3);
        drawDeviceText(g, xbase, y, direction, deviceName, Pfixed, Qfixed);
    }

    private void drawDeviceText(Graphics g, int xbase, int y, int direction, String deviceName, String P, String Q) {
        int yOffsetName = (direction == 1) ? -25 : 25;
        int yOffsetP = (direction == 1) ? -35 : 15;
        int yOffsetQ = (direction == 1) ? -15 : 35;

        g.setColor(Color.BLACK);
        g.drawString(deviceName, xbase - 30, y + yOffsetName);
        g.setColor(Color.RED);
        g.drawString("P=" + P, xbase + 15, y + yOffsetP);
        g.setColor(new Color(128, 0, 128));
        g.drawString("Q=" + Q, xbase + 15, y + yOffsetQ);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (this.stationNo == -1) {
            setBackground(Color.WHITE);
            g.drawImage(this.img, 100, 0, 800, 700, 0, 0, 2600, 2600, this);
            return;
        }
        setBackground(Color.PINK);
        initializeGraphics(g);

        if (this.eqSet == null || this.eqSet.isEmpty())
            return;

        drawBusbarSections(g);
        drawStationUnits(g);
        drawStationInfo(g);
    }

    private void initializeGraphics(Graphics g) {
        float lineWidth = 6.0F;
        ((Graphics2D) g).setStroke(new BasicStroke(lineWidth));
        g.setColor(Color.RED);
        g.setFont(new Font("Times New Roman", Font.PLAIN, 14));
    }

    private void drawBusbarSections(Graphics g) {
        BusbarSection busbar1 = null, busbar2 = null;
        for (ConductingEquipment eq : this.eqSet) {
            if (eq instanceof BusbarSection) {
                if (eq.getName().contains("-II")) {
                    busbar2 = (BusbarSection) eq;
                } else {
                    busbar1 = (BusbarSection) eq;
                }
            }
        }

        TopologicalNode tp1 = ((Terminal) busbar1.getTerminals().get(0)).getConnectivityNode().getTopologicalNode();
        TopologicalNode tp2 = (busbar2 != null) ? ((Terminal) busbar2.getTerminals().get(0)).getConnectivityNode().getTopologicalNode() : null;

        drawBusbarLines(g, busbar1, busbar2, tp1, tp2);
    }

    private void drawBusbarLines(Graphics g, BusbarSection busbar1, BusbarSection busbar2, TopologicalNode tp1, TopologicalNode tp2) {
        if (this.stationType == 1) {
            g.drawLine(300, this.y0, 400, this.y0);
            g.setColor(Color.BLACK);
            g.drawString(busbar1.getName(), 260, this.y0);
        } else if (this.stationType == 2) {
            this.y1 = this.y0 - 10;
            this.y2 = this.y0 + 10;
            g.drawLine(50, this.y1, 800, this.y1);
            if (tp1 != tp2) g.setColor(Color.GREEN);
            g.drawLine(50, this.y2, 800, this.y2);
            g.setColor(Color.BLACK);
            g.drawString(busbar1.getName(), 20, this.y1 - 10);
            g.drawString(busbar2.getName(), 20, this.y2 + 20);
        } else if (this.stationType == 3) {
            this.y1 = 100;
            this.y2 = 580;
            g.drawLine(50, this.y1, 800, this.y1);
            if (tp1 != tp2) g.setColor(Color.GREEN);
            g.drawLine(50, this.y2, 800, this.y2);
            g.setColor(Color.BLACK);
            g.drawString(busbar1.getName(), 20, this.y1 - 10);
            g.drawString(busbar2.getName(), 20, this.y2 + 20);
        }
    }

    private void drawStationUnits(Graphics g) {
        int nUnit = this.data.size();
        Collections.sort(this.data);
        if (this.stationType == 1) {
            for (int i = 0; i < nUnit; i++) paintType1Unit(g, i + 1);
        } else if (this.stationType == 2) {
            for (int i = 1; i < nUnit; i++) paintType2Unit(g, i);
            paintConUnit(g, nUnit);
        } else if (this.stationType == 3) {
            paintType3Unit(g);
        }
    }

    private void drawStationInfo(Graphics g) {
        g.setColor(Color.BLACK);
        g.setFont(new Font("Times New Roman", Font.PLAIN, 20));
        g.drawString("Station #" + this.stationNo, 350, this.stationType == 3 ? 20 : 50);
        System.out.println("End paint");
        System.out.println(new Date().toString());
    }

    private boolean isBrkClicked(int mx, int my, int rx, int ry) {
        return mx >= rx && mx <= rx + 20 && my >= ry && my <= ry + 30;
    }

    private boolean isDisClicked(int mx, int my, int rx, int ry) {
        if (mx < rx || mx > rx + 20 || my < ry || my > ry + 20) return false;
        return (mx - rx - 10) * (mx - rx - 10) + (my - ry - 10) * (my - ry - 10) <= 100;
    }

    public void mouseClicked(MouseEvent e) {
        int x = e.getX();  // 获取点击位置的X坐标
        int y = e.getY();  // 获取点击位置的Y坐标
        boolean flag = false;

        // 遍历所有绘图单元
        for (int j = 0; j < this.paintData.size(); j++) {
            PaintUnit paintUnit = this.paintData.get(j);  // 获取当前单元的PaintUnit对象
            SCADADataUnit scadaDataUnit = this.data.get(j);  // 获取当前单元的SCADADataUnit对象

            // 检查并切换状态
            if (checkAndToggleStatus(scadaDataUnit, paintUnit, x, y)) {
                this.lastClickUnit = j;  // 更新最后点击的单元索引
                flag = true;  // 标记已找到并切换状态的元素
                break;  // 跳出循环
            }
        }

        // 如果有状态被切换，则执行强制更新操作
        if (flag) {
            performForceUpdate();
        }
    }


    private boolean checkAndToggleStatus(SCADADataUnit scadaDataUnit, PaintUnit paintUnit, int clickX, int clickY) {
        // 检查并切换开关状态（DIS11）
        if (checkAndToggleDisconnector(scadaDataUnit, clickX, clickY,
                paintUnit.getX(PaintUnit.CoordinateType.DIS11), paintUnit.getY(PaintUnit.CoordinateType.DIS11),
                scadaDataUnit.getDis11(), scadaDataUnit.getDis11name(), scadaDataUnit.getDis11Status(),
                status -> scadaDataUnit.setDis11Status(status))) {
            return true;
        }

        // 检查并切换开关状态（DIS12），仅适用于 stationtype > 1
        if (this.stationType > 1 && checkAndToggleDisconnector(scadaDataUnit, clickX, clickY,
                paintUnit.getX(PaintUnit.CoordinateType.DIS12), paintUnit.getY(PaintUnit.CoordinateType.DIS12),
                scadaDataUnit.getDis12(), scadaDataUnit.getDis12name(), scadaDataUnit.getDis12Status(),
                status -> scadaDataUnit.setDis12Status(status))) {
            return true;
        }

        // 检查并切换开关状态（DIS2）
        if (checkAndToggleDisconnector(scadaDataUnit, clickX, clickY,
                paintUnit.getX(PaintUnit.CoordinateType.DIS2), paintUnit.getY(PaintUnit.CoordinateType.DIS2),
                scadaDataUnit.getDis2(), scadaDataUnit.getDis2name(), scadaDataUnit.getDis2Status(),
                status -> scadaDataUnit.setDis2Status(status))) {
            return true;
        }

        // 检查并切换断路器状态（BRK）
        return checkAndToggleBreaker(scadaDataUnit, clickX, clickY,
                paintUnit.getX(PaintUnit.CoordinateType.BRK), paintUnit.getY(PaintUnit.CoordinateType.BRK));
    }


    private boolean checkAndToggleDisconnector(SCADADataUnit scadaDataUnit, int clickX, int clickY, int elementX, int elementY,
                                               Object disconnector, String disName, boolean currentStatus, Consumer<Boolean> toggleAction) {
        if (isDisClicked(clickX, clickY, elementX, elementY)) {
            System.out.println(disName + " is clicked");
            boolean newStatus = !currentStatus;
            toggleAction.accept(newStatus);
            this.model.writeRTDB((Switch) disconnector, newStatus ? "1" : "0",true);
            return true;
        }
        return false;
    }

    private boolean checkAndToggleBreaker(SCADADataUnit scadaDataUnit, int clickX, int clickY, int breakerX, int breakerY) {
        if (isBrkClicked(clickX, clickY, breakerX, breakerY)) {
            System.out.println(scadaDataUnit.getBrkname() + " is clicked @ " + new Date().toString());
            scadaDataUnit.setBrkStatus(!scadaDataUnit.getBrkStatus());
            this.model.writeRTDB(scadaDataUnit.getBrk(), scadaDataUnit.getBrkStatus() ? "1" : "0",true);
            return true;
        }
        return false;
    }


    private void performForceUpdate() {
        System.out.println("Force update");
        System.out.println(new Date().toString());
        long t0 = System.currentTimeMillis();
        MyThread.safeSleep(1000L);
        System.out.println(new Date().toString());
        long t1 = System.currentTimeMillis();
        System.out.println("dt=" + (t1 - t0));
        this.model.updateMeas(this.eqSet);
        this.model.buildTopo();
        updateDataUnit();
        paint(getGraphics());
        System.out.println("Force update finished");
        System.out.println(new Date().toString());
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }


}
