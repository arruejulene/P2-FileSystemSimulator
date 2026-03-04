package proyecto2so.json;

import proyecto2so.core.FileSystemService;

public class TestScenarioApplier {
    public void applyFromJson(String filePath, FileSystemService fs) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("La ruta del JSON no puede ser nula o vacía.");
        }

        TestScenarioLoader loader = new TestScenarioLoader();
        TestScenario scenario = loader.load(filePath);
        apply(scenario, fs);
    }

    public void apply(TestScenario scenario, FileSystemService fs) {
        if (scenario == null) {
            throw new IllegalArgumentException("El escenario no puede ser null.");
        }

        if (fs == null) {
            throw new IllegalArgumentException("FileSystemService no puede ser null.");
        }

        if (fs.getRoot() == null || fs.getDisk() == null) {
            throw new IllegalStateException("FileSystemService debe estar inicializado antes de aplicar el escenario.");
        }

        ensureSystemDirectory(fs);

        SystemFileSeed[] systemFiles = scenario.getSystemFiles();

        for (int i = 0; i < systemFiles.length; i++) {
            fs.createFile("/system", systemFiles[i].getName(), "system", systemFiles[i].getBlocks());
        }
    }

    public int calculateRequiredBlocks(TestScenario scenario) {
        if (scenario == null) {
            throw new IllegalArgumentException("El escenario no puede ser null.");
        }

        int total = 0;
        SystemFileSeed[] systemFiles = scenario.getSystemFiles();

        for (int i = 0; i < systemFiles.length; i++) {
            total += systemFiles[i].getBlocks();
        }

        return total;
    }

    private void ensureSystemDirectory(FileSystemService fs) {
        try {
            fs.createDirectory("/", "system", "system");
        } catch (IllegalStateException e) {
            if (fs.getRoot() == null || fs.getRoot().getSubdirectoryCount() == 0) {
                throw e;
            }

            boolean exists = false;
            for (int i = 0; i < fs.getRoot().getSubdirectories().length; i++) {
                if ("system".equals(fs.getRoot().getSubdirectories()[i].getName())) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                throw e;
            }
        }
    }
}