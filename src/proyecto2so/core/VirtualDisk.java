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
    
    public int[] findFreeBlocks(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("La cantidad solicitada debe ser mayor que 0.");
        }

        if (countFreeBlocks() < amount) {
            throw new IllegalStateException("No hay suficientes bloques libres.");
        }

        int[] freeBlockIds = new int[amount];
        int index = 0;

        for (int i = 0; i < totalBlocks && index < amount; i++) {
            if (blocks[i].isFree()) {
                freeBlockIds[index] = blocks[i].getId();
                index++;
            }
    }

    return freeBlockIds;
    }
    
    public void occupyBlock(int blockId, String fileName, int nextBlockId) {
        Block block = getBlockById(blockId);

        if (!block.isFree()) {
            throw new IllegalStateException("El bloque " + blockId + " ya está ocupado.");
        }

        block.occupy(fileName, nextBlockId);
    }
}