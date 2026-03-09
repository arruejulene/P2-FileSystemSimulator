package proyecto2so.demo;

import proyecto2so.core.FileSystemService;
import proyecto2so.json.SystemStateManager;

public class JournalingDirectoryRestartRecoveryDemo {
    public static void main(String[] args) {
        String fsPath = "filesystem_state.json";
        String journalPath = "journal_state.json";
        SystemStateManager stateManager = new SystemStateManager();

        FileSystemService beforeRestart = new FileSystemService();
        beforeRestart.initialize(120);
        beforeRestart.createDirectory("/", "system", "admin");
        beforeRestart.createDirectory("/system", "logs", "admin");
        beforeRestart.createFile("/system/logs", "a.log", "admin", 2);
        beforeRestart.createDirectory("/system/logs", "old", "admin");
        beforeRestart.createFile("/system/logs/old", "b.log", "admin", 2);

        beforeRestart.simulateCrashAfterNextCriticalOperation();
        try {
            beforeRestart.deleteDirectoryRecursive("/system/logs");
        } catch (RuntimeException ex) {
            System.out.println("Crash simulado borrando directorio: " + ex.getMessage());
        }

        stateManager.save(fsPath, journalPath, beforeRestart);

        FileSystemService afterRestart = new FileSystemService();
        int reverted = stateManager.loadAndRecover(fsPath, journalPath, afterRestart);

        System.out.println("Operaciones revertidas tras reinicio: " + reverted);
        System.out.println("Existe /system/logs: " + (afterRestart.getFileByPath("/system/logs/a.log") != null));
        System.out.println("Existe /system/logs/old/b.log: " + (afterRestart.getFileByPath("/system/logs/old/b.log") != null));
    }
}
