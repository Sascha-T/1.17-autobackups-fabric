package de.saschat.autobackup;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupThread extends Thread {
    public static ReentrantLock mutex = new ReentrantLock();

    public String suffix;
    public BackupCallback cb;
    public FailureCallback failure;

    public BackupThread(String suffix) {
        this.suffix = suffix;
    }
    public BackupThread(String suffix, BackupCallback cb) {
        this.suffix = suffix;
        this.cb = cb;
    }
    public BackupThread(String suffix, BackupCallback cb, FailureCallback failure) {
        this.suffix = suffix;
        this.cb = cb;
        this.failure = failure;
    }

    public void run() {
        try {
            mutex.lock();
            System.out.println("Disabling autosave...");
            Iterable<ServerWorld> worlds = AutoBackup.server.getWorlds();
            HashMap<ServerWorld, Boolean> map = new HashMap<>();
            for (ServerWorld w: worlds) {
                w.savingDisabled = true;
            }
            System.out.println("Disabled autosave.");

            AutoBackup.server.getPlayerManager().saveAllPlayerData();
            AutoBackup.server.save(true, true, true);
            System.out.println("Saved all.");

            File save = AutoBackup.server.getSavePath(WorldSavePath.ROOT).toFile();
            String date = new SimpleDateFormat("yyyy-MM-dd-HH-mm").format(Date.from(Instant.now()));

            String name = date + "-" + this.suffix;
            Path save_to;
            int num = 0;
            while (true) {
                save_to = Paths.get(AutoBackup.server.getRunDirectory().getPath(),"backup", name + "-" + num + ".zip");
                File f = new File(save_to.toAbsolutePath().toString());
                if(!f.exists()) {
                    break;
                }
                num = num + 1;
            }

            List<String> paths = generateFileList(save);
            try {
                FileOutputStream fos = new FileOutputStream(save_to.toAbsolutePath().toString());
                ZipOutputStream zos = new ZipOutputStream(fos);
                for (String path: paths) {
                    String pathE = Paths.get(save.getAbsolutePath()).relativize(Paths.get(path)).toString();
                    if(pathE.equals("session.lock"))
                        continue;
                    ZipEntry ze = new ZipEntry(pathE);
                    zos.putNextEntry(ze);
                    try {
                        FileInputStream in = new FileInputStream(path);
                        int len = 0;
                        byte[] buffer = new byte[1024];
                        while ((len = in.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                        in.close();
                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }
                    zos.closeEntry();
                }
                zos.close();
            } catch(Exception ex) {
                if(failure != null)
                    failure.onBackup(ex);
            }

            System.out.println("Enabling autosave...");
            for (ServerWorld w: worlds) {
                w.savingDisabled = false;
            }

            if(cb != null)
                cb.onBackup(save_to.toAbsolutePath().toString());
        } catch(Exception ex) {
            if(failure != null)
                failure.onBackup(ex);
        } finally {
            mutex.unlock();
        }
    }

    private static List<String> generateFileList(File node) {
        List<String> list = new LinkedList<>();
        if(node.isDirectory()) {
            for (File f: node.listFiles()) {
                if(f.isDirectory()) {
                    list.addAll(generateFileList(f));
                } else {
                    if(!f.getName().equals("..") || !f.getName().equals("."))
                        list.add(f.getAbsolutePath());
                }
            }
        }
        return list;
    }
}
