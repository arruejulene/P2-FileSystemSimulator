package proyecto2so.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.JTree;
import proyecto2so.core.AllocationEntry;
import proyecto2so.core.DirectoryNode;
import proyecto2so.core.FileNode;
import proyecto2so.kernel.IOEngine;
import proyecto2so.kernel.LockSnapshot;
import proyecto2so.kernel.ProcessControlBlock;
import proyecto2so.kernel.ProcessState;
import proyecto2so.scheduler.SchedulingPolicy;
import proyecto2so.core.RequestOp;

public class MainWindow extends JFrame {
    private final GuiSimulationController controller;
    private final DiskViewPanel diskPanel;
    private final JTree fsTree;
    private final JTable allocationTable;
    private final JTable processTable;
    private final JTable queuesTable;
    private final JTable locksTable;
    private final JTextArea logArea;
    private final JLabel statusLabel;
    private final Timer timer;
    private final Map<Integer, ProcessState> stateMemory;
    private boolean autoRunning;

    public MainWindow() {
        super("Proyecto 2 SO - GUI Core");
        this.controller = new GuiSimulationController();
        this.diskPanel = new DiskViewPanel();
        this.fsTree = new JTree();
        this.allocationTable = createTable(new String[]{"Archivo", "Bloques", "Bloque inicial"});
        this.processTable = createTable(new String[]{"PID", "Usuario", "Op", "Pos", "Recurso", "Estado", "Ticks", "Bloqueo"});
        this.queuesTable = createTable(new String[]{"Cola", "Cantidad", "PIDs"});
        this.locksTable = createTable(new String[]{"Recurso", "Shared", "Exclusive PID"});
        this.logArea = new JTextArea();
        this.statusLabel = new JLabel("Scheduler: FIFO");
        this.stateMemory = new HashMap<>();
        this.autoRunning = false;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1300, 820));

        setupLayout();
        refreshAll();

        this.timer = new Timer(800, e -> {
            if (autoRunning) {
                tickAndRefresh();
            }
        });
        this.timer.start();
    }

    private void setupLayout() {
        JPanel topBar = new JPanel();
        JButton tickBtn = new JButton("Tick");
        JButton runBtn = new JButton("Run/Pause");
        JButton addReadBtn = new JButton("Add READ");
        JButton addUpdateBtn = new JButton("Add UPDATE");
        JButton addDeleteBtn = new JButton("Add DELETE");
        JComboBox<SchedulingPolicy> policyCombo = new JComboBox<>(SchedulingPolicy.values());

        tickBtn.addActionListener(e -> tickAndRefresh());
        runBtn.addActionListener(e -> {
            autoRunning = !autoRunning;
            appendLog(autoRunning ? "Auto-run activado." : "Auto-run pausado.");
        });
        addReadBtn.addActionListener(e -> addProcess(RequestOp.READ));
        addUpdateBtn.addActionListener(e -> addProcess(RequestOp.UPDATE));
        addDeleteBtn.addActionListener(e -> addProcess(RequestOp.DELETE));
        policyCombo.addActionListener(e -> {
            SchedulingPolicy policy = (SchedulingPolicy) policyCombo.getSelectedItem();
            controller.setPolicy(policy);
            statusLabel.setText("Scheduler: " + policy.name());
            appendLog("Política cambiada a " + policy.name());
            refreshAll();
        });

        topBar.add(tickBtn);
        topBar.add(runBtn);
        topBar.add(addReadBtn);
        topBar.add(addUpdateBtn);
        topBar.add(addDeleteBtn);
        topBar.add(new JLabel("Policy:"));
        topBar.add(policyCombo);
        topBar.add(statusLabel);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("Sistema de Archivos"));

        JScrollPane treeScroll = new JScrollPane(fsTree);
        treeScroll.setPreferredSize(new Dimension(340, 300));

        JScrollPane allocationScroll = new JScrollPane(allocationTable);
        allocationScroll.setPreferredSize(new Dimension(340, 260));

        JTabbedPane leftTabs = new JTabbedPane();
        leftTabs.addTab("JTree", treeScroll);
        leftTabs.addTab("Asignación", allocationScroll);
        leftPanel.add(leftTabs, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Monitoreo"));

        JSplitPane rightSplitVertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightSplitVertical.setResizeWeight(0.32);
        rightSplitVertical.setTopComponent(wrap("Disco (tiempo real)", diskPanel));

        JPanel runtimePanel = new JPanel(new BorderLayout());
        JTabbedPane runtimeTabs = new JTabbedPane();
        runtimeTabs.addTab("Procesos", new JScrollPane(processTable));
        runtimeTabs.addTab("Colas", new JScrollPane(queuesTable));
        runtimeTabs.addTab("Locks activos", new JScrollPane(locksTable));

        logArea.setEditable(false);
        logArea.setRows(10);
        runtimePanel.add(runtimeTabs, BorderLayout.CENTER);
        runtimePanel.add(wrap("Log de eventos", new JScrollPane(logArea)), BorderLayout.SOUTH);

        rightSplitVertical.setBottomComponent(runtimePanel);
        rightPanel.add(rightSplitVertical, BorderLayout.CENTER);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        mainSplit.setResizeWeight(0.28);

        setLayout(new BorderLayout());
        add(topBar, BorderLayout.NORTH);
        add(mainSplit, BorderLayout.CENTER);
    }

    private JPanel wrap(String title, java.awt.Component comp) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(comp, BorderLayout.CENTER);
        return panel;
    }

    private JTable createTable(String[] columns) {
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        return new JTable(model);
    }

    private void addProcess(RequestOp op) {
        ProcessControlBlock pcb = controller.addProcess(op);
        if (pcb == null) {
            appendLog("No se pudo crear proceso: no hay archivos.");
            return;
        }
        appendLog("Proceso creado PID=" + pcb.getPid() + " op=" + op + " recurso=" + pcb.getResourcePath());
        refreshAll();
    }

    private void tickAndRefresh() {
        IOEngine engine = controller.getEngine();
        try {
            if (!engine.isDone()) {
                controller.tick();
            }
        } catch (RuntimeException ex) {
            autoRunning = false;
            appendLog("Error en tick: " + ex.getMessage());
        }
        refreshAll();
    }

    private void refreshAll() {
        refreshTree();
        refreshAllocationTable();
        refreshDisk();
        refreshProcessTable();
        refreshQueuesTable();
        refreshLocksTable();
    }

    private void refreshTree() {
        DirectoryNode root = controller.getFs().getRoot();
        DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode("root");
        if (root != null) {
            buildTreeNode(root, treeRoot);
        }
        fsTree.setModel(new DefaultTreeModel(treeRoot));
    }

    private void buildTreeNode(DirectoryNode directory, DefaultMutableTreeNode parent) {
        FileNode[] files = directory.getFiles();
        for (int i = 0; i < files.length; i++) {
            parent.add(new DefaultMutableTreeNode(files[i].getName()));
        }

        DirectoryNode[] subdirs = directory.getSubdirectories();
        for (int i = 0; i < subdirs.length; i++) {
            DefaultMutableTreeNode child = new DefaultMutableTreeNode(subdirs[i].getName());
            parent.add(child);
            buildTreeNode(subdirs[i], child);
        }
    }

    private void refreshAllocationTable() {
        DefaultTableModel model = (DefaultTableModel) allocationTable.getModel();
        model.setRowCount(0);

        AllocationEntry[] entries = controller.getFs().getAllocationTable();
        for (int i = 0; i < entries.length; i++) {
            model.addRow(new Object[]{
                entries[i].getFileName(),
                entries[i].getBlockCount(),
                entries[i].getFirstBlockId()
            });
        }
    }

    private void refreshDisk() {
        diskPanel.setState(controller.getFs().getDisk(), controller.getEngine().getHeadPos());
    }

    private void refreshProcessTable() {
        DefaultTableModel model = (DefaultTableModel) processTable.getModel();
        model.setRowCount(0);

        for (int i = 0; i < controller.getProcesses().size(); i++) {
            ProcessControlBlock pcb = controller.getProcesses().get(i);

            ProcessState previous = stateMemory.get(pcb.getPid());
            if (previous != pcb.getState()) {
                if (previous != null) {
                    appendLog("PID " + pcb.getPid() + ": " + previous + " -> " + pcb.getState());
                }
                stateMemory.put(pcb.getPid(), pcb.getState());
            }

            model.addRow(new Object[]{
                pcb.getPid(),
                pcb.getUser(),
                pcb.getRequest().getOp(),
                pcb.getRequest().getPos(),
                pcb.getResourcePath(),
                pcb.getState(),
                pcb.getRemainingTicks(),
                pcb.getBlockedReason()
            });
        }
    }

    private void refreshQueuesTable() {
        DefaultTableModel model = (DefaultTableModel) queuesTable.getModel();
        model.setRowCount(0);

        IOEngine engine = controller.getEngine();
        model.addRow(new Object[]{"NEW", engine.getNewQueueSnapshot().length, pidsFromSnapshot(engine.getNewQueueSnapshot())});
        model.addRow(new Object[]{"READY", engine.getReadyQueueSnapshot().length, pidsFromSnapshot(engine.getReadyQueueSnapshot())});
        model.addRow(new Object[]{"IO_PENDING", engine.getIoPendingSnapshot().length, pidsFromSnapshot(engine.getIoPendingSnapshot())});
        model.addRow(new Object[]{"RUNNING", engine.getRunningSnapshot().length, pidsFromSnapshot(engine.getRunningSnapshot())});
        model.addRow(new Object[]{"BLOCKED", engine.getBlockedSnapshot().length, pidsFromSnapshot(engine.getBlockedSnapshot())});
        model.addRow(new Object[]{"TERMINATED", engine.getTerminatedSnapshot().length, pidsFromSnapshot(engine.getTerminatedSnapshot())});
    }

    private String pidsFromSnapshot(Object[] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            ProcessControlBlock pcb = (ProcessControlBlock) values[i];
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(pcb.getPid());
        }
        return sb.toString();
    }

    private void refreshLocksTable() {
        DefaultTableModel model = (DefaultTableModel) locksTable.getModel();
        model.setRowCount(0);

        LockSnapshot[] locks = controller.getEngine().getLockSnapshots();
        for (int i = 0; i < locks.length; i++) {
            model.addRow(new Object[]{
                locks[i].getResource(),
                locks[i].getSharedCount(),
                locks[i].getExclusiveOwnerPid() == -1 ? "-" : locks[i].getExclusiveOwnerPid()
            });
        }
    }

    private void appendLog(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public static void open() {
        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.pack();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }
}
