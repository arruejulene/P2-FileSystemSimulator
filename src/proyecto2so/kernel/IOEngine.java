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
        this.terminated = new SinglyLinkedList<>();
    }

   

    public void submitProcess(ProcessControlBlock pcb) {
        pcb.setState(ProcessState.NEW);
        newQueue.enqueue(pcb);
    }

    public boolean isDone() {
        return newQueue.isEmpty() && readyQueue.isEmpty() && ioPending.isEmpty() && blocked.isEmpty();
    }

    public int getHeadPos() { return scheduler.getHeadPos(); }
    public int getTotalHeadMovement() { return scheduler.getTotalHeadMovement(); }

    public SinglyLinkedList<ProcessControlBlock> getBlockedList() { return blocked; }
    public SinglyLinkedList<ProcessControlBlock> getTerminatedList() { return terminated; }

    
    public void tick() {
        admitNewProcesses();
        moveReadyToIoPending();

        
        ProcessControlBlock next = scheduler.selectNext(ioPending);
        if (next != null) {
            
            ioPending.remove(next);

            boolean locked = lockManager.tryAcquire(next.getResourcePath(), next.getPid(), next.getNeededLock());

            if (!locked) {
                next.setState(ProcessState.BLOCKED);
                next.setBlockedReason("Recurso ocupado: " + next.getResourcePath());
                blocked.addLast(next);
            } else {
                
                next.setState(ProcessState.RUNNING);

                try {
                    executeRequest(next);
                    next.setState(ProcessState.TERMINATED);
                    terminated.addLast(next);
                } finally {
                    lockManager.release(next.getResourcePath(), next.getPid(), next.getNeededLock());
                }
            }
        }


        tryUnblockProcesses();
    }

    // ----------------- Internos -----------------

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
            // en este punto el proceso ya está listo para pedir E/S
            ioPending.addLast(pcb);
        }
    }

    private void tryUnblockProcesses() {
        
        boolean progressed = true;

        while (progressed) {
            progressed = false;

            final ProcessControlBlock[] candidate = new ProcessControlBlock[1];

            blocked.forEach(pcb -> {
                if (candidate[0] != null) return;
                if (pcb == null) return;

                boolean can = lockManager.canAcquire(pcb.getResourcePath(), pcb.getPid(), pcb.getNeededLock());
                if (can) {
                    candidate[0] = pcb;
                }
            });

            if (candidate[0] != null) {
                ProcessControlBlock pcb = candidate[0];
                blocked.remove(pcb);

                
                boolean locked = lockManager.tryAcquire(pcb.getResourcePath(), pcb.getPid(), pcb.getNeededLock());
                if (locked) {
                    pcb.setBlockedReason(null);
                    pcb.setState(ProcessState.READY);
                    readyQueue.enqueue(pcb);
                    progressed = true;
                } else {
                    
                    pcb.setState(ProcessState.BLOCKED);
                    blocked.addLast(pcb);
                }
            }
        }
    }

    private void executeRequest(ProcessControlBlock pcb) {
        Request req = pcb.getRequest();
        int pos = req.getPos();

        
        scheduler.moveHeadTo(pos);

        
        String path = pcb.getResourcePath(); // ya viene seteado al crear el pcb

        if (req.getOp() == RequestOp.READ) {
            FileNode file = fs.getFileByPath(path);
            if (file == null) {
                throw new IllegalStateException("READ: Archivo no existe: " + path);
            }
            
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

    private String fileNameFromPath(String path) {
        int idx = path.lastIndexOf('/');
        if (idx == -1) return path;
        if (idx == path.length() - 1) return path;
        return path.substring(idx + 1);
    }
}