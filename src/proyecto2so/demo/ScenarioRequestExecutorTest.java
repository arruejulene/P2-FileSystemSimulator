package proyecto2so.demo;

import proyecto2so.core.FileSystemService;
import proyecto2so.core.JsonScenario;
import proyecto2so.core.JsonScenarioLoader;
import proyecto2so.json.ScenarioRequestExecutor;
import proyecto2so.json.TestScenarioApplier;

public class ScenarioRequestExecutorTest {
    public static void main(String[] args) throws Exception {
        System.out.println("ENTRANDO A ScenarioRequestExecutorTest");

        String jsonPath = "test_input.json";

        JsonScenario scenario = JsonScenarioLoader.loadFromFile(jsonPath);

        FileSystemService fs = new FileSystemService();
        fs.initialize(200);

        TestScenarioApplier applier = new TestScenarioApplier();
        applier.apply(scenario, fs);

        if (fs.getFileCount() != 8) {
            throw new RuntimeException("Error: antes de ejecutar requests deberían existir 8 archivos.");
        }

        ScenarioRequestExecutor executor = new ScenarioRequestExecutor();
        executor.execute(fs, scenario);

        if (executor.getCurrentHead() != 180) {
            throw new RuntimeException("Error: el cabezal final debería terminar en 180.");
        }

        if (executor.getTotalHeadMovement() != 208) {
            throw new RuntimeException("Error: el movimiento total del cabezal debería ser 208.");
        }

        if (!executor.wasDeleted(119)) {
            throw new RuntimeException("Error: la request DELETE en 119 debería marcarse como eliminada.");
        }

        if (executor.resolveCurrentPath(119) != null) {
            throw new RuntimeException("Error: el archivo eliminado en 119 ya no debería tener ruta activa.");
        }

        if (fs.getFileByPath("/system/image_01.png") != null) {
            throw new RuntimeException("Error: image_01.png debería haber sido eliminado.");
        }

        if (fs.getFileCount() != 7) {
            throw new RuntimeException("Error: después de DELETE deberían quedar 7 archivos.");
        }

        assertExists(fs, executor.resolveCurrentPath(11));
        assertExists(fs, executor.resolveCurrentPath(34));
        assertExists(fs, executor.resolveCurrentPath(62));
        assertExists(fs, executor.resolveCurrentPath(70));
        assertExists(fs, executor.resolveCurrentPath(95));
        assertExists(fs, executor.resolveCurrentPath(131));
        assertExists(fs, executor.resolveCurrentPath(180));

        if (!"/system/script.py_upd".equals(executor.resolveCurrentPath(62))) {
            throw new RuntimeException("Error: UPDATE en 62 debería renombrar script.py.");
        }

        if (!"/system/config.sys_upd".equals(executor.resolveCurrentPath(95))) {
            throw new RuntimeException("Error: UPDATE en 95 debería renombrar config.sys.");
        }

        if (!"/system/data_log.csv_upd".equals(executor.resolveCurrentPath(131))) {
            throw new RuntimeException("Error: UPDATE en 131 debería renombrar data_log.csv.");
        }

        if (fs.getDisk().countFreeBlocks() != 99) {
            throw new RuntimeException("Error: después de eliminar 12 bloques deberían quedar 99 bloques libres.");
        }

        System.out.println("Requests READ/UPDATE/DELETE ejecutadas correctamente.");
        System.out.println("Uso de initial_head correcto.");
        System.out.println("Movimiento del cabezal correcto.");
        System.out.println("DELETE liberó bloques correctamente.");
        System.out.println("Paso final de JSON completado correctamente.");
    }

    private static void assertExists(FileSystemService fs, String path) {
        if (path == null) {
            throw new RuntimeException("Error: la ruta no debería ser null.");
        }

        if (fs.getFileByPath(path) == null) {
            throw new RuntimeException("Error: debería existir " + path + ".");
        }
    }
}