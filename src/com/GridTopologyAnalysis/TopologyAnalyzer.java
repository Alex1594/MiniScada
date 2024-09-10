package com.GridTopologyAnalysis;

import com.CIMModel.*;
import com.Utils.FileReader4EFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class TopologyAnalyzer {


    private HashMap<String, ConnectivityNode> connectivityNodeMap;
    private HashMap<String, Terminal> terminalMap = new HashMap<>();
    private HashMap<String, Measurement> measurementMap = new HashMap<>();
    private HashMap<String, Disconnector> disconnectorMap = new HashMap<>();
    private HashMap<String, BusbarSection> busbarsectionMap = new HashMap<>();
    private HashMap<String, Breaker> breakerMap = new HashMap<>();
    private HashMap<String, ACLineSegment> aclinesegmentMap = new HashMap<>();
    private HashMap<String, SynchronousMachine> synchronousmachineMap = new HashMap<>();
    private HashMap<String, Energyconsumer> energyconsumerMap = new HashMap<>();
    private HashMap<String, Powertransformer> powertransformerMap = new HashMap<>();

    public TopologyAnalyzer() {
        connectivityNodeMap = new HashMap<>();
    }

    public void Connectivitynode_Util() {
        FileReader4EFormat myreader = new FileReader4EFormat();
        myreader.setConfigFile(ClassLoader.getSystemClassLoader().getResourceAsStream("resource/QSconfig.properties"));
        myreader.setSourceFile(ClassLoader.getSystemClassLoader().getResourceAsStream("resource/CIME-IEEE39.txt"));
        myreader.parseQSfile();

        HashMap<String, HashMap<String, Object>> dataMapcn = myreader.getDataMap();

        HashMap<String, Object> connectivityNodeDataMap = (HashMap<String, Object>) dataMapcn.get("Connectivitynode");

        connectivityNodeMap.clear();

        for (Object value : connectivityNodeDataMap.values()) {
            HashMap<String, Object> connectivityNodeData = (HashMap<String, Object>) value;
            String cnid = (String) connectivityNodeData.get("ID");
            String name = (String) connectivityNodeData.get("name");
            ConnectivityNode node = new ConnectivityNode(cnid);
            node.setName(name);
            connectivityNodeMap.put(cnid, node);
        }

        for (Map.Entry<String, ConnectivityNode> entry : connectivityNodeMap.entrySet()) {
            // System.out.println("Created ConnectivityNode with ID: " + entry.getKey() + " and name: " + entry.getValue().getName());
        }
    }

    public void Terminal_Util() {
        FileReader4EFormat myreader = new FileReader4EFormat();
        myreader.setConfigFile(ClassLoader.getSystemClassLoader().getResourceAsStream("resource/QSconfig.properties"));
        myreader.setSourceFile(ClassLoader.getSystemClassLoader().getResourceAsStream("resource/CIME-IEEE39.txt"));
        myreader.parseQSfile();

        HashMap<String, HashMap<String, Object>> dataMapte = myreader.getDataMap();

        HashMap<String, Object> terminalDataMap = (HashMap<String, Object>) dataMapte.get("Terminal");

        terminalMap.clear();

        for (Object value : terminalDataMap.values()) {
            HashMap<String, Object> terminalData = (HashMap<String, Object>) value;
            String idt = (String) terminalData.get("ID");
            String name = (String) terminalData.get("name");
            String connectivityNodeId = (String) terminalData.get("connectivitynode_id");

            Terminal ter = new Terminal(idt);
            ter.setName(name);
            terminalMap.put(idt, ter);

            ConnectivityNode node = connectivityNodeMap.get(connectivityNodeId);
            if (node != null) {
                node.addTerminal(ter);
            }
        }

        for (Map.Entry<String, Terminal> entry : terminalMap.entrySet()) {
            // System.out.println("Created Terminal with ID: " + entry.getKey() + " and name: " + entry.getValue().getName());
        }
    }

    public void Measurement_Util() {
        FileReader4EFormat myreader = new FileReader4EFormat();
        myreader.setConfigFile(ClassLoader.getSystemClassLoader().getResourceAsStream("resource/QSconfig.properties"));
        myreader.setSourceFile(ClassLoader.getSystemClassLoader().getResourceAsStream("resource/CIME-IEEE39.txt"));
        myreader.parseQSfile();

        HashMap<String, HashMap<String, Object>> dataMapme = myreader.getDataMap();

        HashMap<String, Object> measurementDataMap = (HashMap<String, Object>) dataMapme.get("MeasureMent");

        measurementMap.clear();

        for (Object value : measurementDataMap.values()) {
            HashMap<String, Object> measurementData = (HashMap<String, Object>) value;
            String id = (String) measurementData.get("ID");
            String name = (String) measurementData.get("name");
            String terminalId = (String) measurementData.get("terminal0_id");

            Measurement measurement = new Measurement(id);
            measurement.setName(name);
            measurementMap.put(id, measurement);

            Terminal terminal = terminalMap.get(terminalId);
            if (terminal != null) {
                terminal.addMeasurement(measurement);
            }
        }

        for (Map.Entry<String, Measurement> entry : measurementMap.entrySet()) {
            // System.out.println("Created Measurement with ID: " + entry.getKey() + " and name: " + entry.getValue().getName());
        }
    }

    public void Disconnector_Util() {
        FileReader4EFormat myreader = new FileReader4EFormat();
        myreader.setConfigFile(ClassLoader.getSystemClassLoader().getResourceAsStream("resource/QSconfig.properties"));
        myreader.setSourceFile(ClassLoader.getSystemClassLoader().getResourceAsStream("resource/CIME-IEEE39.txt"));
        myreader.parseQSfile();

        HashMap<String, HashMap<String, Object>> dataMap = myreader.getDataMap();
        HashMap<String, Object> disconnectorDataMap = (HashMap<String, Object>) dataMap.get("Disconnector");

        if (disconnectorDataMap == null) {
            System.out.println("No data found for 'Disconnector'");
            return;
        }

        for (Object value : disconnectorDataMap.values()) {
            HashMap<String, Object> disconnectorData = (HashMap<String, Object>) value;

            String id = (String) disconnectorData.get("ID");
            String name = (String) disconnectorData.get("name");
            String terminal0Id = (String) disconnectorData.get("terminal0_id");
            String terminal1Id = (String) disconnectorData.get("terminal1_id");
            String normalOpenStr = (String) disconnectorData.get("normalOpen");

            String normalOpen = normalOpenStr;

            Disconnector disconnector = new Disconnector(id);
            disconnector.setName(name);
            disconnector.setNormalOpen(normalOpen);

            Terminal terminal0 = terminalMap.get(terminal0Id);
            Terminal terminal1 = terminalMap.get(terminal1Id);

            if (terminal0 != null) {
                disconnector.setTerminal0(terminal0);
                terminal0.setConductingEquipment(disconnector);
            }
            if (terminal1 != null) {
                disconnector.setTerminal1(terminal1);
                terminal1.setConductingEquipment(disconnector);
            }

            disconnectorMap.put(id, disconnector);
        }

        for (Map.Entry<String, Disconnector> entry : disconnectorMap.entrySet()) {
            // System.out.println("Created Disconnector with ID: " + entry.getKey() + " and name: " + entry.getValue().getName());
            // System.out.println("Terminal0: " + (entry.getValue().getTerminal0() != null ? entry.getValue().getTerminal0().getName() : "null"));
            // System.out.println("Terminal1: " + (entry.getValue().getTerminal1() != null ? entry.getValue().getTerminal1().getName() : "null"));
        }
    }

    public void BusbarSection_Util() {
        FileReader4EFormat myreader = new FileReader4EFormat();
        myreader.setConfigFile(ClassLoader.getSystemClassLoader().getResourceAsStream("resource/QSconfig.properties"));
        myreader.setSourceFile(ClassLoader.getSystemClassLoader().getResourceAsStream("resource/CIME-IEEE39.txt"));
        myreader.parseQSfile();

        HashMap<String, HashMap<String, Object>> dataMap = myreader.getDataMap();
        HashMap<String, Object> busbarsectionDataMap = (HashMap<String, Object>) dataMap.get("BusbarSection");

        if (busbarsectionDataMap == null) {
            System.out.println("No data found for 'BusbarSection'");
            return;
        }

        for (Object value : busbarsectionDataMap.values()) {
            HashMap<String, Object> busbarsectionData = (HashMap<String, Object>) value;

            String id = (String) busbarsectionData.get("ID");
            String name = (String) busbarsectionData.get("name");
            String terminal0Id = (String) busbarsectionData.get("terminal0_id");

            BusbarSection busbarsection = new BusbarSection(id);
            busbarsection.setName(name);

            Terminal terminal0 = terminalMap.get(terminal0Id);

            if (terminal0 != null) {
                busbarsection.setTerminal0(terminal0);
                terminal0.setConductingEquipment(busbarsection);
            }

            busbarsectionMap.put(id, busbarsection);
        }

        for (Map.Entry<String, BusbarSection> entry : busbarsectionMap.entrySet()) {
            // System.out.println("Created BusbarSection with ID: " + entry.getKey() + " and name: " + entry.getValue().getName());
            // System.out.println("Terminal0: " + (entry.getValue().getTerminal0() != null ? entry.getValue().getTerminal0().getName() : "null"));
        }
    }

    public void Aclinesegment_Util() {
        FileReader4EFormat myreader = new FileReader4EFormat();
        myreader.setConfigFile(ClassLoader.getSystemClassLoader().getResourceAsStream("resource/QSconfig.properties"));
        myreader.setSourceFile(ClassLoader.getSystemClassLoader().getResourceAsStream("resource/CIME-IEEE39.txt"));
        myreader.parseQSfile();

        HashMap<String, HashMap<String, Object>> dataMap = myreader.getDataMap();
        HashMap<String, Object> aclinesegmentDataMap = (HashMap<String, Object>) dataMap.get("Aclinesegment");

        if (aclinesegmentDataMap == null) {
            System.out.println("No data found for 'Aclinesegment'");
            return;
        }

        for (Object value : aclinesegmentDataMap.values()) {
            HashMap<String, Object> aclinesegmentData = (HashMap<String, Object>) value;

            String id = (String) aclinesegmentData.get("ID");
            String name = (String) aclinesegmentData.get("name");
            String terminal0Id = (String) aclinesegmentData.get("terminal0_id");
            String terminal1Id = (String) aclinesegmentData.get("terminal1_id");

            ACLineSegment aclinesegment = new ACLineSegment(id);
            aclinesegment.setName(name);

            Terminal terminal0 = terminalMap.get(terminal0Id);
            Terminal terminal1 = terminalMap.get(terminal1Id);

            if (terminal0 != null) {
                aclinesegment.setTerminal0(terminal0);
                terminal0.setConductingEquipment(aclinesegment);
            }
            if (terminal1 != null) {
                aclinesegment.setTerminal1(terminal1);
                terminal1.setConductingEquipment(aclinesegment);
            }

            aclinesegmentMap.put(id, aclinesegment);
        }

        for (Map.Entry<String, ACLineSegment> entry : aclinesegmentMap.entrySet()) {
            // System.out.println("Created Aclinesegment with ID: " + entry.getKey() + " and name: " + entry.getValue().getName());
            // System.out.println("Terminal0: " + (entry.getValue().getTerminal0() != null ? entry.getValue().getTerminal0().getName() : "null"));
            // System.out.println("Terminal1: " + (entry.getValue().getTerminal1() != null ? entry.getValue().getTerminal1().getName() : "null"));
        }
    }

    public void Powertransformer_Util() {
        FileReader4EFormat myreader = new FileReader4EFormat();
        myreader.setConfigFile(ClassLoader.getSystemClassLoader().getResourceAsStream("resource/QSconfig.properties"));
        myreader.setSourceFile(ClassLoader.getSystemClassLoader().getResourceAsStream("resource/CIME-IEEE39.txt"));
        myreader.parseQSfile();

        HashMap<String, HashMap<String, Object>> dataMap = myreader.getDataMap();
        HashMap<String, Object> powertransformerDataMap = (HashMap<String, Object>) dataMap.get("Powertransformer");

        if (powertransformerDataMap == null) {
            System.out.println("No data found for 'Powertransformer'");
            return;
        }

        for (Object value : powertransformerDataMap.values()) {
            HashMap<String, Object> powertransformerData = (HashMap<String, Object>) value;

            String id = (String) powertransformerData.get("ID");
            String name = (String) powertransformerData.get("name");
            String terminal0Id = (String) powertransformerData.get("terminal0_id");
            String terminal1Id = (String) powertransformerData.get("terminal1_id");

            Powertransformer powertransformer = new Powertransformer(id);
            powertransformer.setName(name);

            Terminal terminal0 = terminalMap.get(terminal0Id);
            Terminal terminal1 = terminalMap.get(terminal1Id);

            if (terminal0 != null) {
                powertransformer.setTerminal0(terminal0);
                terminal0.setConductingEquipment(powertransformer);
            }
            if (terminal1 != null) {
                powertransformer.setTerminal1(terminal1);
                terminal1.setConductingEquipment(powertransformer);
            }

            powertransformerMap.put(id, powertransformer);
        }

        for (Map.Entry<String, Powertransformer> entry : powertransformerMap.entrySet()) {
            // System.out.println("Created Powertransformer with ID: " + entry.getKey() + " and name: " + entry.getValue().getName());
            //System.out.println("Terminal0: " + (entry.getValue().getTerminal0() != null ? entry.getValue().getTerminal0().getName() : "null"));
            //System.out.println("Terminal1: " + (entry.getValue().getTerminal1() != null ? entry.getValue().getTerminal1().getName() : "null"));
        }
    }
    public void Breaker_Util() {
        FileReader4EFormat myreader = new FileReader4EFormat();
        myreader.setConfigFile(ClassLoader.getSystemClassLoader().getResourceAsStream("resource/QSconfig.properties"));
        myreader.setSourceFile(ClassLoader.getSystemClassLoader().getResourceAsStream("resource/CIME-IEEE39.txt"));
        myreader.parseQSfile();

        HashMap<String, HashMap<String, Object>> dataMap = myreader.getDataMap();
        HashMap<String, Object> breakerDataMap = (HashMap<String, Object>) dataMap.get("Breaker");

        if (breakerDataMap == null) {
            System.out.println("No data found for 'Breaker'");
            return;
        }

        for (Object value : breakerDataMap.values()) {
            HashMap<String, Object> breakerData = (HashMap<String, Object>) value;

            String id = (String) breakerData.get("ID");
            String name = (String) breakerData.get("name");
            String terminal0Id = (String) breakerData.get("terminal0_id");
            String terminal1Id = (String) breakerData.get("terminal1_id");
            String normalOpenStr = (String) breakerData.get("normalOpen");


            Breaker breaker = new Breaker(id);
            breaker.setName(name);
            breaker.setNormalOpen(normalOpenStr);

            Terminal terminal0 = terminalMap.get(terminal0Id);
            Terminal terminal1 = terminalMap.get(terminal1Id);

            if (terminal0 != null) {
                breaker.setTerminal0(terminal0);
                terminal0.setConductingEquipment(breaker);
            }
            if (terminal1 != null) {
                breaker.setTerminal1(terminal1);
                terminal1.setConductingEquipment(breaker);
            }

            breakerMap.put(id, breaker);
        }

        for (Map.Entry<String, Breaker> entry : breakerMap.entrySet()) {
            // System.out.println("Created Breaker with ID: " + entry.getKey() + " and name: " + entry.getValue().getName());
            // System.out.println("Terminal0: " + (entry.getValue().getTerminal0() != null ? entry.getValue().getTerminal0().getName() : "null"));
            // System.out.println("Terminal1: " + (entry.getValue().getTerminal1() != null ? entry.getValue().getTerminal1().getName() : "null"));
        }
    }

    public void SynchronousMachine_Util() {
        FileReader4EFormat myreader = new FileReader4EFormat();
        myreader.setConfigFile(ClassLoader.getSystemClassLoader().getResourceAsStream("resource/QSconfig.properties"));
        myreader.setSourceFile(ClassLoader.getSystemClassLoader().getResourceAsStream("resource/CIME-IEEE39.txt"));
        myreader.parseQSfile();

        HashMap<String, HashMap<String, Object>> dataMap = myreader.getDataMap();
        HashMap<String, Object> synchronousmachineDataMap = (HashMap<String, Object>) dataMap.get("SynchronousMachine");

        if (synchronousmachineDataMap == null) {
            System.out.println("No data found for 'SynchronousMachine'");
            return;
        }

        for (Object value : synchronousmachineDataMap.values()) {
            HashMap<String, Object> synchronousmachineData = (HashMap<String, Object>) value;

            String id = (String) synchronousmachineData.get("ID");
            String name = (String) synchronousmachineData.get("name");
            String terminal0Id = (String) synchronousmachineData.get("terminal0_id");

            SynchronousMachine synchronousmachine = new SynchronousMachine(id);
            synchronousmachine.setName(name);

            Terminal terminal0 = terminalMap.get(terminal0Id);

            if (terminal0 != null) {
                synchronousmachine.setTerminal0(terminal0);
                terminal0.setConductingEquipment(synchronousmachine);
            }

            synchronousmachineMap.put(id, synchronousmachine);
        }

        for (Map.Entry<String, SynchronousMachine> entry : synchronousmachineMap.entrySet()) {
            //System.out.println("Created SynchronousMachine with ID: " + entry.getKey() + " and name: " + entry.getValue().getName());
            //System.out.println("Terminal0: " + (entry.getValue().getTerminal0() != null ? entry.getValue().getTerminal0().getName() : "null"));
        }
    }

    public void Energyconsumer_Util() {
        FileReader4EFormat myreader = new FileReader4EFormat();
        myreader.setConfigFile(ClassLoader.getSystemClassLoader().getResourceAsStream("resource/QSconfig.properties"));
        myreader.setSourceFile(ClassLoader.getSystemClassLoader().getResourceAsStream("resource/CIME-IEEE39.txt"));
        myreader.parseQSfile();

        HashMap<String, HashMap<String, Object>> dataMap = myreader.getDataMap();
        HashMap<String, Object> energyconsumerDataMap = (HashMap<String, Object>) dataMap.get("Energyconsumer");

        if (energyconsumerDataMap == null) {
            System.out.println("No data found for 'Energyconsumer'");
            return;
        }

        for (Object value : energyconsumerDataMap.values()) {
            HashMap<String, Object> energyconsumerData = (HashMap<String, Object>) value;

            String id = (String) energyconsumerData.get("ID");
            String name = (String) energyconsumerData.get("name");
            String terminal0Id = (String) energyconsumerData.get("terminal0_id");

            Energyconsumer energyconsumer = new Energyconsumer(id);
            energyconsumer.setName(name);

            Terminal terminal0 = terminalMap.get(terminal0Id);

            if (terminal0 != null) {
                energyconsumer.setTerminal0(terminal0);
                terminal0.setConductingEquipment(energyconsumer);
            }

            energyconsumerMap.put(id, energyconsumer);
        }

        for (Map.Entry<String, Energyconsumer> entry : energyconsumerMap.entrySet()) {
            // System.out.println("Created Energyconsumer with ID: " + entry.getKey() + " and name: " + entry.getValue().getName());
            //System.out.println("Terminal0: " + (entry.getValue().getTerminal0() != null ? entry.getValue().getTerminal0().getName() : "null"));
        }
    }


    /*public void printTerminalsForConnectivityNode(String cnid) {
        ConnectivityNode node = connectivityNodeMap.get(cnid);
        if (node != null) {
            System.out.println("ConnectivityNode with ID: " + cnid + " has the following terminals:");
            for (Terminal terminal : node.getTerminals()) {
                System.out.println("Terminal Name: " + terminal.getName());
            }
        } else {
            System.out.println("ConnectivityNode with ID: " + cnid + " not found.");
        }
    }*/
    public void analyzeTopology(String breakerId) {
        if (!breakerMap.containsKey(breakerId)) {
            System.out.println("Breaker with ID " + breakerId + " not found.");
            return;
        }

        Breaker startBreaker = breakerMap.get(breakerId);
        Set<ConductingEquipment> visited = new HashSet<>();
        List<List<ConductingEquipment>> allPaths = new ArrayList<>();
        List<ConductingEquipment> currentPath = new ArrayList<>();

        dfs(startBreaker, currentPath, visited, allPaths);

        if (!allPaths.isEmpty()) {
            System.out.println("Paths from Breaker to BusbarSection/Powertransformer/Aclinesegment:");
            for (List<ConductingEquipment> path : allPaths) {
                for (ConductingEquipment equipment : path) {
                    System.out.print("Device Type: " + equipment.getClass().getSimpleName() + ", Name: " + equipment.getName() + " -> ");
                }
                System.out.println("End");
            }
        } else {
            System.out.println("No path found from Breaker to BusbarSection/Powertransformer/Aclinesegment.");
        }
    }

    private void dfs(ConductingEquipment current, List<ConductingEquipment> currentPath,
                     Set<ConductingEquipment> visited, List<List<ConductingEquipment>> allPaths) {
        // Check if the current equipment is a target type
        if (current instanceof BusbarSection || current instanceof Powertransformer || current instanceof ACLineSegment) {
            currentPath.add(current);
            allPaths.add(new ArrayList<>(currentPath));
            currentPath.remove(current);
            return;
        }

        visited.add(current);
        currentPath.add(current);

        for (Terminal terminal : current.getTerminals()) {
            if (terminal.getConnectivityNode() != null) {
                for (Terminal connectedTerminal : terminal.getConnectivityNode().getTerminals()) {
                    ConductingEquipment connectedEquipment = connectedTerminal.getConductingEquipment();
                    if (connectedEquipment != null && !visited.contains(connectedEquipment)) {
                        dfs(connectedEquipment, currentPath, visited, allPaths);
                    }
                }
            }
        }

        // Backtrack
        visited.remove(current);
        currentPath.remove(current);
    }
    public static void main(String[] args) {
        TopologyAnalyzer topologyAnalyzer = new TopologyAnalyzer();
        topologyAnalyzer.Connectivitynode_Util();
        topologyAnalyzer.Terminal_Util();
        topologyAnalyzer.Measurement_Util();
        topologyAnalyzer.Disconnector_Util();
        topologyAnalyzer.BusbarSection_Util();
        topologyAnalyzer.Breaker_Util();
        topologyAnalyzer.Powertransformer_Util();
        topologyAnalyzer.Aclinesegment_Util();
        topologyAnalyzer.SynchronousMachine_Util();
        topologyAnalyzer.Energyconsumer_Util();
        topologyAnalyzer.analyzeTopology("0405");
    }
}