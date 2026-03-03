package proyecto2so.core;

public class Block {
    private final int id;
    private boolean free;
    private String fileName;
    private int nextBlockId;

    public Block(int id) {
        this.id = id;
        this.free = true;
        this.fileName = null;
        this.nextBlockId = -1;
    }

    public int getId() {
        return id;
    }

    public boolean isFree() {
        return free;
    }

    public String getFileName() {
        return fileName;
    }

    public int getNextBlockId() {
        return nextBlockId;
    }

    public void occupy(String fileName, int nextBlockId) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("fileName no puede ser nulo o vacío.");
        }

        this.free = false;
        this.fileName = fileName;
        this.nextBlockId = nextBlockId;
    }

    public void release() {
        this.free = true;
        this.fileName = null;
        this.nextBlockId = -1;
    }

    @Override
    public String toString() {
        return "Block{" +
                "id=" + id +
                ", free=" + free +
                ", fileName='" + fileName + '\'' +
                ", nextBlockId=" + nextBlockId +
                '}';
    }
}