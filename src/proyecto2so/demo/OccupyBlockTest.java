package proyecto2so.demo;

import proyecto2so.core.Block;
import proyecto2so.core.VirtualDisk;

public class OccupyBlockTest {
    public static void main(String[] args) {
        System.out.println("ENTRANDO A OccupyBlockTest");

        VirtualDisk disk = new VirtualDisk(10);

        int[] freeBlocks = disk.findFreeBlocks(3);

        disk.occupyBlock(freeBlocks[0], "A", freeBlocks[1]);
        disk.occupyBlock(freeBlocks[1], "A", freeBlocks[2]);
        disk.occupyBlock(freeBlocks[2], "A", -1);

        Block first = disk.getBlockById(freeBlocks[0]);
        Block second = disk.getBlockById(freeBlocks[1]);
        Block third = disk.getBlockById(freeBlocks[2]);

        if (first.isFree() || second.isFree() || third.isFree()) {
            throw new RuntimeException("Error: los bloques deberían estar ocupados.");
        }

        if (!"A".equals(first.getFileName()) ||
            !"A".equals(second.getFileName()) ||
            !"A".equals(third.getFileName())) {
            throw new RuntimeException("Error: los bloques no quedaron asociados al archivo correcto.");
        }

        if (first.getNextBlockId() != freeBlocks[1]) {
            throw new RuntimeException("Error: el primer bloque no apunta correctamente al segundo.");
        }

        if (second.getNextBlockId() != freeBlocks[2]) {
            throw new RuntimeException("Error: el segundo bloque no apunta correctamente al tercero.");
        }

        if (third.getNextBlockId() != -1) {
            throw new RuntimeException("Error: el último bloque debería apuntar a -1.");
        }

        if (disk.countFreeBlocks() != 7) {
            throw new RuntimeException("Error: deberían quedar 7 bloques libres.");
        }

        boolean controlledFailure = false;

        try {
            disk.occupyBlock(freeBlocks[0], "B", -1);
        } catch (IllegalStateException e) {
            controlledFailure = true;
        }

        if (!controlledFailure) {
            throw new RuntimeException("Error: ocupar un bloque ya ocupado debería fallar.");
        }

        System.out.println("Cadena de 3 bloques creada correctamente.");
        System.out.println("Conteo de bloques libres correcto.");
        System.out.println("Fallo controlado al intentar reocupar un bloque correcto.");
        System.out.println("Paso 4 completado correctamente.");
    }
}