package pl.indianbartonka.discord.jda.voice;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import pl.indianbartonka.bds.event.EventHandler;
import pl.indianbartonka.bds.event.Listener;
import pl.indianbartonka.bds.event.player.PlayerMovementEvent;
import pl.indianbartonka.bds.event.player.PlayerQuitEvent;
import pl.indianbartonka.util.logger.Logger;
import pl.indianbartonka.bds.player.position.Position;
import pl.indianbartonka.util.DateUtil;
import pl.indianbartonka.util.ThreadUtil;
import pl.indianbartonka.discord.DiscordExtension;
import pl.indianbartonka.discord.core.config.ProximityVoiceChatConfig;
import pl.indianbartonka.discord.core.manager.ILinkingManager;
import pl.indianbartonka.discord.jda.DiscordJDA;
import pl.indianbartonka.discord.jda.voice.component.Group;
import pl.indianbartonka.discord.jda.voice.component.VoiceChatMember;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.managers.channel.concrete.VoiceChannelManager;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import org.jetbrains.annotations.Nullable;

public class ProximityVoiceChat implements Listener {

    private final DiscordExtension discordExtension;
    private final ProximityVoiceChatConfig proximityVoiceChatConfig;
    private final DiscordJDA discordJDA;
    private final ILinkingManager linkingManager;
    private final Logger logger;
    private final ThreadUtil threadUtil;
    private final ExecutorService service;
    private final PlayerGroupManager playerGroupManager;
    private final Map<String, VoiceChatMember> playerPosition;
    private final Map<String, VoiceChannel> voiceChannels;
    private final List<String> toTimeRemove;
    private final Guild guild;
    private final long categoryID, lobbyID;
    private final Category voiceCategory;
    private final VoiceChannel lobbyChannel;
    private String lastLobbyName;

    public ProximityVoiceChat(final DiscordExtension discordExtension, final ProximityVoiceChatConfig proximityVoiceChatConfig) {
        this.discordExtension = discordExtension;
        this.proximityVoiceChatConfig = proximityVoiceChatConfig;
        this.discordJDA = discordExtension.getDiscordJDA();
        this.linkingManager = this.discordJDA.getLinkingManager();
        this.logger = discordExtension.getLogger();
        this.threadUtil = new ThreadUtil("Proximity Voice Chat");
        this.service = Executors.newCachedThreadPool(this.threadUtil);
        this.playerGroupManager = new PlayerGroupManager();
        this.playerPosition = new HashMap<>();
        this.voiceChannels = new HashMap<>();
        this.toTimeRemove = new ArrayList<>();
        this.guild = this.discordJDA.getGuild();
        this.categoryID = this.proximityVoiceChatConfig.getCategoryID();
        this.lobbyID = this.proximityVoiceChatConfig.getLobbyID();
        this.voiceCategory = this.discordJDA.getCategoryByID(this.categoryID);

        if (this.voiceCategory == null) {
            throw new NullPointerException("Nie znaleziono kategorii z ID " + this.categoryID);
        }

        this.lobbyChannel = this.guild.getVoiceChannelById(this.lobbyID);

        if (this.lobbyChannel == null) {
            throw new NullPointerException("Nie znaleziono kanału z ID " + this.lobbyID);
        }

        this.lastLobbyName = proximityVoiceChatConfig.getLobbyName();

        this.setLobbyChannel();
        this.removeOldChannels();
        this.start();
    }

    private void removeOldChannels() {
        this.logger.info("Nazwa kategorii dla Proximity Voice Chat:&1 " + this.voiceCategory.getName());

        for (final VoiceChannel voiceChannel : this.voiceCategory.getVoiceChannels()) {
            if (voiceChannel != this.lobbyChannel) {
                this.logger.info("&cUsunięto:&1 " + voiceChannel.getName());
                voiceChannel.getMembers().forEach(member -> this.movePlayerToVoiceChannel(member, this.lobbyChannel));
                voiceChannel.delete().queue();
            }
        }
    }

