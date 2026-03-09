package proyecto2so.journal;

import java.util.ArrayList;

public class JournalManager {
    private final ArrayList<JournalEntry> entries;
    private long nextId;
    private boolean crashAfterApply;

    public JournalManager() {
        this.entries = new ArrayList<>();
        this.nextId = 1L;
        this.crashAfterApply = false;
    }

    public JournalEntry beginCreateFile(String fullPath) {
        return append(JournalOperation.CREATE_FILE, fullPath, null, null);
    }

    public JournalEntry beginDeleteFile(String fullPath, DeletedFileSnapshot snapshot) {
        return append(JournalOperation.DELETE_FILE, fullPath, null, snapshot);
    }

    public JournalEntry beginRenameFile(String oldPath, String newPath) {
        return append(JournalOperation.RENAME_FILE, oldPath, newPath, null);
    }

    public void confirm(long entryId) {
        JournalEntry e = findById(entryId);
        if (e == null) {
            throw new IllegalStateException("No existe entrada de journal con id " + entryId + ".");
        }
        e.confirm();
    }

    public JournalEntry[] getEntries() {
        return entries.toArray(new JournalEntry[0]);
    }

    public void replaceEntries(JournalEntry[] loadedEntries) {
        entries.clear();
        nextId = 1L;

        if (loadedEntries == null) {
            return;
        }

        long maxId = 0L;

        for (int i = 0; i < loadedEntries.length; i++) {
            JournalEntry entry = loadedEntries[i];
            if (entry == null) {
                continue;
            }

            entries.add(entry);
            if (entry.getId() > maxId) {
                maxId = entry.getId();
            }
        }

        nextId = maxId + 1L;
    }

    public void simulateCrashAfterNextApply() {
        this.crashAfterApply = true;
    }

    public void failIfCrashRequested() {
        if (!crashAfterApply) {
            return;
        }
        crashAfterApply = false;
        throw new IllegalStateException("Fallo simulado: operación interrumpida después de aplicar cambios y antes de confirmar journal.");
    }

    private JournalEntry append(
            JournalOperation op,
            String primaryPath,
            String secondaryPath,
            DeletedFileSnapshot deletedFileSnapshot
    ) {
        JournalEntry e = new JournalEntry(nextId, op, primaryPath, secondaryPath, deletedFileSnapshot);
        entries.add(e);
        nextId++;
        return e;
    }

    private JournalEntry findById(long entryId) {
        for (int i = 0; i < entries.size(); i++) {
            JournalEntry e = entries.get(i);
            if (e.getId() == entryId) {
                return e;
            }
        }
        return null;
    }
}
