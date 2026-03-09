package proyecto2so.demo;

import proyecto2so.core.FileSystemService;
import proyecto2so.json.SystemStateManager;

public class JournalingRestartRecoveryDemo {
    public static void main(String[] args) {
        String fsPath = "filesystem_state.json";
        String journalPath = "journal_state.json";

        SystemStateManager stateManager = new SystemStateManager();

        FileSystemService beforeRestart = new FileSystemService();
        beforeRestart.initialize(80);
        beforeRestart.createDirectory("/", "system", "admin");
        beforeRestart.createFile("/system", "alpha.txt", "admin", 3);

        beforeRestart.simulateCrashAfterNextCriticalOperation();

        try {
            beforeRestart.renameFile("/system/alpha.txt", "alpha_v2.txt");
        } catch (RuntimeException ex) {
            System.out.println("Crash simulado antes del reinicio: " + ex.getMessage());
        }

        stateManager.save(fsPath, journalPath, beforeRestart);

        FileSystemService afterRestart = new FileSystemService();
        int reverted = stateManager.loadAndRecover(fsPath, journalPath, afterRestart);

        System.out.println("Operaciones revertidas tras reinicio: " + reverted);
        System.out.println("Existe /system/alpha.txt: " + (afterRestart.getFileByPath("/system/alpha.txt") != null));
        System.out.println("Existe /system/alpha_v2.txt: " + (afterRestart.getFileByPath("/system/alpha_v2.txt") != null));
    }
}
