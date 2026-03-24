/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyecto2so.kernel;

/**
 *
 * @author ani
 */


import proyecto2so.core.Request;
import proyecto2so.core.RequestOp;

public class ProcessControlBlock {
    private final int pid;
    private final String user;
    private ProcessState state;
    private final Request request;
    private final String resourcePath;
    private final QueuedFsOperation queuedFsOperation;
    private final LockType neededLock;
    private String blockedReason;

    
    private int remainingTicks;

    public ProcessControlBlock(int pid, String user, Request request, String resourcePath) {
        this(pid, user, request, resourcePath, null);
    }

    public ProcessControlBlock(int pid, String user, Request request, String resourcePath, QueuedFsOperation queuedFsOperation) {
        if (pid <= 0) throw new IllegalArgumentException("pid debe ser > 0");
        if (user == null || user.trim().isEmpty()) throw new IllegalArgumentException("user inválido");
        if (request == null) throw new IllegalArgumentException("request no puede ser null");
        if (resourcePath == null || resourcePath.trim().isEmpty()) throw new IllegalArgumentException("resourcePath inválido");

        this.pid = pid;
        this.user = user;
        this.request = request;
        this.resourcePath = resourcePath;
        this.queuedFsOperation = queuedFsOperation;
        this.neededLock = lockFromOp(request.getOp());
        this.state = ProcessState.NEW;
        this.blockedReason = null;
        this.remainingTicks = defaultTicksFor(request.getOp());
    }

    private LockType lockFromOp(RequestOp op) {
        if (op == RequestOp.READ) return LockType.SHARED;
        return LockType.EXCLUSIVE;
    }

    private int defaultTicksFor(RequestOp op) {
        if (op == RequestOp.READ) return 2;
        if (op == RequestOp.UPDATE) return 3;
        if (op == RequestOp.DELETE) return 2;
        if (op == RequestOp.CREATE_FILE) return 3;
        if (op == RequestOp.CREATE_DIRECTORY) return 2;
        if (op == RequestOp.DELETE_NODE) return 3;
        return 2;
    }

    public int getPid() { return pid; }
    public String getUser() { return user; }

    public ProcessState getState() { return state; }
    public void setState(ProcessState state) { this.state = state; }

    public Request getRequest() { return request; }
    public String getResourcePath() { return resourcePath; }
    public QueuedFsOperation getQueuedFsOperation() { return queuedFsOperation; }
    public LockType getNeededLock() { return neededLock; }

    public String getBlockedReason() { return blockedReason; }
    public void setBlockedReason(String blockedReason) { this.blockedReason = blockedReason; }

    public int getRemainingTicks() { return remainingTicks; }
    public void setRemainingTicks(int remainingTicks) { this.remainingTicks = remainingTicks; }

    public void consumeTick() {
        if (remainingTicks > 0) remainingTicks--;
    }

    public boolean isFinishedExecution() {
        return remainingTicks <= 0;
    }

    @Override
    public String toString() {
        return "PCB{pid=" + pid
                + ", user='" + user + '\''
                + ", state=" + state
                + ", op=" + request.getOp()
                + ", pos=" + request.getPos()
                + ", resource='" + resourcePath + '\''
                + ", ticks=" + remainingTicks
                + '}';
    }
}
