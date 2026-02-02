# Changelog

## 6.0.4

- Fixed an erroneous warning message. oops!

## 6.0.3

- Fixed a crash with certain Minecraft versions. Again.

## 6.0.2

- Fixed Dialtone being broken for 1.18-1.20.

## 6.0.1

- Fixed a crash with certain Minecraft versions.

## 6.0.0

- Changed mod ID to `e4mc` from `e4mc_minecraft`.
- Introduced Dialtone! When installed on *both sides*, e4mc will now establish a direct connection.

## 5.5.4

- Fixed a regression on 1.21.11 and above due to Minecraft using Netty 4.2.

## 5.5.3

- Mildly improved performance and footprint by reusing Minecraft's `EventLoopGroup`.
- Fixed the inability to join using certain Forge versions with the message "Disconnected".
- Fixed an issue where e4mc would keep an unjoinable ghost session running after closing the world.

## 5.5.2

- Fixed some errors by updating dependencies.

## 5.5.1

- Fixed a crash on some NeoForge versions

## 5.5.0

- Added `/e4mc doctor`, which prints e4mc-related diagnostics for troubleshooting

## 5.4.2

- Now supports 1.21.9, 1.21.10, 1.21.11

## 5.4.1

- You can no longer lock yourself out of a world by banning yourself or forgetting to whitelist yourself

## 5.4.0

- Now supports 1.21.6
- Now restores basic administration commands such as /ban and /whitelist for LAN servers.

## 5.3.1

- Now supports 1.21.5

## 5.3.0

- Added Malay and Malay (Jawi) translations (by  NuruddinPlays)
- Added Spanish translation (by Biquaternions)
- Fixed constant redownloads of natives

## 5.2.1

- Removed accidentally added debug message

## 5.2.0

- Added NeoForge support
- Added French translation (by vazanoir)
- Added Ukrainian translation (by Tarteroycc)
