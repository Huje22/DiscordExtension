package pl.indianbartonka.discord.core.manager;

public interface IStatsChannelsManager {
    void setTpsCount(double tps);

    void onShutdown();
}
