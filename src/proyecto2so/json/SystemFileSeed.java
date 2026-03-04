package proyecto2so.json;

public class SystemFileSeed {
    private final int position;
    private final String name;
    private final int blocks;

    public SystemFileSeed(int position, String name, int blocks) {
        if (position < 0) {
            throw new IllegalArgumentException("La posición no puede ser negativa.");
        }

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede ser nulo o vacío.");
        }

        if (blocks <= 0) {
            throw new IllegalArgumentException("La cantidad de bloques debe ser mayor que 0.");
        }

        this.position = position;
        this.name = name;
        this.blocks = blocks;
    }

    public int getPosition() {
        return position;
    }

    public String getName() {
        return name;
    }

    public int getBlocks() {
        return blocks;
    }
}