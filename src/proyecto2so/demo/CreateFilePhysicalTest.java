package proyecto2so.demo;

import proyecto2so.core.FileNode;
import proyecto2so.core.FileSystemService;

public class CreateFilePhysicalTest {
    public static void main(String[] args) {
        System.out.println("ENTRANDO A CreateFilePhysicalTest");

        FileSystemService fs = new FileSystemService();
        fs.initialize(10);

        fs.createDirectory("/", "usuarios", "admin");
        fs.createDirectory("/usuarios", "iraia", "iraia");

        fs.createFile("/usuarios/iraia", "a.txt", "iraia", 3);

        if (fs.getFileCount() != 1) {
            throw new RuntimeException("Error: debería existir 1 archivo en el índice global.");
        }

        FileNode fileA = fs.getFileIndex()[0];

        if (!"a.txt".equals(fileA.getName())) {
            throw new RuntimeException("Error: el archivo debería llamarse a.txt.");
        }

        if (fileA.getFirstBlockId() < 0) {
            throw new RuntimeException("Error: a.txt debería tener un firstBlockId válido.");
        }

        int[] chainA = fs.getDisk().traverseChain(fileA.getFirstBlockId());

        if (chainA.length != 3) {
            throw new RuntimeException("Error: la cadena de a.txt debería tener longitud 3.");
        }

        if (fs.getDisk().countFreeBlocks() != 7) {
            throw new RuntimeException("Error: después de crear a.txt deberían quedar 7 bloques libres.");
        }

        fs.createFile("/usuarios/iraia", "b.txt", "iraia", 2);

        if (fs.getFileCount() != 2) {
            throw new RuntimeException("Error: debería existir 2 archivos en el índice global.");
        }

        FileNode fileB = fs.getFileIndex()[1];

        if (!"b.txt".equals(fileB.getName())) {
            throw new RuntimeException("Error: el segundo archivo debería llamarse b.txt.");
        }

        if (fileB.getFirstBlockId() < 0) {
            throw new RuntimeException("Error: b.txt debería tener un firstBlockId válido.");
        }

        int[] chainB = fs.getDisk().traverseChain(fileB.getFirstBlockId());

        if (chainB.length != 2) {
            throw new RuntimeException("Error: la cadena de b.txt debería tener longitud 2.");
        }

        if (fs.getDisk().countFreeBlocks() != 5) {
            throw new RuntimeException("Error: después de crear b.txt deberían quedar 5 bloques libres.");
        }

        int freeBeforeFailure = fs.getDisk().countFreeBlocks();
        int fileCountBeforeFailure = fs.getFileCount();
        boolean controlledFailure = false;

        try {
            fs.createFile("/usuarios/iraia", "gigante.txt", "iraia", 20);
        } catch (IllegalStateException e) {
            controlledFailure = true;
        }

        if (!controlledFailure) {
            throw new RuntimeException("Error: crear un archivo demasiado grande debería fallar.");
        }

        if (fs.getDisk().countFreeBlocks() != freeBeforeFailure) {
            throw new RuntimeException("Error: el conteo de libres no debería cambiar tras el fallo.");
        }

        if (fs.getFileCount() != fileCountBeforeFailure) {
            throw new RuntimeException("Error: no debería agregarse un archivo al índice tras el fallo.");
        }

        System.out.println("Asignación física de a.txt correcta.");
        System.out.println("Asignación física de b.txt correcta.");
        System.out.println("Fallo controlado sin cambios parciales correcto.");
        System.out.println("Paso 12 completado correctamente.");
    }
}