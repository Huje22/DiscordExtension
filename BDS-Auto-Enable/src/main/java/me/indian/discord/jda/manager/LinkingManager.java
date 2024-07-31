package me.indian.discord.jda.manager;

import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import me.indian.bds.BDSAutoEnable;
import me.indian.bds.logger.Logger;
import me.indian.bds.server.stats.StatsManager;
import me.indian.bds.util.GsonUtil;
import me.indian.bds.util.MathUtil;
import me.indian.discord.DiscordExtension;
import me.indian.discord.core.config.LinkingConfig;
import me.indian.discord.core.manager.ILinkingManager;
import me.indian.discord.jda.DiscordJDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.Nullable;

public class LinkingManager implements ILinkingManager {

    private final DiscordExtension discordExtension;
    private final BDSAutoEnable bdsAutoEnable;
    private final StatsManager statsManager;
    private final LinkingConfig linkingConfig;
    private final DiscordJDA discordJDA;
    private final Logger logger;
    private final File linkedAccountsJson;
    private final HashMap<Long, Long> linkedAccounts;
    private final HashMap<Long, String> accountsToLink;
    private final List<Member> linkedMembers;

    public LinkingManager(final DiscordExtension discordExtension) {
        this.discordExtension = discordExtension;
        this.bdsAutoEnable = this.discordExtension.getBdsAutoEnable();
        this.statsManager = this.bdsAutoEnable.getServerManager().getStatsManager();
        this.linkingConfig = this.discordExtension.getLinkingConfig();
        this.discordJDA = this.discordExtension.getDiscordJDA();
        this.logger = this.discordExtension.getLogger();
        this.linkedAccountsJson = new File(this.discordExtension.getDataFolder(), "linkedAccounts.json");
        this.createJson();
        this.linkedAccounts = this.loadLinkedAccounts();
        this.accountsToLink = new HashMap<>();
        this.linkedMembers = new ArrayList<>();

        this.startTasks();
    }

    @Override
    public boolean isLinked(final String name) {
        return this.linkedAccounts.containsKey(this.statsManager.getXuidByName(name));
    }

    @Override
    public boolean isLinked(final long id) {
        return this.linkedAccounts.containsValue(id);
    }

    @Override
    public boolean linkAccount(final String code, final long id) {
        final long xuid = this.getXuidToLinkByCode(code);
        if (xuid == -1) return false;

        if (this.accountsToLink.get(xuid).equals(code)) {
            this.accountsToLink.remove(xuid);
            this.linkedAccounts.put(xuid, id);
            this.doForLinked();
            this.saveLinkedAccounts();
            return true;
        }

        return false;
    }

    @Override
    public void unLinkAccount(final String name) {
        if (this.isLinked(name)) {
            this.linkedAccounts.remove(this.statsManager.getXuidByName(name));
        }
    }

    @Override
    public void unLinkAccount(final long id) {
        if (this.isLinked(id)) {
            this.linkedAccounts.remove(this.getXuidByID(id), id);
        }
    }

    @Override
    public boolean isCanUnlink() {
        return this.linkingConfig.isCanUnlink();
    }

    @Override
    public void addAccountToLink(final String name, final String code) {
        if (this.isLinked(name)) return;
        this.accountsToLink.put(this.statsManager.getXuidByName(name), code);
    }

    @Override
    public long getXuidToLinkByCode(final String code) {
        for (final Map.Entry<Long, String> map : this.accountsToLink.entrySet()) {
            if (Objects.equals(map.getValue(), code)) return map.getKey();
        }
        return -1;
    }

    @Override
    public long getXuidByID(final long id) {
        for (final Map.Entry<Long, Long> map : this.linkedAccounts.entrySet()) {
            if (Objects.equals(map.getValue(), id)) return map.getKey();
        }
        return -1;
    }

    @Override
    @Nullable
    public String getNameByID(final long id) {
        for (final Map.Entry<Long, Long> map : this.linkedAccounts.entrySet()) {
            if (Objects.equals(map.getValue(), id)) return this.statsManager.getNameByXuid(map.getKey());
        }
        return null;
    }

    @Override
    public long getIdByName(final String name) {
        return this.linkedAccounts.get(this.statsManager.getXuidByName(name));
    }

    @Override
    @Nullable
    public Member getMember(final String name) {
        if (!this.isLinked(name)) return null;
        return this.discordJDA.getGuild().getMemberById(this.getIdByName(name));
    }

