/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyecto2so.kernel;

/**
 *
 * @author ani
 */


import proyecto2so.core.FileNode;
import proyecto2so.core.FileSystemService;
import proyecto2so.core.Request;
import proyecto2so.core.RequestOp;
import proyecto2so.core.DirectoryNode;
import proyecto2so.ds.Queue;
import proyecto2so.ds.SinglyLinkedList;
import proyecto2so.scheduler.DiskScheduler;

public class IOEngine {

    private final FileSystemService fs;
    private final DiskScheduler scheduler;
    private final LockManager lockManager;

    private final Queue<ProcessControlBlock> newQueue;
    private final Queue<ProcessControlBlock> readyQueue;
    private final SinglyLinkedList<ProcessControlBlock> ioPending;
    private final SinglyLinkedList<ProcessControlBlock> blocked;
    private final SinglyLinkedList<ProcessControlBlock> running;
    private final SinglyLinkedList<ProcessControlBlock> terminated;

    public IOEngine(FileSystemService fs, DiskScheduler scheduler) {
        if (fs == null) throw new IllegalArgumentException("FileSystemService no puede ser null");
        if (scheduler == null) throw new IllegalArgumentException("DiskScheduler no puede ser null");

        this.fs = fs;
        this.scheduler = scheduler;
        this.lockManager = new LockManager();

        this.newQueue = new Queue<>();
        this.readyQueue = new Queue<>();
        this.ioPending = new SinglyLinkedList<>();
        this.blocked = new SinglyLinkedList<>();
        this.running = new SinglyLinkedList<>();
        this.terminated = new SinglyLinkedList<>();
    }

    public void submitProcess(ProcessControlBlock pcb) {
        pcb.setState(ProcessState.NEW);
        newQueue.enqueue(pcb);
    }

    public boolean isDone() {
        return newQueue.isEmpty()
                && readyQueue.isEmpty()
                && ioPending.isEmpty()
                && blocked.isEmpty()
                && running.isEmpty();
    }

    public int getHeadPos() {
        return scheduler.getHeadPos();
    }

    public int getTotalHeadMovement() {
        return scheduler.getTotalHeadMovement();
    }

    public SinglyLinkedList<ProcessControlBlock> getBlockedList() {
        return blocked;
    }

    public SinglyLinkedList<ProcessControlBlock> getRunningList() {
        return running;
    }

    public SinglyLinkedList<ProcessControlBlock> getTerminatedList() {
        return terminated;
    }

    public Object[] getNewQueueSnapshot() {
        return newQueue.toArray();
    }

    public Object[] getReadyQueueSnapshot() {
        return readyQueue.toArray();
    }

    public Object[] getIoPendingSnapshot() {
        return ioPending.toArray();
    }

    public Object[] getBlockedSnapshot() {
        return blocked.toArray();
    }

    public Object[] getRunningSnapshot() {
        return running.toArray();
    }

    public Object[] getTerminatedSnapshot() {
        return terminated.toArray();
    }

    public LockSnapshot[] getLockSnapshots() {
        return lockManager.getSnapshots();
    }

    public void reset() {
        newQueue.clear();
        readyQueue.clear();
        ioPending.clear();
        blocked.clear();
        running.clear();
        terminated.clear();
        lockManager.clear();
    }

    public void tick() {
        admitNewProcesses();
        moveReadyToIoPending();
        tryStartProcesses();
        advanceRunningProcesses();
        tryUnblockProcesses();
    }

    private void admitNewProcesses() {
        while (!newQueue.isEmpty()) {
            ProcessControlBlock pcb = newQueue.dequeue();
            pcb.setState(ProcessState.READY);
            readyQueue.enqueue(pcb);
        }
    }

    private void moveReadyToIoPending() {
        while (!readyQueue.isEmpty()) {
            ProcessControlBlock pcb = readyQueue.dequeue();
            ioPending.addLast(pcb);
        }
    }

    
    private void tryStartProcesses() {
        ProcessControlBlock next = scheduler.selectNext(ioPending);
        if (next == null) {
            return;
        }

        boolean acquired = lockManager.tryAcquire(
                next.getResourcePath(),
                next.getPid(),
                next.getNeededLock()
        );

        ioPending.remove(next);

        if (acquired) {
            next.setState(ProcessState.RUNNING);
            // mover cabezal cuando la solicitud comienza a ejecutarse
            scheduler.moveHeadTo(next.getRequest().getPos());
            running.addLast(next);
            return;
        }

        // si no se puede adquirir el lock, lo mandamos a bloqueados
        next.setState(ProcessState.BLOCKED);
        next.setBlockedReason("Recurso ocupado: " + next.getResourcePath());
        blocked.addLast(next);
    }

    
    private void advanceRunningProcesses() {
        Object[] snapshot = running.toArray();

        for (int i = 0; i < snapshot.length; i++) {
            ProcessControlBlock pcb = (ProcessControlBlock) snapshot[i];
            if (pcb == null) {
                continue;
            }

            pcb.consumeTick();
            if (!pcb.isFinishedExecution()) {
                continue;
            }

            running.remove(pcb);

            try {
                executeRequest(pcb);
                pcb.setState(ProcessState.TERMINATED);
                terminated.addLast(pcb);
            } finally {
                lockManager.release(pcb.getResourcePath(), pcb.getPid(), pcb.getNeededLock());
            }
        }
    }

