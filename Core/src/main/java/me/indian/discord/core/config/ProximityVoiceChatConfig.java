package me.indian.discord.core.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import eu.okaeri.configs.annotation.Header;

@Header("################################################################")
@Header("#           Ustawienia Czatu głosowego zbliżeniowego           #")
@Header("################################################################")
public class ProximityVoiceChatConfig extends OkaeriConfig {

    @Comment({""})
    @Comment({""})
    @CustomKey("Enable")
    private boolean enable = true;

    @Comment({""})
    @Comment({"ID Kategorii voice chatu"})
    @CustomKey("CategoryID")
    private long categoryID = 1L;

    @Comment({""})
    @Comment({"ID Kanału lobby"})
    @CustomKey("LobbyID")
    private long lobbyID = 1L;

    @Comment({""})
    @Comment({"Nazwa kanału Lobby"})
    @Comment({"UWAGA: Discord to rozjebane gówno i nie zawsze chce zmienić nazwe kanału"})
    @CustomKey("LobbyName")
    private String lobbyName = "Lobby (Gracze: <members>)";

    @Comment({""})
    @Comment({"Dozwolona odległość graczy od siebie"})
    @CustomKey("ProximityThreshold")
    private int proximityThreshold = 30;

    @Comment({""})
    @Comment({"Czas odświeżania w sekundach"})
    @CustomKey("RefreshTime")
    private int refreshTime = 1;

    @Comment({""})
    @Comment({"Czy użytkownicy mogą odzywać się na lobby?"})
    @CustomKey("SpeakInLobby")
    private boolean speakInLobby = true;

    public boolean isEnable() {
        return this.enable;
    }

    public void setEnable(final boolean enable) {
        this.enable = enable;
    }

    public long getCategoryID() {
        return this.categoryID;
    }

    public long getLobbyID() {
        return this.lobbyID;
    }

    public String getLobbyName() {
        return this.lobbyName;
    }

    public int getProximityThreshold() {
        return this.proximityThreshold;
    }

    public int getRefreshTime() {
        return this.refreshTime;
    }

    public boolean isSpeakInLobby() {
        return this.speakInLobby;
    }
}
