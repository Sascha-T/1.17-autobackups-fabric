package de.saschat.autobackup;

public interface FailureCallback {
    void onBackup(Exception e);
}
