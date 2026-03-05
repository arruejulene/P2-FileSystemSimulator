/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyecto2so.scheduler;

/**
 *
 * @author ani
 */


import proyecto2so.core.Request;
import proyecto2so.ds.SinglyLinkedList;
import proyecto2so.kernel.ProcessControlBlock;
import proyecto2so.scheduler.SchedulingPolicy;

public class DiskScheduler {

    private SchedulingPolicy policy;
    private int headPos;
    private int totalHeadMovement;

  
    private int direction;

    public DiskScheduler(SchedulingPolicy policy, int initialHeadPos) {
        this.policy = policy;
        this.headPos = initialHeadPos;
        this.totalHeadMovement = 0;
        this.direction = +1; 
    }

    public void setPolicy(SchedulingPolicy policy) {
        this.policy = policy;
    }

    public SchedulingPolicy getPolicy() {
        return policy;
    }

    public int getHeadPos() {
        return headPos;
    }

    public int getTotalHeadMovement() {
        return totalHeadMovement;
    }

    public void setDirectionUp() {
        this.direction = +1;
    }

    public void setDirectionDown() {
        this.direction = -1;
    }

    
    public ProcessControlBlock selectNext(SinglyLinkedList<ProcessControlBlock> ioPending) {
        if (ioPending == null || ioPending.isEmpty()) return null;

        switch (policy) {
            case FIFO:
                return selectFIFO(ioPending);
            case SSTF:
                return selectSSTF(ioPending);
            case SCAN:
                return selectSCAN(ioPending);
            case C_SCAN:
                return selectCSCAN(ioPending);
            default:
                return selectFIFO(ioPending);
        }
    }

    
    public void moveHeadTo(int newPos) {
        int movement = abs(newPos - headPos);
        totalHeadMovement += movement;
        headPos = newPos;
    }

    
    private ProcessControlBlock selectFIFO(SinglyLinkedList<ProcessControlBlock> ioPending) {
        return ioPending.peekFirst();
    }

    private ProcessControlBlock selectSSTF(SinglyLinkedList<ProcessControlBlock> ioPending) {
        final ProcessControlBlock[] best = new ProcessControlBlock[1];
        final int[] bestDist = new int[] { Integer.MAX_VALUE };

        ioPending.forEach(pcb -> {
            if (pcb == null) return;
            int pos = pcb.getRequest().getPos();
            int dist = abs(pos - headPos);
            if (dist < bestDist[0]) {
                bestDist[0] = dist;
                best[0] = pcb;
            }
        });

        return best[0];
    }

    private ProcessControlBlock selectSCAN(SinglyLinkedList<ProcessControlBlock> ioPending) {
        ProcessControlBlock chosen = scanPickInDirection(ioPending, direction);

        
        if (chosen == null) {
            direction = -direction;
            chosen = scanPickInDirection(ioPending, direction);
        }

        return chosen;
    }

    private ProcessControlBlock scanPickInDirection(SinglyLinkedList<ProcessControlBlock> ioPending, int dir) {
        final ProcessControlBlock[] chosen = new ProcessControlBlock[1];

        if (dir > 0) {
            
            final int[] bestPos = new int[] { Integer.MAX_VALUE };

            ioPending.forEach(pcb -> {
                if (pcb == null) return;
                int pos = pcb.getRequest().getPos();
                if (pos >= headPos && pos < bestPos[0]) {
                    bestPos[0] = pos;
                    chosen[0] = pcb;
                }
            });

        } else {
            
            final int[] bestPos = new int[] { Integer.MIN_VALUE };

            ioPending.forEach(pcb -> {
                if (pcb == null) return;
                int pos = pcb.getRequest().getPos();
                if (pos <= headPos && pos > bestPos[0]) {
                    bestPos[0] = pos;
                    chosen[0] = pcb;
                }
            });
        }

        return chosen[0];
    }

    private ProcessControlBlock selectCSCAN(SinglyLinkedList<ProcessControlBlock> ioPending) {
        
        ProcessControlBlock chosen = cscanPickAtOrAbove(ioPending);

        if (chosen == null) {
            
            chosen = pickSmallestPos(ioPending);
        }

        return chosen;
    }

    private ProcessControlBlock cscanPickAtOrAbove(SinglyLinkedList<ProcessControlBlock> ioPending) {
        final ProcessControlBlock[] chosen = new ProcessControlBlock[1];
        final int[] bestPos = new int[] { Integer.MAX_VALUE };

        ioPending.forEach(pcb -> {
            if (pcb == null) return;
            int pos = pcb.getRequest().getPos();
            if (pos >= headPos && pos < bestPos[0]) {
                bestPos[0] = pos;
                chosen[0] = pcb;
            }
        });

        return chosen[0];
    }

    private ProcessControlBlock pickSmallestPos(SinglyLinkedList<ProcessControlBlock> ioPending) {
        final ProcessControlBlock[] chosen = new ProcessControlBlock[1];
        final int[] bestPos = new int[] { Integer.MAX_VALUE };

        ioPending.forEach(pcb -> {
            if (pcb == null) return;
            int pos = pcb.getRequest().getPos();
            if (pos < bestPos[0]) {
                bestPos[0] = pos;
                chosen[0] = pcb;
            }
        });

        return chosen[0];
    }

    private int abs(int x) {
        return x < 0 ? -x : x;
    }
}