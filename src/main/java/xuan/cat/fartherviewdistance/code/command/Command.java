package xuan.cat.fartherviewdistance.code.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xuan.cat.fartherviewdistance.code.ChunkIndex;
import xuan.cat.fartherviewdistance.code.ChunkServer;
import xuan.cat.fartherviewdistance.code.data.ConfigData;
import xuan.cat.fartherviewdistance.code.data.CumulativeReport;

public final class Command implements CommandExecutor {
    private final ChunkServer chunkServer;
    private final ConfigData configData;

    public Command(ChunkServer chunkServer, ConfigData configData) {
        this.chunkServer = chunkServer;
        this.configData = configData;
    }

    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String message, String[] parameters) {
        if (!sender.hasPermission("command.viewdistance")) {
            // 沒有權限
            sender.sendMessage(ChatColor.RED + chunkServer.lang.get(sender, "command.no_permission"));
        } else {
            if (parameters.length < 1) {
                // 缺少參數
                sender.sendMessage(ChatColor.RED + chunkServer.lang.get(sender, "command.missing_parameters"));
            } else {
                switch (parameters[0]) {
                    case "reload":
                        try {
                            configData.reload();
                            ChunkIndex.getChunkServer().reloadMultithreaded();
                            sender.sendMessage(ChatColor.YELLOW + chunkServer.lang.get(sender, "command.reread_configuration_successfully"));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            sender.sendMessage(ChatColor.RED + chunkServer.lang.get(sender, "command.reread_configuration_error"));
                        }
                        break;
                    case "report":
                        // 生成報告
                        if (parameters.length < 2) {
                            // 缺少參數
                            sender.sendMessage(ChatColor.RED + chunkServer.lang.get(sender, "command.missing_parameters"));
                        } else {
                            switch (parameters[1]) {
                                case "server": {
                                    sendReportHead(sender);
                                    sendReportCumulative(sender, "*SERVER", chunkServer.serverCumulativeReport);
                                    break;
                                }
                                case "thread": {
                                    sendReportHead(sender);
                                    chunkServer.threadsCumulativeReport.forEach(((threadNumber, cumulativeReport) -> sendReportCumulative(sender, "*THREAD#" + threadNumber, cumulativeReport)));
                                    break;
                                }
                                case "world": {
                                    sendReportHead(sender);
                                    chunkServer.worldsCumulativeReport.forEach(((world, cumulativeReport) -> sendReportCumulative(sender, world.getName(), cumulativeReport)));
                                    break;
                                }
                                case "player": {
                                    sendReportHead(sender);
                                    chunkServer.playersViewMap.forEach(((player, view) -> sendReportCumulative(sender, player.getName(), view.cumulativeReport)));
                                    break;
                                }
                                default:
                                    // 未知的參數類型
                                    sender.sendMessage(ChatColor.RED + chunkServer.lang.get(sender, "command.unknown_parameter_type") + " " + parameters[0]);
                                    break;
                            }
                        }
                        break;
                    case "start":
                        chunkServer.globalPause = false;
                        sender.sendMessage(ChatColor.YELLOW + chunkServer.lang.get(sender, "command.continue_execution"));
                        break;
                    case "stop":
                        chunkServer.globalPause = true;
                        sender.sendMessage(ChatColor.YELLOW + chunkServer.lang.get(sender, "command.suspension_execution"));
                        break;
                    case "permissionCheck":
                        // 檢查玩家權限
                        if (parameters.length < 2) {
                            // 缺少參數
                            sender.sendMessage(ChatColor.RED + chunkServer.lang.get(sender, "command.missing_parameters"));
                        } else {
                            Player player = Bukkit.getPlayer(parameters[1]);
                            if (player == null) {
                                // 玩家不存在
                                sender.sendMessage(ChatColor.RED + chunkServer.lang.get(sender, "command.players_do_not_exist"));
                            } else {
                                chunkServer.getView(player).permissionsNeed = true;
                                // 已重新檢查玩家權限
                                sender.sendMessage(ChatColor.YELLOW + chunkServer.lang.get(sender, "command.rechecked_player_permissions"));
                            }
                        }
                        break;
                    case "debug":
                        // 除錯
                        if (parameters.length < 2) {
                            // 缺少參數
                            sender.sendMessage(ChatColor.RED + chunkServer.lang.get(sender, "command.missing_parameters"));
                        } else {
                            switch (parameters[1]) {
                                case "view": {
                                    if (parameters.length < 3) {
                                        // 缺少參數
                                        sender.sendMessage(ChatColor.RED + chunkServer.lang.get(sender, "command.missing_parameters"));
                                    } else {
                                        Player player = Bukkit.getPlayer(parameters[2]);
                                        if (player == null) {
                                            // 玩家不存在
                                            sender.sendMessage(ChatColor.RED + chunkServer.lang.get(sender, "command.players_do_not_exist"));
                                        } else {
                                            chunkServer.getView(player).getMap().debug(sender);
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                        break;
                    default:
                        // 未知的參數類型
                        sender.sendMessage(ChatColor.RED + chunkServer.lang.get(sender, "command.unknown_parameter_type") + " " + parameters[0]);
                        break;
                }
            }
        }
        return true;
    }


    private void sendReportHead(CommandSender sender) {
        // 來源 | 快速 5秒/1分鐘/5分鐘 | 慢速 5秒/1分鐘/5分鐘 | 流量 5秒/1分鐘/5分鐘
        String timeSegment = chunkServer.lang.get(sender, "command.report.5s") + "/" + chunkServer.lang.get(sender, "command.report.1m") + "/" + chunkServer.lang.get(sender, "command.report.5m");
        sender.sendMessage(ChatColor.YELLOW + chunkServer.lang.get(sender, "command.report.source") + ChatColor.WHITE + " | " + ChatColor.GREEN + chunkServer.lang.get(sender, "command.report.fast") + " " + timeSegment + ChatColor.WHITE + " | " + ChatColor.RED + chunkServer.lang.get(sender, "command.report.slow") + " " + timeSegment + ChatColor.WHITE + " | " + ChatColor.GOLD + chunkServer.lang.get(sender, "command.report.flow") + " " + timeSegment);
    }
    private void sendReportCumulative(CommandSender sender, String source, CumulativeReport cumulativeReport) {
        sender.sendMessage(ChatColor.YELLOW + source + ChatColor.WHITE + " | " + ChatColor.GREEN + cumulativeReport.reportLoadFast5s() + "/" + cumulativeReport.reportLoadFast1m() + "/" + cumulativeReport.reportLoadFast5m() + ChatColor.WHITE + " | " + ChatColor.RED + cumulativeReport.reportLoadSlow5s() + "/" + cumulativeReport.reportLoadSlow1m() + "/" + cumulativeReport.reportLoadSlow5m() + ChatColor.WHITE + " | " + ChatColor.GOLD + cumulativeReport.reportConsume5s() + "/" + cumulativeReport.reportConsume1m() + "/" + cumulativeReport.reportConsume5m());
    }
}
