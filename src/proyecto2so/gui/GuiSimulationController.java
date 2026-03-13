package proyecto2so.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import proyecto2so.core.FileNode;
import proyecto2so.core.FileSystemService;
import proyecto2so.core.Request;
import proyecto2so.core.RequestOp;
import proyecto2so.json.SystemStateManager;
import proyecto2so.kernel.IOEngine;
import proyecto2so.kernel.ProcessControlBlock;
import proyecto2so.scheduler.DiskScheduler;
import proyecto2so.scheduler.SchedulingPolicy;

public class GuiSimulationController {
    private static final String FS_STATE_PATH = "filesystem_state.json";
    private static final String JOURNAL_STATE_PATH = "journal_state.json";

    private final FileSystemService fs;
    private final DiskScheduler scheduler;
    private final IOEngine engine;
    private final SystemStateManager stateManager;
    private final ArrayList<ProcessControlBlock> processes;
    private final Random random;
    private int nextPid;
    private int startupRecoveredCount;
    private boolean restoredFromDisk;

    public GuiSimulationController() {
        this.fs = new FileSystemService();
        this.fs.initialize(200);
        this.scheduler = new DiskScheduler(SchedulingPolicy.FIFO, 0);
        this.engine = new IOEngine(fs, scheduler);
        this.stateManager = new SystemStateManager();
        this.processes = new ArrayList<>();
        this.random = new Random();
        this.nextPid = 1;
        this.startupRecoveredCount = 0;
        this.restoredFromDisk = false;

        bootstrapState();
    }

    private void bootstrapState() {
        File fsState = new File(FS_STATE_PATH);
        File journalState = new File(JOURNAL_STATE_PATH);

        if (fsState.exists() && journalState.exists()) {
            try {
                startupRecoveredCount = stateManager.loadAndRecover(FS_STATE_PATH, JOURNAL_STATE_PATH, fs);
                if (hasUsableState()) {
                    restoredFromDisk = true;
                    return;
                }
            } catch (RuntimeException ex) {
                // fallback to clean seed if persisted state is corrupted/incomplete
            }
        }

        fs.initialize(200);
        seedFileSystem();
        persistState();
    }

    private boolean hasUsableState() {
        if (fs.getRoot() == null || fs.getDisk() == null) {
            return false;
        }
        // For GUI startup, require at least one file to avoid empty simulation screens.
        return fs.getFileCount() > 0;
    }

    private void seedFileSystem() {
        fs.createDirectory("/", "system", "admin");
        fs.createDirectory("/", "users", "admin");
        fs.createDirectory("/users", "docs", "user");
        fs.createDirectory("/users", "media", "user");

        fs.createFile("/system", "kernel.bin", "admin", 4);
        fs.createFile("/system", "config.sys", "admin", 3);
        fs.createFile("/users/docs", "notes.txt", "user", 2);
        fs.createFile("/users/docs", "todo.txt", "user", 2);
        fs.createFile("/users/media", "song.mp3", "user", 5);
    }

    public FileSystemService getFs() {
        return fs;
    }

    public IOEngine getEngine() {
        return engine;
    }

    public ArrayList<ProcessControlBlock> getProcesses() {
        return processes;
    }

    public void setPolicy(SchedulingPolicy policy) {
        scheduler.setPolicy(policy);
    }

    public SchedulingPolicy getPolicy() {
        return scheduler.getPolicy();
    }

    public int getStartupRecoveredCount() {
        return startupRecoveredCount;
    }

    public boolean wasRestoredFromDisk() {
        return restoredFromDisk;
    }

    public ProcessControlBlock addProcess(RequestOp op) {
        FileNode[] files = fs.getFileIndex();
        if (files.length == 0) {
            return null;
        }

        FileNode target = files[random.nextInt(files.length)];
        int pos = Math.max(0, target.getFirstBlockId());

        ProcessControlBlock pcb = new ProcessControlBlock(
                nextPid,
                "user" + nextPid,
                new Request(pos, op),
                target.getPath()
        );
        nextPid++;

        engine.submitProcess(pcb);
        processes.add(pcb);
        persistState();
        return pcb;
    }

    public void tick() {
        try {
            engine.tick();
        } finally {
            persistState();
        }
    }

    public void persistState() {
        stateManager.save(FS_STATE_PATH, JOURNAL_STATE_PATH, fs);
    }
}
