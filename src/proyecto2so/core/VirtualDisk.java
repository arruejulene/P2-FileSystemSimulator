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

    public int[] traverseChain(int firstBlockId) {
        if (firstBlockId < 0 || firstBlockId >= totalBlocks) {
            throw new IllegalArgumentException("ID de bloque inicial fuera de rango.");
        }

        Block firstBlock = getBlockById(firstBlockId);

        if (firstBlock.isFree()) {
            throw new IllegalStateException("No se puede recorrer una cadena desde un bloque libre.");
        }

        int[] visited = new int[totalBlocks];
        boolean[] seen = new boolean[totalBlocks];
        int count = 0;
        int currentId = firstBlockId;

        while (currentId != -1) {
            if (currentId < 0 || currentId >= totalBlocks) {
                throw new IllegalStateException("La cadena contiene una referencia inválida.");
            }

            if (seen[currentId]) {
                throw new IllegalStateException("Se detectó un ciclo en la cadena de bloques.");
            }

            Block currentBlock = getBlockById(currentId);

            if (currentBlock.isFree()) {
                throw new IllegalStateException("La cadena apunta a un bloque libre.");
            }

            seen[currentId] = true;
            visited[count] = currentId;
            count++;

            currentId = currentBlock.getNextBlockId();
        }

        int[] chain = new int[count];

        for (int i = 0; i < count; i++) {
            chain[i] = visited[i];
        }

        return chain;
    }
    
    public void freeChain(int firstBlockId) {
        int[] chain = traverseChain(firstBlockId);

        for (int i = 0; i < chain.length; i++) {
            Block block = getBlockById(chain[i]);
            block.release();
        }
    }
    
    public void occupyChainAt(int startPos, int blocks, String fileName) {
    if (blocks <= 0) {
        throw new IllegalArgumentException("blocks must be > 0");
    }

    if (startPos < 0 || startPos >= totalBlocks) {
        throw new IllegalArgumentException("startPos out of range");
    }

    if (startPos + blocks - 1 >= totalBlocks) {
        throw new IllegalArgumentException("Chain exceeds disk size");
    }

    for (int i = 0; i < blocks; i++) {
        Block b = getBlockById(startPos + i);

        if (!b.isFree()) {
            throw new IllegalStateException("Block " + (startPos + i) + " is not free");
        }
    }

    for (int i = 0; i < blocks; i++) {
        int id = startPos + i;
        int next = (i == blocks - 1) ? -1 : (id + 1);

        occupyBlock(id, fileName, next);
    }
}
    
}