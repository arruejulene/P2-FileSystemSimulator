package proyecto2so.core;

public class FileNode extends FSNode {
    private int sizeInBlocks;
    private int firstBlockId;

    public FileNode(String name, String owner, DirectoryNode parent, int sizeInBlocks, int firstBlockId) {
        super(name, owner, parent);

        if (sizeInBlocks <= 0) {
            throw new IllegalArgumentException("El tamaño en bloques debe ser mayor que 0.");
        }

        this.sizeInBlocks = sizeInBlocks;
        this.firstBlockId = firstBlockId;
    }

    public int getSizeInBlocks() {
        return sizeInBlocks;
    }

    public int getFirstBlockId() {
        return firstBlockId;
    }

    public void setSizeInBlocks(int sizeInBlocks) {
        if (sizeInBlocks <= 0) {
            throw new IllegalArgumentException("El tamaño en bloques debe ser mayor que 0.");
        }

        this.sizeInBlocks = sizeInBlocks;
    }

    public void setFirstBlockId(int firstBlockId) {
        this.firstBlockId = firstBlockId;
    }
}
