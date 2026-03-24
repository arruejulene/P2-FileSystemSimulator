package proyecto2so.core;

public class JsonScenario {
    private final String testId;
    private final int initialHead;
    private final Request[] requests;
    private final SystemFileSeed[] systemFiles;

    public JsonScenario(String testId, int initialHead, Request[] requests, SystemFileSeed[] systemFiles) {
        this.testId = testId;
        this.initialHead = initialHead;
        this.requests = requests;
        this.systemFiles = systemFiles;
    }

    public String getTestId() { return testId; }
    public int getInitialHead() { return initialHead; }
    public Request[] getRequests() { return requests; }
    public SystemFileSeed[] getSystemFiles() { return systemFiles; }
}