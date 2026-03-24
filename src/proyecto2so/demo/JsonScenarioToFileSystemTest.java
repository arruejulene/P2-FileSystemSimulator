package proyecto2so.demo;

import proyecto2so.core.FileNode;
import proyecto2so.core.FileSystemService;
import proyecto2so.core.JsonScenario;
import proyecto2so.core.JsonScenarioLoader;
import proyecto2so.json.TestScenarioApplier;

public class JsonScenarioToFileSystemTest {
    public static void main(String[] args) throws Exception {
        System.out.println("ENTRANDO A JsonScenarioToFileSystemTest");

        String jsonPath = "test_input.json";

        JsonScenario scenario = JsonScenarioLoader.loadFromFile(jsonPath);

        TestScenarioApplier applier = new TestScenarioApplier();

        int requiredBlocks = applier.calculateRequiredBlocks(scenario);

        if (requiredBlocks != 113) {
            throw new RuntimeException("Error: el escenario debería requerir 113 bloques.");
        }

        FileSystemService fs = new FileSystemService();
        fs.initialize(200);

        applier.apply(scenario, fs);

        if (fs.getRoot().getSubdirectoryCount() != 1) {
            throw new RuntimeException("Error: debería existir un único directorio /system.");
        }

        if (!"system".equals(fs.getRoot().getSubdirectories()[0].getName())) {
            throw new RuntimeException("Error: el directorio creado debería llamarse system.");
        }

        if (fs.getFileCount() != 8) {
            throw new RuntimeException("Error: deberían haberse creado 8 archivos.");
        }

        assertFile(fs, "/system/boot_sect.bin", 2);
        assertFile(fs, "/system/readme.txt", 1);
        assertFile(fs, "/system/script.py", 8);
        assertFile(fs, "/system/style.css", 6);
        assertFile(fs, "/system/config.sys", 4);
        assertFile(fs, "/system/image_01.png", 12);
        assertFile(fs, "/system/data_log.csv", 28);
        assertFile(fs, "/system/video_clip.mp4", 52);

        if (fs.getDisk().countFreeBlocks() != 87) {
            throw new RuntimeException("Error: deberían quedar 87 bloques libres.");
        }

        System.out.println("Escenario JSON aplicado correctamente.");
        System.out.println("Archivos creados en /system correctamente.");
        System.out.println("Bloques asignados correctamente.");
        System.out.println("Paso de carga real del escenario completado correctamente.");
    }

    private static void assertFile(FileSystemService fs, String path, int expectedBlocks) {
        FileNode file = fs.getFileByPath(path);

        if (file == null) {
            throw new RuntimeException("Error: no se creó el archivo " + path + ".");
        }

        if (file.getSizeInBlocks() != expectedBlocks) {
            throw new RuntimeException("Error: " + path + " debería tener " + expectedBlocks + " bloques.");
        }

        if (file.getFirstBlockId() < 0) {
            throw new RuntimeException("Error: " + path + " debería tener bloques físicos asignados.");
        }

        int[] chain = fs.getFileBlockChain(path);

        if (chain.length != expectedBlocks) {
            throw new RuntimeException("Error: la cadena de " + path + " debería tener longitud " + expectedBlocks + ".");
        }
    }
}