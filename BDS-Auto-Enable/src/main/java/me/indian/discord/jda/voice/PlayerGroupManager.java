package me.indian.discord.jda.voice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import me.indian.bds.player.position.Position;
import me.indian.discord.jda.voice.component.Group;
import me.indian.discord.jda.voice.component.VoiceChatMember;
import net.dv8tion.jda.api.entities.Member;

public class PlayerGroupManager {

    private final Map<String, VoiceChatMember> voiceChatMembers;
    private final Map<String, Group> groupMap;
    private final Lock removeMembersLock, groupRemoveLock;

    public PlayerGroupManager() {
        this.voiceChatMembers = new HashMap<>();
        this.groupMap = new HashMap<>();
        this.removeMembersLock = new ReentrantLock();
        this.groupRemoveLock = new ReentrantLock();
    }

    public void addVoiceChatMember(final VoiceChatMember voiceChatMember) {
        this.voiceChatMembers.put(voiceChatMember.getName(), voiceChatMember);
    }

    public void removeVoiceChatMember(final VoiceChatMember voiceChatMember) {
        this.removeMembersLock.lock();

        try {
            this.voiceChatMembers.remove(voiceChatMember.getName());
        } finally {
            this.removeMembersLock.unlock();
        }
    }

    public List<Group> createGroups(final int maxDistance) {
        if (!this.voiceChatMembers.isEmpty()) {
            for (final VoiceChatMember voiceChatMember : this.voiceChatMembers.values()) {
                this.removeFromGroups(voiceChatMember);
                final Group group = this.findOrCreateGroup(voiceChatMember, maxDistance);
                group.addVoiceMember(voiceChatMember);
                this.groupMap.put(group.getId(), group);
            }
        }
        return this.groupMap.values().stream().toList();
    }

    public void removeGroup(final Group group) {
        this.groupRemoveLock.lock();

        try {
            this.groupMap.remove(group.getId());
        } finally {
            this.groupRemoveLock.unlock();
        }
    }

    public void removeFromGroups(final VoiceChatMember voiceChatMember) {
        for (final Group group : this.groupMap.values()) {
            if (group.getVoiceChatMembers().contains(voiceChatMember)) {
                group.removeVoiceMember(voiceChatMember);
            }
        }
    }

    public VoiceChatMember getVoiceChatMemberByMember(final Member member) {
        return this.voiceChatMembers.values().stream()
                .filter(voiceChatMember -> voiceChatMember.getMember().getIdLong() == member.getIdLong())
                .findFirst().orElse(null);
    }

    private Group findOrCreateGroup(final VoiceChatMember voiceChatMember, final int maxDistance) {
        for (final Group group : this.groupMap.values()) {
            if (this.isWithinDistance(voiceChatMember, group.getVoiceChatMembers(), maxDistance)) {
                return group;
            }
        }

        final Group newGroup = new Group(UUID.randomUUID().toString());
        this.groupMap.put(newGroup.getId(), newGroup);
        return newGroup;
    }

    private boolean isWithinDistance(final VoiceChatMember voiceChatMember, final List<VoiceChatMember> voiceChatMembers, final int maxDistance) {
        for (final VoiceChatMember member : voiceChatMembers) {
            if (member == null || voiceChatMember == null) continue;

            final Position memberPos = member.getPosition();
            final Position neighborPos = voiceChatMember.getPosition();

            if (memberPos.dimension() != neighborPos.dimension()) return false;
            if (this.calculateDistance(memberPos, neighborPos) <= maxDistance) return true;
        }
        return false;
    }

    private double calculateDistance(final Position memberPos, final Position neighborPos) {
        final double xDistance = memberPos.x() - neighborPos.x();
        final double yDistance = memberPos.y() - neighborPos.y();
        final double zDistance = memberPos.z() - neighborPos.z();

        return Math.sqrt(xDistance * xDistance + yDistance * yDistance + zDistance * zDistance);
    }

    public Map<String, Group> getGroupMap() {
        return this.groupMap;
    }
}