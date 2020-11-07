package de.saschat.autobackup;

import java.util.TimerTask;

public class BackupTask extends TimerTask {
    @Override
    public void run() {
        try {
            BackupThread thr = BackupManager.performBackup("automatic");
            thr.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
