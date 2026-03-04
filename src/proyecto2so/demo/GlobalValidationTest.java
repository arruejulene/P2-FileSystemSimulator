package proyecto2so.demo;

import proyecto2so.core.FileSystemService;

public class GlobalValidationTest {
    public static void main(String[] args) throws Exception {
        System.out.println("ENTRANDO A GlobalValidationTest");

        FileSystemService fs = new FileSystemService();
        fs.initialize(20);

        fs.createDirectory("/", "docs", "iraia");
        fs.createFile("/docs", "ok.txt", "iraia", 3);

        int initialFiles = fs.getFileCount();
        int initialFreeBlocks = fs.getDisk().countFreeBlocks();
        int initialRootSubdirs = fs.getRoot().getSubdirectoryCount();
        int initialDocsSubdirs = fs.getRoot().getSubdirectories()[0].getSubdirectoryCount();

        expectFailureAndNoStateChange(
            fs,
            initialFiles,
            initialFreeBlocks,
            initialRootSubdirs,
            initialDocsSubdirs,
            new CheckedAction() {
                public void run() {
                    fs.createFile("/docs", "", "iraia", 1);
                }
            },
            "Validación de nombre vacío correcta."
        );

        expectFailureAndNoStateChange(
            fs,
            initialFiles,
            initialFreeBlocks,
            initialRootSubdirs,
            initialDocsSubdirs,
            new CheckedAction() {
                public void run() {
                    fs.createDirectory("/ruta/que/no/existe", "tmp", "iraia");
                }
            },
            "Validación de ruta inexistente correcta."
        );

        expectFailureAndNoStateChange(
            fs,
            initialFiles,
            initialFreeBlocks,
            initialRootSubdirs,
            initialDocsSubdirs,
            new CheckedAction() {
                public void run() {
                    fs.createFile("/docs", "bad.txt", "iraia", -5);
                }
            },
            "Validación de tamaño inválido correcta."
        );

        expectFailureAndNoStateChange(
            fs,
            initialFiles,
            initialFreeBlocks,
            initialRootSubdirs,
            initialDocsSubdirs,
            new CheckedAction() {
                public void run() {
                    fs.deleteFile("/docs/no_existe.txt");
                }
            },
            "Validación de borrado inexistente correcta."
        );

        expectFailureAndNoStateChange(
            fs,
            initialFiles,
            initialFreeBlocks,
            initialRootSubdirs,
            initialDocsSubdirs,
            new CheckedAction() {
                public void run() {
                    fs.createFile("/docs", "ok.txt", "iraia", 2);
                }
            },
            "Validación de duplicado correcta."
        );

        expectFailureAndNoStateChange(
            fs,
            initialFiles,
            initialFreeBlocks,
            initialRootSubdirs,
            initialDocsSubdirs,
            new CheckedAction() {
                public void run() {
                    fs.createFile("/docs", "huge.bin", "iraia", 100);
                }
            },
            "Validación de falta de espacio correcta."
        );

        System.out.println("Árbol intacto tras todos los fallos.");
        System.out.println("Disco intacto tras todos los fallos.");
        System.out.println("Paso 19 completado correctamente.");
    }

    private static void expectFailureAndNoStateChange(
        FileSystemService fs,
        int expectedFiles,
        int expectedFreeBlocks,
        int expectedRootSubdirs,
        int expectedDocsSubdirs,
        CheckedAction action,
        String successMessage
    ) {
        boolean failed = false;

        try {
            action.run();
        } catch (RuntimeException ex) {
            failed = true;
        } catch (Exception ex) {
            failed = true;
        }

        if (!failed) {
            throw new RuntimeException("Error: la operación inválida debería fallar.");
        }

        if (fs.getFileCount() != expectedFiles) {
            throw new RuntimeException("Error: cambió la cantidad de archivos tras un fallo.");
        }

        if (fs.getDisk().countFreeBlocks() != expectedFreeBlocks) {
            throw new RuntimeException("Error: cambió la cantidad de bloques libres tras un fallo.");
        }

        if (fs.getRoot().getSubdirectoryCount() != expectedRootSubdirs) {
            throw new RuntimeException("Error: cambió la cantidad de directorios raíz tras un fallo.");
        }

        if (fs.getRoot().getSubdirectories()[0].getSubdirectoryCount() != expectedDocsSubdirs) {
            throw new RuntimeException("Error: cambió la estructura interna de /docs tras un fallo.");
        }

        if (fs.getFileByPath("/docs/ok.txt") == null) {
            throw new RuntimeException("Error: se dañó el archivo válido existente tras un fallo.");
        }

        System.out.println(successMessage);
    }

    private interface CheckedAction {
        void run() throws Exception;
    }
}
