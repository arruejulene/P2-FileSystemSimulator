package proyecto2so.json;

public class TestScenario {
    private final String testId;
    private final int initialHead;
    private final DiskRequest[] requests;
    private final SystemFileSeed[] systemFiles;

    public TestScenario(String testId, int initialHead, DiskRequest[] requests, SystemFileSeed[] systemFiles) {
        if (testId == null || testId.trim().isEmpty()) {
            throw new IllegalArgumentException("testId no puede ser nulo o vacío.");
        }

        if (initialHead < 0) {
            throw new IllegalArgumentException("initialHead no puede ser negativo.");
        }

        if (requests == null) {
            throw new IllegalArgumentException("requests no puede ser null.");
        }

        if (systemFiles == null) {
            throw new IllegalArgumentException("systemFiles no puede ser null.");
        }

        this.testId = testId;
        this.initialHead = initialHead;
        this.requests = requests;
        this.systemFiles = systemFiles;
    }

    public String getTestId() {
        return testId;
    }

    public int getInitialHead() {
        return initialHead;
    }

    public DiskRequest[] getRequests() {
        return requests;
    }

    public SystemFileSeed[] getSystemFiles() {
        return systemFiles;
    }
}