package proyecto2so.core;

public class FileSystemService {
    private DirectoryNode root;
    private VirtualDisk disk;
    private FileNode[] fileIndex;
    private int fileCount;

    public FileSystemService() {
        this.root = null;
        this.disk = null;
        this.fileIndex = new FileNode[10];
        this.fileCount = 0;
    }

    public void initialize(int totalBlocks) {
        this.root = new DirectoryNode("root", "admin", null);
        this.disk = new VirtualDisk(totalBlocks);
        this.fileIndex = new FileNode[10];
        this.fileCount = 0;
    }

    public DirectoryNode getRoot() {
        return root;
    }

    public VirtualDisk getDisk() {
        return disk;
    }

    public FileNode[] getFileIndex() {
        FileNode[] result = new FileNode[fileCount];

        for (int i = 0; i < fileCount; i++) {
            result[i] = fileIndex[i];
        }

        return result;
    }

    public int getFileCount() {
        return fileCount;
    }
}
