package proyecto2so.core;

public enum RequestOp {
    READ,
    UPDATE,
    DELETE,
    CREATE_FILE,
    CREATE_DIRECTORY,
    DELETE_NODE;

    public static RequestOp fromString(String s) {
        if (s == null) throw new IllegalArgumentException("op is null");
        s = s.trim().toUpperCase();
        if ("READ".equals(s)) return READ;
        if ("UPDATE".equals(s)) return UPDATE;
        if ("DELETE".equals(s)) return DELETE;
        if ("CREATE_FILE".equals(s)) return CREATE_FILE;
        if ("CREATE_DIRECTORY".equals(s)) return CREATE_DIRECTORY;
        if ("DELETE_NODE".equals(s)) return DELETE_NODE;
        throw new IllegalArgumentException("Unknown op: " + s);
    }
}
