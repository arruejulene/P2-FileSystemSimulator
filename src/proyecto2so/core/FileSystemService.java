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

    public void createDirectory(String parentPath, String directoryName, String owner) {
        if (root == null || disk == null) {
            throw new IllegalStateException("El sistema de archivos no ha sido inicializado.");
        }

        DirectoryNode parentDirectory = resolveDirectory(parentPath);

        if (parentDirectory == null) {
            throw new IllegalStateException("La ruta padre no existe.");
        }

        DirectoryNode newDirectory = new DirectoryNode(directoryName, owner, parentDirectory);
        parentDirectory.addSubdirectory(newDirectory);
    }

    public void createFile(String parentPath, String fileName, String owner, int sizeInBlocks) {
    if (root == null || disk == null) {
        throw new IllegalStateException("El sistema de archivos no ha sido inicializado.");
    }

    if (fileName == null || fileName.trim().isEmpty()) {
        throw new IllegalArgumentException("El nombre del archivo no puede ser nulo o vacío.");
    }

    if (owner == null || owner.trim().isEmpty()) {
        throw new IllegalArgumentException("El owner no puede ser nulo o vacío.");
    }

    if (sizeInBlocks <= 0) {
        throw new IllegalArgumentException("El tamaño en bloques debe ser mayor que 0.");
    }

    DirectoryNode parentDirectory = resolveDirectory(parentPath);

    if (parentDirectory == null) {
        throw new IllegalStateException("La ruta padre no existe.");
    }

    if (parentDirectory.containsName(fileName)) {
        throw new IllegalStateException("Ya existe un nodo con ese nombre en este directorio.");
    }

    int[] allocatedBlocks = disk.findFreeBlocks(sizeInBlocks);

    try {
        for (int i = 0; i < allocatedBlocks.length; i++) {
            int nextBlockId = (i == allocatedBlocks.length - 1) ? -1 : allocatedBlocks[i + 1];
            disk.occupyBlock(allocatedBlocks[i], fileName, nextBlockId);
        }

        FileNode newFile = new FileNode(fileName, owner, parentDirectory, sizeInBlocks, allocatedBlocks[0]);
        parentDirectory.addFile(newFile);
        addToFileIndex(newFile);

    } catch (Exception ex) {
        for (int i = 0; i < allocatedBlocks.length; i++) {
            Block block = disk.getBlockById(allocatedBlocks[i]);
            if (!block.isFree() && fileName.equals(block.getFileName())) {
                block.release();
            }
        }
        throw ex;
    }
}

    public FileNode getFileByPath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("La ruta del archivo no puede ser nula o vacía.");
        }

        if (!filePath.startsWith("/")) {
            throw new IllegalArgumentException("La ruta debe empezar con '/'.");
        }

        if ("/".equals(filePath)) {
            return null;
        }

        int lastSlash = filePath.lastIndexOf('/');

        String parentPath;
        String fileName;

        if (lastSlash == 0) {
            parentPath = "/";
            fileName = filePath.substring(1);
        } else {
            parentPath = filePath.substring(0, lastSlash);
            fileName = filePath.substring(lastSlash + 1);
        }

        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("La ruta del archivo no es válida.");
        }

        DirectoryNode parentDirectory = resolveDirectory(parentPath);

        if (parentDirectory == null) {
            return null;
        }

        FileNode[] files = parentDirectory.getFiles();

        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().equals(fileName)) {
                return files[i];
            }
        }

        return null;
    }

    public int[] getFileBlockChain(String filePath) {
        FileNode file = getFileByPath(filePath);

        if (file == null) {
            throw new IllegalStateException("El archivo no existe.");
        }

        if (file.getFirstBlockId() < 0) {
            throw new IllegalStateException("El archivo no tiene bloques asignados.");
        }

        return disk.traverseChain(file.getFirstBlockId());
    }

    public void deleteFile(String filePath) {
        if (root == null || disk == null) {
            throw new IllegalStateException("El sistema de archivos no ha sido inicializado.");
        }

        FileNode file = getFileByPath(filePath);

        if (file == null) {
            throw new IllegalStateException("El archivo no existe.");
        }

        if (file.getFirstBlockId() >= 0) {
            disk.freeChain(file.getFirstBlockId());
        }

        DirectoryNode parent = file.getParent();

        if (parent == null) {
            throw new IllegalStateException("El archivo no tiene directorio padre.");
        }

        parent.removeFileByName(file.getName());
        removeFromFileIndex(file);
    }

    public void deleteDirectoryRecursive(String directoryPath) {
        if (root == null || disk == null) {
            throw new IllegalStateException("El sistema de archivos no ha sido inicializado.");
        }

        if ("/".equals(directoryPath)) {
            throw new IllegalStateException("No se puede eliminar el directorio raíz.");
        }

        DirectoryNode target = resolveDirectory(directoryPath);

        if (target == null) {
            throw new IllegalStateException("El directorio no existe.");
        }

        deleteDirectoryContents(target);

        DirectoryNode parent = target.getParent();

        if (parent == null) {
            throw new IllegalStateException("El directorio no tiene padre.");
        }

        parent.removeSubdirectoryByName(target.getName());
    }

    public void renameFile(String filePath, String newName) {
        if (root == null || disk == null) {
            throw new IllegalStateException("El sistema de archivos no ha sido inicializado.");
        }

        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("El nuevo nombre no puede ser nulo o vacío.");
        }

        FileNode file = getFileByPath(filePath);

        if (file == null) {
            throw new IllegalStateException("El archivo no existe.");
        }

        DirectoryNode parent = file.getParent();

        if (parent == null) {
            throw new IllegalStateException("El archivo no tiene directorio padre.");
        }

        if (!file.getName().equals(newName) && parent.containsName(newName)) {
            throw new IllegalStateException("Ya existe un nodo con ese nombre en el directorio padre.");
        }

        file.setName(newName);
    }

    public void renameDirectory(String directoryPath, String newName) {
        if (root == null || disk == null) {
            throw new IllegalStateException("El sistema de archivos no ha sido inicializado.");
        }

        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("El nuevo nombre no puede ser nulo o vacío.");
        }

        if ("/".equals(directoryPath)) {
            throw new IllegalStateException("No se puede renombrar la raíz.");
        }

        DirectoryNode directory = resolveDirectory(directoryPath);

        if (directory == null) {
            throw new IllegalStateException("El directorio no existe.");
        }

        DirectoryNode parent = directory.getParent();

        if (parent == null) {
            throw new IllegalStateException("El directorio no tiene padre.");
        }

        if (!directory.getName().equals(newName) && parent.containsName(newName)) {
            throw new IllegalStateException("Ya existe un nodo con ese nombre en el directorio padre.");
        }

        directory.setName(newName);
    }

    public AllocationEntry[] getAllocationTable() {
        AllocationEntry[] table = new AllocationEntry[fileCount];

        for (int i = 0; i < fileCount; i++) {
            FileNode file = fileIndex[i];
            table[i] = new AllocationEntry(
                    file.getName(),
                    file.getSizeInBlocks(),
                    file.getFirstBlockId()
            );
        }

        return table;
    }

    private void deleteDirectoryContents(DirectoryNode directory) {
        FileNode[] files = directory.getFiles();

        while (files.length > 0) {
            deleteFile(files[0].getPath());
            files = directory.getFiles();
        }

        DirectoryNode[] subdirs = directory.getSubdirectories();

        while (subdirs.length > 0) {
            deleteDirectoryRecursive(subdirs[0].getPath());
            subdirs = directory.getSubdirectories();
        }
    }

    private void removeFromFileIndex(FileNode file) {
        int foundIndex = -1;

        for (int i = 0; i < fileCount; i++) {
            if (fileIndex[i] == file) {
                foundIndex = i;
                break;
            }
        }

        if (foundIndex == -1) {
            throw new IllegalStateException("El archivo no existe en el índice global.");
        }

        for (int i = foundIndex; i < fileCount - 1; i++) {
            fileIndex[i] = fileIndex[i + 1];
        }

        fileIndex[fileCount - 1] = null;
        fileCount--;
    }

    private void addToFileIndex(FileNode file) {
        if (fileCount >= fileIndex.length) {
            FileNode[] newArray = new FileNode[fileIndex.length * 2];

            for (int i = 0; i < fileIndex.length; i++) {
                newArray[i] = fileIndex[i];
            }

            fileIndex = newArray;
        }

        fileIndex[fileCount] = file;
        fileCount++;
    }

    private DirectoryNode resolveDirectory(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("La ruta no puede ser nula o vacía.");
        }

        if (root == null) {
            return null;
        }

        if ("/".equals(path)) {
            return root;
        }

        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("La ruta debe empezar con '/'.");
        }

        String[] parts = path.split("/");
        DirectoryNode current = root;

        for (int i = 1; i < parts.length; i++) {
            if (parts[i] == null || parts[i].isEmpty()) {
                continue;
            }

            DirectoryNode next = findSubdirectoryByName(current, parts[i]);

            if (next == null) {
                return null;
            }

            current = next;
        }

        return current;
    }

    private DirectoryNode findSubdirectoryByName(DirectoryNode parent, String name) {
        DirectoryNode[] subdirs = parent.getSubdirectories();

        for (int i = 0; i < subdirs.length; i++) {
            if (subdirs[i].getName().equals(name)) {
                return subdirs[i];
            }
        }

        return null;
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
    
    private SystemFileSeed findSystemFileByPos(SystemFileSeed[] seeds, int pos) {
    for (int i = 0; i < seeds.length; i++) {
        if (seeds[i].getPos() == pos) return seeds[i];
    }
    return null;
    
  

    
    
}
    
}