package proyecto2so.gui;

import java.util.ArrayList;
import java.util.Random;
import proyecto2so.core.FileNode;
import proyecto2so.core.FileSystemService;
import proyecto2so.core.Request;
import proyecto2so.core.RequestOp;
import proyecto2so.kernel.IOEngine;
import proyecto2so.kernel.ProcessControlBlock;
import proyecto2so.scheduler.DiskScheduler;
import proyecto2so.scheduler.SchedulingPolicy;

public class GuiSimulationController {
    private final FileSystemService fs;
    private final DiskScheduler scheduler;
    private final IOEngine engine;
    private final ArrayList<ProcessControlBlock> processes;
    private final Random random;
    private int nextPid;

    public GuiSimulationController() {
        this.fs = new FileSystemService();
        this.fs.initialize(200);
        this.scheduler = new DiskScheduler(SchedulingPolicy.FIFO, 0);
        this.engine = new IOEngine(fs, scheduler);
        this.processes = new ArrayList<>();
        this.random = new Random();
        this.nextPid = 1;

        seedFileSystem();
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
        return pcb;
    }

    public void tick() {
        engine.tick();
    }
}
