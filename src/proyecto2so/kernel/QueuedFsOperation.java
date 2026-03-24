package proyecto2so.kernel;

public class QueuedFsOperation {
    public enum Type {
        CREATE_FILE,
        CREATE_DIRECTORY,
        DELETE_NODE
    }

    private final Type type;
    private final String targetPath;
    private final String parentPath;
    private final String nodeName;
    private final String owner;
    private final int sizeInBlocks;

    private QueuedFsOperation(
            Type type,
            String targetPath,
            String parentPath,
            String nodeName,
            String owner,
            int sizeInBlocks
    ) {
        this.type = type;
        this.targetPath = targetPath;
        this.parentPath = parentPath;
        this.nodeName = nodeName;
        this.owner = owner;
        this.sizeInBlocks = sizeInBlocks;
    }

    public static QueuedFsOperation createFile(
            String targetPath,
            String parentPath,
            String nodeName,
            String owner,
            int sizeInBlocks
    ) {
        return new QueuedFsOperation(Type.CREATE_FILE, targetPath, parentPath, nodeName, owner, sizeInBlocks);
    }

    public static QueuedFsOperation createDirectory(
            String targetPath,
            String parentPath,
            String nodeName,
            String owner
    ) {
        return new QueuedFsOperation(Type.CREATE_DIRECTORY, targetPath, parentPath, nodeName, owner, 0);
    }

    public static QueuedFsOperation deleteNode(String targetPath) {
        return new QueuedFsOperation(Type.DELETE_NODE, targetPath, null, null, null, 0);
    }

    public Type getType() {
        return type;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public String getParentPath() {
        return parentPath;
    }

    public String getNodeName() {
        return nodeName;
    }

    public String getOwner() {
        return owner;
    }

    public int getSizeInBlocks() {
        return sizeInBlocks;
    }
}
