package proyecto2so.demo;

import proyecto2so.json.DiskRequest;
import proyecto2so.json.SystemFileSeed;
import proyecto2so.json.TestScenario;
import proyecto2so.json.TestScenarioLoader;

public class JsonPackageTest {
    public static void main(String[] args) {
        System.out.println("ENTRANDO A JsonPackageTest");

        String jsonPath = "test_input.json";

        TestScenarioLoader loader = new TestScenarioLoader();
        TestScenario scenario = loader.load(jsonPath);

        if (!"P1".equals(scenario.getTestId())) {
            throw new RuntimeException("Error: test_id debería ser P1.");
        }

        if (scenario.getInitialHead() != 50) {
            throw new RuntimeException("Error: initial_head debería ser 50.");
        }

        DiskRequest[] requests = scenario.getRequests();

        if (requests.length != 8) {
            throw new RuntimeException("Error: deberían existir 8 requests.");
        }

        assertRequest(requests[0], 11, "READ");
        assertRequest(requests[1], 34, "READ");
        assertRequest(requests[2], 62, "UPDATE");
        assertRequest(requests[3], 70, "READ");
        assertRequest(requests[4], 95, "UPDATE");
        assertRequest(requests[5], 119, "DELETE");
        assertRequest(requests[6], 131, "UPDATE");
        assertRequest(requests[7], 180, "READ");

        SystemFileSeed[] files = scenario.getSystemFiles();

        if (files.length != 8) {
            throw new RuntimeException("Error: deberían existir 8 system_files.");
        }

        assertSystemFile(files, 11, "boot_sect.bin", 2);
        assertSystemFile(files, 34, "readme.txt", 1);
        assertSystemFile(files, 62, "script.py", 8);
        assertSystemFile(files, 70, "style.css", 6);
        assertSystemFile(files, 95, "config.sys", 4);
        assertSystemFile(files, 119, "image_01.png", 12);
        assertSystemFile(files, 131, "data_log.csv", 28);
        assertSystemFile(files, 180, "video_clip.mp4", 52);

        System.out.println("Carga de test_id e initial_head correcta.");
        System.out.println("Carga de requests correcta.");
        System.out.println("Carga de system_files correcta.");
        System.out.println("Paso JSON scenario completado correctamente.");
    }

    private static void assertRequest(DiskRequest request, int expectedPos, String expectedOp) {
        if (request.getPos() != expectedPos) {
            throw new RuntimeException("Error: request con pos esperada " + expectedPos + " no coincide.");
        }

        if (!expectedOp.equals(request.getOp())) {
            throw new RuntimeException("Error: request en pos " + expectedPos + " debería tener op " + expectedOp + ".");
        }
    }

    private static void assertSystemFile(SystemFileSeed[] files, int expectedPosition, String expectedName, int expectedBlocks) {
        for (int i = 0; i < files.length; i++) {
            if (files[i].getPosition() == expectedPosition) {
                if (!expectedName.equals(files[i].getName())) {
                    throw new RuntimeException("Error: archivo en posición " + expectedPosition + " debería llamarse " + expectedName + ".");
                }

                if (files[i].getBlocks() != expectedBlocks) {
                    throw new RuntimeException("Error: archivo en posición " + expectedPosition + " debería tener " + expectedBlocks + " bloques.");
                }

                return;
            }
        }

        throw new RuntimeException("Error: no se encontró system_file en posición " + expectedPosition + ".");
    }
}