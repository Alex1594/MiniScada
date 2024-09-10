package com.Views;

import com.CIMModel.ConductingEquipment;
import com.GridTopologyAnalysis.Analysis;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Set;

public class StationView extends Frame implements MouseListener, ItemListener {
    private static final long serialVersionUID = 1L;

    private Label stats;
    private Analysis model = null;
    private DrawPanel panel;
    private JPanel controlPanel;
    private JLabel selectLabel;
    private JLabel islandLabel;
    private JLabel nIslandLabel;
    private JLabel topoNodeLabel;
    private JLabel nTopoNodeLabel;
    private JComboBox<String> stationComboBox;
    private int stationNumber = -1;

    public StationView(String title) {
        setTitle(title);
        setLayout(new BorderLayout());

        // Initialize DrawPanel and Control Panel
        initializePanels();
        setSize(1000, 700);
        setResizable(false);
        setVisible(true);
        setLocationRelativeTo(null);

        // Add window listener for closing event
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                model.stopModelupdate();
                System.exit(0);
            }
        });
        addMouseListener(this);
    }

    private void initializePanels() {
        this.panel = new DrawPanel();
        add(this.panel, BorderLayout.CENTER);

        // Control panel on the right side
        this.controlPanel = new JPanel();
        this.controlPanel.setPreferredSize(new Dimension(200, 700));
        this.controlPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 30, 40));
        int fontSize = 20;

        // Select label and combo box
        this.selectLabel = new JLabel("Select Station:");
        this.selectLabel.setFont(new Font("Dialog", Font.BOLD, fontSize));
        this.stationComboBox = new JComboBox<>();
        this.controlPanel.add(this.selectLabel);
        this.controlPanel.add(this.stationComboBox);

        // Island and topological node labels
        this.islandLabel = new JLabel("Islands:");
        this.islandLabel.setFont(new Font("Dialog", Font.BOLD, fontSize));
        this.nIslandLabel = new JLabel("1");
        this.nIslandLabel.setFont(new Font("Dialog", Font.PLAIN, fontSize));
        this.controlPanel.add(this.islandLabel);
        this.controlPanel.add(this.nIslandLabel);

        this.topoNodeLabel = new JLabel("Topological Nodes:");
        this.topoNodeLabel.setFont(new Font("Dialog", Font.BOLD, fontSize));
        this.nTopoNodeLabel = new JLabel("39");
        this.nTopoNodeLabel.setFont(new Font("Dialog", Font.PLAIN, fontSize));
        this.controlPanel.add(this.topoNodeLabel);
        this.controlPanel.add(this.nTopoNodeLabel);

        // Add station items to combo box
        for (int i = 1; i < 40; i++)
            this.stationComboBox.addItem("Station #" + i);
        this.stationComboBox.addItemListener(this);
        this.stationComboBox.setSelectedIndex(-1);

        add(this.controlPanel, BorderLayout.EAST);

        // Status label at the bottom
        this.stats = new Label("Station Status");
        add(this.stats, BorderLayout.SOUTH);
    }

    public void updateTopo() {
        this.nIslandLabel.setText(String.valueOf(this.model.getNisland()));
        this.nTopoNodeLabel.setText(String.valueOf(this.model.getNTopoNode()));
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        // Add logic here if needed
    }

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.DESELECTED) return;

        String item = e.getItem().toString();
        System.out.println("Selected item: " + item);
        int pos = item.lastIndexOf("#");
        this.stationNumber = Integer.parseInt(item.substring(pos + 1));

        // Update stats label and redraw the panel
        this.stats.setText("Station #" + this.stationNumber);
        Set<ConductingEquipment> eqSet = this.model.updateStation(this.stationNumber);
        this.panel.setStationNo(this.stationNumber);
        this.panel.setDeviceSet(eqSet);
        this.panel.updateDataUnit();
        this.panel.updateUI();
    }

    public void setModel(Analysis m) {
        this.model = m;
        this.panel.setModel(m);
        updateTopo();
    }

    public static void main(String[] args) {
        String cimFile = "resource/CIME-IEEE39.txt";
        Analysis model = new Analysis(cimFile);
        model.init();
        model.updateMeas();
        model.buildTopo();
        StationView stView = new StationView("Mini SCADA");
        stView.setModel(model);

        MyThread worker = new MyThread(model, stView.panel, stView);
        (new java.lang.Thread(worker)).start();
    }
}
