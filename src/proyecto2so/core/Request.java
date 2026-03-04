package proyecto2so.core;

public class Request {
    private final int pos;
    private final RequestOp op;

    public Request(int pos, RequestOp op) {
        this.pos = pos;
        this.op = op;
    }

    public int getPos() { return pos; }
    public RequestOp getOp() { return op; }
}
