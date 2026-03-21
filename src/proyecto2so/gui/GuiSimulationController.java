package proyecto2so.gui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import proyecto2so.core.DirectoryNode;
import proyecto2so.core.FileNode;
import proyecto2so.core.FileSystemService;
import proyecto2so.core.JsonScenario;
import proyecto2so.core.JsonScenarioLoader;
import proyecto2so.core.Request;
import proyecto2so.core.RequestOp;
import proyecto2so.core.SystemFileSeed;
import proyecto2so.json.SystemStateManager;
import proyecto2so.json.TestScenarioApplier;
import proyecto2so.journal.JournalEntry;
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
    private int lastLoadedScenarioRequestCount;
    private String[] lastLoadedScenarioRequests;

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
        this.lastLoadedScenarioRequestCount = 0;
        this.lastLoadedScenarioRequests = new String[0];

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

    public int getLastLoadedScenarioRequestCount() {
        return lastLoadedScenarioRequestCount;
    }

    public String[] getLastLoadedScenarioRequests() {
        String[] copy = new String[lastLoadedScenarioRequests.length];
        for (int i = 0; i < lastLoadedScenarioRequests.length; i++) {
            copy[i] = lastLoadedScenarioRequests[i];
        }
        return copy;
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

    public void createDirectory(String parentPath, String directoryName, String owner, boolean isAdmin) {
        requireAdmin(isAdmin, "crear directorios");
        try {
            fs.createDirectory(parentPath, directoryName, owner);
        } finally {
            persistState();
        }
    }

    public void createFile(String parentPath, String fileName, String owner, int sizeInBlocks, boolean isAdmin) {
        requireAdmin(isAdmin, "crear archivos");
        try {
            fs.createFile(parentPath, fileName, owner, sizeInBlocks);
        } finally {
            persistState();
        }
    }

    public void renameNode(String path, String newName, boolean isAdmin) {
        requireAdmin(isAdmin, "renombrar nodos");

        FileNode file = fs.getFileByPath(path);
        if (file != null) {
            try {
                fs.renameFile(path, newName);
            } finally {
                persistState();
            }
            return;
        }

        DirectoryNode directory = findDirectoryByPath(path);
        if (directory != null) {
            try {
                fs.renameDirectory(path, newName);
            } finally {
                persistState();
            }
            return;
        }

        throw new IllegalStateException("No existe nodo en la ruta: " + path);
    }

    public void deleteNode(String path, boolean isAdmin) {
        requireAdmin(isAdmin, "eliminar nodos");

        FileNode file = fs.getFileByPath(path);
        if (file != null) {
            try {
                fs.deleteFile(path);
            } finally {
                persistState();
            }
            return;
        }

        DirectoryNode directory = findDirectoryByPath(path);
        if (directory != null) {
            try {
                fs.deleteDirectoryRecursive(path);
            } finally {
                persistState();
            }
            return;
        }

        throw new IllegalStateException("No existe nodo en la ruta: " + path);
    }

    public String loadScenarioFromJson(String jsonPath, boolean replaceCurrentState, boolean isAdmin) {
        requireAdmin(isAdmin, "cargar escenarios JSON");
        if (jsonPath == null || jsonPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Debe indicar un nombre de archivo JSON.");
        }

        try {
            JsonScenario scenario = JsonScenarioLoader.loadFromFile(jsonPath.trim());
            validateScenarioForLoad(scenario, replaceCurrentState);
            lastLoadedScenarioRequestCount = 0;
            lastLoadedScenarioRequests = new String[0];
            if (replaceCurrentState) {
                fs.initialize(200);
                engine.reset();
                processes.clear();
                nextPid = 1;
            }

            TestScenarioApplier applier = new TestScenarioApplier();
            applier.apply(scenario, fs);
            enqueueScenarioRequests(scenario);
            scheduler.resetHeadPosition(scenario.getInitialHead());
            persistState();
            return scenario.getTestId();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer el JSON: " + jsonPath);
        }
    }

    private void validateScenarioForLoad(JsonScenario scenario, boolean replaceCurrentState) {
        if (scenario == null) {
            throw new IllegalStateException("El escenario cargado es null.");
        }

        ArrayList<String> issues = new ArrayList<>();

        if (scenario.getTestId() == null || scenario.getTestId().trim().isEmpty()) {
            issues.add("test_id es obligatorio.");
        }

        int diskSize = replaceCurrentState ? 200 : fs.getDisk().getTotalBlocks();
        int initialHead = scenario.getInitialHead();
        if (initialHead < 0 || initialHead >= diskSize) {
            issues.add("initial_head debe estar entre 0 y " + (diskSize - 1) + ".");
        }

        SystemFileSeed[] seeds = scenario.getSystemFiles();
        Request[] requests = scenario.getRequests();

        if (seeds == null) {
            issues.add("system_files no puede ser null.");
            seeds = new SystemFileSeed[0];
        }

        if (requests == null) {
            issues.add("requests no puede ser null.");
            requests = new Request[0];
        }

        HashSet<Integer> seenPositions = new HashSet<>();
        HashSet<String> seenNames = new HashSet<>();
        int totalRequestedBlocks = 0;

        DirectoryNode systemDir = replaceCurrentState ? null : findDirectoryByPath("/system");
        HashSet<String> existingSystemNames = new HashSet<>();
        if (!replaceCurrentState && systemDir != null) {
            FileNode[] existingFiles = systemDir.getFiles();
            for (int i = 0; i < existingFiles.length; i++) {
                existingSystemNames.add(existingFiles[i].getName());
            }
            DirectoryNode[] existingDirs = systemDir.getSubdirectories();
            for (int i = 0; i < existingDirs.length; i++) {
                existingSystemNames.add(existingDirs[i].getName());
            }
        }

        for (int i = 0; i < seeds.length; i++) {
            SystemFileSeed seed = seeds[i];
            if (seed == null) {
                issues.add("system_files contiene una entrada null.");
                continue;
            }

            if (seed.getPos() < 0) {
                issues.add("system_files pos=" + seed.getPos() + " es inválido; debe ser >= 0.");
            }
            if (!seenPositions.add(seed.getPos())) {
                issues.add("system_files tiene pos duplicado: " + seed.getPos() + ".");
            }

            String name = seed.getName();
            if (name == null || name.trim().isEmpty()) {
                issues.add("system_files pos=" + seed.getPos() + " tiene nombre vacío.");
            } else {
                if (name.indexOf('/') >= 0 || ".".equals(name) || "..".equals(name)) {
                    issues.add("system_files pos=" + seed.getPos() + " tiene nombre inválido: " + name + ".");
                }
                if (!seenNames.add(name)) {
                    issues.add("system_files tiene nombre duplicado: " + name + ".");
                }
                if (!replaceCurrentState && existingSystemNames.contains(name)) {
                    issues.add("Ya existe en /system un archivo llamado " + name + ".");
                }
            }

            if (seed.getBlocks() <= 0) {
                issues.add("system_files pos=" + seed.getPos() + " debe tener blocks > 0.");
            } else {
                totalRequestedBlocks += seed.getBlocks();
            }
        }

        int availableBlocks = replaceCurrentState ? diskSize : fs.getDisk().countFreeBlocks();
        if (totalRequestedBlocks > availableBlocks) {
            issues.add(
                    "Los system_files requieren " + totalRequestedBlocks
                    + " bloques y solo hay " + availableBlocks + " disponibles."
            );
        }

        for (int i = 0; i < requests.length; i++) {
            Request req = requests[i];
            if (req == null) {
                issues.add("requests contiene una entrada null.");
                continue;
            }

            if (req.getOp() == null) {
                issues.add("requests pos=" + req.getPos() + " tiene op null.");
            }

            if (!seenPositions.contains(req.getPos())) {
                issues.add("requests pos=" + req.getPos() + " no tiene archivo asociado en system_files.");
            }
        }

        if (!issues.isEmpty()) {
            throw new IllegalStateException(buildScenarioValidationMessage(issues));
        }
    }

    private String buildScenarioValidationMessage(ArrayList<String> issues) {
        StringBuilder sb = new StringBuilder("JSON de escenario inválido:");
        for (int i = 0; i < issues.size(); i++) {
            sb.append("\n- ").append(issues.get(i));
        }
        return sb.toString();
    }

    public String[] runJournalCaseCreateCrash(String parentPath, String fileName, int blocks, boolean isAdmin) {
        requireAdmin(isAdmin, "ejecutar casos de journaling");

        if (parentPath == null || parentPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Ruta padre inválida.");
        }
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre de archivo inválido.");
        }
        if (blocks <= 0) {
            throw new IllegalArgumentException("El tamaño en bloques debe ser mayor que 0.");
        }

        String normalizedParent = parentPath.trim();
        String fullPath = "/".equals(normalizedParent) ? "/" + fileName.trim() : normalizedParent + "/" + fileName.trim();

        if (fs.getFileByPath(fullPath) != null) {
            throw new IllegalStateException("Ya existe el archivo del caso: " + fullPath);
        }

        int freeBefore = fs.getDisk().countFreeBlocks();
        fs.simulateCrashAfterNextCriticalOperation();

        String crashMessage = "No ocurrió crash.";
        try {
            fs.createFile(normalizedParent, fileName.trim(), "admin", blocks);
        } catch (RuntimeException ex) {
            crashMessage = ex.getMessage() == null ? "Crash simulado." : ex.getMessage();
        }

        boolean existsAfterCrash = fs.getFileByPath(fullPath) != null;
        int freeAfterCrash = fs.getDisk().countFreeBlocks();
        int reverted = fs.recoverPendingJournalEntries();
        boolean existsAfterRecovery = fs.getFileByPath(fullPath) != null;
        int freeAfterRecovery = fs.getDisk().countFreeBlocks();

        JournalEntry[] entries = fs.getJournalEntries();
        String lastEntry = "Sin entradas de journal.";
        if (entries.length > 0) {
            JournalEntry e = entries[entries.length - 1];
            lastEntry = "Última entrada: #" + e.getId()
                    + " op=" + e.getOperation().name()
                    + " status=" + e.getStatus().name();
        }

        persistState();

        return new String[]{
            "[J1] CREATE " + fullPath + " (" + blocks + " bloques)",
            "[J1] Crash simulado: " + crashMessage,
            "[J1] Tras crash -> existe=" + existsAfterCrash + ", libres=" + freeAfterCrash + " (antes=" + freeBefore + ")",
            "[J1] Recovery aplicado -> revertidas=" + reverted,
            "[J1] Tras recovery -> existe=" + existsAfterRecovery + ", libres=" + freeAfterRecovery,
            "[J1] " + lastEntry
        };
    }

    private void enqueueScenarioRequests(JsonScenario scenario) {
        Request[] requests = scenario.getRequests();
        lastLoadedScenarioRequestCount = requests.length;
        lastLoadedScenarioRequests = new String[requests.length];
        for (int i = 0; i < requests.length; i++) {
            Request req = requests[i];
            String resourcePath = resolveScenarioResourcePath(scenario.getSystemFiles(), req.getPos());
            int diskPos = resolveScenarioDiskPos(resourcePath);

            ProcessControlBlock pcb = new ProcessControlBlock(
                    nextPid,
                    "scenario",
                    new Request(diskPos, req.getOp()),
                    resourcePath
            );
            nextPid++;

            engine.submitProcess(pcb);
            processes.add(pcb);
            lastLoadedScenarioRequests[i] = "PID=" + pcb.getPid()
                    + " op=" + req.getOp().name()
                    + " posJSON=" + req.getPos()
                    + " posDisco=" + diskPos
                    + " recurso=" + resourcePath;
        }
    }

    private String resolveScenarioResourcePath(SystemFileSeed[] seeds, int pos) {
        for (int i = 0; i < seeds.length; i++) {
            if (seeds[i].getPos() == pos) {
                return "/system/" + seeds[i].getName();
            }
        }
        throw new IllegalStateException("El request pos=" + pos + " no tiene archivo asociado en system_files.");
    }

    private int resolveScenarioDiskPos(String resourcePath) {
        FileNode file = fs.getFileByPath(resourcePath);
        if (file == null) {
            throw new IllegalStateException("No existe archivo para request: " + resourcePath);
        }

        int firstBlock = file.getFirstBlockId();
        if (firstBlock < 0) {
            throw new IllegalStateException("Archivo sin bloque inicial para request: " + resourcePath);
        }

        return firstBlock;
    }

    private void requireAdmin(boolean isAdmin, String action) {
        if (!isAdmin) {
            throw new IllegalStateException("Permiso denegado: solo administrador puede " + action + ".");
        }
    }

    private DirectoryNode findDirectoryByPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        DirectoryNode root = fs.getRoot();
        if (root == null) {
            return null;
        }

        if ("/".equals(path)) {
            return root;
        }

        String[] parts = path.split("/");
        DirectoryNode current = root;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if (part == null || part.isEmpty()) {
                continue;
            }

            DirectoryNode next = null;
            DirectoryNode[] subdirs = current.getSubdirectories();
            for (int j = 0; j < subdirs.length; j++) {
                if (subdirs[j].getName().equals(part)) {
                    next = subdirs[j];
                    break;
                }
            }

            if (next == null) {
                return null;
            }
            current = next;
        }

        return current;
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
