package proyecto2so.demo;

import proyecto2so.core.FileNode;
import proyecto2so.core.FileSystemService;

public class RenameTest {
    public static void main(String[] args) {
        System.out.println("ENTRANDO A RenameTest");

        FileSystemService fs = new FileSystemService();
        fs.initialize(10);

        fs.createDirectory("/", "docs", "iraia");
        fs.createFile("/docs", "a.txt", "iraia", 3);

        FileNode originalFile = fs.getFileByPath("/docs/a.txt");

        if (originalFile == null) {
            throw new RuntimeException("Error: el archivo original debería existir.");
        }

        int originalFirstBlockId = originalFile.getFirstBlockId();
        int originalSize = originalFile.getSizeInBlocks();

        fs.renameFile("/docs/a.txt", "final.txt");

        if (fs.getFileByPath("/docs/a.txt") != null) {
            throw new RuntimeException("Error: a.txt ya no debería existir.");
        }

        FileNode renamedFile = fs.getFileByPath("/docs/final.txt");

        if (renamedFile == null) {
            throw new RuntimeException("Error: final.txt debería existir.");
        }

        if (renamedFile.getFirstBlockId() != originalFirstBlockId) {
            throw new RuntimeException("Error: el renombrado no debería cambiar firstBlockId.");
        }

        if (renamedFile.getSizeInBlocks() != originalSize) {
            throw new RuntimeException("Error: el renombrado no debería cambiar el tamaño.");
        }

        if (!"/docs/final.txt".equals(renamedFile.getPath())) {
            throw new RuntimeException("Error: la ruta del archivo renombrado no es correcta.");
        }

        fs.renameDirectory("/docs", "archivos");

        if (fs.getRoot().getSubdirectoryCount() != 1) {
            throw new RuntimeException("Error: debería seguir existiendo un solo directorio en la raíz.");
        }

        if (!"archivos".equals(fs.getRoot().getSubdirectories()[0].getName())) {
            throw new RuntimeException("Error: docs debería haberse renombrado a archivos.");
        }

        if (fs.getFileByPath("/docs/final.txt") != null) {
            throw new RuntimeException("Error: la ruta antigua del archivo ya no debería existir.");
        }

        FileNode movedPathFile = fs.getFileByPath("/archivos/final.txt");

        if (movedPathFile == null) {
            throw new RuntimeException("Error: el archivo debería existir bajo la nueva ruta.");
        }

        if (movedPathFile.getFirstBlockId() != originalFirstBlockId) {
            throw new RuntimeException("Error: renombrar el directorio no debería cambiar los bloques del archivo.");
        }

        boolean duplicateNameBlocked = false;

        fs.createFile("/archivos", "otro.txt", "iraia", 1);

        try {
            fs.renameFile("/archivos/final.txt", "otro.txt");
        } catch (IllegalStateException e) {
            duplicateNameBlocked = true;
        }

        if (!duplicateNameBlocked) {
            throw new RuntimeException("Error: renombrar a un nombre duplicado debería fallar.");
        }

        boolean rootProtected = false;

        try {
            fs.renameDirectory("/", "nuevoRoot");
        } catch (IllegalStateException e) {
            rootProtected = true;
        }

        if (!rootProtected) {
            throw new RuntimeException("Error: no se debería poder renombrar la raíz.");
        }

        System.out.println("Renombrado de archivo correcto.");
        System.out.println("Renombrado de directorio correcto.");
        System.out.println("Bloques conservados correctamente.");
        System.out.println("Validaciones de duplicado y raíz correctas.");
        System.out.println("Paso 16 completado correctamente.");
    }
}