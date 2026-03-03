package proyecto2so.core;

public class VirtualDisk {
    private final Block[] blocks;
    private final int totalBlocks;

    public VirtualDisk(int totalBlocks) {
        if (totalBlocks <= 0) {
            throw new IllegalArgumentException("El número de bloques debe ser mayor que 0.");
        }

        this.totalBlocks = totalBlocks;
        this.blocks = new Block[totalBlocks];

        for (int i = 0; i < totalBlocks; i++) {
            this.blocks[i] = new Block(i);
        }
    }

    public int getTotalBlocks() {
        return totalBlocks;
    }

    public Block[] getBlocks() {
        return blocks;
    }

    public Block getBlockById(int id) {
        if (id < 0 || id >= totalBlocks) {
            throw new IllegalArgumentException("ID de bloque fuera de rango.");
        }

        return blocks[id];
    }

    public int countFreeBlocks() {
        int count = 0;

        for (int i = 0; i < totalBlocks; i++) {
            if (blocks[i].isFree()) {
                count++;
            }
        }

        return count;
    }
}