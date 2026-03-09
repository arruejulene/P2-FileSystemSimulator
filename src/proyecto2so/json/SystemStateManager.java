package proyecto2so.json;

import proyecto2so.core.FileSystemService;
import proyecto2so.journal.JournalEntry;

public class SystemStateManager {
    private final FileSystemJsonManager fsJsonManager;
    private final JournalJsonManager journalJsonManager;

    public SystemStateManager() {
        this.fsJsonManager = new FileSystemJsonManager();
        this.journalJsonManager = new JournalJsonManager();
    }

    public void save(String fsStatePath, String journalPath, FileSystemService fs) {
        if (fs == null) {
            throw new IllegalArgumentException("FileSystemService no puede ser null.");
        }

        fsJsonManager.save(fsStatePath, fs);
        journalJsonManager.save(journalPath, fs.getJournalEntries());
    }

    public int loadAndRecover(String fsStatePath, String journalPath, FileSystemService fs) {
        if (fs == null) {
            throw new IllegalArgumentException("FileSystemService no puede ser null.");
        }

        fsJsonManager.load(fsStatePath, fs);
        JournalEntry[] loadedJournal = journalJsonManager.load(journalPath);
        fs.replaceJournalEntries(loadedJournal);

        int reverted = fs.recoverPendingJournalEntries();

        // Persistimos el journal ya consistente después de recovery.
        journalJsonManager.save(journalPath, fs.getJournalEntries());
        return reverted;
    }
}