    @EventHandler
    private void onPlayerMovement(final PlayerMovementEvent event) {
        final String playerName = event.getPlayer().getPlayerName();
        final Position position = event.getPosition();

        if (this.linkingManager.isLinked(playerName)) {
            final Member member = this.linkingManager.getMember(playerName);

            VoiceChatMember voiceChatMember = this.playerPosition.get(playerName);

            if (voiceChatMember == null) {
                voiceChatMember = new VoiceChatMember(playerName, member, position);
                this.playerPosition.put(playerName, voiceChatMember);
            } else {
                voiceChatMember.setPosition(position);
            }

            if (this.getPlayerChannel(voiceChatMember.getMember()) != null) {
                this.playerGroupManager.addVoiceChatMember(voiceChatMember);
            }
        }
    }

    @EventHandler
    private void onPlayerQuit(final PlayerQuitEvent event) {
        final String playerName = event.getPlayer().getPlayerName();
        final VoiceChatMember voiceChatMember = this.playerPosition.get(playerName);

        if (voiceChatMember != null) {
            this.movePlayerToVoiceChannel(voiceChatMember.getMember(), this.lobbyChannel);
            this.playerGroupManager.removeVoiceChatMember(voiceChatMember);
            this.playerGroupManager.removeFromGroups(voiceChatMember);
            this.playerPosition.remove(playerName);
        }
    }

