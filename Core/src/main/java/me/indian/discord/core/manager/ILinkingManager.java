package me.indian.discord.core.manager;

import java.util.HashMap;
import java.util.List;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.Nullable;

public interface ILinkingManager {
    boolean isLinked(String name);

    boolean isLinked(long id);

    boolean linkAccount(String code, long id);

    void unLinkAccount(String name);

    void unLinkAccount(long id);

    boolean isCanUnlink();

    void addAccountToLink(String name, String code);

    long getXuidToLinkByCode(String code);

    long getXuidByID(long id);

    @Nullable
    String getNameByID(long id);

    long getIdByName(String name);

    @Nullable
    Member getMember(String name);

    boolean hasPermissions(String name, Permission permission);

    HashMap<Long, Long> getLinkedAccounts();

    List<Member> getLinkedMembers();

    void saveLinkedAccounts();
}