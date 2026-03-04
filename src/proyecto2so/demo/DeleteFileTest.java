package proyecto2so.demo;

import proyecto2so.core.FileNode;
import proyecto2so.core.FileSystemService;

public class DeleteFileTest {
    public static void main(String[] args) {
        System.out.println("ENTRANDO A DeleteFileTest");

        FileSystemService fs = new FileSystemService();
        fs.initialize(10);

        fs.createDirectory("/", "usuarios", "admin");
        fs.createDirectory("/usuarios", "iraia", "iraia");

        fs.createFile("/usuarios/iraia", "borrar.txt", "iraia", 4);

        FileNode file = fs.getFileByPath("/usuarios/iraia/borrar.txt");

        if (file == null) {
            throw new RuntimeException("Error: el archivo debería existir antes de borrarlo.");
        }

        int firstBlockId = file.getFirstBlockId();
        int freeBeforeDelete = fs.getDisk().countFreeBlocks();

        fs.deleteFile("/usuarios/iraia/borrar.txt");

        if (fs.getFileByPath("/usuarios/iraia/borrar.txt") != null) {
            throw new RuntimeException("Error: el archivo no debería existir después de borrarlo.");
        }

        if (fs.getFileCount() != 0) {
            throw new RuntimeException("Error: el índice global debería quedar vacío.");
        }

        if (fs.getDisk().countFreeBlocks() != freeBeforeDelete + 4) {
            throw new RuntimeException("Error: deberían haberse liberado exactamente 4 bloques.");
        }

        boolean blocksReleased = false;

        try {
            fs.getDisk().traverseChain(firstBlockId);
        } catch (IllegalStateException e) {
            blocksReleased = true;
        }

        if (!blocksReleased) {
            throw new RuntimeException("Error: la cadena borrada no debería poder recorrerse.");
        }

        int freeBeforeFailure = fs.getDisk().countFreeBlocks();
        int fileCountBeforeFailure = fs.getFileCount();
        boolean controlledFailure = false;

        try {
            fs.deleteFile("/usuarios/iraia/no-existe.txt");
        } catch (IllegalStateException e) {
            controlledFailure = true;
        }

        if (!controlledFailure) {
            throw new RuntimeException("Error: borrar un archivo inexistente debería fallar.");
        }

        if (fs.getDisk().countFreeBlocks() != freeBeforeFailure) {
            throw new RuntimeException("Error: el disco no debería cambiar tras fallar el borrado.");
        }

        if (fs.getFileCount() != fileCountBeforeFailure) {
            throw new RuntimeException("Error: el índice global no debería cambiar tras fallar el borrado.");
        }

        System.out.println("Borrado lógico y físico correcto.");
        System.out.println("Liberación exacta de bloques correcta.");
        System.out.println("Fallo controlado para archivo inexistente correcto.");
        System.out.println("Paso 14 completado correctamente.");
    }
}