    private void start() {
        final TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                ProximityVoiceChat.this.removeEmptyGroups();
                ProximityVoiceChat.this.connectPlayersInProximity(ProximityVoiceChat.this.proximityVoiceChatConfig.getProximityThreshold());
                ProximityVoiceChat.this.updateLobbyChannel();
            }
        };

        new Timer(this.threadUtil.getThreadName()).scheduleAtFixedRate(timerTask, 0, DateUtil.secondToMillis(this.proximityVoiceChatConfig.getRefreshTime()));
    }

    public void connectPlayersInProximity(final int proximityThreshold) {
        for (final Group group : this.playerGroupManager.createGroups(proximityThreshold)) {
            final List<VoiceChatMember> groupMembers = new ArrayList<>(group.getVoiceChatMembers());
            if (groupMembers.isEmpty()) continue;

            if (groupMembers.size() == 1) {
                for (final VoiceChatMember voiceChatMember : groupMembers) {
                    if (voiceChatMember != null) {
                        this.movePlayerToVoiceChannel(voiceChatMember.getMember(), this.lobbyChannel);
                    }
                }
            } else {
                final VoiceChannel newVoiceChannel = this.createNewVoiceChannel(group.getId());

                if (newVoiceChannel != null) {
                    for (final VoiceChatMember voiceChatMember : groupMembers) {
                        this.movePlayerToVoiceChannel(voiceChatMember.getMember(), newVoiceChannel);
                    }
                } else {
                    this.logger.error("Nie udało się utworzyć kanału dla grupy&7 " + group.getId() + "&r!");
                }
            }
        }
    }

    private void removeEmptyGroups() {
        final Map<String, Group> groupToRemove = new HashMap<>();
        final Map<String, VoiceChannel> voiceChannelsToRemove = new HashMap<>();

        for (final Group group : this.playerGroupManager.getGroupMap().values()) {
            if (group.getVoiceChatMembers().isEmpty()) {
                final VoiceChannel voiceChannel = this.voiceChannels.get(group.getId());

                groupToRemove.put(group.getId(), group);

                if (voiceChannel != null) {
                    voiceChannelsToRemove.put(group.getId(), voiceChannel);
                    voiceChannel.delete().queue();
                }
            }
        }

        try {
            groupToRemove.forEach((id, group) -> this.playerGroupManager.removeGroup(group));
            voiceChannelsToRemove.forEach((id, voiceChannel) -> this.voiceChannels.remove(id));
        } catch (final Exception exception) {
            this.logger.error("Nie udało się przeprowadzić operacji usuwania kanałów", exception);
        }
    }

    private VoiceChannel createNewVoiceChannel(final String id) {
        try {
            final VoiceChannel voiceChannel = this.voiceChannels.get(id);

            if (voiceChannel != null) return voiceChannel;
            if (this.voiceCategory != null) {
                final ChannelAction<VoiceChannel> channelAction = this.voiceCategory.createVoiceChannel(id)
                        .addPermissionOverride(this.guild.getPublicRole(), EnumSet.of(Permission.VOICE_SPEAK), EnumSet.of(Permission.VIEW_CHANNEL));

                final VoiceChannel createdChannel = channelAction.complete();
                this.voiceChannels.put(id, createdChannel);

                return createdChannel;
            }
        } catch (final Exception exception) {
            this.logger.error("Nie udało się utworzyć kanału dla grupy&7 " + id + "&r!", exception);
        }

        return null;
    }

    private void movePlayerToVoiceChannel(final Member member, final VoiceChannel voiceChannel) {
        if (!voiceChannel.getMembers().contains(member)) {
            if (this.getPlayerChannel(member) != null) {
                this.guild.moveVoiceMember(member, voiceChannel).queue();
            } else {
                this.timeRemove(this.playerGroupManager.getVoiceChatMemberByMember(member));
            }
        }
    }

    private void setLobbyChannel() {
        final Role linkingRole = this.guild.getRoleById(this.discordExtension.getLinkingConfig().getLinkedRoleID());
        final VoiceChannelManager lobbyManager = this.lobbyChannel.getManager();

        lobbyManager.putPermissionOverride(this.guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL, Permission.VOICE_SPEAK))
                .queue();

        if (linkingRole != null) {
            final EnumSet<Permission> allowPerms = EnumSet.of(Permission.VIEW_CHANNEL);
            final EnumSet<Permission> denyPerms = EnumSet.noneOf(Permission.class);

            if (this.proximityVoiceChatConfig.isSpeakInLobby()) {
                allowPerms.add(Permission.VOICE_SPEAK);
            } else {
                denyPerms.add(Permission.VOICE_SPEAK);
            }

            lobbyManager.putPermissionOverride(linkingRole, allowPerms, denyPerms).queue();
        }

        this.updateLobbyChannel();
    }

    private void updateLobbyChannel() {
        final VoiceChannelManager lobbyManager = this.lobbyChannel.getManager();
        final String lobbyName = this.proximityVoiceChatConfig.getLobbyName().replaceAll("<members>", String.valueOf(this.getMembersSize(false)));

        if (!lobbyName.equals(this.lastLobbyName)) {
            this.lastLobbyName = lobbyName;
            lobbyManager.setName(lobbyName).queue();
        }
    }

    public int getMembersSize(final boolean countLobby) {
        int allSize = 0;

        for (final VoiceChannel voiceChannel : this.voiceCategory.getVoiceChannels()) {
            if (voiceChannel == this.lobbyChannel && !countLobby) continue;
            allSize += voiceChannel.getMembers().size();
        }

        return allSize;
    }

    private void timeRemove(final VoiceChatMember voiceChatMember) {
        if (voiceChatMember != null) {
            if (this.toTimeRemove.contains(voiceChatMember.getName())) return;
            this.toTimeRemove.add(voiceChatMember.getName());
            ThreadUtil.sleep(5);
            if (this.getPlayerChannel(voiceChatMember.getMember()) == null) {
                this.toTimeRemove.remove(voiceChatMember.getName());
                this.playerGroupManager.removeFromGroups(voiceChatMember);
                this.playerGroupManager.removeVoiceChatMember(voiceChatMember);

                this.logger.error("Gracz &c" + voiceChatMember.getName() + "&r nie jest na żadnym kanale!");
            }
        }
    }

    @Nullable
    private VoiceChannel getPlayerChannel(final Member member) {
        return this.voiceCategory.getVoiceChannels().stream()
                .filter(voiceChannel -> voiceChannel.getMembers().contains(member))
                .findFirst()
                .orElse(null);
    }
}