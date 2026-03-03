package proyecto2so.demo;

import proyecto2so.core.VirtualDisk;

public class TraverseChainTest {
    public static void main(String[] args) {
        System.out.println("ENTRANDO A TraverseChainTest");

        VirtualDisk disk = new VirtualDisk(10);

        int[] freeBlocks = disk.findFreeBlocks(4);

        disk.occupyBlock(freeBlocks[0], "A", freeBlocks[1]);
        disk.occupyBlock(freeBlocks[1], "A", freeBlocks[2]);
        disk.occupyBlock(freeBlocks[2], "A", freeBlocks[3]);
        disk.occupyBlock(freeBlocks[3], "A", -1);

        int[] chain = disk.traverseChain(freeBlocks[0]);

        if (chain.length != 4) {
            throw new RuntimeException("Error: la cadena debería tener longitud 4.");
        }

        if (chain[0] != freeBlocks[0]) {
            throw new RuntimeException("Error: el primer bloque recorrido no es correcto.");
        }

        if (chain[1] != freeBlocks[1]) {
            throw new RuntimeException("Error: el segundo bloque recorrido no es correcto.");
        }

        if (chain[2] != freeBlocks[2]) {
            throw new RuntimeException("Error: el tercer bloque recorrido no es correcto.");
        }

        if (chain[3] != freeBlocks[3]) {
            throw new RuntimeException("Error: el cuarto bloque recorrido no es correcto.");
        }

        boolean controlledFailure = false;

        try {
            disk.occupyBlock(9, "B", 9);
            disk.traverseChain(9);
        } catch (IllegalStateException e) {
            controlledFailure = true;
        }

        if (!controlledFailure) {
            throw new RuntimeException("Error: una cadena con ciclo debería fallar de forma controlada.");
        }

        System.out.println("Recorrido de cadena correcto.");
        System.out.println("Detección de ciclo correcta.");
        System.out.println("Paso 5 completado correctamente.");
    }
}
