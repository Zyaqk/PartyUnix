package me.zya1lk.partyunix;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PartyUNIX extends JavaPlugin implements Listener {

    private Map<Player, List<Player>> parties = new HashMap<>();
    private Map<Player, Player> partyLeaders = new HashMap<>();
    private Map<Player, Player> partyInvites = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        removeFromParty(player);

        if (partyLeaders.containsKey(player)) {
            List<Player> partyMembers = parties.get(player);
            if (partyMembers != null && !partyMembers.isEmpty()) {
                for (Player partyMember : partyMembers) {
                    partyMember.sendMessage("Лидер пати вышел из сервера. Пати было распущенно.");
                    partyLeaders.remove(partyMember);
                }
                parties.remove(player);
            }
        } else {
            for (Map.Entry<Player, List<Player>> entry : parties.entrySet()) {
                List<Player> partyMembers = entry.getValue();
                if (partyMembers.contains(player)) {
                    partyMembers.remove(player);
                    player.sendMessage("Лидер пати вышел из сервера. Пати было распущенно.");
                    if (partyMembers.isEmpty()) {
                        parties.remove(entry.getKey());
                        partyLeaders.remove(entry.getKey());
                    }
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String[] args = event.getMessage().split(" ");
        Player player = event.getPlayer();

        if (args[0].equalsIgnoreCase("/party")) {
            if (args.length > 1) {
                String subCommand = args[1].toLowerCase();
                switch (subCommand) {
                    case "invite":
                        if (partyLeaders.containsKey(player)) {
                            if (args.length > 2) {
                                invitePlayerToParty(player, args[2]);
                            } else {
                                player.sendMessage("Неверный формат команды. Используйте /party invite <игрок>");
                            }
                        } else {
                            player.sendMessage("Вы не являетесь лидером пати и не можете приглашать игроков.");
                        }
                        break;
                    case "create":
                        createParty(player);
                        break;
                    case "leave":
                        leaveParty(player);
                        break;
                    case "list":
                        listPartyMembers(player);
                        break;
                    case "chat":
                        if (args.length > 2) {
                            sendPartyChatMessage(player, args);
                        } else {
                            player.sendMessage("Неверный формат команды. Используйте /party chat <сообщение>");
                        }
                        break;
                    case "kick":
                        if (partyLeaders.containsKey(player)) {
                            if (args.length > 2) {
                                kickPlayerFromParty(player, args[2]);
                            } else {
                                player.sendMessage("Неверный формат команды. Используйте /party kick <игрок>");
                            }
                        } else {
                            player.sendMessage("Вы не являетесь лидером пати и не можете кикать игроков.");
                        }
                        break;
                    case "accept":
                        acceptPartyInvite(player);
                        break;
                    default:
                        player.sendMessage("Неверная подкоманда. Используйте /party [create/leave/list/chat/invite/accept]");
                }
                event.setCancelled(true);
            } else {
                player.sendMessage("Неверный формат команды. Используйте /party [create/leave/list/chat/invite/accept]");
                event.setCancelled(true);
            }
        }
    }

    private void executeDuelsCommandForPlayer(Player leader) {
        List<Player> partyMembers = parties.get(leader);

        if (partyMembers != null && !partyMembers.isEmpty()) {
            for (Player partyMember : partyMembers) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "duels " + partyMember.getName());
                partyMember.sendMessage("Команда /duels была выполнена для вас лидером пати.");
            }
        }
    }

    private void invitePlayerToParty(Player inviter, String invitedPlayerName) {
        Player invitedPlayer = Bukkit.getPlayer(invitedPlayerName);
        if (invitedPlayer != null) {
            for (Map.Entry<Player, List<Player>> entry : parties.entrySet()) {
                List<Player> partyMembers = entry.getValue();
                if (partyMembers.contains(invitedPlayer)) {
                    inviter.sendMessage("Игрок " + invitedPlayer.getName() + " §0уже состоит в пати и не может быть приглашен.");
                    return;
                }
            }

            partyInvites.put(invitedPlayer, inviter);
            invitedPlayer.sendMessage("Вас игрок " + inviter.getName() + " пригласил вас в пати! Пропишите /party accept");
            inviter.sendMessage("Вы пригласили игрока " + invitedPlayer.getName() + " в пати.");
        } else {
            inviter.sendMessage("Игрок " + invitedPlayerName + " не найден.");
        }
    }

    private void kickPlayerFromParty(Player leader, String playerName) {
        Player playerToKick = Bukkit.getPlayer(playerName);

        if (playerToKick != null) {
            List<Player> partyMembers = parties.get(leader);

            if (partyMembers != null && partyMembers.contains(playerToKick)) {
                partyMembers.remove(playerToKick);
                playerToKick.sendMessage("Вы были кикнуты из пати.");
                leader.sendMessage("Вы кикнули игрока " + playerToKick.getName() + " из пати.");

                for (Player partyMember : partyMembers) {
                    partyMember.sendMessage("Игрок " + playerToKick.getName() + " был кикнут из пати.");
                }

                if (partyMembers.isEmpty()) {
                    parties.remove(leader);
                    partyLeaders.remove(leader);
                }
            } else {
                leader.sendMessage("Игрок " + playerName + " не состоит в вашем пати.");
            }
        } else {
            leader.sendMessage("Игрок " + playerName + " не найден.");
        }
    }

    private void removeFromParty(Player player) {
        for (Map.Entry<Player, List<Player>> entry : parties.entrySet()) {
            Player partyLeader = entry.getKey();
            List<Player> partyMembers = entry.getValue();

            if (partyMembers.contains(player)) {
                partyMembers.remove(player);
                player.sendMessage("Вы покинули пати, так как вышли из сервера.");

                for (Player partyMember : partyMembers) {
                    partyMember.sendMessage("Игрок " + player.getName() + " покинул пати.");
                }

                if (partyMembers.isEmpty()) {
                    parties.remove(partyLeader);
                    partyLeaders.remove(partyLeader);
                }
                break;
            }
        }
    }

    private void acceptPartyInvite(Player player) {
        if (partyInvites.containsKey(player)) {
            Player inviter = partyInvites.get(player);
            List<Player> partyMembers = parties.get(inviter);
            partyMembers.add(player);
            parties.put(inviter, partyMembers);
            partyInvites.remove(player);
            player.sendMessage("Вы приняли приглашение и вступили в пати " + inviter.getName() + ".");
        } else {
            player.sendMessage("У вас нет активных приглашений в пати.");
        }
    }

    private void createParty(Player player) {
        if (!parties.containsKey(player)) {
            for (Map.Entry<Player, List<Player>> entry : parties.entrySet()) {
                List<Player> partyMembers = entry.getValue();
                if (partyMembers.contains(player)) {
                    player.sendMessage("Вы уже состоите в другой пати. Покиньте ее командой /party leave, чтобы создать новую.");
                    return;
                }
            }

            List<Player> partyMembers = new ArrayList<>();
            partyMembers.add(player);
            parties.put(player, partyMembers);
            partyLeaders.put(player, player);
            player.sendMessage("Пати создано!");
        } else {
            player.sendMessage("Вы уже находитесь в пати. Для создания новой пати покиньте текущую командой /party leave.");
        }
    }


    private void leaveParty(Player player) {
        Player partyLeader = null;

        for (Map.Entry<Player, List<Player>> entry : parties.entrySet()) {
            Player leader = entry.getKey();
            List<Player> partyMembers = entry.getValue();

            if (partyMembers.contains(player)) {
                partyLeader = leader;
                partyMembers.remove(player);
                if (partyMembers.isEmpty()) {
                    parties.remove(partyLeader);
                    partyLeaders.remove(partyLeader);
                }
                player.sendMessage("Вы покинули пати.");
                return;
            }
        }

        player.sendMessage("Вы не состоите в пати!");
    }


    private void listPartyMembers(Player player) {
        boolean isInParty = false;
        Player partyLeader = null;

        for (Map.Entry<Player, List<Player>> entry : parties.entrySet()) {
            Player leader = entry.getKey();
            List<Player> partyMembers = entry.getValue();

            if (partyMembers.contains(player)) {
                isInParty = true;
                partyLeader = leader;
                break;
            }
        }

        if (isInParty) {
            List<Player> partyMembers = parties.get(partyLeader);

            player.sendMessage("Лидер пати: " + partyLeader.getName());
            player.sendMessage("Список участников:");
            for (Player partyMember : partyMembers) {
                player.sendMessage("- " + partyMember.getName());
            }
        } else {
            player.sendMessage("Вы не состоите в пати!");
        }
    }


    private void sendPartyChatMessage(Player player, String[] args) {
        boolean isInParty = false;
        List<Player> partyMembers = null;

        for (Map.Entry<Player, List<Player>> entry : parties.entrySet()) {
            List<Player> members = entry.getValue();

            if (members.contains(player)) {
                isInParty = true;
                partyMembers = members;
                break;
            }
        }

        if (isInParty) {
            StringBuilder messageBuilder = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                messageBuilder.append(args[i]).append(" ");
            }
            String message = messageBuilder.toString().trim();

            for (Player partyMember : partyMembers) {
                partyMember.sendMessage("[Пати] " + player.getName() + ": " + message);
            }
        } else {
            player.sendMessage("Вы не состоите в пати!");
        }
    }
}
