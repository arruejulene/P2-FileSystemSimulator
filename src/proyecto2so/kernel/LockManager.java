/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyecto2so.kernel;

/**
 *
 * @author ani
 */

import proyecto2so.ds.SinglyLinkedList;

public class LockManager {
    private final SinglyLinkedList<LockEntry> entries;

    public LockManager() {
        entries = new SinglyLinkedList<>();
    }

    public boolean tryAcquire(String resource, int pid, LockType type) {
        if (type == LockType.SHARED) return tryAcquireShared(resource, pid);
        return tryAcquireExclusive(resource, pid);
    }

    public void release(String resource, int pid, LockType type) {
        if (type == LockType.SHARED) releaseShared(resource, pid);
        else releaseExclusive(resource, pid);
    }

    public boolean tryAcquireShared(String resource, int pid) {
        LockEntry e = getOrCreate(resource);

        if (e.exclusiveOwnerPid != -1 && e.exclusiveOwnerPid != pid) {
            return false;
        }

        e.sharedCount++;
        return true;
    }

    public boolean tryAcquireExclusive(String resource, int pid) {
        LockEntry e = getOrCreate(resource);

        if (e.exclusiveOwnerPid == pid) return true;

        if (e.sharedCount > 0) return false;

        if (e.exclusiveOwnerPid != -1 && e.exclusiveOwnerPid != pid) return false;

        e.exclusiveOwnerPid = pid;
        return true;
    }

    private void releaseShared(String resource, int pid) {
        LockEntry e = find(resource);
        if (e == null) return;

        if (e.sharedCount > 0) e.sharedCount--;

        cleanupIfFree(e);
    }

    private void releaseExclusive(String resource, int pid) {
        LockEntry e = find(resource);
        if (e == null) return;

        if (e.exclusiveOwnerPid == pid) e.exclusiveOwnerPid = -1;

        cleanupIfFree(e);
    }

    private void cleanupIfFree(LockEntry e) {
        if (e.isFree()) entries.remove(e);
    }

    private LockEntry getOrCreate(String resource) {
        LockEntry e = find(resource);
        if (e != null) return e;

        LockEntry created = new LockEntry(resource);
        entries.addLast(created);
        return created;
    }

    private LockEntry find(String resource) {
        final LockEntry[] found = new LockEntry[1];

        entries.forEach(value -> {
            if (found[0] == null && value != null && resource.equals(value.resource)) {
                found[0] = value;
            }
        });

        return found[0];
    }
    
    public boolean canAcquire(String resource, int pid, LockType type) {
    LockEntry e = find(resource);

    
    if (e == null) return true;

    if (type == LockType.SHARED) {
       
        return e.exclusiveOwnerPid == -1 || e.exclusiveOwnerPid == pid;
    } else {
        
        if (e.exclusiveOwnerPid == pid) return true;
        return e.exclusiveOwnerPid == -1 && e.sharedCount == 0;
    }
}

    public LockSnapshot[] getSnapshots() {
        Object[] values = entries.toArray();
        LockSnapshot[] snapshots = new LockSnapshot[values.length];

        for (int i = 0; i < values.length; i++) {
            LockEntry entry = (LockEntry) values[i];
            snapshots[i] = new LockSnapshot(entry.resource, entry.sharedCount, entry.exclusiveOwnerPid);
        }

        return snapshots;
    }
}
