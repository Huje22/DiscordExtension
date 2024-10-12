package pl.indianbartonka.discord.core.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Header;
import java.util.LinkedHashMap;
import java.util.Map;

@Header("################################################################")
@Header("#           Ustawienia łączenia kont                            #")
@Header("################################################################")
public class LinkingConfig extends OkaeriConfig {


    @Comment({""})
    @Comment({"ID roli którą bedzie otrzymywał użytkownik po połączeniu kont"})
    private long linkedRoleID = 1L;

    @Comment({""})
    @Comment({"ID roli którą bedzie otrzymywał użytkownik po połączeniu kont jeśli ma sie 5h czasu gry na serwerze"})
    private long linkedPlaytimeRoleID = 1L;

    @Comment({""})
    @Comment({"Czy użytkownik może pisać na kanale bez połączonych kont?"})
    private boolean canType = false;

    @Comment({""})
    @Comment({"Wiadomość która zostanie wysłana na pv do użytkownika bez połączonych kont"})
    private String cantTypeMessage = "Aby wysyłać wiadomości na tym kanale musisz mieć połączone konta Discord i Minecraft ";

    @Comment({""})
    @Comment({"Czy użytkownik może sam rozłączyć swoje konto Discord z kontem Minecraft?"})
    private boolean canUnlink = false;

    @Comment({""})
    @Comment({"Własne role, jak to wygląda?"})
    @Comment({"ID ROLI : IKONA"})
    @Comment({"Pozwala to na wyświetlenie custom rang"})
    private Map<Long, String> customRoles = new LinkedHashMap<>();

    @Comment({""})
    @Comment({"Czy używać custom roli na czacie"})
    private boolean useCustomRolesInChat = false;

    @Comment({""})
    @Comment({"Czy używać custom roli w nazwie gracza"})
    private boolean useCustomRolesInName = true;

    @Comment({""})
    @Comment({"Czy używać tylko custom roli"})
    private boolean onlyCustomRoles = false;

    public LinkingConfig() {
        if (this.customRoles.isEmpty()) {
            this.customRoles.put(1138353019667361833L, "&1OWNER&f");
        }
    }

    public long getLinkedRoleID() {
        return this.linkedRoleID;
    }

    public long getLinkedPlaytimeRoleID() {
        return this.linkedPlaytimeRoleID;
    }

    public boolean isCanType() {
        return this.canType;
    }

    public String getCantTypeMessage() {
        return this.cantTypeMessage;
    }

    public boolean isCanUnlink() {
        return this.canUnlink;
    }

    public Map<Long, String> getCustomRoles() {
        return this.customRoles;
    }

    public boolean isUseCustomRolesInChat() {
        return this.useCustomRolesInChat;
    }

    public boolean isUseCustomRolesInName() {
        return this.useCustomRolesInName;
    }

    public boolean isOnlyCustomRoles() {
        return this.onlyCustomRoles;
    }
}