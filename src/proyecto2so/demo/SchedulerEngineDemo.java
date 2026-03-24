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

public class SchedulerEngineDemo {

    public static void main(String[] args) {
        runWithPolicy(SchedulingPolicy.FIFO);
        runWithPolicy(SchedulingPolicy.SSTF);
        runWithPolicy(SchedulingPolicy.SCAN);
        runWithPolicy(SchedulingPolicy.C_SCAN);
    }

    private static void runWithPolicy(SchedulingPolicy policy) {
        FileSystemService fs = new FileSystemService();
        fs.initialize(200);
        fs.createDirectory("/", "system", "admin");

        fs.createFile("/system", "f1", "admin", 5);
        fs.createFile("/system", "f2", "admin", 5);
        fs.createFile("/system", "f3", "admin", 5);

        DiskScheduler scheduler = new DiskScheduler(policy, 50);
        IOEngine engine = new IOEngine(fs, scheduler);

        engine.submitProcess(new ProcessControlBlock(1,"u1",new Request(95,RequestOp.READ),"/system/f1"));
        engine.submitProcess(new ProcessControlBlock(2,"u2",new Request(34,RequestOp.READ),"/system/f2"));
        engine.submitProcess(new ProcessControlBlock(3,"u3",new Request(119,RequestOp.READ),"/system/f3"));
        engine.submitProcess(new ProcessControlBlock(4,"u4",new Request(11,RequestOp.READ),"/system/f1"));
        engine.submitProcess(new ProcessControlBlock(5,"u5",new Request(62,RequestOp.READ),"/system/f2"));

        while (!engine.isDone()) {
            engine.tick();
        }

        System.out.println("Policy: " + policy);
        System.out.println("Head final: " + engine.getHeadPos());
        System.out.println("Movimiento total: " + engine.getTotalHeadMovement());
        System.out.println("---------------------------");
    }
}