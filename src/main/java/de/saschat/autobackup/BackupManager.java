package de.saschat.autobackup;

public class BackupManager {
    public static BackupThread performBackup(String suffix) {
        BackupThread thread = new BackupThread(suffix);
        thread.start();
        return thread;
    }

    public static BackupThread performBackup(String suffix, BackupCallback cb) {
        BackupThread thread = new BackupThread(suffix, cb);
        thread.start();
        return thread;
    }

    public static BackupThread performBackup(String suffix, BackupCallback cb, FailureCallback failure) {
        BackupThread thread = new BackupThread(suffix, cb, failure);
        thread.start();
        return thread;
    }
}
