package proyecto2so.demo;

import proyecto2so.core.FileNode;
import proyecto2so.core.FileSystemService;

public class JsonPersistenceTest {
    public static void main(String[] args) {
        System.out.println("ENTRANDO A JsonPersistenceTest");

        String jsonPath = "filesystem_state.json";

        FileSystemService fs = new FileSystemService();
        fs.initialize(10);

        fs.createDirectory("/", "docs", "iraia");
        fs.createDirectory("/docs", "personal", "iraia");
        fs.createFile("/docs", "a.txt", "iraia", 2);
        fs.createFile("/docs/personal", "b.txt", "iraia", 3);

        int freeBeforeSave = fs.getDisk().countFreeBlocks();

        FileNode fileA = fs.getFileByPath("/docs/a.txt");
        FileNode fileB = fs.getFileByPath("/docs/personal/b.txt");

        if (fileA == null || fileB == null) {
            throw new RuntimeException("Error: los archivos deberían existir antes de guardar.");
        }

        int firstBlockA = fileA.getFirstBlockId();
        int firstBlockB = fileB.getFirstBlockId();

        fs.saveToJson(jsonPath);

        FileSystemService loaded = new FileSystemService();
        loaded.loadFromJson(jsonPath);

        if (loaded.getRoot() == null) {
            throw new RuntimeException("Error: la raíz no debería ser null tras cargar.");
        }

        if (loaded.getDisk().getTotalBlocks() != 10) {
            throw new RuntimeException("Error: el disco cargado debería tener 10 bloques.");
        }

        if (loaded.getDisk().countFreeBlocks() != freeBeforeSave) {
            throw new RuntimeException("Error: el conteo de bloques libres no coincide tras cargar.");
        }

        FileNode loadedA = loaded.getFileByPath("/docs/a.txt");
        FileNode loadedB = loaded.getFileByPath("/docs/personal/b.txt");

        if (loadedA == null || loadedB == null) {
            throw new RuntimeException("Error: los archivos deberían existir tras cargar.");
        }

        if (loadedA.getSizeInBlocks() != 2 || loadedB.getSizeInBlocks() != 3) {
            throw new RuntimeException("Error: los tamaños de archivo no coinciden tras cargar.");
        }

        if (loadedA.getFirstBlockId() != firstBlockA) {
            throw new RuntimeException("Error: firstBlockId de a.txt no coincide tras cargar.");
        }

        if (loadedB.getFirstBlockId() != firstBlockB) {
            throw new RuntimeException("Error: firstBlockId de b.txt no coincide tras cargar.");
        }

        if (loaded.getFileBlockChain("/docs/a.txt").length != 2) {
            throw new RuntimeException("Error: la cadena de a.txt debería seguir teniendo 2 bloques.");
        }

        if (loaded.getFileBlockChain("/docs/personal/b.txt").length != 3) {
            throw new RuntimeException("Error: la cadena de b.txt debería seguir teniendo 3 bloques.");
        }

        boolean invalidJsonHandled = false;

        try {
            FileSystemService broken = new FileSystemService();
            broken.loadFromJson("archivo_inexistente.json");
        } catch (IllegalStateException e) {
            invalidJsonHandled = true;
        }

        if (!invalidJsonHandled) {
            throw new RuntimeException("Error: cargar un JSON inválido/inexistente debería fallar.");
        }

        System.out.println("Guardado en JSON correcto.");
        System.out.println("Carga desde JSON correcta.");
        System.out.println("Jerarquía, bloques y cadenas restaurados correctamente.");
        System.out.println("Validación de JSON inválido correcta.");
        System.out.println("Paso 18 completado correctamente.");
    }
}