package proyecto2so.demo;

import proyecto2so.core.Block;
import proyecto2so.core.VirtualDisk;

public class FreeChainTest {
    public static void main(String[] args) {
        System.out.println("ENTRANDO A FreeChainTest");

        VirtualDisk disk = new VirtualDisk(10);

        int[] freeBlocks = disk.findFreeBlocks(4);

        disk.occupyBlock(freeBlocks[0], "A", freeBlocks[1]);
        disk.occupyBlock(freeBlocks[1], "A", freeBlocks[2]);
        disk.occupyBlock(freeBlocks[2], "A", freeBlocks[3]);
        disk.occupyBlock(freeBlocks[3], "A", -1);

        if (disk.countFreeBlocks() != 6) {
            throw new RuntimeException("Error: antes de liberar deberían quedar 6 bloques libres.");
        }

        disk.freeChain(freeBlocks[0]);

        for (int i = 0; i < freeBlocks.length; i++) {
            Block block = disk.getBlockById(freeBlocks[i]);

            if (!block.isFree()) {
                throw new RuntimeException("Error: el bloque " + block.getId() + " debería estar libre.");
            }

            if (block.getFileName() != null) {
                throw new RuntimeException("Error: el bloque " + block.getId() + " debería tener fileName en null.");
            }

            if (block.getNextBlockId() != -1) {
                throw new RuntimeException("Error: el bloque " + block.getId() + " debería tener nextBlockId en -1.");
            }
        }

        if (disk.countFreeBlocks() != 10) {
            throw new RuntimeException("Error: después de liberar deberían quedar 10 bloques libres.");
        }

        boolean controlledFailure = false;

        try {
            disk.traverseChain(freeBlocks[0]);
        } catch (IllegalStateException e) {
            controlledFailure = true;
        }

        if (!controlledFailure) {
            throw new RuntimeException("Error: no debería poder recorrerse una cadena liberada.");
        }

        System.out.println("Cadena liberada correctamente.");
        System.out.println("Bloques limpiados correctamente.");
        System.out.println("Conteo de bloques libres restaurado correctamente.");
        System.out.println("Paso 6 completado correctamente.");
    }
}
