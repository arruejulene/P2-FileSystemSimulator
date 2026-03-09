package proyecto2so.journal;

public class DeletedDirectorySnapshot {
    private final String fullPath;
    private final String parentPath;
    private final String directoryName;
    private final String owner;
    private final DeletedFileSnapshot[] files;
    private final DeletedDirectorySnapshot[] subdirectories;

    public DeletedDirectorySnapshot(
            String fullPath,
            String parentPath,
            String directoryName,
            String owner,
            DeletedFileSnapshot[] files,
            DeletedDirectorySnapshot[] subdirectories
    ) {
        if (fullPath == null || fullPath.trim().isEmpty()) {
            throw new IllegalArgumentException("fullPath no puede ser nulo o vacío.");
        }
        if (parentPath == null || parentPath.trim().isEmpty()) {
            throw new IllegalArgumentException("parentPath no puede ser nulo o vacío.");
        }
        if (directoryName == null || directoryName.trim().isEmpty()) {
            throw new IllegalArgumentException("directoryName no puede ser nulo o vacío.");
        }
        if (owner == null || owner.trim().isEmpty()) {
            throw new IllegalArgumentException("owner no puede ser nulo o vacío.");
        }

        this.fullPath = fullPath;
        this.parentPath = parentPath;
        this.directoryName = directoryName;
        this.owner = owner;
        this.files = files == null ? new DeletedFileSnapshot[0] : copyFiles(files);
        this.subdirectories = subdirectories == null ? new DeletedDirectorySnapshot[0] : copyDirs(subdirectories);
    }

    public String getFullPath() {
        return fullPath;
    }

    public String getParentPath() {
        return parentPath;
    }

    public String getDirectoryName() {
        return directoryName;
    }

    public String getOwner() {
        return owner;
    }

    public DeletedFileSnapshot[] getFiles() {
        return copyFiles(files);
    }

    public DeletedDirectorySnapshot[] getSubdirectories() {
        return copyDirs(subdirectories);
    }

    private DeletedFileSnapshot[] copyFiles(DeletedFileSnapshot[] source) {
        DeletedFileSnapshot[] dest = new DeletedFileSnapshot[source.length];
        for (int i = 0; i < source.length; i++) {
            dest[i] = source[i];
        }
        return dest;
    }

    private DeletedDirectorySnapshot[] copyDirs(DeletedDirectorySnapshot[] source) {
        DeletedDirectorySnapshot[] dest = new DeletedDirectorySnapshot[source.length];
        for (int i = 0; i < source.length; i++) {
            dest[i] = source[i];
        }
        return dest;
    }
}
