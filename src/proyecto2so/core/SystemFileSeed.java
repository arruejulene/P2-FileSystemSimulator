package proyecto2so.core;

public class SystemFileSeed {
    private final int pos;
    private final String name;
    private final int blocks;

    public SystemFileSeed(int pos, String name, int blocks) {
        this.pos = pos;
        this.name = name;
        this.blocks = blocks;
    }

    public int getPos() { return pos; }
    public String getName() { return name; }
    public int getBlocks() { return blocks; }
}