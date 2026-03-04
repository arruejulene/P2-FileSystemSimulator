package proyecto2so.core;

public enum RequestOp {
    READ,
    UPDATE,
    DELETE;

    public static RequestOp fromString(String s) {
        if (s == null) throw new IllegalArgumentException("op is null");
        s = s.trim().toUpperCase();
        if ("READ".equals(s)) return READ;
        if ("UPDATE".equals(s)) return UPDATE;
        if ("DELETE".equals(s)) return DELETE;
        throw new IllegalArgumentException("Unknown op: " + s);
    }
}