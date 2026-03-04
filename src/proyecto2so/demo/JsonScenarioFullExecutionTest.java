package proyecto2so.demo;

import proyecto2so.core.FileNode;
import proyecto2so.core.FileSystemService;
import proyecto2so.core.JsonScenario;
import proyecto2so.core.JsonScenarioLoader;
import proyecto2so.core.Request;
import proyecto2so.core.RequestOp;
import proyecto2so.core.SystemFileSeed;
import proyecto2so.json.TestScenarioApplier;

public class JsonScenarioFullExecutionTest {

    public static void main(String[] args) throws Exception {
        System.out.println("ENTRANDO A JsonScenarioFullExecutionTest");

        String path = "test_input.json";

        JsonScenario scenario = JsonScenarioLoader.loadFromFile(path);

        FileSystemService fs = new FileSystemService();
        fs.initialize(300);

        TestScenarioApplier applier = new TestScenarioApplier();
        applier.apply(scenario, fs);

        if (fs.getFileCount() != 8) {
            throw new RuntimeException("Error: deberían existir 8 archivos tras aplicar system_files.");
        }

        executeRequestsWithoutExtraMethods(fs, scenario);

        if (fs.getFileByPath("/system/image_01.png") != null) {
            throw new RuntimeException("Error: DELETE no eliminó image_01.png.");
        }

        if (fs.getFileCount() != 7) {
            throw new RuntimeException("Error: tras DELETE deberían quedar 7 archivos.");
        }

        assertFileExists(fs, "/system/boot_sect.bin");
        assertFileExists(fs, "/system/readme.txt");
        assertFileExists(fs, "/system/upd_script.py");
        assertFileExists(fs, "/system/style.css");
        assertFileExists(fs, "/system/upd_config.sys");
        assertFileExists(fs, "/system/upd_data_log.csv");
        assertFileExists(fs, "/system/video_clip.mp4");

        System.out.println("System_files creados correctamente.");
        System.out.println("Requests ejecutadas correctamente.");
        System.out.println("DELETE validado correctamente.");
        System.out.println("Escenario JSON completo ejecutado correctamente.");
    }

    private static void executeRequestsWithoutExtraMethods(FileSystemService fs, JsonScenario scenario) {
        Request[] requests = scenario.getRequests();
        SystemFileSeed[] seeds = scenario.getSystemFiles();

        for (int i = 0; i < requests.length; i++) {
            Request request = requests[i];
            SystemFileSeed seed = findSeedByPos(seeds, request.getPos());

            if (seed == null) {
                throw new RuntimeException("Error: no existe system_file para pos " + request.getPos() + ".");
            }

            String path = "/system/" + seed.getName();

            if (request.getOp() == RequestOp.READ) {
                FileNode file = fs.getFileByPath(path);

                if (file == null) {
                    throw new RuntimeException("Error: READ no encontró " + path + ".");
                }

                int[] chain = fs.getFileBlockChain(path);

                if (chain.length != file.getSizeInBlocks()) {
                    throw new RuntimeException("Error: READ detectó cadena inconsistente en " + path + ".");
                }

            } else if (request.getOp() == RequestOp.UPDATE) {
                FileNode before = fs.getFileByPath(path);

                if (before == null) {
                    throw new RuntimeException("Error: UPDATE no encontró " + path + ".");
                }

                int oldHead = before.getFirstBlockId();
                int oldSize = before.getSizeInBlocks();

                String newName = "upd_" + seed.getName();
                fs.renameFile(path, newName);

                String newPath = "/system/" + newName;
                FileNode after = fs.getFileByPath(newPath);

                if (after == null) {
                    throw new RuntimeException("Error: UPDATE no renombró correctamente " + path + ".");
                }

                if (after.getFirstBlockId() != oldHead) {
                    throw new RuntimeException("Error: UPDATE cambió bloques en " + newPath + ".");
                }

                if (after.getSizeInBlocks() != oldSize) {
                    throw new RuntimeException("Error: UPDATE cambió tamaño en " + newPath + ".");
                }

                seed = replaceSeedName(seed, newName, seeds);

            } else if (request.getOp() == RequestOp.DELETE) {
                if (fs.getFileByPath(path) == null) {
                    throw new RuntimeException("Error: DELETE no encontró " + path + ".");
                }

                fs.deleteFile(path);

                if (fs.getFileByPath(path) != null) {
                    throw new RuntimeException("Error: DELETE no eliminó " + path + ".");
                }
            }
        }
    }

    private static SystemFileSeed findSeedByPos(SystemFileSeed[] seeds, int pos) {
        for (int i = 0; i < seeds.length; i++) {
            if (seeds[i].getPos() == pos) {
                return seeds[i];
            }
        }
        return null;
    }

    private static SystemFileSeed replaceSeedName(SystemFileSeed original, String newName, SystemFileSeed[] seeds) {
        for (int i = 0; i < seeds.length; i++) {
            if (seeds[i].getPos() == original.getPos()) {
                seeds[i] = new SystemFileSeed(original.getPos(), newName, original.getBlocks());
                return seeds[i];
            }
        }
        return original;
    }

    private static void assertFileExists(FileSystemService fs, String path) {
        if (fs.getFileByPath(path) == null) {
            throw new RuntimeException("Error: debería existir " + path + ".");
        }
    }
}