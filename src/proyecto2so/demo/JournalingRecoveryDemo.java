package proyecto2so.demo;

import proyecto2so.core.FileSystemService;
import proyecto2so.journal.JournalEntry;

public class JournalingRecoveryDemo {
    public static void main(String[] args) {
        FileSystemService fs = new FileSystemService();
        fs.initialize(80);
        fs.createDirectory("/", "system", "admin");
        fs.createFile("/system", "alpha.txt", "admin", 3);

        System.out.println("Estado inicial:");
        System.out.println("Existe /system/alpha.txt: " + (fs.getFileByPath("/system/alpha.txt") != null));

        fs.simulateCrashAfterNextCriticalOperation();

        try {
            fs.renameFile("/system/alpha.txt", "alpha_v2.txt");
        } catch (RuntimeException ex) {
            System.out.println("Crash simulado durante rename: " + ex.getMessage());
        }

        System.out.println("Después del crash:");
        System.out.println("Existe /system/alpha.txt: " + (fs.getFileByPath("/system/alpha.txt") != null));
        System.out.println("Existe /system/alpha_v2.txt: " + (fs.getFileByPath("/system/alpha_v2.txt") != null));

        int reverted = fs.recoverPendingJournalEntries();
        System.out.println("Operaciones revertidas en recovery: " + reverted);

        System.out.println("Después de recovery:");
        System.out.println("Existe /system/alpha.txt: " + (fs.getFileByPath("/system/alpha.txt") != null));
        System.out.println("Existe /system/alpha_v2.txt: " + (fs.getFileByPath("/system/alpha_v2.txt") != null));

        JournalEntry[] journal = fs.getJournalEntries();
        for (int i = 0; i < journal.length; i++) {
            JournalEntry e = journal[i];
            System.out.println("Journal #" + e.getId() + " op=" + e.getOperation() + " status=" + e.getStatus());
        }
    }
}
