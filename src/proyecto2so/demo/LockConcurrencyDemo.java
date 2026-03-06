/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package proyecto2so.demo;

/**
 *
 * @author ani
 */


import proyecto2so.core.FileSystemService;
import proyecto2so.core.Request;
import proyecto2so.core.RequestOp;
import proyecto2so.kernel.IOEngine;
import proyecto2so.kernel.ProcessControlBlock;
import proyecto2so.scheduler.DiskScheduler;
import proyecto2so.scheduler.SchedulingPolicy;

public class LockConcurrencyDemo {

    public static void main(String[] args) {
        FileSystemService fs = new FileSystemService();
        fs.initialize(200);

        fs.createDirectory("/", "system", "admin");
        fs.createFile("/system", "sharedFile", "admin", 5);

        DiskScheduler scheduler = new DiskScheduler(SchedulingPolicy.FIFO, 0);
        IOEngine engine = new IOEngine(fs, scheduler);

        ProcessControlBlock p1 = new ProcessControlBlock(
                1, "user1", new Request(10, RequestOp.READ), "/system/sharedFile"
        );

        ProcessControlBlock p2 = new ProcessControlBlock(
                2, "user2", new Request(20, RequestOp.READ), "/system/sharedFile"
        );

        ProcessControlBlock p3 = new ProcessControlBlock(
                3, "user3", new Request(30, RequestOp.UPDATE), "/system/sharedFile"
        );

        engine.submitProcess(p1);
        engine.submitProcess(p2);
        engine.submitProcess(p3);

        int tick = 1;
        while (!engine.isDone()) {
            System.out.println("----- TICK " + tick + " -----");
            engine.tick();

            System.out.println("P1: " + p1);
            System.out.println("P2: " + p2);
            System.out.println("P3: " + p3);
            System.out.println("Running: " + engine.getRunningList().size());
            System.out.println("Blocked: " + engine.getBlockedList().size());
            System.out.println("Terminated: " + engine.getTerminatedList().size());
            System.out.println();

            tick++;
        }

        System.out.println("=== RESULTADO FINAL ===");
        System.out.println("Archivo original existe: " + (fs.getFileByPath("/system/sharedFile") != null));
        System.out.println("Movimiento total del cabezal: " + engine.getTotalHeadMovement());
        System.out.println("Procesos terminados: " + engine.getTerminatedList().size());
    }
}