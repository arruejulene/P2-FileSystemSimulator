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

public class IOEngineDemo {

    public static void main(String[] args) {
        // 1) Crear e inicializar el sistema de archivos
        FileSystemService fs = new FileSystemService();
        fs.initialize(200);

        // 2) Crear directorio /system
        fs.createDirectory("/", "system", "admin");

        // 3) Crear archivos de prueba
        fs.createFile("/system", "file1", "admin", 5);
        fs.createFile("/system", "file2", "admin", 3);
        fs.createFile("/system", "file3", "admin", 4);
        fs.createFile("/system", "file4", "admin", 2);

        // 4) Crear scheduler
        DiskScheduler scheduler = new DiskScheduler(SchedulingPolicy.SCAN, 50);

        // 5) Crear engine
        IOEngine engine = new IOEngine(fs, scheduler);

        // 6) Crear procesos de prueba
        // Cada uno trabaja sobre un archivo distinto para evitar conflictos de nombre
        engine.submitProcess(new ProcessControlBlock(
                1,
                "ana",
                new Request(20, RequestOp.READ),
                "/system/file1"
        ));

        engine.submitProcess(new ProcessControlBlock(
                2,
                "samuel",
                new Request(70, RequestOp.UPDATE),
                "/system/file2"
        ));

        engine.submitProcess(new ProcessControlBlock(
                3,
                "dayleen",
                new Request(40, RequestOp.READ),
                "/system/file3"
        ));

        engine.submitProcess(new ProcessControlBlock(
                4,
                "ana",
                new Request(90, RequestOp.DELETE),
                "/system/file4"
        ));

        // 7) Ejecutar simulación por ticks
        while (!engine.isDone()) {
            engine.tick();
        }

        // 8) Resultados
        System.out.println("Simulación finalizada");
        System.out.println("Posición final del cabezal: " + engine.getHeadPos());
        System.out.println("Movimiento total del cabezal: " + engine.getTotalHeadMovement());
        System.out.println("Procesos terminados: " + engine.getTerminatedList().size());
        System.out.println("Procesos bloqueados restantes: " + engine.getBlockedList().size());
        System.out.println("Cantidad final de archivos: " + fs.getFileCount());

        // 9) Estado final de los archivos
        System.out.println("file1 existe: " + (fs.getFileByPath("/system/file1") != null));
        System.out.println("file2 existe: " + (fs.getFileByPath("/system/file2") != null));
        System.out.println("file3 existe: " + (fs.getFileByPath("/system/file3") != null));
        System.out.println("file4 existe: " + (fs.getFileByPath("/system/file4") != null));
    }
}