package me.indian.discord.core.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Header;

@Header("################################################################")
@Header("#           Ustawienia wiadomości                              #")
@Header("################################################################")
public class MessagesConfig extends OkaeriConfig {

    @Comment({""})
    @Comment({"Jak mają wyglądać wiadomości graczy gdy Paczka jest załadowana"})
    @Comment({"Można zmienić to na format Vanillowy gdy użyje się !format <true/false> "})
    @Comment({"<role> - rola z discord gdy użytkownik ma połączone konta"})
    @Comment({"<player> - nazwa gracza"})
    @Comment({"<message> - wiadomość gracza"})
    private boolean formatChat = true;
    private String chatMessageFormat = "&d<role>&r <player> »» <message>";

    @Comment({""})
    @Comment({"Czy pokazywać role gracza w jego nazwie"})
    private boolean showInName = true;

    @Comment({""})
    @Comment({"Część wyświetlona gdy ktoś odpowie na czyjąś wiadomość"})
    private String replyStatement = " (&dOdpowiada na&a: &a<author> &r»»&b <msg> &r)";

    @Comment({""})
    @Comment({"Część wyświetlana, gdy ktoś odpowiada na wiadomość od bota, aby odpowiedzi na wiadomości graczy wyglądały lepiej"})
    private String botReplyStatement = " (&dOdpowiada na&a: <msg> &r)";

    @Comment({""})
    @Comment({"Informacja o tym że wiadomość została edytowana"})
    private String edited = " (Edytowano)";

    @Comment({""})
    @Comment({"Informacja o tym że w wiadomości jest załącznik"})
    private String attachment = " (Załącznik) ";

    @Comment({""})
    @Comment({"Informacja o tym że wiadomość to webhook"})
    private String webhook = " (Webhook)";

    @Comment({""})
    @Comment({"Informacja o dołączeniu gracza"})
    private boolean sendJoinMessage = true;
    private String joinMessage = "Gracz **<name>** dołączył do gry";

    @Comment({""})
    @Comment({"Informacja o wyjściu gracza"})
    private boolean sendLeaveMessage = true;
    private String leaveMessage = "Gracz **<name>** opuścił gre";

    @Comment({""})
    @Comment({"Informacja o śmierci gracza"})
    private boolean sendDeathMessage = true;
    private String deathMessage = "Gracz **<name>** <deathMessage> <itemName>";

    @Comment({""})
    @Comment({"Wygląd wiadomości z Minecraft na Discord"})
    private boolean sendMinecraftToDiscordMessage = true;
    private String minecraftToDiscordMessage = "**<name>** »» <msg>";

    @Comment({""})
    @Comment({"Wygląd wiadomości z Discord na Minecraft "})
    private boolean sendDiscordToMinecraft = true;
    private String discordToMinecraftMessage = "&7[&bDiscord&r |&d <role>&7] &l&a<name>&r<reply> »» <msg>";

    @Comment({""})
    @Comment({"Informacja na discord o włączeniu servera"})
    private boolean sendEnabledMessage = true;
    private String enabledMessage = ":white_check_mark: Server włączony";

    @Comment({""})
    @Comment({"Informacja na discord gdy server się wyłączy"})
    private boolean sendDisabledMessage = true;
    private String disabledMessage = ":octagonal_sign: Server wyłączony";

    @Comment({""})
    @Comment({"Informacja na discord gdy backup zostanie utworzony"})
    private boolean sendBackupMessage = false;
    private String backupDoneMessage = "**Backup został utworzony!**";

    @Comment({""})
    @Comment({"Informacja na discord gdy backup nie zostanie utworzony"})
    private boolean sendBackupFailMessage = false;
    private String backupFailMessage = "**Nie udało się utworzyć backup z powodu!** \n ```<exception>```";

    @Comment({""})
    @Comment({"Informacja gdy server pobiera najnowszą wersje"})
    private boolean sendServerUpdateMessage = true;
    private String serverUpdate = "Wersja **<version>** jest pobierana a następnie zostanie załadowana , aktualna załadowana to **<current>**";

    @Comment({""})
    @Comment({"Informacja gdy server jest restartowany"})
    private String restartMessage = "**Server jest restartowany**";


    public boolean isFormatChat() {
        return this.formatChat;
    }

    public String getChatMessageFormat() {
        return this.chatMessageFormat;
    }

    public boolean isShowInName() {
        return this.showInName;
    }

    public String getReplyStatement() {
        return this.replyStatement;
    }

    public String getBotReplyStatement() {
        return this.botReplyStatement;
    }

    public String getEdited() {
        return this.edited;
    }

    public String getAttachment() {
        return this.attachment;
    }

    public String getWebhook() {
        return this.webhook;
    }

    public boolean isSendJoinMessage() {
        return this.sendJoinMessage;
    }

    public String getJoinMessage() {
        return this.joinMessage;
    }

    public boolean isSendLeaveMessage() {
        return this.sendLeaveMessage;
    }

    public String getLeaveMessage() {
        return this.leaveMessage;
    }

    public boolean isSendDeathMessage() {
        return this.sendDeathMessage;
    }

    public String getDeathMessage() {
        return this.deathMessage;
    }

    public boolean isSendMinecraftToDiscordMessage() {
        return this.sendDiscordToMinecraft;
    }

    public String getMinecraftToDiscordMessage() {
        return this.minecraftToDiscordMessage;
    }

    public boolean isSendDiscordToMinecraft() {
        return this.sendDiscordToMinecraft;
    }

    public String getDiscordToMinecraftMessage() {
        return this.discordToMinecraftMessage;
    }

    public boolean isSendEnabledMessage() {
        return this.sendEnabledMessage;
    }

    public String getEnabledMessage() {
        return this.enabledMessage;
    }

    public boolean isSendDisabledMessage() {
        return this.sendDisabledMessage;
    }

    public String getDisabledMessage() {
        return this.disabledMessage;
    }

    public boolean isSendBackupMessage() {
        return this.sendBackupMessage;
    }

    public String getBackupDoneMessage() {
        return this.backupDoneMessage;
    }

    public boolean isSendBackupFailMessage() {
        return this.sendBackupFailMessage;
    }

    public String getBackupFailMessage() {
        return this.backupFailMessage;
    }

    public boolean isSendServerUpdateMessage() {
        return this.sendServerUpdateMessage;
    }

    public String getServerUpdate() {
        return this.serverUpdate;
    }

    public String getRestartMessage() {
        return this.restartMessage;
    }
}