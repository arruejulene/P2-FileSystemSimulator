package proyecto2so.core;

public class DirectoryNode extends FSNode {
    private DirectoryNode[] subdirectories;
    private FileNode[] files;
    private int subdirectoryCount;
    private int fileCount;

    public DirectoryNode(String name, String owner, DirectoryNode parent) {
        super(name, owner, parent);
        this.subdirectories = new DirectoryNode[2];
        this.files = new FileNode[2];
        this.subdirectoryCount = 0;
        this.fileCount = 0;
    }

    public void addSubdirectory(DirectoryNode directory) {
        if (directory == null) {
            throw new IllegalArgumentException("El subdirectorio no puede ser nulo.");
        }

        ensureSubdirectoryCapacity();
        subdirectories[subdirectoryCount] = directory;
        subdirectoryCount++;
        directory.setParent(this);
    }

    public void addFile(FileNode file) {
        if (file == null) {
            throw new IllegalArgumentException("El archivo no puede ser nulo.");
        }

        ensureFileCapacity();
        files[fileCount] = file;
        fileCount++;
        file.setParent(this);
    }

    public DirectoryNode[] getSubdirectories() {
        DirectoryNode[] result = new DirectoryNode[subdirectoryCount];

        for (int i = 0; i < subdirectoryCount; i++) {
            result[i] = subdirectories[i];
        }

        return result;
    }

    public FileNode[] getFiles() {
        FileNode[] result = new FileNode[fileCount];

        for (int i = 0; i < fileCount; i++) {
            result[i] = files[i];
        }

        return result;
    }

    public int getSubdirectoryCount() {
        return subdirectoryCount;
    }

    public int getFileCount() {
        return fileCount;
    }

    private void ensureSubdirectoryCapacity() {
        if (subdirectoryCount < subdirectories.length) {
            return;
        }

        DirectoryNode[] newArray = new DirectoryNode[subdirectories.length * 2];

        for (int i = 0; i < subdirectories.length; i++) {
            newArray[i] = subdirectories[i];
        }

        subdirectories = newArray;
    }

    private void ensureFileCapacity() {
        if (fileCount < files.length) {
            return;
        }

        FileNode[] newArray = new FileNode[files.length * 2];

        for (int i = 0; i < files.length; i++) {
            newArray[i] = files[i];
        }

        files = newArray;
    }
}