package proyecto2so.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.JTree;
import proyecto2so.core.AllocationEntry;
import proyecto2so.core.DirectoryNode;
import proyecto2so.core.FileNode;
import proyecto2so.kernel.IOEngine;
import proyecto2so.kernel.LockSnapshot;
import proyecto2so.kernel.ProcessControlBlock;
import proyecto2so.scheduler.SchedulingPolicy;
import proyecto2so.core.RequestOp;
import proyecto2so.journal.JournalEntry;

public class MainWindow extends JFrame {
    private enum UiRole {
        ADMIN,
        USER
    }

    private static final Color BG_APP = new Color(28, 28, 30);
    private static final Color BG_PANEL = new Color(42, 42, 45);
    private static final Color BG_PANEL_SOFT = new Color(54, 54, 58);
    private static final Color BG_INPUT = new Color(64, 64, 68);
    private static final Color FG_TEXT = new Color(232, 232, 235);
    private static final Color FG_COMBO_TEXT = new Color(90, 90, 96);
    private static final Color FG_MUTED = new Color(190, 190, 195);
    private static final Color BORDER = new Color(86, 86, 92);
    private static final Color ACCENT = new Color(112, 112, 122);
    private static final Color BTN_BG = new Color(92, 92, 98);
    private static final Color BTN_BG_PRESSED = new Color(108, 108, 114);
    private static final Color BTN_BORDER = new Color(124, 124, 132);

    private final GuiSimulationController controller;
    private final DiskViewPanel diskPanel;
    private final JTree fsTree;
    private final JTable allocationTable;
    private final JTable locksTable;
    private final JTable queuesTable;
    private final JTable journalTable;
    private final JTextArea logArea;
    private final JLabel cycleLabel;
    private final JLabel systemStatusLabel;
    private final JLabel totalHeadMovementLabel;
    private final JButton runBtn;
    private final JButton createFileBtn;
    private final JButton createDirBtn;
    private final JButton renameNodeBtn;
    private final JButton deleteNodeBtn;
    private final JButton loadScenarioBtn;
    private final JButton saveScenarioBtn;
    private final JButton addReadBtn;
    private final JButton crashBtn;
    private final JComboBox<SchedulingPolicy> policyCombo;
    private final JComboBox<String> roleCombo;
    private final JTextField cycleMsField;
    private final CardLayout centerViewLayout;
    private final JPanel centerViewPanel;
    private final Timer timer;
    private boolean autoRunning;
    private int cycleCount;
    private UiRole currentRole;
    private boolean suppressPolicyEvent;
    private Timer cycleMsApplyTimer;

