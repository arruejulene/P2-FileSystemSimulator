package proyecto2so.kernel;

public class LockSnapshot {
    private final String resource;
    private final int sharedCount;
    private final int exclusiveOwnerPid;

    public LockSnapshot(String resource, int sharedCount, int exclusiveOwnerPid) {
        this.resource = resource;
        this.sharedCount = sharedCount;
        this.exclusiveOwnerPid = exclusiveOwnerPid;
    }

    public String getResource() {
        return resource;
    }

    public int getSharedCount() {
        return sharedCount;
    }

    public int getExclusiveOwnerPid() {
        return exclusiveOwnerPid;
    }
}