    @Override
    public boolean hasPermissions(final String name, final Permission permission) {
        if (!this.isLinked(name)) return false;
        final Member member = this.discordJDA.getGuild().getMemberById(this.getIdByName(name));
        return (member != null && member.hasPermission(permission));
    }

    @Override
    public HashMap<Long, Long> getLinkedAccounts() {
        return this.linkedAccounts;
    }

    @Override
    public List<Member> getLinkedMembers() {
        this.linkedMembers.clear();

        for (final Map.Entry<Long, Long> map : this.linkedAccounts.entrySet()) {
            final Member member = this.discordJDA.getGuild().getMemberById(map.getValue());
            if (member == null) continue;
            this.linkedMembers.add(member);
        }

        return this.linkedMembers;
    }

    @Override
    public void saveLinkedAccounts() {
        try (final FileWriter writer = new FileWriter(this.linkedAccountsJson)) {
            writer.write(GsonUtil.getGson().toJson(this.linkedAccounts));
            this.logger.info("Pomyślnie zapisano&b połączone konta z discord");
        } catch (final Exception exception) {
            this.logger.critical("Nie udało się zapisać&b połączonych kont z discord", exception);
        }
    }

    private HashMap<Long, Long> loadLinkedAccounts() {
        try (final FileReader reader = new FileReader(this.linkedAccountsJson)) {
            final Type type = new TypeToken<HashMap<Long, Long>>() {
            }.getType();
            final HashMap<Long, Long> loadedMap = GsonUtil.getGson().fromJson(reader, type);
            return (loadedMap == null ? new HashMap<>() : loadedMap);
        } catch (final Exception exception) {
            this.logger.critical("Nie udało się załadować&b połączonych kont z discord", exception);
        }
        return new HashMap<>();
    }

    private void startTasks() {
        final long saveTime = MathUtil.minutesTo(30, TimeUnit.MILLISECONDS);
        final long forLinkedTime = MathUtil.minutesTo(1, TimeUnit.MILLISECONDS);
        final Timer timer = new Timer("LinkedAccountsTimer", true);

        final TimerTask saveAccountsTimer = new TimerTask() {
            @Override
            public void run() {
                LinkingManager.this.saveLinkedAccounts();
            }
        };

        final TimerTask doForLinkedTask = new TimerTask() {
            @Override
            public void run() {
                LinkingManager.this.doForLinked();
            }
        };

        timer.scheduleAtFixedRate(saveAccountsTimer, saveTime, saveTime);
        timer.scheduleAtFixedRate(doForLinkedTask, forLinkedTime, forLinkedTime);
    }

    private void doForLinked() {
        for (final Map.Entry<Long, Long> map : this.linkedAccounts.entrySet()) {
            final String name = this.statsManager.getNameByXuid(map.getKey());
            if (name == null) continue;
            final long id = map.getValue();
            final Guild guild = this.discordJDA.getGuild();
            final Member member = guild.getMemberById(id);

            if (member == null) continue;
            if (guild.getSelfMember().canInteract(member)) {
                final String minecraftName = this.getNameByID(id);
                if (minecraftName == null) continue;
                if (member.getNickname() == null || !member.getNickname().equals(minecraftName)) {
                    member.modifyNickname(minecraftName.replaceAll("\"", "")).queue();
                }
            }

            final long hours = MathUtil.hoursFrom(this.bdsAutoEnable.getServerManager().getStatsManager().getPlayTime(name), TimeUnit.MILLISECONDS);
            final Role playtimeRole = guild.getRoleById(this.linkingConfig.getLinkedPlaytimeRoleID());
            final Role linkingRole = guild.getRoleById(this.linkingConfig.getLinkedRoleID());

            if (hours >= 5 && playtimeRole != null && !member.getRoles().contains(playtimeRole) && guild.getSelfMember().canInteract(playtimeRole)) {
                guild.addRoleToMember(member, playtimeRole).queue();
            }

            if (linkingRole != null && !member.getRoles().contains(linkingRole) && guild.getSelfMember().canInteract(linkingRole)) {
                guild.addRoleToMember(member, linkingRole).queue();
            }
        }
    }

    private void createJson() {
        if (!this.linkedAccountsJson.exists()) {
            try {
                if (!this.linkedAccountsJson.createNewFile()) {
                    this.logger.critical("Nie można utworzyć&b linkedAccounts.json");
                }
            } catch (final Exception exception) {
                this.logger.critical("Nie udało się utworzyć&b linkedAccounts.json", exception);
            }
        }
    }
}