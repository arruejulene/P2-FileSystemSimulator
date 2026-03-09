package proyecto2so.core;

import proyecto2so.journal.DeletedFileSnapshot;
import proyecto2so.journal.JournalEntry;
import proyecto2so.journal.JournalManager;
import proyecto2so.journal.JournalOperation;
import proyecto2so.journal.JournalStatus;

public class FileSystemService {
    private DirectoryNode root;
    private VirtualDisk disk;
    private FileNode[] fileIndex;
    private int fileCount;
    private final JournalManager journalManager;
    private boolean journalingEnabled;

    public FileSystemService() {
        this.root = null;
        this.disk = null;
        this.fileIndex = new FileNode[10];
        this.fileCount = 0;
        this.journalManager = new JournalManager();
        this.journalingEnabled = true;
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
        ensureInitialized();
        String fullPath = childPath(parentPath, fileName);

        if (!journalingEnabled) {
            createFileInternal(parentPath, fileName, owner, sizeInBlocks);
            return;
        }

        JournalEntry entry = journalManager.beginCreateFile(fullPath);

        createFileInternal(parentPath, fileName, owner, sizeInBlocks);
        journalManager.failIfCrashRequested();
        journalManager.confirm(entry.getId());
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
        ensureInitialized();

        FileNode file = getFileByPath(filePath);

        if (file == null) {
            throw new IllegalStateException("El archivo no existe.");
        }

        if (!journalingEnabled) {
            deleteFileInternal(file);
            return;
        }

        DeletedFileSnapshot snapshot = buildDeletedFileSnapshot(file, filePath);
        JournalEntry entry = journalManager.beginDeleteFile(filePath, snapshot);

        deleteFileInternal(file);
        journalManager.failIfCrashRequested();
        journalManager.confirm(entry.getId());
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
        ensureInitialized();

        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("El nuevo nombre no puede ser nulo o vacío.");
        }

        String parentPath = parentPathFromFilePath(filePath);
        String newPath = childPath(parentPath, newName);

        if (!journalingEnabled) {
            renameFileInternal(filePath, newName);
            return;
        }

        JournalEntry entry = journalManager.beginRenameFile(filePath, newPath);
        renameFileInternal(filePath, newName);
        journalManager.failIfCrashRequested();
        journalManager.confirm(entry.getId());
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
            deleteFileInternal(files[0]);
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

    public JournalEntry[] getJournalEntries() {
        return journalManager.getEntries();
    }

    public void replaceJournalEntries(JournalEntry[] entries) {
        journalManager.replaceEntries(entries);
    }

    public void simulateCrashAfterNextCriticalOperation() {
        journalManager.simulateCrashAfterNextApply();
    }

    public int recoverPendingJournalEntries() {
        JournalEntry[] entries = journalManager.getEntries();
        int undone = 0;

        boolean previousJournalingEnabled = journalingEnabled;
        journalingEnabled = false;

        try {
            for (int i = entries.length - 1; i >= 0; i--) {
                JournalEntry entry = entries[i];

                if (entry.getStatus() != JournalStatus.PENDING) {
                    continue;
                }

                undoPendingEntry(entry);
                journalManager.confirm(entry.getId());
                undone++;
            }
        } finally {
            journalingEnabled = previousJournalingEnabled;
        }

        return undone;
    }

    private void undoPendingEntry(JournalEntry entry) {
        if (entry.getOperation() == JournalOperation.CREATE_FILE) {
            FileNode created = getFileByPath(entry.getPrimaryPath());
            if (created != null) {
                deleteFileInternal(created);
            }
            return;
        }

        if (entry.getOperation() == JournalOperation.DELETE_FILE) {
            if (getFileByPath(entry.getPrimaryPath()) == null) {
                restoreDeletedFile(entry.getDeletedFileSnapshot());
            }
            return;
        }

        if (entry.getOperation() == JournalOperation.RENAME_FILE) {
            String oldPath = entry.getPrimaryPath();
            String newPath = entry.getSecondaryPath();

            FileNode oldNode = getFileByPath(oldPath);
            if (oldNode != null) {
                return;
            }

            FileNode renamedNode = getFileByPath(newPath);
            if (renamedNode == null) {
                return;
            }

            renameFileInternal(newPath, fileNameFromPath(oldPath));
        }
    }

