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

        if (containsName(directory.getName())) {
            throw new IllegalStateException("Ya existe un nodo con ese nombre en este directorio.");
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

        if (containsName(file.getName())) {
            throw new IllegalStateException("Ya existe un nodo con ese nombre en este directorio.");
        }

        ensureFileCapacity();
        files[fileCount] = file;
        fileCount++;
        file.setParent(this);
    }

    public void removeFileByName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del archivo no puede ser nulo o vacío.");
        }

        int foundIndex = -1;

        for (int i = 0; i < fileCount; i++) {
            if (files[i].getName().equals(fileName)) {
                foundIndex = i;
                break;
            }
        }

        if (foundIndex == -1) {
            throw new IllegalStateException("El archivo no existe en este directorio.");
        }

        for (int i = foundIndex; i < fileCount - 1; i++) {
            files[i] = files[i + 1];
        }

        files[fileCount - 1] = null;
        fileCount--;
    }

    public void removeSubdirectoryByName(String directoryName) {
        if (directoryName == null || directoryName.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del directorio no puede ser nulo o vacío.");
        }

        int foundIndex = -1;

        for (int i = 0; i < subdirectoryCount; i++) {
            if (subdirectories[i].getName().equals(directoryName)) {
                foundIndex = i;
                break;
            }
        }

        if (foundIndex == -1) {
            throw new IllegalStateException("El subdirectorio no existe en este directorio.");
        }

        for (int i = foundIndex; i < subdirectoryCount - 1; i++) {
            subdirectories[i] = subdirectories[i + 1];
        }

        subdirectories[subdirectoryCount - 1] = null;
        subdirectoryCount--;
    }

    public boolean containsName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede ser nulo o vacío.");
        }

        for (int i = 0; i < subdirectoryCount; i++) {
            if (subdirectories[i].getName().equals(name)) {
                return true;
            }
        }

        for (int i = 0; i < fileCount; i++) {
            if (files[i].getName().equals(name)) {
                return true;
            }
        }

        return false;
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