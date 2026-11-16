package com.bshs.geohint.ui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 화면 중앙 알림을 플레이어마다 줄 세워 하나씩 보여준다.
 * (두 알림이 동시에 오면 앞의 것을 덮어써 깜빡이는 문제를 막는다)
 */
public class TitleQueue {

    /** in/stay/out 은 tick (20 tick = 1초) */
    public record Frame(Component title, Component sub, int in, int stay, int out,
                        String sound, float pitch) {

        public static Frame of(Component title, Component sub, int stay) {
            return new Frame(title, sub, 5, stay, 10, null, 1f);
        }

        public static Frame of(Component title, Component sub, int stay, String sound, float pitch) {
            return new Frame(title, sub, 5, stay, 10, sound, pitch);
        }

        /** 애니메이션 프레임 (페이드 없음) */
        public static Frame anim(Component title, Component sub, int stay, String sound, float pitch) {
            return new Frame(title, sub, 0, stay, 0, sound, pitch);
        }

        public int busy() {
            return in + stay;
        }
    }

    private final Map<UUID, Deque<Frame>> queues = new HashMap<>();
    private final Map<UUID, Long> busyUntil = new HashMap<>();
    private long tick = 0;

    public void start(Plugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, this::run, 1L, 1L);
    }

    private void run() {
        tick++;
        for (Player p : Bukkit.getOnlinePlayers()) {
            Deque<Frame> q = queues.get(p.getUniqueId());
            if (q == null || q.isEmpty()) continue;
            if (tick < busyUntil.getOrDefault(p.getUniqueId(), 0L)) continue;
            Frame f = q.poll();
            show(p, f);
            busyUntil.put(p.getUniqueId(), tick + f.busy());
        }
    }

    private static void show(Player p, Frame f) {
        Title.Times times = Title.Times.times(ticks(f.in()), ticks(f.stay()), ticks(f.out()));
        p.showTitle(Title.title(
                f.title() == null ? Component.empty() : f.title(),
                f.sub() == null ? Component.empty() : f.sub(),
                times));
        if (f.sound() != null) Ui.play(p, f.sound(), f.pitch());
    }

    private static Duration ticks(int t) {
        return Duration.ofMillis(t * 50L);
    }

    public void push(Player p, Frame... frames) {
        Deque<Frame> q = queues.computeIfAbsent(p.getUniqueId(), k -> new ArrayDeque<>());
        for (Frame f : frames) q.add(f);
    }

    public void pushAll(Collection<? extends Player> players, Frame... frames) {
        for (Player p : players) push(p, frames);
    }

    /** 대기 중인 알림을 지우고 바로 보여준다 (카운트다운·채점 연출용) */
    public void now(Player p, Frame... frames) {
        clear(p);
        push(p, frames);
    }

    public void nowAll(Collection<? extends Player> players, Frame... frames) {
        for (Player p : players) now(p, frames);
    }

    public void clear(Player p) {
        Deque<Frame> q = queues.get(p.getUniqueId());
        if (q != null) q.clear();
        busyUntil.put(p.getUniqueId(), 0L);
    }
}
