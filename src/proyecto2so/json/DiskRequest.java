package proyecto2so.json;

public class DiskRequest {
    private final int pos;
    private final String op;

    public DiskRequest(int pos, String op) {
        if (pos < 0) {
            throw new IllegalArgumentException("La posición no puede ser negativa.");
        }

        if (op == null || op.trim().isEmpty()) {
            throw new IllegalArgumentException("La operación no puede ser nula o vacía.");
        }

        this.pos = pos;
        this.op = op;
    }

    public int getPos() {
        return pos;
    }

    public String getOp() {
        return op;
    }
}