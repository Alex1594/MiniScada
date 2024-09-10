package com.GridTopologyAnalysis;


import com.CIMModel.*;
import com.Utils.FileReader4EFormat;
import com.cloudpss.eventchain.ControlParam;
import com.cloudpss.eventchain.EventMessage;
import com.cloudpss.eventchain.SimulationControl;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;
import net.spy.memcached.MemcachedClient;

public class Analysis {
    private FileReader4EFormat reader;
    private HashMap<String, HashMap<String, Object>> dataMap;
    private HashMap<String, ConnectivityNode> connectivityNodes = new HashMap<>();
    private HashMap<String, Terminal> terminals = new HashMap<>();
    private HashMap<String, BusbarSection> busbars = new HashMap<>();
    private HashMap<String, Disconnector> disconnectors = new HashMap<>();
    private HashMap<String, Breaker> breakers = new HashMap<>();
    private HashMap<String, Energyconsumer> loads = new HashMap<>();
    private HashMap<String, SynchronousMachine> generators = new HashMap<>();
    private HashMap<String, ACLineSegment> lines = new HashMap<>();
    private HashMap<String, TransformerWinding> transformerWindings = new HashMap<>();
    private HashMap<String, Measurement> measurements = new HashMap<>();
    private HashMap<String, Boolean> visit = new HashMap<>();

    private HashMap<ConnectivityNode, TopologicalNode> cnToTopoNode = new HashMap<>();
    private HashMap<TopologicalNode, TopologicalIsland> topoNodeToIsland = new HashMap<>();
    private List<TopologicalNode> allTopoNodes = new ArrayList<>();
    private List<TopologicalIsland> allIslands = new ArrayList<>();

    private MemcachedClient memcachedClient;
    private SimulationControl control;
    private Properties controlConfig = new Properties();
    private HashMap<String, String> controlMap = new HashMap<>();

    private String lastCmdKey;
    private int nToponode = 0;
    private int nIsland = 0;
    private boolean isWriting = false;
    private boolean isInitialized = false;

