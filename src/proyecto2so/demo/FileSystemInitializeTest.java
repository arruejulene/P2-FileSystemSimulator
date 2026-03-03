package proyecto2so.demo;

import proyecto2so.core.FileSystemService;

public class FileSystemInitializeTest {
    public static void main(String[] args) {
        System.out.println("ENTRANDO A FileSystemInitializeTest");

        FileSystemService fs = new FileSystemService();
        fs.initialize(12);

        if (fs.getRoot() == null) {
            throw new RuntimeException("Error: la raíz no debería ser null.");
        }

        if (!"/".equals(fs.getRoot().getPath())) {
            throw new RuntimeException("Error: la raíz debería tener ruta '/'.");
        }

        if (fs.getDisk() == null) {
            throw new RuntimeException("Error: el disco no debería ser null.");
        }

        if (fs.getDisk().getTotalBlocks() != 12) {
            throw new RuntimeException("Error: el disco debería tener 12 bloques.");
        }

        if (fs.getDisk().countFreeBlocks() != 12) {
            throw new RuntimeException("Error: deberían existir 12 bloques libres.");
        }

        if (fs.getFileCount() != 0) {
            throw new RuntimeException("Error: no debería haber archivos registrados.");
        }

        if (fs.getFileIndex().length != 0) {
            throw new RuntimeException("Error: el índice de archivos debería iniciar vacío.");
        }

        System.out.println("Raíz inicializada correctamente.");
        System.out.println("Disco inicializado correctamente.");
        System.out.println("Índice de archivos inicializado vacío correctamente.");
        System.out.println("Paso 9 completado correctamente.");
    }
}