package proyecto2so.demo;

import proyecto2so.core.DirectoryNode;
import proyecto2so.core.FileNode;
import proyecto2so.core.FileSystemService;

public class CreateFileLogicalTest {
    public static void main(String[] args) {
        System.out.println("ENTRANDO A CreateFileLogicalTest");

        FileSystemService fs = new FileSystemService();
        fs.initialize(12);

        fs.createDirectory("/", "usuarios", "admin");
        fs.createDirectory("/usuarios", "iraia", "iraia");

        fs.createFile("/usuarios/iraia", "apunte.txt", "iraia", 3);

        DirectoryNode usuarios = fs.getRoot().getSubdirectories()[0];
        DirectoryNode iraia = usuarios.getSubdirectories()[0];

        if (iraia.getFileCount() != 1) {
            throw new RuntimeException("Error: /usuarios/iraia debería tener 1 archivo.");
        }

        FileNode file = iraia.getFiles()[0];

        if (!"apunte.txt".equals(file.getName())) {
            throw new RuntimeException("Error: el archivo debería llamarse apunte.txt.");
        }

        if (!"iraia".equals(file.getOwner())) {
            throw new RuntimeException("Error: el owner del archivo debería ser iraia.");
        }

        if (file.getSizeInBlocks() != 3) {
            throw new RuntimeException("Error: el tamaño del archivo debería ser 3 bloques.");
        }

        if (file.getFirstBlockId() != -1) {
            throw new RuntimeException("Error: en esta fase lógica, firstBlockId debería ser -1.");
        }

        if (!"/usuarios/iraia/apunte.txt".equals(file.getPath())) {
            throw new RuntimeException("Error: la ruta del archivo no es correcta.");
        }

        if (fs.getFileCount() != 1) {
            throw new RuntimeException("Error: debería haber 1 archivo en el índice global.");
        }

        if (!"apunte.txt".equals(fs.getFileIndex()[0].getName())) {
            throw new RuntimeException("Error: el índice global no contiene el archivo correcto.");
        }

        boolean invalidSizeBlocked = false;

        try {
            fs.createFile("/usuarios/iraia", "invalido.txt", "iraia", 0);
        } catch (IllegalArgumentException e) {
            invalidSizeBlocked = true;
        }

        if (!invalidSizeBlocked) {
            throw new RuntimeException("Error: un archivo con tamaño 0 debería fallar.");
        }

        boolean invalidPathBlocked = false;

        try {
            fs.createFile("/ruta/inexistente", "otro.txt", "iraia", 2);
        } catch (IllegalStateException e) {
            invalidPathBlocked = true;
        }

        if (!invalidPathBlocked) {
            throw new RuntimeException("Error: crear archivo en ruta inexistente debería fallar.");
        }

        System.out.println("Creación lógica de archivo correcta.");
        System.out.println("Metadata del archivo correcta.");
        System.out.println("Índice global actualizado correctamente.");
        System.out.println("Validaciones de tamaño y ruta correctas.");
        System.out.println("Paso 11 completado correctamente.");
    }
}