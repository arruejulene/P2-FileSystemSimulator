package proyecto2so.core;

public class AllocationEntry {
    private final String fileName;
    private final int blockCount;
    private final int firstBlockId;

    public AllocationEntry(String fileName, int blockCount, int firstBlockId) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del archivo no puede ser nulo o vacío.");
        }

        if (blockCount <= 0) {
            throw new IllegalArgumentException("La cantidad de bloques debe ser mayor que 0.");
        }

        this.fileName = fileName;
        this.blockCount = blockCount;
        this.firstBlockId = firstBlockId;
    }

    public String getFileName() {
        return fileName;
    }

    public int getBlockCount() {
        return blockCount;
    }

    public int getFirstBlockId() {
        return firstBlockId;
    }
}