    private void restoreDeletedFile(DeletedFileSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalStateException("No hay snapshot para restaurar archivo eliminado.");
        }

        if (getFileByPath(snapshot.getFullPath()) != null) {
            return;
        }

        DirectoryNode parent = resolveDirectory(snapshot.getParentPath());

        if (parent == null) {
            throw new IllegalStateException("No existe el directorio padre para restaurar " + snapshot.getFullPath() + ".");
        }

        if (parent.containsName(snapshot.getFileName())) {
            throw new IllegalStateException("Ya existe un nodo con ese nombre al restaurar " + snapshot.getFullPath() + ".");
        }

        int[] chain = snapshot.getChainBlockIds();
        if (chain.length != snapshot.getSizeInBlocks()) {
            throw new IllegalStateException("Snapshot inválido: tamaño y cadena no coinciden para " + snapshot.getFullPath() + ".");
        }

        for (int i = 0; i < chain.length; i++) {
            Block b = disk.getBlockById(chain[i]);
            if (!b.isFree()) {
                throw new IllegalStateException("No se puede restaurar; bloque ocupado: " + chain[i] + ".");
            }
        }

        for (int i = 0; i < chain.length; i++) {
            int next = (i == chain.length - 1) ? -1 : chain[i + 1];
            disk.occupyBlock(chain[i], snapshot.getFileName(), next);
        }

        FileNode restored = new FileNode(
                snapshot.getFileName(),
                snapshot.getOwner(),
                parent,
                snapshot.getSizeInBlocks(),
                chain[0]
        );

        parent.addFile(restored);
        addToFileIndex(restored);
    }

    private DeletedFileSnapshot buildDeletedFileSnapshot(FileNode file, String fullPath) {
        if (file == null) {
            throw new IllegalArgumentException("file no puede ser null.");
        }

        if (fullPath == null || fullPath.trim().isEmpty()) {
            throw new IllegalArgumentException("fullPath no puede ser nulo o vacío.");
        }

        if (file.getFirstBlockId() < 0) {
            throw new IllegalStateException("El archivo no tiene bloques asignados.");
        }

        int[] chain = disk.traverseChain(file.getFirstBlockId());
        String parentPath = parentPathFromFilePath(fullPath);

        return new DeletedFileSnapshot(
                fullPath,
                parentPath,
                file.getName(),
                file.getOwner(),
                file.getSizeInBlocks(),
                chain
        );
    }

    private void deleteFileInternal(FileNode file) {
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

    private void createFileInternal(String parentPath, String fileName, String owner, int sizeInBlocks) {
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

    private void renameFileInternal(String filePath, String newName) {
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

    private String childPath(String parentPath, String childName) {
        if (parentPath == null || parentPath.trim().isEmpty()) {
            throw new IllegalArgumentException("La ruta padre no puede ser nula o vacía.");
        }
        if (childName == null || childName.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del nodo no puede ser nulo o vacío.");
        }

        if ("/".equals(parentPath)) {
            return "/" + childName;
        }

        return parentPath + "/" + childName;
    }

    private String parentPathFromFilePath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("La ruta no puede ser nula o vacía.");
        }
        if (!filePath.startsWith("/")) {
            throw new IllegalArgumentException("La ruta debe empezar con '/'.");
        }

        int lastSlash = filePath.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "/";
        }

        return filePath.substring(0, lastSlash);
    }

    private String fileNameFromPath(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("La ruta no puede ser nula o vacía.");
        }
        if (!filePath.startsWith("/")) {
            throw new IllegalArgumentException("La ruta debe empezar con '/'.");
        }

        int lastSlash = filePath.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == filePath.length() - 1) {
            throw new IllegalArgumentException("La ruta del archivo no es válida.");
        }

        return filePath.substring(lastSlash + 1);
    }

    private void ensureInitialized() {
        if (root == null || disk == null) {
            throw new IllegalStateException("El sistema de archivos no ha sido inicializado.");
        }
    }
}
