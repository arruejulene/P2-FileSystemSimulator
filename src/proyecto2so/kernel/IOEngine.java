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
        boolean startedSomething = true;

        while (startedSomething) {
            startedSomething = false;

            ProcessControlBlock next = scheduler.selectNext(ioPending);
            if (next == null) return;

            boolean acquired = lockManager.tryAcquire(
                    next.getResourcePath(),
                    next.getPid(),
                    next.getNeededLock()
            );

            if (acquired) {
                ioPending.remove(next);
                next.setState(ProcessState.RUNNING);

                // mover cabezal cuando la solicitud comienza a ejecutarse
                scheduler.moveHeadTo(next.getRequest().getPos());

                running.addLast(next);
                startedSomething = true;
            } else {
                // si no se puede adquirir el lock, lo mandamos a bloqueados
                ioPending.remove(next);
                next.setState(ProcessState.BLOCKED);
                next.setBlockedReason("Recurso ocupado: " + next.getResourcePath());
                blocked.addLast(next);
                startedSomething = true;
            }
        }
    }

    
    private void advanceRunningProcesses() {
        boolean progressed = true;

        while (progressed) {
            progressed = false;

            final ProcessControlBlock[] finished = new ProcessControlBlock[1];

            running.forEach(pcb -> {
                if (finished[0] != null) return;
                if (pcb == null) return;

                pcb.consumeTick();

                if (pcb.isFinishedExecution()) {
                    finished[0] = pcb;
                }
            });

            if (finished[0] != null) {
                ProcessControlBlock pcb = finished[0];
                running.remove(pcb);

                try {
                    executeRequest(pcb);
                    pcb.setState(ProcessState.TERMINATED);
                    terminated.addLast(pcb);
                } finally {
                    lockManager.release(pcb.getResourcePath(), pcb.getPid(), pcb.getNeededLock());
                }

                progressed = true;
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
}