    public MainWindow() {
        super("Proyecto 2 SO");
        this.controller = new GuiSimulationController();
        this.diskPanel = new DiskViewPanel();
        this.fsTree = new JTree();
        this.allocationTable = createTable(new String[]{"Archivo", "Bloques", "Bloque inicial"});
        this.locksTable = createTable(new String[]{"Recurso", "Shared", "Exclusive PID"});
        this.queuesTable = createTable(new String[]{"Cola", "Cantidad", "POS"});
        this.journalTable = createTable(new String[]{"Operación", "Estado"});
        this.logArea = new JTextArea();
        this.cycleLabel = new JLabel("Ciclo: 0");
        this.systemStatusLabel = new JLabel("Estado del Sistema: Normal");
        this.totalHeadMovementLabel = new JLabel("Movimiento total del cabezal: 0");
        this.runBtn = createTopButton("Run/Pause");
        this.createFileBtn = createTopButton("Crear Archivo");
        this.createDirBtn = createTopButton("Crear Directorio");
        this.renameNodeBtn = createTopButton("Actualizar Nodo");
        this.deleteNodeBtn = createTopButton("Eliminar Nodo");
        this.loadScenarioBtn = createTopButton("Cargar Escenario JSON");
        this.saveScenarioBtn = createTopButton("Guardar Escenario JSON");
        this.addReadBtn = createTopButton("Add READ");
        this.crashBtn = createTopButton("Simular Crash");
        this.policyCombo = new JComboBox<>(SchedulingPolicy.values());
        this.roleCombo = new JComboBox<>(new String[]{"Administrador", "Usuario"});
        this.cycleMsField = new JTextField("800", 5);
        this.centerViewLayout = new CardLayout();
        this.centerViewPanel = new JPanel(centerViewLayout);
        this.autoRunning = false;
        this.cycleCount = 0;
        this.currentRole = UiRole.ADMIN;
        this.suppressPolicyEvent = false;
        this.cycleMsApplyTimer = null;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1300, 820));
        getContentPane().setBackground(BG_APP);

        setupLayout();
        setupTreeRenderer();
        applyTheme();
        refreshAll();
        logStartupRecoveryInfo();

        this.timer = new Timer(800, e -> {
            if (autoRunning) {
                tickAndRefresh();
            }
        });
        this.timer.start();
    }

    private void setupLayout() {
        JPanel topBar = new JPanel();
        topBar.setBackground(BG_PANEL);
        JComboBox<String> centerViewSelector = new JComboBox<>(new String[]{"Simulación del Disco", "Locks"});
        styleComboBox(centerViewSelector);
        styleComboBox(policyCombo);
        styleComboBox(roleCombo);
        styleTextField(cycleMsField);

        runBtn.addActionListener(e -> {
            autoRunning = !autoRunning;
            appendLog(autoRunning ? "Auto-run activado." : "Auto-run pausado.");
        });
        createFileBtn.addActionListener(e -> createFileFromDialog());
        createDirBtn.addActionListener(e -> createDirectoryFromDialog());
        renameNodeBtn.addActionListener(e -> renameNodeFromDialog());
        deleteNodeBtn.addActionListener(e -> deleteNodeFromDialog());
        loadScenarioBtn.addActionListener(e -> loadScenarioFromJsonDialog());
        saveScenarioBtn.addActionListener(e -> saveScenarioToJsonDialog());
        addReadBtn.addActionListener(e -> addProcessWithRoleValidation(RequestOp.READ));
        crashBtn.addActionListener(e -> {
            if (!canUseCrash()) {
                denyAction("Solo administrador puede simular fallos.");
                return;
            }
            controller.getFs().simulateCrashAfterNextCriticalOperation();
            systemStatusLabel.setText("Estado del Sistema: Crash programado");
            appendLog("Crash programado para la próxima operación crítica.");
        });
        policyCombo.addActionListener(e -> {
            if (suppressPolicyEvent) {
                return;
            }
            if (!canChangePolicy()) {
                denyAction("Solo administrador puede cambiar la política.");
                suppressPolicyEvent = true;
                policyCombo.setSelectedItem(controller.getPolicy());
                suppressPolicyEvent = false;
                return;
            }
            SchedulingPolicy policy = (SchedulingPolicy) policyCombo.getSelectedItem();
            controller.setPolicy(policy);
            appendLog("Política cambiada a " + policy.name());
            refreshAll();
        });
        roleCombo.addActionListener(e -> {
            String value = (String) roleCombo.getSelectedItem();
            UiRole newRole = "Usuario".equals(value) ? UiRole.USER : UiRole.ADMIN;
            setRole(newRole);
        });
        cycleMsField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                scheduleCycleDurationApply();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                scheduleCycleDurationApply();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                scheduleCycleDurationApply();
            }
        });
        centerViewSelector.addActionListener(e -> {
            String option = (String) centerViewSelector.getSelectedItem();
            if ("Locks".equals(option)) {
                centerViewLayout.show(centerViewPanel, "LOCKS");
            } else {
                centerViewLayout.show(centerViewPanel, "DISK");
            }
        });

        topBar.add(runBtn);
        topBar.add(createFileBtn);
        topBar.add(createDirBtn);
        topBar.add(renameNodeBtn);
        topBar.add(deleteNodeBtn);
        topBar.add(addReadBtn);
        topBar.add(new JLabel("Modo:"));
        topBar.add(roleCombo);
        topBar.add(new JLabel("Policy:"));
        topBar.add(policyCombo);
        topBar.add(loadScenarioBtn);
        topBar.add(saveScenarioBtn);
        topBar.add(new JLabel("Ciclo ms:"));
        topBar.add(cycleMsField);
        topBar.add(cycleLabel);

        JPanel fsPanel = new JPanel(new BorderLayout());
        fsPanel.setBackground(BG_PANEL);
        applyTitledBorderStyle(fsPanel, "Sistema de Archivos");

        JScrollPane treeScroll = new JScrollPane(fsTree);
        treeScroll.setPreferredSize(new Dimension(340, 300));

        JScrollPane allocationScroll = new JScrollPane(allocationTable);
        allocationScroll.setPreferredSize(new Dimension(340, 260));

        JTabbedPane leftTabs = new JTabbedPane();
        leftTabs.addTab("JTree", treeScroll);
        leftTabs.addTab("Asignación", allocationScroll);
        fsPanel.add(leftTabs, BorderLayout.CENTER);

        centerViewPanel.add(diskPanel, "DISK");
        centerViewPanel.add(new JScrollPane(locksTable), "LOCKS");
        centerViewLayout.show(centerViewPanel, "DISK");

        JPanel diskContainer = new JPanel(new BorderLayout());
        diskContainer.setBackground(BG_PANEL);
        applyTitledBorderStyle(diskContainer, "Vista Central");
        JPanel centerHeader = new JPanel();
        centerHeader.setBackground(BG_PANEL);
        centerHeader.add(new JLabel("Ver:"));
        centerHeader.add(centerViewSelector);
        diskContainer.add(centerHeader, BorderLayout.NORTH);
        diskContainer.add(centerViewPanel, BorderLayout.CENTER);
        JPanel journalContainer = new JPanel(new BorderLayout());
        journalContainer.setBackground(BG_PANEL);
        applyTitledBorderStyle(journalContainer, "Journal");
        journalContainer.add(new JScrollPane(journalTable), BorderLayout.CENTER);
        JPanel journalActions = new JPanel(new BorderLayout());
        journalActions.setBackground(BG_PANEL);
        journalActions.add(crashBtn, BorderLayout.CENTER);
        journalActions.add(systemStatusLabel, BorderLayout.SOUTH);
        journalContainer.add(journalActions, BorderLayout.SOUTH);

        JSplitPane middleSplitLeft = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, fsPanel, diskContainer);
        middleSplitLeft.setResizeWeight(0.38);
        styleSplitPane(middleSplitLeft);

        JSplitPane middleSection = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, middleSplitLeft, journalContainer);
        middleSection.setResizeWeight(0.78);
        styleSplitPane(middleSection);

        JPanel bottomSection = new JPanel(new BorderLayout());
        bottomSection.setBackground(BG_PANEL);
        logArea.setEditable(false);
        logArea.setRows(10);
        JPanel logPanel = wrap("Log de eventos", new JScrollPane(logArea));
        JPanel queuesPanel = new JPanel(new BorderLayout());
        queuesPanel.setBackground(BG_PANEL);
        applyTitledBorderStyle(queuesPanel, "Colas de procesos");
        queuesPanel.add(new JScrollPane(queuesTable), BorderLayout.CENTER);
        JPanel queuesFooter = new JPanel(new BorderLayout());
        queuesFooter.setBackground(BG_PANEL);
        queuesFooter.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        queuesFooter.add(totalHeadMovementLabel, BorderLayout.WEST);
        queuesPanel.add(queuesFooter, BorderLayout.SOUTH);
        JSplitPane bottomSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, logPanel, queuesPanel);
        bottomSplit.setResizeWeight(0.72);
        styleSplitPane(bottomSplit);
        bottomSection.add(bottomSplit, BorderLayout.CENTER);

        JSplitPane middleBottomSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, middleSection, bottomSection);
        middleBottomSplit.setResizeWeight(0.625); // of remaining 80% -> 50% total height
        styleSplitPane(middleBottomSplit);

        JSplitPane rootVertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT, wrap("Controles", topBar), middleBottomSplit);
        rootVertical.setResizeWeight(0.10); // top = 1/10
        styleSplitPane(rootVertical);

        setLayout(new BorderLayout());
        add(rootVertical, BorderLayout.CENTER);
        setRole(UiRole.ADMIN);
    }

    private JPanel wrap(String title, java.awt.Component comp) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PANEL);
        applyTitledBorderStyle(panel, title);
        panel.add(comp, BorderLayout.CENTER);
        return panel;
    }

    private void applyTitledBorderStyle(JPanel panel, String title) {
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(BORDER), title));
        if (panel.getBorder() instanceof javax.swing.border.TitledBorder) {
            javax.swing.border.TitledBorder tb = (javax.swing.border.TitledBorder) panel.getBorder();
            tb.setTitleColor(FG_TEXT);
            tb.setTitleFont(new Font("SansSerif", Font.BOLD, 12));
        }
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

    private JButton createTopButton(String text) {
        RoundedButton button = new RoundedButton(text);
        button.setBackground(BTN_BG);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        return button;
    }

    private <T> void styleComboBox(JComboBox<T> combo) {
        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton arrow = new JButton("▼");
                arrow.setBackground(BG_INPUT);
                arrow.setForeground(FG_COMBO_TEXT);
                arrow.setFocusPainted(false);
                arrow.setBorder(BorderFactory.createLineBorder(BORDER));
                arrow.setFont(new Font("SansSerif", Font.BOLD, 10));
                return arrow;
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                g.setColor(BG_INPUT);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }

            @Override
            public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
                if (comboBox == null) {
                    return;
                }
                ListCellRenderer<Object> renderer = comboBox.getRenderer();
                if (renderer == null) {
                    return;
                }

                Component c = renderer.getListCellRendererComponent(
                        listBox,
                        comboBox.getSelectedItem(),
                        -1,
                        false,
                        false
                );
                c.setBackground(BG_INPUT);
                c.setForeground(FG_COMBO_TEXT);
                currentValuePane.paintComponent(g, c, comboBox, bounds.x, bounds.y, bounds.width, bounds.height, true);
            }
        });
        combo.setBackground(BG_INPUT);
        combo.setForeground(FG_COMBO_TEXT);
        combo.setBorder(BorderFactory.createLineBorder(BORDER));
        combo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        combo.setOpaque(true);
        combo.setFocusable(false);

        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
            ) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                // index == -1 => selected value shown when combo is closed
                if (index == -1) {
                    c.setBackground(BG_INPUT);
                    c.setForeground(FG_COMBO_TEXT);
                    if (c instanceof JLabel) {
                        ((JLabel) c).setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
                    }
                    return c;
                }

                if (isSelected) {
                    c.setBackground(ACCENT);
                    c.setForeground(new Color(30, 30, 34));
                } else {
                    c.setBackground(BG_INPUT);
                    c.setForeground(FG_COMBO_TEXT);
                }
                if (list != null) {
                    list.setBackground(BG_INPUT);
                    list.setForeground(FG_COMBO_TEXT);
                    list.setSelectionBackground(ACCENT);
                    list.setSelectionForeground(new Color(30, 30, 34));
                }
                if (c instanceof JLabel) {
                    ((JLabel) c).setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                }
                return c;
            }
        });
    }

    private void styleTextField(JTextField field) {
        field.setBackground(BG_INPUT);
        field.setForeground(FG_TEXT);
        field.setCaretColor(FG_TEXT);
        field.setBorder(BorderFactory.createLineBorder(BORDER));
        field.setFont(new Font("SansSerif", Font.PLAIN, 12));
    }

    private void applyTheme() {
        applyThemeRecursively(getContentPane());

        styleTable(allocationTable);
        styleTable(locksTable);
        styleTable(queuesTable);
        styleTable(journalTable);

        fsTree.setBackground(BG_PANEL_SOFT);
        fsTree.setForeground(FG_TEXT);
        fsTree.setOpaque(true);

        logArea.setBackground(new Color(24, 24, 26));
        logArea.setForeground(FG_TEXT);
        logArea.setCaretColor(FG_TEXT);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        cycleLabel.setForeground(FG_TEXT);
        systemStatusLabel.setForeground(FG_TEXT);
        totalHeadMovementLabel.setForeground(FG_TEXT);
    }

    private void styleSplitPane(JSplitPane split) {
        split.setBorder(null);
        split.setDividerSize(2);
        split.setContinuousLayout(true);
        split.setOpaque(true);
        split.setBackground(BG_PANEL);
        split.setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                BasicSplitPaneDivider divider = new BasicSplitPaneDivider(this);
                divider.setBackground(BG_PANEL);
                divider.setBorder(BorderFactory.createEmptyBorder());
                return divider;
            }
        });
    }

    private void applyThemeRecursively(Component c) {
        if (c instanceof JPanel) {
            c.setBackground(BG_PANEL);
            c.setForeground(FG_TEXT);
        } else if (c instanceof JLabel) {
            c.setForeground(FG_TEXT);
        } else if (c instanceof JButton) {
            if (c instanceof RoundedButton) {
                c.setBackground(BTN_BG);
                c.setForeground(Color.WHITE);
            } else {
                c.setBackground(BG_INPUT);
                c.setForeground(FG_TEXT);
                ((JButton) c).setBorder(BorderFactory.createLineBorder(BORDER));
            }
            ((JButton) c).setFocusPainted(false);
        } else if (c instanceof JComboBox) {
            c.setBackground(BG_INPUT);
            c.setForeground(FG_TEXT);
        } else if (c instanceof JTextField) {
            c.setBackground(BG_INPUT);
            c.setForeground(FG_TEXT);
            ((JTextField) c).setCaretColor(FG_TEXT);
            ((JTextField) c).setBorder(BorderFactory.createLineBorder(BORDER));
        } else if (c instanceof JScrollPane) {
            c.setBackground(BG_PANEL);
            JScrollPane sp = (JScrollPane) c;
            sp.getViewport().setBackground(BG_PANEL_SOFT);
            sp.setBorder(BorderFactory.createLineBorder(BORDER));
        } else if (c instanceof JSplitPane) {
            JSplitPane split = (JSplitPane) c;
            split.setBackground(BG_PANEL);
            if (split.getUI() instanceof BasicSplitPaneUI) {
                ((BasicSplitPaneUI) split.getUI()).getDivider().setBackground(BG_PANEL);
                ((BasicSplitPaneUI) split.getUI()).getDivider().setBorder(BorderFactory.createEmptyBorder());
            }
        }

        if (c instanceof java.awt.Container) {
            Component[] children = ((java.awt.Container) c).getComponents();
            for (int i = 0; i < children.length; i++) {
                applyThemeRecursively(children[i]);
            }
        }
    }

    private void styleTable(JTable table) {
        table.setBackground(BG_PANEL_SOFT);
        table.setForeground(FG_TEXT);
        table.setGridColor(BORDER);
        table.setSelectionBackground(ACCENT);
        table.setSelectionForeground(FG_TEXT);
        table.setRowHeight(24);
        table.getTableHeader().setBackground(BG_INPUT);
        table.getTableHeader().setForeground(FG_TEXT);
        table.getTableHeader().setBorder(BorderFactory.createLineBorder(BORDER));
    }

    private static class RoundedButton extends JButton {
        private static final int ARC = 16;

        RoundedButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (getModel().isPressed()) {
                g2.setColor(BTN_BG_PRESSED);
            } else {
                g2.setColor(getBackground());
            }
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARC, ARC);

            g2.setColor(BTN_BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
            g2.dispose();

            super.paintComponent(g);
        }
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

    private void createDirectoryFromDialog() {
        String parentPath = promptRequired("Ruta padre del directorio (ej: / o /users):", "/");
        if (parentPath == null) {
            return;
        }
        String directoryName = promptRequired("Nombre del directorio:", "");
        if (directoryName == null) {
            return;
        }

        try {
            ProcessControlBlock pcb = controller.createDirectory(parentPath, directoryName, currentOwner(), isAdminMode());
            appendLog(
                    "Proceso encolado PID=" + pcb.getPid()
                    + " op=CREATE_DIRECTORY recurso=" + pcb.getResourcePath()
            );
            refreshAll();
        } catch (RuntimeException ex) {
            showActionError("No se pudo crear directorio", ex);
        }
    }

    private void createFileFromDialog() {
        String parentPath = promptRequired("Ruta padre del archivo (ej: /system):", "/");
        if (parentPath == null) {
            return;
        }
        String fileName = promptRequired("Nombre del archivo:", "");
        if (fileName == null) {
            return;
        }
        String sizeValue = promptRequired("Tamaño en bloques (entero > 0):", "1");
        if (sizeValue == null) {
            return;
        }

        int blocks;
        try {
            blocks = Integer.parseInt(sizeValue.trim());
        } catch (NumberFormatException ex) {
            denyAction("Tamaño inválido. Debe ser un entero mayor que 0.");
            return;
        }

        if (blocks <= 0) {
            denyAction("Tamaño inválido. Debe ser mayor que 0.");
            return;
        }

        try {
            ProcessControlBlock pcb = controller.createFile(parentPath, fileName, currentOwner(), blocks, isAdminMode());
            appendLog(
                    "Proceso encolado PID=" + pcb.getPid()
                    + " op=CREATE_FILE recurso=" + pcb.getResourcePath()
                    + " bloques=" + blocks
            );
            refreshAll();
        } catch (RuntimeException ex) {
            showActionError("No se pudo crear archivo", ex);
        }
    }

    private void renameNodeFromDialog() {
        String path = promptRequired("Ruta del nodo a renombrar:", "");
        if (path == null) {
            return;
        }
        String newName = promptRequired("Nuevo nombre:", "");
        if (newName == null) {
            return;
        }

        try {
            controller.renameNode(path, newName, isAdminMode());
            appendLog("Nodo renombrado: " + path + " -> " + newName);
            refreshAll();
        } catch (RuntimeException ex) {
            showActionError("No se pudo renombrar nodo", ex);
        }
    }

    private void deleteNodeFromDialog() {
        String path = promptRequired("Ruta del nodo a eliminar (archivo o directorio):", "");
        if (path == null) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Se eliminará recursivamente si es directorio.\n¿Continuar con " + path + "?",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            ProcessControlBlock pcb = controller.deleteNode(path, isAdminMode());
            appendLog(
                    "Proceso encolado PID=" + pcb.getPid()
                    + " op=DELETE_NODE recurso=" + pcb.getResourcePath()
            );
            refreshAll();
        } catch (RuntimeException ex) {
            showActionError("No se pudo eliminar nodo", ex);
        }
    }

    private void loadScenarioFromJsonDialog() {
        if (!isAdminMode()) {
            denyAction("Solo administrador puede cargar escenarios JSON.");
            return;
        }

        int useExternal = JOptionPane.showConfirmDialog(
                this,
                "¿Deseas cargar un escenario desde archivo JSON?",
                "Cargar escenario",
                JOptionPane.YES_NO_OPTION
        );
        if (useExternal != JOptionPane.YES_OPTION) {
            return;
        }

        String fileName = promptRequired(
                "Introduzca nombre del JSON a cargar (debe estar en la carpeta origen del proyecto)",
                "test_input.json"
        );
        if (fileName == null) {
            return;
        }

        File file = resolveJsonFile(fileName);
        if (!file.exists() || !file.isFile()) {
            showActionError("No se pudo cargar escenario", new IllegalStateException("Archivo no encontrado: " + fileName));
            return;
        }

        int replace = JOptionPane.showConfirmDialog(
                this,
                "¿Reemplazar estado actual del sistema antes de cargar el escenario?",
                "Modo de carga",
                JOptionPane.YES_NO_OPTION
        );
        boolean replaceState = replace == JOptionPane.YES_OPTION;

        try {
            String testId = controller.loadScenarioFromJson(file.getPath(), replaceState, true);
            appendLog(
                    "Escenario JSON cargado: " + file.getPath()
                    + " (test_id=" + testId
                    + ", requests encolados=" + controller.getLastLoadedScenarioRequestCount() + ")."
            );
            String[] loadedRequests = controller.getLastLoadedScenarioRequests();
            for (int i = 0; i < loadedRequests.length; i++) {
                appendLog("Request encolado -> " + loadedRequests[i]);
            }
            systemStatusLabel.setText("Estado del Sistema: Normal");
            refreshAll();
        } catch (RuntimeException ex) {
            showActionError("No se pudo cargar escenario", ex);
        }
    }

    private void saveScenarioToJsonDialog() {
        if (!isAdminMode()) {
            denyAction("Solo administrador puede guardar escenarios JSON.");
            return;
        }

        String fileName = promptRequired(
                "Introduzca nombre del JSON a guardar (se guardará en la carpeta origen del proyecto)",
                "scenario_export.json"
        );
        if (fileName == null) {
            return;
        }

        try {
            String savedPath = controller.saveScenarioToJson(fileName, true);
            appendLog("Escenario JSON guardado en: " + savedPath);
        } catch (RuntimeException ex) {
            showActionError("No se pudo guardar escenario", ex);
        }
    }

    private File resolveJsonFile(String inputPath) {
        File direct = new File(inputPath);
        if (direct.exists()) {
            return direct;
        }

        File fromWorkingDir = new File(System.getProperty("user.dir"), inputPath);
        if (fromWorkingDir.exists()) {
            return fromWorkingDir;
        }

        return direct;
    }

    private String promptRequired(String prompt, String defaultValue) {
        String input = JOptionPane.showInputDialog(this, prompt, defaultValue);
        if (input == null) {
            return null;
        }
        String value = input.trim();
        if (value.isEmpty()) {
            denyAction("Entrada inválida. El valor no puede estar vacío.");
            return null;
        }
        return value;
    }

    private void showActionError(String title, RuntimeException ex) {
        String message = ex.getMessage() == null ? "Error desconocido." : ex.getMessage();
        appendLog(title + ": " + message);
        refreshAll();
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private void addProcessWithRoleValidation(RequestOp op) {
        if (!isOperationAllowed(op)) {
            denyAction("Modo usuario: operación " + op.name() + " no permitida.");
            return;
        }
        addProcess(op);
    }

    private boolean isOperationAllowed(RequestOp op) {
        if (currentRole == UiRole.ADMIN) {
            return true;
        }
        return op == RequestOp.READ;
    }

    private boolean canUseCrash() {
        return currentRole == UiRole.ADMIN;
    }

    private boolean canChangePolicy() {
        return currentRole == UiRole.ADMIN;
    }

    private void denyAction(String message) {
        appendLog("[DENEGADO] " + message);
        JOptionPane.showMessageDialog(this, message, "Acción no permitida", JOptionPane.WARNING_MESSAGE);
    }

    private void setRole(UiRole role) {
        this.currentRole = role;
        boolean isAdmin = role == UiRole.ADMIN;
        createFileBtn.setEnabled(isAdmin);
        createDirBtn.setEnabled(isAdmin);
        renameNodeBtn.setEnabled(isAdmin);
        deleteNodeBtn.setEnabled(isAdmin);
        loadScenarioBtn.setEnabled(isAdmin);
        saveScenarioBtn.setEnabled(isAdmin);
        crashBtn.setEnabled(isAdmin);
        policyCombo.setEnabled(isAdmin);
        String roleName = isAdmin ? "Administrador" : "Usuario";
        appendLog("Modo activo: " + roleName);
    }

    private void scheduleCycleDurationApply() {
        if (cycleMsApplyTimer == null) {
            cycleMsApplyTimer = new Timer(450, e -> applyCycleDurationSilently());
            cycleMsApplyTimer.setRepeats(false);
        }
        cycleMsApplyTimer.restart();
    }

    private void applyCycleDurationSilently() {
        if (timer == null) {
            return;
        }
        String raw = cycleMsField.getText();
        int ms;
        try {
            ms = Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return;
        }

        if (ms < 100 || ms > 10000) {
            return;
        }

        if (ms == timer.getDelay()) {
            return;
        }
        timer.setDelay(ms);
        timer.setInitialDelay(ms);
        appendLog("Duración de ciclo configurada a " + ms + " ms.");
    }

    private boolean isAdminMode() {
        return currentRole == UiRole.ADMIN;
    }

    private String currentOwner() {
        return isAdminMode() ? "admin" : "user";
    }

    private void tickAndRefresh() {
        IOEngine engine = controller.getEngine();
        try {
            if (!engine.isDone()) {
                controller.tick();
                cycleCount++;
                cycleLabel.setText("Ciclo: " + cycleCount);
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
        refreshLocksTable();
        refreshQueuesTable();
        refreshJournalTable();
        refreshHeadMovementLabel();
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
            parent.add(new DefaultMutableTreeNode(files[i]));
        }

        DirectoryNode[] subdirs = directory.getSubdirectories();
        for (int i = 0; i < subdirs.length; i++) {
            DefaultMutableTreeNode child = new DefaultMutableTreeNode(subdirs[i]);
            parent.add(child);
            buildTreeNode(subdirs[i], child);
        }
    }

    private void setupTreeRenderer() {
        fsTree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(
                    JTree tree,
                    Object value,
                    boolean selected,
                    boolean expanded,
                    boolean leaf,
                    int row,
                    boolean hasFocus
            ) {
                super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
                setOpaque(false);
                setForeground(FG_TEXT);
                setTextNonSelectionColor(FG_TEXT);
                setTextSelectionColor(FG_TEXT);
                setBackgroundNonSelectionColor(BG_PANEL_SOFT);
                setBackgroundSelectionColor(new Color(76, 76, 82));
                setBorderSelectionColor(new Color(110, 110, 118));

                if (!(value instanceof DefaultMutableTreeNode)) {
                    return this;
                }

                Object userObject = ((DefaultMutableTreeNode) value).getUserObject();

                if (userObject instanceof FileNode) {
                    FileNode file = (FileNode) userObject;
                    setText(file.getName());
                    setIcon(new ColorSquareIcon(FileColorPalette.colorForFile(file.getName())));
                    return this;
                }

                if (userObject instanceof DirectoryNode) {
                    DirectoryNode directory = (DirectoryNode) userObject;
                    setText(directory.getName());
                    return this;
                }

                return this;
            }
        });
    }

    private static class ColorSquareIcon implements Icon {
        private final Color color;

        ColorSquareIcon(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, java.awt.Graphics g, int x, int y) {
            g.setColor(color);
            g.fillRect(x, y, getIconWidth(), getIconHeight());
            g.setColor(new Color(60, 60, 60));
            g.drawRect(x, y, getIconWidth(), getIconHeight());
        }

        @Override
        public int getIconWidth() {
            return 12;
        }

        @Override
        public int getIconHeight() {
            return 12;
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

    private void refreshQueuesTable() {
        DefaultTableModel model = (DefaultTableModel) queuesTable.getModel();
        model.setRowCount(0);

        IOEngine engine = controller.getEngine();
        model.addRow(new Object[]{"NEW", engine.getNewQueueSnapshot().length, requestPosFromSnapshot(engine.getNewQueueSnapshot())});
        model.addRow(new Object[]{"READY", engine.getReadyQueueSnapshot().length, requestPosFromSnapshot(engine.getReadyQueueSnapshot())});
        model.addRow(new Object[]{"IO_PENDING", engine.getIoPendingSnapshot().length, requestPosFromSnapshot(engine.getIoPendingSnapshot())});
        model.addRow(new Object[]{"RUNNING", engine.getRunningSnapshot().length, requestPosFromSnapshot(engine.getRunningSnapshot())});
        model.addRow(new Object[]{"BLOCKED", engine.getBlockedSnapshot().length, requestPosFromSnapshot(engine.getBlockedSnapshot())});
        model.addRow(new Object[]{"TERMINATED", engine.getTerminatedSnapshot().length, requestPosFromSnapshot(engine.getTerminatedSnapshot())});
    }

    private String requestPosFromSnapshot(Object[] values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            ProcessControlBlock pcb = (ProcessControlBlock) values[i];
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(pcb.getRequest().getPos());
        }
        return sb.toString();
    }

    private void refreshJournalTable() {
        DefaultTableModel model = (DefaultTableModel) journalTable.getModel();
        model.setRowCount(0);

        JournalEntry[] entries = controller.getFs().getJournalEntries();
        for (int i = 0; i < entries.length; i++) {
            model.addRow(new Object[]{
                entries[i].getOperation().name(),
                entries[i].getStatus().name()
            });
        }
    }

    private void refreshHeadMovementLabel() {
        totalHeadMovementLabel.setText(
                "Movimiento total del cabezal: " + controller.getEngine().getTotalHeadMovement()
        );
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

    private void logStartupRecoveryInfo() {
        if (controller.wasRestoredFromDisk()) {
            appendLog("Estado restaurado desde disco.");
            appendLog("Recovery automático al iniciar. Operaciones revertidas: " + controller.getStartupRecoveredCount());
        } else {
            appendLog("Inicio limpio: se creó un estado inicial.");
        }
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