    public Analysis(String filename) {
        this.reader = new FileReader4EFormat();
        this.reader.setConfigFile(getClass().getClassLoader().getResourceAsStream("resource/QSconfig.properties"));
        this.reader.setSourceFile(getClass().getClassLoader().getResourceAsStream(filename));
        try {
            this.memcachedClient = new MemcachedClient(new InetSocketAddress[] { new InetSocketAddress("166.111.60.221", 11222) });
        } catch (IOException e) {
            e.printStackTrace();
        }
        InputStream is = getClass().getClassLoader().getResourceAsStream("resource/control.properties");
        this.controlConfig = new Properties();
        try {
            this.controlConfig.load(is);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.controlMap = new HashMap<>();
        Iterator<String> it2 = this.controlConfig.stringPropertyNames().iterator();
        while (it2.hasNext()) {
            String key = it2.next();
            this.controlMap.put(key, this.controlConfig.getProperty(key));
        }
    }

    public void writeRTDB(Switch sw, String val, boolean logFlag) {
        setIsWriting(true);
        String key = ((Measurement) sw.getMeasurements().get(0)).getName();

        if (key == null) {
            System.err.println("Measurement key is null for switch: " + sw.getName());
            return;
        }

        if (logFlag) {
            System.out.printf("**** write %s val = %s [ %s ]%n", key, val, sw.getName());
        }

        handleSwitchControl(sw, key, val, logFlag);
        setIsWriting(false);
    }

    private void handleSwitchControl(Switch sw, String key, String val, boolean logFlag) {
        if (sw instanceof Disconnector) {
            memcachedClient.set(key, 900, val);
            return;
        }

        String controlKey = controlMap.get(key);
        if (controlKey != null) {
            lastCmdKey = key;
            String taskId = (String) memcachedClient.get("TASKID");
            control = new SimulationControl(taskId);

            if (taskId != null && control != null) {
                ControlParam ctrlParam = new ControlParam(controlKey, val, new EventMessage(key + " is set to " + val));
                control.control(ctrlParam);

                if (logFlag) {
                    System.out.printf("### write to CloudPSS %s key=%s val=%s [ %s ] @ %s%n", key, controlKey, val, sw.getName(), new Date());
                }

                memcachedClient.set("LASTCMDKEY", 900, lastCmdKey);
            }
        } else {
            memcachedClient.set(key, 900, val);
        }
    }

    public void init() {
        reader.parseQSfile();
        dataMap = reader.getDataMap();
        createAllEntities();
        isInitialized = true;
    }

    public boolean isInit() {
        return isInitialized;
    }

    private void createAllEntities() {
        createConnectivityNodes();
        createTerminals();
        createBusbars();
        createDisconnectors();
        createBreakers();
        createEnergyConsumers();
        createSynchronousMachines();
        createACLines();
        createTransformers();
        createMeasurements();
    }

    private void createConnectivityNodes() {
        String tag = "Connectivitynode";
        Map<String, Object> tagMap = dataMap.get(tag);

        if (tagMap != null) {
            tagMap.forEach((key, value) -> {
                Map<String, String> data = (HashMap<String, String>)tagMap.get(key);
                ConnectivityNode node = new ConnectivityNode("C_" + (String)data.get("ID"));
                node.setName(data.get("name"));
                connectivityNodes.put(node.getId(), node);
            });
        }
    }

    private void createTerminals() {
        String tag = "Terminal";
        Map<String, Object> tagMap = dataMap.get(tag);

        if (tagMap != null) {
            tagMap.forEach((key, value) -> {
                Map<String, String> data = (HashMap<String, String>)tagMap.get(key);
                Terminal terminal = new Terminal("T_" + data.get("ID"));
                terminal.setName(data.get("name"));
                ConnectivityNode cn = connectivityNodes.get("C_" + data.get("connectivitynode_id"));

                if (cn != null) {
                    terminal.setConnectivityNode(cn);
                    terminals.put(terminal.getId(), terminal);
                } else {
                    System.err.println("Can't find ConnectivityNode for Terminal " + terminal.getName());
                }
            });
        }
    }

    // 创建母线段 (BusbarSection)
    private void createBusbars() {
        String tag = "BusbarSection";
        Map<String, Object> tagMap = dataMap.get(tag);

        if (tagMap != null) {
            tagMap.forEach((key, value) -> {
                Map<String, String> data =  (HashMap<String, String>)tagMap.get(key);
                BusbarSection busbar = new BusbarSection(data.get("ID"));
                busbar.setName(data.get("name"));
                String terminal_id = data.get("terminal0_id");
                Terminal terminal = this.terminals.get("T_" + terminal_id);
                terminal.setConductingEquipment((ConductingEquipment)busbar);
                List<Terminal> ts = busbar.getTerminals();
                busbars.put(data.get("ID"), busbar);
            });
        }
    }

    // 创建隔离开关 (Disconnector)
    private void createDisconnectors() {
        String tag = "Disconnector";
        Map<String, Object> tagMap = dataMap.get(tag);

        if (tagMap != null) {
            tagMap.forEach((key, value) -> {
                Map<String, String> data =  (HashMap<String, String>)tagMap.get(key);
                Disconnector disconnector = new Disconnector("DIS_" + (String)data.get("ID"));
                disconnector.setName(data.get("name"));
                disconnector.setNormalOpen(data.get("normalOpen"));
                String terminal0_id = data.get("terminal0_id");
                String terminal1_id = data.get("terminal1_id");
                Terminal terminal0 = this.terminals.get("T_" + terminal0_id);
                terminal0.setConductingEquipment((ConductingEquipment)disconnector);
                Terminal terminal1 = this.terminals.get("T_" + terminal1_id);
                terminal1.setConductingEquipment((ConductingEquipment)disconnector);
                disconnectors.put(data.get("ID"), disconnector);
            });
        }
    }

    // 创建断路器 (Breaker)
    private void createBreakers() {
        String tag = "Breaker";
        Map<String, Object> tagMap = dataMap.get(tag);

        if (tagMap != null) {
            tagMap.forEach((key, value) -> {
                Map<String, String> data =  (HashMap<String, String>)tagMap.get(key);
                Breaker breaker = new Breaker("BRK_" + (String)data.get("ID"));
                breaker.setName(data.get("name"));
                breaker.setNormalOpen(data.get("normalOpen"));
                String terminal0_id = data.get("terminal0_id");
                String terminal1_id = data.get("terminal1_id");
                Terminal terminal0 = this.terminals.get("T_" + terminal0_id);
                terminal0.setConductingEquipment((ConductingEquipment)breaker);
                Terminal terminal1 = this.terminals.get("T_" + terminal1_id);
                terminal1.setConductingEquipment((ConductingEquipment)breaker);
                breakers.put(data.get("ID"), breaker);
            });
        }
    }

    // 创建负载 (Energyconsumer)
    private void createEnergyConsumers() {
        String tag = "Energyconsumer";
        Map<String, Object> tagMap = dataMap.get(tag);

        if (tagMap != null) {
            tagMap.forEach((key, value) -> {
                Map<String, String> data =  (HashMap<String, String>)tagMap.get(key);
                Energyconsumer load = new Energyconsumer(data.get("ID"));
                load.setName(data.get("name"));
                String terminal_id = data.get("terminal0_id");
                Terminal terminal = this.terminals.get("T_" + terminal_id);
                terminal.setConductingEquipment((ConductingEquipment)load);
                loads.put(data.get("ID"), load);
            });
        }
    }

    // 创建发电机 (SynchronousMachine)
    private void createSynchronousMachines() {
        String tag = "SynchronousMachine";
        Map<String, Object> tagMap = dataMap.get(tag);

        if (tagMap != null) {
            tagMap.forEach((key, value) -> {
                Map<String, String> data =  (HashMap<String, String>)tagMap.get(key);
                SynchronousMachine generator = new SynchronousMachine(data.get("ID"));
                generator.setName(data.get("name"));
                String terminal_id = data.get("terminal0_id");
                Terminal terminal = this.terminals.get("T_" + terminal_id);
                terminal.setConductingEquipment((ConductingEquipment)generator);
                generators.put(data.get("ID"), generator);
            });
        }
    }

    // 创建输电线路 (ACLineSegment)
    private void createACLines() {
        String tag = "Aclinesegment";
        Map<String, Object> tagMap = dataMap.get(tag);

        if (tagMap != null) {
            tagMap.forEach((key, value) -> {
                Map<String, String> data = (Map<String, String>) value;
                ACLineSegment line = new ACLineSegment(data.get("ID"));
                line.setName(data.get("name"));
                String terminal0_id = data.get("terminal0_id");
                String terminal1_id = data.get("terminal1_id");
                Terminal terminal0 = this.terminals.get("T_" + terminal0_id);
                terminal0.setConductingEquipment((ConductingEquipment)line);
                Terminal terminal1 = this.terminals.get("T_" + terminal1_id);
                terminal1.setConductingEquipment((ConductingEquipment)line);
                lines.put(data.get("ID"), line);
            });
        }
    }

    // 创建变压器绕组 (TransformerWinding)
    private void createTransformers() {
        String tag = "Powertransformer";
        Map<String, Object> tagMap = dataMap.get(tag);

        if (tagMap != null) {
            tagMap.forEach((key, value) -> {
                Map<String, String> data = (Map<String, String>) value;
                TransformerWinding transformer = new TransformerWinding( data.get("ID"));
                transformer.setName(data.get("name"));
                String terminal0_id = data.get("terminal0_id");
                String terminal1_id = data.get("terminal1_id");
                Terminal terminal0 = this.terminals.get("T_" + terminal0_id);
                terminal0.setConductingEquipment((ConductingEquipment)transformer);
                Terminal terminal1 = this.terminals.get("T_" + terminal1_id);
                terminal1.setConductingEquipment((ConductingEquipment)transformer);
                transformerWindings.put(transformer.getId(), transformer);
            });
        }
    }

    // 创建测量值 (Measurement)
    private void createMeasurements() {
        String tag = "MeasureMent";
        Map<String, Object> tagMap = this.dataMap.get(tag);

        if (tagMap == null) {
            System.err.println("No measurement data found.");
            return;
        }

        // 获取并排序键值数组
        Set<String> keySet = tagMap.keySet();
        Object[] sortedKeys = keySet.toArray();
        Arrays.sort(sortedKeys);

        // 遍历键并创建测量对象
        Object[] array = sortedKeys;
        for (int i = array.length ,  b = 0; b < i; b++) {
            Object key = array[b];
            Map<String, String> data = (HashMap<String, String>)tagMap.get(key);

            // 查找终端并获取相关设备
            Terminal terminal = this.terminals.get("T_" + data.get("terminal0_id"));
            if (terminal == null) {
                System.err.println("Terminal not found for ID: " + data.get("terminal0_id"));
                continue;
            }
            ConductingEquipment equipment = terminal.getConductingEquipment();

            // 创建测量对象并设置属性
            Measurement measurement = new Measurement(data.get("ID"));
            measurement.setName(data.get("name"));
            measurement.setMeastype((equipment instanceof Breaker || equipment instanceof Disconnector) ? 1 : 0);
            measurement.setTerminal(terminal);

            // 将测量对象放入测量集合中
            this.measurements.put(measurement.getName(), measurement);
        }
    }


    // Update measurements for all devices
    public void updateMeas() {
        for (String key : this.measurements.keySet()) {
            Measurement m = this.measurements.get(key);
            ConductingEquipment eq = (ConductingEquipment) m.getMember();
            String value = (String) this.memcachedClient.get(key);

            // Check if the key matches the last command key for logging
            if (this.lastCmdKey != null && this.lastCmdKey.equals(key)) {
                System.out.println("Last command key: " + key + " value = " + value);
            }

            // Process measurements for Breakers and Disconnectors
            if (value == null) {
                if (eq instanceof Breaker || eq instanceof Disconnector) {
                    Switch sw = (Switch) eq;
                    if (sw.getNormalOpen().equals("1")) {
                        m.setDiscreteValue(false);
                        writeRTDB(sw, "0", false);
                    } else {
                        m.setDiscreteValue(true);
                        writeRTDB(sw, "1", false);
                    }
                }
            } else {
                // For breakers and disconnectors, set the discrete value
                if (eq instanceof Breaker || eq instanceof Disconnector) {
                    m.setDiscreteValue(value.equals("1"));
                }
            }
        }
    }

    // Update measurements for a specific set of ConductingEquipment
    public void updateMeas(Set<ConductingEquipment> eqs) {
        System.out.println("========" + new Date() + " Update Meas ========");
        System.out.println("Memcached UpdateTime = " + this.memcachedClient.get("UpdateTime"));

        if (eqs == null) {
            System.err.println("Set of equipment is null.");
            return;
        }

        for (ConductingEquipment eq : eqs) {
            List<Measurement> meas = eq.getMeasurements();
            if (meas == null) continue;

            for (Measurement m : meas) {
                String key = m.getName();
                String value = (String) this.memcachedClient.get(key);

                if (value == null) continue;

                // Handle discrete values for Breakers and Disconnectors
                if (eq instanceof Breaker || eq instanceof Disconnector) {
                    m.setDiscreteValue(value.equals("1"));
                }

                // Additional logging for breakers
                if (eq instanceof Breaker && key.equals(this.lastCmdKey)) {
                    System.out.println("Memcached key: " + key + " value = " + value);
                }
            }
        }
    }

//    // 获取设备 (根据 ID 查找设备)
//    public ConductingEquipment getDevice(String id) {
//        if (busbars.containsKey(id)) return busbars.get(id);
//        if (disconnectors.containsKey(id)) return disconnectors.get(id);
//        if (breakers.containsKey(id)) return breakers.get(id);
//        if (loads.containsKey(id)) return loads.get(id);
//        if (generators.containsKey(id)) return generators.get(id);
//        if (lines.containsKey(id)) return lines.get(id);
//        if (transformerWindings.containsKey(id)) return transformerWindings.get(id);
//        return null;
//    }
//
//    // 获取开关状态
//    public boolean getSwitchStatus(Switch sw) {
//        if (sw.getNormalOpen().equals("1")) return false;
//
//        String key = ((Measurement) sw.getMeasurements().get(0)).getName();
//        String value = (String) memcachedClient.get(key);
//
//        // 如果值为 "0"，表示断开，否则闭合
//        return value == null || !value.equals("0");
//    }
//
//    private void createTopologicalNode(TopologicalNode topoNode, ConnectivityNode node, List<TopologicalNode> topoNodeList) {
//        if (topoNode == null) {
//            topoNode = new TopologicalNode();
//            this.nToponode++;
//            topoNode.setId("Tp_" + node.getName());
//            topoNode.setName("Tp_" + this.nToponode);
//            topoNodeList.add(topoNode);
//        }
//
//        node.setTopologicalNode(topoNode);
//        this.cnToTopoNode.put(node, topoNode);
//        topoNode.getConnectivityNodes().add(node);
//
//        // 获取并检查终端列表
//        List terminalList = node.getTerminals();
//        if (terminalList == null) {
//            System.err.println("No terminal found for ConnectivityNode with ID: " + node.getId());
//            return;
//        }
//
//        // 遍历每个终端
//        for (Object obj : terminalList) {
//            Terminal terminal = (Terminal)obj;
//            ConductingEquipment equipment = terminal.getConductingEquipment();
//
//            // 处理开关设备
//            if (equipment instanceof Switch) {
//                List crossingTerminals = equipment.getTerminals();
//                for (Object obje : crossingTerminals) {
//                    Terminal crossingTerminal = (Terminal)obje;
//                    if (crossingTerminal == terminal)
//                        continue;
//                    ConnectivityNode crossingCn = crossingTerminal.getConnectivityNode();
//                    if (this.cnToTopoNode.containsKey(crossingCn))
//                        continue;
//                    if (true) {
//                        this.cnToTopoNode.put(crossingCn, topoNode);
//                        createTopologicalNode(topoNode, crossingCn, topoNodeList);
//                    }
//                }
//                continue;
//            }
//            // 处理母线段 (BusbarSection)
//            if (equipment instanceof BusbarSection) {
//                List crossingTerminals = equipment.getTerminals();
//                for (Object obje : crossingTerminals) {
//                    Terminal crossingTerminal = (Terminal)obje;
//                    if (crossingTerminal == terminal)
//                        continue;
//                    ConnectivityNode crossingCn = crossingTerminal.getConnectivityNode();
//                    if (this.cnToTopoNode.containsKey(crossingCn))
//                        continue;
//                    this.cnToTopoNode.put(crossingCn, topoNode);
//                    createTopologicalNode(topoNode, crossingCn, topoNodeList);
//                }
//            }
//        }
//    }
//
//
//    // 构建拓扑节点
//    public void buildTopologicalNodes() {
//        this.cnToTopoNode.clear();
//        this.allTopoNodes.clear();
//        this.nToponode = 0;
//        this.nIsland = 0;
//        for (ConnectivityNode node : this.connectivityNodes.values()) {
//            if (this.cnToTopoNode.containsKey(node))
//                continue;
//            createTopologicalNode(null, node, this.allTopoNodes);
//        }
//    }
//
//    private void createTopoIslands(TopologicalIsland island, TopologicalNode node, List<TopologicalIsland> container) {
//        if (island == null) {
//            island = new TopologicalIsland();
//            this.nIsland++;
//            island.setName("Island_" + this.nIsland);
//            container.add(island);
//        }
//
//        // 关联拓扑节点和岛屿
//        this.topoNodeToIsland.put(node, island);
//        node.setTopologicalIsland(island);
//        island.getTopologicalNodes().add(node);
//
//        // 遍历 ConnectivityNodes
//        for (Object n : node.getConnectivityNodes()) {
//            ConnectivityNode cn = (ConnectivityNode) n;
//            List terminalList = cn.getTerminals();
//            if (terminalList == null) {
//                System.err.println("Terminal list is null for ConnectivityNode with ID: " + cn.getId());
//                continue;
//            }
//
//            // 遍历终端
//            for (Object obj : terminalList) {
//                Terminal terminal = (Terminal)obj;
//                ConductingEquipment equipment = terminal.getConductingEquipment();
//
//                if (equipment instanceof ACLineSegment) {
//                    Terminal otherTerminal;
//                    List<Terminal> tmpTerminalList = equipment.getTerminals();
//                    if (tmpTerminalList == null || tmpTerminalList.size() != 2) {
//                        System.err.println(" ACLineSegment " + equipment.getId() + " doesnot contains any terminals " +
//                                " or does not contain two terminals.");
//                        continue;
//                    }
//                    if (terminal == tmpTerminalList.get(0)) {
//                        otherTerminal = tmpTerminalList.get(1);
//                    } else {
//                        otherTerminal = tmpTerminalList.get(0);
//                    }
//                    ConnectivityNode otherNode = otherTerminal.getConnectivityNode();
//                    TopologicalNode tn = this.cnToTopoNode.get(otherNode);
//                    if (this.topoNodeToIsland.containsKey(tn) || tn == null)
//                        continue;
//                    this.topoNodeToIsland.put(tn, island);
//                    createTopoIslands(island, tn, container);
//                    continue;
//                }
//
//                // 处理 TransformerWinding
//                if (equipment instanceof TransformerWinding) {
//                    Terminal otherTerminal;
//                    List<Terminal> tmpTerminalList = equipment.getTerminals();
//                    if (tmpTerminalList == null || tmpTerminalList.size() != 2) {
//                        System.err.println(" TransformerWinding " + equipment.getId() + " doesnot contains any terminals " +
//                                " or does not contain two terminals.");
//                        continue;
//                    }
//                    if (terminal == tmpTerminalList.get(0)) {
//                        otherTerminal = tmpTerminalList.get(1);
//                    } else {
//                        otherTerminal = tmpTerminalList.get(0);
//                    }
//                    ConnectivityNode otherNode = otherTerminal.getConnectivityNode();
//                    TopologicalNode tn = this.cnToTopoNode.get(otherNode);
//                    if (this.topoNodeToIsland.containsKey(tn) || tn == null)
//                        continue;
//                    this.topoNodeToIsland.put(tn, island);
//                    createTopoIslands(island, tn, container);
//                }
//            }
//        }
//    }
//
//    // Build the topology based on the connectivity nodes and terminals
//    public void buildTopo() {
//        buildTopologicalNodes();
//        this.topoNodeToIsland.clear();
//        this.allIslands.clear();
//
//        // Create islands for each topological node
//        for (TopologicalNode node : this.cnToTopoNode.values()) {
//            if (this.topoNodeToIsland.containsKey(node))
//                continue;
//            createTopoIslands(null, node, this.allIslands);
//        }
//
//        // Print the details of each island and associated nodes
//        for (ConnectivityNode node : this.cnToTopoNode.keySet()) {
//            TopologicalNode tpNode = this.cnToTopoNode.get(node);
//            if (tpNode.getTopologicalIsland().getName().equals("Island_1")) continue;
//
//            System.out.print("Connectivity Node = " + node.getName() + " TopoNode = " + tpNode.getName());
//            System.out.println(" Island = " + tpNode.getTopologicalIsland().getName());
//
//            for (Terminal t : node.getTerminals()) {
//                System.out.println("\t" + t.getName() + " " + t.getConductingEquipment().getName()  +" "+ t.getMeasurements().size());
//            }
//        }
//    }
private void getDevice(Terminal terminal, Set<ConductingEquipment> set, int level) {
    this.visit.put(terminal.getName(), true);
    StringBuilder prf = new StringBuilder();
    for (int i = 0; i < level; i++) {
        prf.append("\t");
    }

    ConductingEquipment equipment = terminal.getConductingEquipment();
    if (!set.contains(equipment)) {
        set.add(equipment);
        if (equipment instanceof Breaker || equipment instanceof Disconnector) {
            for (Terminal otherTerminal : equipment.getTerminals()) {
                if (otherTerminal != terminal) {
                    getDevice(otherTerminal, set, level + 1);
                }
            }
        } else if (!(equipment instanceof BusbarSection)) {
            return;
        }
    }

    ConnectivityNode cn = terminal.getConnectivityNode();
    for (Terminal otherTerminal : cn.getTerminals()) {
        if (otherTerminal != terminal && this.visit.get(otherTerminal.getName()) == null) {
            getDevice(otherTerminal, set, level + 1);
        }
    }
}

    private boolean getSwitchStatus(Terminal terminal) {
        Switch equipment = (Switch) terminal.getConductingEquipment();
        List<Measurement> measurements = equipment.getMeasurements();

        if (!measurements.isEmpty()) {
            return measurements.get(0).getDiscreteValue();
        }

        return !"1".equalsIgnoreCase(equipment.getNormalOpen());
    }

    private void createTopologicalNode(TopologicalNode topoNode, ConnectivityNode node, List<TopologicalNode> topoNodeList) {
        if (topoNode == null) {
            topoNode = new TopologicalNode();
            this.nToponode++;
            topoNode.setId("Tp_" + node.getName());
            topoNode.setName("Tp_" + this.nToponode);
            topoNodeList.add(topoNode);
        }

        node.setTopologicalNode(topoNode);
        this.cnToTopoNode.put(node, topoNode);
        topoNode.getConnectivityNodes().add(node);

        List<Terminal> terminalList = node.getTerminals();
        if (terminalList == null) {
            System.err.println("No terminal found for ConnectivityNode with ID: " + node.getId());
            return;
        }

        for (Terminal terminal : terminalList) {
            ConductingEquipment equipment = terminal.getConductingEquipment();

            if (equipment instanceof Switch) {
                handleCrossingTerminals(topoNode, equipment, terminal, topoNodeList, true);
            } else if (equipment instanceof BusbarSection) {
                handleCrossingTerminals(topoNode, equipment, terminal, topoNodeList, false);
            }
        }
    }

    private void handleCrossingTerminals(TopologicalNode topoNode, ConductingEquipment equipment, Terminal currentTerminal,
                                         List<TopologicalNode> topoNodeList, boolean checkSwitchStatus) {
        for (Terminal crossingTerminal : equipment.getTerminals()) {
            if (crossingTerminal == currentTerminal) continue;

            ConnectivityNode crossingCn = crossingTerminal.getConnectivityNode();
            if (this.cnToTopoNode.containsKey(crossingCn)) continue;

            if (!checkSwitchStatus || getSwitchStatus(crossingTerminal)) {
                this.cnToTopoNode.put(crossingCn, topoNode);
                createTopologicalNode(topoNode, crossingCn, topoNodeList);
            }
        }
    }

    public void buildTopologicalNodes() {
        this.cnToTopoNode.clear();
        this.allTopoNodes.clear();
        this.nToponode = 0;
        this.nIsland = 0;

        for (ConnectivityNode node : this.connectivityNodes.values()) {
            if (!this.cnToTopoNode.containsKey(node)) {
                createTopologicalNode(null, node, this.allTopoNodes);
            }
        }
    }

    private Terminal getOtherTerminal(ConductingEquipment equipment, Terminal terminal) {
        List<Terminal> terminals = equipment.getTerminals();
        if (terminals == null || terminals.size() != 2) {
            System.err.println(equipment.getClass().getSimpleName() + " " + equipment.getId() + " does not contain exactly two terminals.");
            return null;
        }
        return terminal == terminals.get(0) ? terminals.get(1) : terminals.get(0);
    }

    private void createTopoIslands(TopologicalIsland island, TopologicalNode node, List<TopologicalIsland> container) {
        if (island == null) {
            island = new TopologicalIsland();
            this.nIsland++;
            island.setName("Island_" + this.nIsland);
            container.add(island);
        }

        this.topoNodeToIsland.put(node, island);
        node.setTopologicalIsland(island);
        island.getTopologicalNodes().add(node);

        for (ConnectivityNode cn : node.getConnectivityNodes()) {
            List<Terminal> terminalList = cn.getTerminals();
            if (terminalList == null) {
                System.err.println("Terminal list is null for ConnectivityNode with ID: " + cn.getId());
                continue;
            }

            for (Terminal terminal : terminalList) {
                ConductingEquipment equipment = terminal.getConductingEquipment();

                if (equipment instanceof ACLineSegment || equipment instanceof TransformerWinding) {
                    Terminal otherTerminal = getOtherTerminal(equipment, terminal);
                    if (otherTerminal == null) continue;

                    ConnectivityNode otherNode = otherTerminal.getConnectivityNode();
                    TopologicalNode tn = this.cnToTopoNode.get(otherNode);
                    if (this.topoNodeToIsland.containsKey(tn) || tn == null) continue;

                    this.topoNodeToIsland.put(tn, island);
                    createTopoIslands(island, tn, container);
                }
            }
        }
    }

    public void buildTopo() {
        buildTopologicalNodes();
        this.topoNodeToIsland.clear();
        this.allIslands.clear();

        for (TopologicalNode node : this.cnToTopoNode.values()) {
            if (!this.topoNodeToIsland.containsKey(node)) {
                createTopoIslands(null, node, this.allIslands);
            }
        }

        for (ConnectivityNode node : this.cnToTopoNode.keySet()) {
            TopologicalNode topoNode = this.cnToTopoNode.get(node);
            if (!topoNode.getTopologicalIsland().getName().equals("Island_1")) {
                System.out.print("cn=" + node.getName() + "\ttp=" + topoNode.getName());
                System.out.println("\tisland=" + topoNode.getTopologicalIsland().getName());

                for (Terminal terminal : node.getTerminals()) {
                    System.out.println("\t" + terminal.getName() + " " + terminal.getConductingEquipment().getName() + " " + terminal.getMeasurements().size());
                }
            }
        }
    }

    public Set<ConductingEquipment> updateStation(int stno) {
        Set<ConductingEquipment> set = new HashSet<>();

        for (BusbarSection bus : this.busbars.values()) {
            String busName = bus.getName();
            if (busName.equalsIgnoreCase("BUS" + stno) || busName.contains("BUS" + stno)) {
                Terminal terminal = bus.getTerminals().get(0);
                System.out.println("\n\n--------------------------" + terminal.getName());
                this.visit = new HashMap<>();
                getDevice(terminal, set, 0);

                for (ConductingEquipment device : set) {
                    System.out.println(device.getName());
                }
            }
        }

        return set;
    }

    // Gracefully stop the model update process
    public void stopModelupdate() {
        if (this.memcachedClient != null) {
            this.memcachedClient.shutdown();
        }
    }


    private void setIsWriting(boolean value) {
        isWriting = value;
    }

    public int getNisland() {
        return this.allIslands.size();
    }

    public int getNTopoNode() {
        return this.allTopoNodes.size();
    }


    public static void main(String[] args) {
        Analysis model = new Analysis("resource/CIME-IEEE39.txt");
        model.init();
        System.out.println("*********************************************");
        //model.updateMeas();
        model.buildTopo();
    }

}
