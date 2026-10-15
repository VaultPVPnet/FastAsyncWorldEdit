package com.fastasyncworldedit.bukkit.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Decides whether a sender may see FAWE commands at all. Players without a single explicitly granted
 * {@code fawe.*} or {@code worldedit.*} permission get every FAWE command hidden, as if it did not exist.
 * <p>
 * Not for external use. May change at any point.
 */
@ApiStatus.Internal
public final class CommandAccess {

    private CommandAccess() {
    }

    /**
     * Check if the sender holds at least one {@code fawe.} or {@code worldedit.} permission that was not
     * handed out to everyone by default (e.g. {@code fawe.plotsquared}, which is {@code default: true}).
     * Non-player senders (console, command blocks) and operators always have access.
     *
     * @param sender the sender
     * @return true if FAWE commands should be visible and usable for the sender
     */
    public static boolean hasAccess(CommandSender sender) {
        if (!(sender instanceof Player player) || player.isOp()) {
            return true;
        }
        Set<String> defaults = null;
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (!info.getValue()) {
                continue;
            }
            String name = info.getPermission().toLowerCase(Locale.ROOT);
            if (!name.equals("*") && !name.startsWith("fawe.") && !name.startsWith("worldedit.")) {
                continue;
            }
            if (defaults == null) {
                defaults = defaultGrantedPermissions();
            }
            if (!defaults.contains(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Send the same feedback the vanilla command dispatcher gives for an unknown command.
     *
     * @param sender the sender
     * @param label  the command label that was typed
     */
    public static void sendUnknownCommand(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.RED + "Unknown or incomplete command. See below for error");
        sender.sendMessage(ChatColor.RED + "" + ChatColor.UNDERLINE + label
                + ChatColor.RESET + ChatColor.RED + ChatColor.ITALIC + "<--[HERE]");
    }

    /**
     * Collect every permission that non-op players receive by default, including children.
     */
    private static Set<String> defaultGrantedPermissions() {
        Set<String> result = new HashSet<>();
        Deque<Permission> queue = new ArrayDeque<>(Bukkit.getPluginManager().getDefaultPermissions(false));
        while (!queue.isEmpty()) {
            Permission permission = queue.poll();
            if (!result.add(permission.getName().toLowerCase(Locale.ROOT))) {
                continue;
            }
            for (Map.Entry<String, Boolean> child : permission.getChildren().entrySet()) {
                if (!child.getValue()) {
                    continue;
                }
                Permission childPermission = Bukkit.getPluginManager().getPermission(child.getKey());
                if (childPermission != null) {
                    queue.add(childPermission);
                } else {
                    result.add(child.getKey().toLowerCase(Locale.ROOT));
                }
            }
        }
        return result;
    }

}
