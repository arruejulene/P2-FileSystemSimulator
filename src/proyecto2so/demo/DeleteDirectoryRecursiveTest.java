package proyecto2so.demo;

import proyecto2so.core.FileSystemService;

public class DeleteDirectoryRecursiveTest {
    public static void main(String[] args) {
        System.out.println("ENTRANDO A DeleteDirectoryRecursiveTest");

        FileSystemService fs = new FileSystemService();
        fs.initialize(10);

        fs.createDirectory("/", "docs", "iraia");
        fs.createFile("/docs", "a.txt", "iraia", 2);
        fs.createDirectory("/docs", "personal", "iraia");
        fs.createFile("/docs/personal", "b.txt", "iraia", 3);

        if (fs.getFileCount() != 2) {
            throw new RuntimeException("Error: deberían existir 2 archivos antes del borrado.");
        }

        int freeBeforeDelete = fs.getDisk().countFreeBlocks();

        fs.deleteDirectoryRecursive("/docs");

        if (fs.getRoot().getSubdirectoryCount() != 0) {
            throw new RuntimeException("Error: /docs debería haber sido eliminado de la raíz.");
        }

        if (fs.getFileByPath("/docs/a.txt") != null) {
            throw new RuntimeException("Error: a.txt no debería existir después del borrado.");
        }

        if (fs.getFileByPath("/docs/personal/b.txt") != null) {
            throw new RuntimeException("Error: b.txt no debería existir después del borrado.");
        }

        if (fs.getFileCount() != 0) {
            throw new RuntimeException("Error: el índice global debería quedar vacío.");
        }

        if (fs.getDisk().countFreeBlocks() != freeBeforeDelete + 5) {
            throw new RuntimeException("Error: deberían haberse liberado exactamente 5 bloques.");
        }

        boolean missingDirectoryHandled = false;

        try {
            fs.deleteDirectoryRecursive("/no-existe");
        } catch (IllegalStateException e) {
            missingDirectoryHandled = true;
        }

        if (!missingDirectoryHandled) {
            throw new RuntimeException("Error: borrar un directorio inexistente debería fallar.");
        }

        boolean rootProtected = false;

        try {
            fs.deleteDirectoryRecursive("/");
        } catch (IllegalStateException e) {
            rootProtected = true;
        }

        if (!rootProtected) {
            throw new RuntimeException("Error: no se debería poder borrar la raíz.");
        }

        System.out.println("Borrado recursivo correcto.");
        System.out.println("Liberación total de bloques correcta.");
        System.out.println("Validaciones de directorio inexistente y raíz correctas.");
        System.out.println("Paso 15 completado correctamente.");
    }
}
