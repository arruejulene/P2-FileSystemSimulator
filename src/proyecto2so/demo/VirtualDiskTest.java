package proyecto2so.demo;

import proyecto2so.core.Block;
import proyecto2so.core.VirtualDisk;

public class VirtualDiskTest {
    public static void main(String[] args) {
        System.out.println("ENTRANDO A VirtualDiskTest");

        VirtualDisk disk = new VirtualDisk(10);

        System.out.println("=== PRUEBA PASO 2: VIRTUAL DISK ===");

        if (disk.getTotalBlocks() != 10) {
            throw new RuntimeException("Error: totalBlocks debería ser 10.");
        }

        if (disk.getBlocks().length != 10) {
            throw new RuntimeException("Error: el arreglo de bloques debería tener longitud 10.");
        }

        for (int i = 0; i < 10; i++) {
            Block block = disk.getBlockById(i);

            if (block.getId() != i) {
                throw new RuntimeException("Error: el bloque en la posición " + i + " no tiene el id correcto.");
            }

            if (!block.isFree()) {
                throw new RuntimeException("Error: el bloque " + i + " debería iniciar libre.");
            }

            if (block.getFileName() != null) {
                throw new RuntimeException("Error: el bloque " + i + " no debería tener fileName asignado.");
            }

            if (block.getNextBlockId() != -1) {
                throw new RuntimeException("Error: el bloque " + i + " debería tener nextBlockId = -1.");
            }
        }

        if (disk.countFreeBlocks() != 10) {
            throw new RuntimeException("Error: countFreeBlocks() debería devolver 10.");
        }

        Block block5 = disk.getBlockById(5);

        if (block5.getId() != 5) {
            throw new RuntimeException("Error: getBlockById(5) no devolvió el bloque correcto.");
        }

        System.out.println("Total de bloques correcto: " + disk.getTotalBlocks());
        System.out.println("Cantidad de bloques libres correcta: " + disk.countFreeBlocks());
        System.out.println("Bloque 5 recuperado correctamente: " + block5);
        System.out.println("✅ Paso 2 completado correctamente.");
    }
}