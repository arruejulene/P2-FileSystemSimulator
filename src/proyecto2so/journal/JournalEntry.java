package proyecto2so.journal;

public class JournalEntry {
    private final long id;
    private final long createdAtMillis;
    private final JournalOperation operation;
    private JournalStatus status;
    private final String primaryPath;
    private final String secondaryPath;
    private final DeletedFileSnapshot deletedFileSnapshot;

    public JournalEntry(
            long id,
            JournalOperation operation,
            String primaryPath,
            String secondaryPath,
            DeletedFileSnapshot deletedFileSnapshot
    ) {
        this(
                id,
                System.currentTimeMillis(),
                operation,
                JournalStatus.PENDING,
                primaryPath,
                secondaryPath,
                deletedFileSnapshot
        );
    }

    public JournalEntry(
            long id,
            long createdAtMillis,
            JournalOperation operation,
            JournalStatus status,
            String primaryPath,
            String secondaryPath,
            DeletedFileSnapshot deletedFileSnapshot
    ) {
        if (id <= 0) {
            throw new IllegalArgumentException("id debe ser > 0.");
        }
        if (createdAtMillis <= 0) {
            throw new IllegalArgumentException("createdAtMillis debe ser > 0.");
        }
        if (operation == null) {
            throw new IllegalArgumentException("operation no puede ser null.");
        }
        if (status == null) {
            throw new IllegalArgumentException("status no puede ser null.");
        }
        if (primaryPath == null || primaryPath.trim().isEmpty()) {
            throw new IllegalArgumentException("primaryPath no puede ser nulo o vacío.");
        }

        this.id = id;
        this.createdAtMillis = createdAtMillis;
        this.operation = operation;
        this.status = status;
        this.primaryPath = primaryPath;
        this.secondaryPath = secondaryPath;
        this.deletedFileSnapshot = deletedFileSnapshot;
    }

    public long getId() {
        return id;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public JournalOperation getOperation() {
        return operation;
    }

    public JournalStatus getStatus() {
        return status;
    }

    public void confirm() {
        this.status = JournalStatus.CONFIRMED;
    }

    public String getPrimaryPath() {
        return primaryPath;
    }

    public String getSecondaryPath() {
        return secondaryPath;
    }

    public DeletedFileSnapshot getDeletedFileSnapshot() {
        return deletedFileSnapshot;
    }
}
