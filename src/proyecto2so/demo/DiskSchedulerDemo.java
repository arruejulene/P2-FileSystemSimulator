/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyecto2so.demo;

/**
 *
 * @author ani
 */

import proyecto2so.core.Request;
import proyecto2so.core.RequestOp;
import proyecto2so.ds.SinglyLinkedList;
import proyecto2so.kernel.ProcessControlBlock;
import proyecto2so.scheduler.DiskScheduler;
import proyecto2so.scheduler.SchedulingPolicy;

public class DiskSchedulerDemo {

    public static void main(String[] args) {
        int[] positions = {95, 180, 34, 119, 11, 123, 62, 64};
        int head = 50;

        runPolicy(SchedulingPolicy.FIFO, head, positions);
        runPolicy(SchedulingPolicy.SSTF, head, positions);
        runPolicy(SchedulingPolicy.SCAN, head, positions);
        runPolicy(SchedulingPolicy.C_SCAN, head, positions);
    }

    private static void runPolicy(SchedulingPolicy policy, int initialHead, int[] positions) {
        DiskScheduler scheduler = new DiskScheduler(policy, initialHead);
        SinglyLinkedList<ProcessControlBlock> io = new SinglyLinkedList<>();

        int pid = 1;
        for (int pos : positions) {
            ProcessControlBlock pcb = new ProcessControlBlock(
                    pid++,
                    "user",
                    new Request(pos, RequestOp.READ),
                    "/system/test"
            );
            io.addLast(pcb);
        }

        System.out.println("\nPolicy: " + policy + " | initial head: " + initialHead);

        while (!io.isEmpty()) {
            ProcessControlBlock next = scheduler.selectNext(io);
            int pos = next.getRequest().getPos();

            System.out.print(pos + " ");

            scheduler.moveHeadTo(pos);

            io.remove(next);
        }

        System.out.println("\nTotal movement: " + scheduler.getTotalHeadMovement());
    }
}