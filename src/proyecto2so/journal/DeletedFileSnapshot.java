package proyecto2so.journal;

public class DeletedFileSnapshot {
    private final String fullPath;
    private final String parentPath;
    private final String fileName;
    private final String owner;
    private final int sizeInBlocks;
    private final int[] chainBlockIds;

    public DeletedFileSnapshot(
            String fullPath,
            String parentPath,
            String fileName,
            String owner,
            int sizeInBlocks,
            int[] chainBlockIds
    ) {
        if (fullPath == null || fullPath.trim().isEmpty()) {
            throw new IllegalArgumentException("fullPath no puede ser nulo o vacío.");
        }
        if (parentPath == null || parentPath.trim().isEmpty()) {
            throw new IllegalArgumentException("parentPath no puede ser nulo o vacío.");
        }
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("fileName no puede ser nulo o vacío.");
        }
        if (owner == null || owner.trim().isEmpty()) {
            throw new IllegalArgumentException("owner no puede ser nulo o vacío.");
        }
        if (sizeInBlocks <= 0) {
            throw new IllegalArgumentException("sizeInBlocks debe ser > 0.");
        }
        if (chainBlockIds == null || chainBlockIds.length == 0) {
            throw new IllegalArgumentException("chainBlockIds no puede ser vacío.");
        }

        this.fullPath = fullPath;
        this.parentPath = parentPath;
        this.fileName = fileName;
        this.owner = owner;
        this.sizeInBlocks = sizeInBlocks;
        this.chainBlockIds = copy(chainBlockIds);
    }

    public String getFullPath() {
        return fullPath;
    }

    public String getParentPath() {
        return parentPath;
    }

    public String getFileName() {
        return fileName;
    }

    public String getOwner() {
        return owner;
    }

    public int getSizeInBlocks() {
        return sizeInBlocks;
    }

    public int[] getChainBlockIds() {
        return copy(chainBlockIds);
    }

    private int[] copy(int[] source) {
        int[] dest = new int[source.length];
        for (int i = 0; i < source.length; i++) {
            dest[i] = source[i];
        }
        return dest;
    }
}