    /**
     * Reintenta desbloquear procesos cuando ya hay recursos disponibles.
     */
    private void tryUnblockProcesses() {
        boolean progressed = true;

        while (progressed) {
            progressed = false;

            final ProcessControlBlock[] candidate = new ProcessControlBlock[1];

            blocked.forEach(pcb -> {
                if (candidate[0] != null) return;
                if (pcb == null) return;

                boolean can = lockManager.canAcquire(
                        pcb.getResourcePath(),
                        pcb.getPid(),
                        pcb.getNeededLock()
                );

                if (can) {
                    candidate[0] = pcb;
                }
            });

            if (candidate[0] != null) {
                ProcessControlBlock pcb = candidate[0];
                blocked.remove(pcb);
                pcb.setBlockedReason(null);
                pcb.setState(ProcessState.READY);
                readyQueue.enqueue(pcb);
                progressed = true;
            }
        }
    }

    private void executeRequest(ProcessControlBlock pcb) {
        Request req = pcb.getRequest();
        QueuedFsOperation queuedOp = pcb.getQueuedFsOperation();

        if (queuedOp != null) {
            executeQueuedFsOperation(queuedOp);
            return;
        }

        String path = resolveExecutionPath(pcb);

        // The target may have been renamed/deleted by a previously completed process.
        // In that case we treat this request as obsolete and finish it without failing the engine loop.
        if (path == null) {
            return;
        }

        if (req.getOp() == RequestOp.READ) {
            fs.getFileBlockChain(path);

        } else if (req.getOp() == RequestOp.UPDATE) {
            String newName = fileNameFromPath(path) + "_upd_" + pcb.getPid();
            fs.renameFile(path, newName);

        } else if (req.getOp() == RequestOp.DELETE) {
            fs.deleteFile(path);

        } else {
            throw new IllegalStateException("Operación no soportada: " + req.getOp());
        }
    }

    private void executeQueuedFsOperation(QueuedFsOperation queuedOp) {
        if (queuedOp.getType() == QueuedFsOperation.Type.CREATE_FILE) {
            fs.createFile(
                    queuedOp.getParentPath(),
                    queuedOp.getNodeName(),
                    queuedOp.getOwner(),
                    queuedOp.getSizeInBlocks()
            );
            return;
        }

        if (queuedOp.getType() == QueuedFsOperation.Type.CREATE_DIRECTORY) {
            fs.createDirectory(
                    queuedOp.getParentPath(),
                    queuedOp.getNodeName(),
                    queuedOp.getOwner()
            );
            return;
        }

        if (queuedOp.getType() == QueuedFsOperation.Type.DELETE_NODE) {
            String path = queuedOp.getTargetPath();
            FileNode file = fs.getFileByPath(path);
            if (file != null) {
                fs.deleteFile(path);
                return;
            }

            if (resolveDirectory(path) != null) {
                fs.deleteDirectoryRecursive(path);
                return;
            }

            throw new IllegalStateException("DELETE_NODE: Nodo no existe: " + path);
        }

        throw new IllegalStateException("Operación encolada no soportada: " + queuedOp.getType());
    }

    private String resolveExecutionPath(ProcessControlBlock pcb) {
        String requestedPath = pcb.getResourcePath();
        FileNode direct = fs.getFileByPath(requestedPath);
        if (direct != null) {
            return requestedPath;
        }

        // Fallback: if the file was renamed, try to resolve by the requested disk position.
        FileNode[] files = fs.getFileIndex();
        int requestedPos = pcb.getRequest().getPos();

        for (int i = 0; i < files.length; i++) {
            if (files[i].getFirstBlockId() == requestedPos) {
                return files[i].getPath();
            }
        }

        return null;
    }

    private String fileNameFromPath(String path) {
        int idx = path.lastIndexOf('/');
        if (idx == -1) return path;
        if (idx == path.length() - 1) return path;
        return path.substring(idx + 1);
    }

    private DirectoryNode resolveDirectory(String path) {
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
}
