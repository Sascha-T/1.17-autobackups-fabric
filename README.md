# Fabric AutoBackups for 1.17
Downloads on the releases page.

## Compiling
Linux:   ``./gradlew build`` \
Windows: ``gradlew build``

## Functionality
This mod can automatically create full world and player data backups at a configurable interval, \
or when a player with OP (permission level 4) runs /backup.

The backups are stored at the root of the Minecraft server in the folder `backups`.

## Configuration
After the initial run of the server, there will be a config.json file in the root of the Minecraft server.
```
{
    "backupInterval": 30
}
```
backupInterval: The amount of minutes inbetween backups. Default: 30
