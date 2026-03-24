package proyecto2so.demo;

import proyecto2so.core.VirtualDisk;

public class FreeBlockSearchTest {
    public static void main(String[] args) {
        System.out.println("ENTRANDO A FreeBlockSearchTest");

        VirtualDisk disk = new VirtualDisk(10);

        int[] firstThree = disk.findFreeBlocks(3);

        if (firstThree.length != 3) {
            throw new RuntimeException("Error: deberían devolverse 3 bloques.");
        }

        if (firstThree[0] != 0 || firstThree[1] != 1 || firstThree[2] != 2) {
            throw new RuntimeException("Error: los primeros 3 bloques libres deberían ser 0, 1 y 2.");
        }

        disk.getBlockById(0).occupy("A", -1);
        disk.getBlockById(1).occupy("A", -1);

        int[] nextThree = disk.findFreeBlocks(3);

        if (nextThree.length != 3) {
            throw new RuntimeException("Error: deberían devolverse 3 bloques después de ocupar 2.");
        }

        if (nextThree[0] == 0 || nextThree[0] == 1 ||
            nextThree[1] == 0 || nextThree[1] == 1 ||
            nextThree[2] == 0 || nextThree[2] == 1) {
            throw new RuntimeException("Error: findFreeBlocks devolvió bloques ocupados.");
        }

        boolean controlledFailure = false;

        try {
            disk.findFreeBlocks(20);
        } catch (IllegalStateException e) {
            controlledFailure = true;
        }

        if (!controlledFailure) {
            throw new RuntimeException("Error: pedir más bloques de los disponibles debería fallar.");
        }

        System.out.println("Primera búsqueda correcta.");
        System.out.println("Segunda búsqueda evita bloques ocupados correctamente.");
        System.out.println("Fallo controlado por falta de espacio correcto.");
        System.out.println("✅ Paso 3 completado correctamente.");
    }
}