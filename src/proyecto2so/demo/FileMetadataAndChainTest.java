package proyecto2so.demo;

import proyecto2so.core.FileNode;
import proyecto2so.core.FileSystemService;

public class FileMetadataAndChainTest {
    public static void main(String[] args) {
        System.out.println("ENTRANDO A FileMetadataAndChainTest");

        FileSystemService fs = new FileSystemService();
        fs.initialize(10);

        fs.createDirectory("/", "usuarios", "admin");
        fs.createDirectory("/usuarios", "iraia", "iraia");

        fs.createFile("/usuarios/iraia", "archivo.txt", "iraia", 4);

        FileNode file = fs.getFileByPath("/usuarios/iraia/archivo.txt");

        if (file == null) {
            throw new RuntimeException("Error: el archivo debería existir.");
        }

        if (!"archivo.txt".equals(file.getName())) {
            throw new RuntimeException("Error: el nombre del archivo no es correcto.");
        }

        if (!"iraia".equals(file.getOwner())) {
            throw new RuntimeException("Error: el owner del archivo no es correcto.");
        }

        if (file.getSizeInBlocks() != 4) {
            throw new RuntimeException("Error: el tamaño del archivo debería ser 4 bloques.");
        }

        if (file.getFirstBlockId() < 0) {
            throw new RuntimeException("Error: el archivo debería tener un firstBlockId válido.");
        }

        if (!"/usuarios/iraia/archivo.txt".equals(file.getPath())) {
            throw new RuntimeException("Error: la ruta del archivo no es correcta.");
        }

        int[] chain = fs.getFileBlockChain("/usuarios/iraia/archivo.txt");

        if (chain.length != 4) {
            throw new RuntimeException("Error: la cadena del archivo debería tener longitud 4.");
        }

        if (chain[0] != file.getFirstBlockId()) {
            throw new RuntimeException("Error: el primer bloque de la cadena no coincide con firstBlockId.");
        }

        boolean missingFileHandled = false;

        try {
            fs.getFileBlockChain("/usuarios/iraia/no-existe.txt");
        } catch (IllegalStateException e) {
            missingFileHandled = true;
        }

        if (!missingFileHandled) {
            throw new RuntimeException("Error: consultar un archivo inexistente debería fallar.");
        }

        System.out.println("Lectura de metadata correcta.");
        System.out.println("Recuperación de cadena correcta.");
        System.out.println("Validación de archivo inexistente correcta.");
        System.out.println("Paso 13 completado correctamente.");
    }
}