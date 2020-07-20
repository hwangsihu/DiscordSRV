/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2020 Austin "Scarsz" Shapiro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package github.scarsz.discordsrv.hooks.chat;

import com.dthielke.herochat.Channel;
import com.dthielke.herochat.ChannelChatEvent;
import com.dthielke.herochat.Chatter;
import com.dthielke.herochat.Herochat;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.MessageUtil;
import github.scarsz.discordsrv.util.PlayerUtil;
import github.scarsz.discordsrv.util.PluginUtil;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.stream.Collectors;

public class HerochatHook implements ChatHook {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMessage(ChannelChatEvent event) {
        // make sure chat channel is registered with a destination
        if (DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(event.getChannel().getName()) == null) return;

        // make sure message isn't just blank
        if (StringUtils.isBlank(event.getMessage())) return;

        DiscordSRV.getPlugin().processChatMessage(event.getSender().getPlayer(), event.getMessage(), event.getChannel().getName(), event.getResult() != Chatter.Result.ALLOWED);
    }

    @Override
    public void broadcastMessageToChannel(String channel, Component message) {
        Channel chatChannel = getChannelByCaseInsensitiveName(channel);
        if (chatChannel == null) return; // no suitable channel found
        String legacy = MessageUtil.toLegacy(message);

        String plainMessage = LangUtil.Message.CHAT_CHANNEL_MESSAGE.toString()
                .replace("%channelcolor%", chatChannel.getColor().toString())
                .replace("%channelname%", chatChannel.getName())
                .replace("%channelnickname%", chatChannel.getNick())
                .replace("%message%", legacy);

        String translatedMessage = MessageUtil.toLegacy(MessageUtil.toComponent(ChatColor.translateAlternateColorCodes('&', plainMessage)));
        chatChannel.sendRawMessage(translatedMessage);

        PlayerUtil.notifyPlayersOfMentions(player ->
                        chatChannel.getMembers().stream()
                                .map(Chatter::getPlayer)
                                .collect(Collectors.toList())
                                .contains(player),
                legacy);
    }

    private static Channel getChannelByCaseInsensitiveName(String name) {
        List<Channel> channels = Herochat.getChannelManager().getChannels();

        if (channels.size() > 0) {
            for (Channel channel : Herochat.getChannelManager().getChannels()) {
                DiscordSRV.debug("\"" + channel.getName() + "\" equalsIgnoreCase \"" + name + "\" == " + channel.getName().equalsIgnoreCase(name));
                if (channel.getName().equalsIgnoreCase(name)) {
                    return channel;
                }
            }
            DiscordSRV.debug("No matching Herochat channels for name \"" + name + "\"");
        } else {
            DiscordSRV.debug("Herochat's channel manager returned no registered channels");
        }
        return null;
    }

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("Herochat");
    }

}
