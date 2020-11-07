package de.saschat.autobackup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;


import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Timer;
import java.util.regex.Pattern;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
@Environment(EnvType.SERVER)
public class AutoBackup implements DedicatedServerModInitializer {
    public static MinecraftServer SERVER;
    public static Timer TIMER = new Timer(true);
    public static BackupTask TASK = new BackupTask();

    public static Config DEFAULT_CONFIG = defaultConfig();
    public static Config CONFIG = DEFAULT_CONFIG;

    private static Gson GSON = new GsonBuilder().setPrettyPrinting().create();;

    public static Config defaultConfig() {
        Config cfg = new Config();
        cfg.backupInterval = 30;
        return cfg;
    }

    @Override
    public void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        CommandRegistrationCallback.EVENT.register(this::registerCommands);
    }

    private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, boolean b) {
        dispatcher.register(CommandManager.literal("backup").requires(source -> source.hasPermissionLevel(4)).executes(context -> {
            System.out.println("Backup initiated");
            BackupManager.performBackup("manual", (path) -> {
                context.getSource().sendFeedback(new LiteralText("Successfully created backup at '" + path + "'"), true);
            }, (err) -> {
                context.getSource().sendFeedback(new LiteralText("Failed to create backup. Check console."), true);
                err.printStackTrace();
            });
            return 1;
        }));
        dispatcher.register(CommandManager.literal("backup").requires(source -> source.hasPermissionLevel(4)).then(CommandManager.argument("suffix", string()).executes(context -> {
            String suffix = getString(context, "suffix");
            Pattern p = Pattern.compile("[^a-zA-Z0-9]");
            boolean hasSpecialChar = p.matcher(suffix).find();
            if(hasSpecialChar) {
                throw new SimpleCommandExceptionType(new LiteralText("Suffix must be alphanumeric.")).create();
            }

            System.out.println("Backup initiated with suffix: " + suffix);
            BackupManager.performBackup("manual-"+suffix, (path) -> {
                context.getSource().sendFeedback(new LiteralText("Successfully created backup at '" + path + "'"), true);
            }, (err) -> {
                context.getSource().sendFeedback(new LiteralText("Failed to create backup. Check console."), true);
                err.printStackTrace();
            });
            return 1;
        })));
    }

    public void onServerStopping(MinecraftServer minecraftServer) {
        try {
            BackupThread tr = BackupManager.performBackup("exit");
            tr.join();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void onServerStarted(MinecraftServer minecraftServer) {
        AutoBackup.SERVER = minecraftServer;

        File save_to = Paths.get(minecraftServer.getRunDirectory().getPath(), "backup").toFile();
        save_to.mkdir();

        File config = Paths.get(minecraftServer.getRunDirectory().getPath(), "backup.json").toFile();
        if(!config.exists()) {
            try {
                String a = GSON.toJson(DEFAULT_CONFIG);
                FileWriter myWriter = new FileWriter(config.getAbsoluteFile().toString());
                myWriter.write(a);
                myWriter.flush();
                myWriter.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            try {
                String cfg = new String(Files.readAllBytes(Paths.get(config.getAbsolutePath())), StandardCharsets.UTF_8);
                CONFIG = GSON.fromJson(cfg, Config.class);
                if(CONFIG.backupInterval == 0) {
                    CONFIG = DEFAULT_CONFIG;
                    throw new RuntimeException("Invalid config.");
                }
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }

        System.out.println("Server started...");
        TIMER.scheduleAtFixedRate(TASK, CONFIG.backupInterval * 60 * 1000, CONFIG.backupInterval * 60 * 1000);
    }


}
