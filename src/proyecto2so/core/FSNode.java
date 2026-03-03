package proyecto2so.core;

public abstract class FSNode {
    private String name;
    private String owner;
    private DirectoryNode parent;

    public FSNode(String name, String owner, DirectoryNode parent) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede ser nulo o vacío.");
        }

        if (owner == null || owner.trim().isEmpty()) {
            throw new IllegalArgumentException("El owner no puede ser nulo o vacío.");
        }

        this.name = name;
        this.owner = owner;
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    public DirectoryNode getParent() {
        return parent;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede ser nulo o vacío.");
        }

        this.name = name;
    }

    public void setParent(DirectoryNode parent) {
        this.parent = parent;
    }

    public String getPath() {
        if (parent == null) {
            return "/";
        }

        String parentPath = parent.getPath();

        if ("/".equals(parentPath)) {
            return parentPath + name;
        }

        return parentPath + "/" + name;
    }
}