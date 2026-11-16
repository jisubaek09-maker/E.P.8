package com.bshs.geohint.listener;

import com.bshs.geohint.GeoHintPlugin;
import com.bshs.geohint.model.PlayerState;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * 우클릭 처리
 *  1) 화석(로드스톤) 우클릭 → 화석 기록
 *  2) /hint 직후 우클릭 → 손에 든 시료 분석 (한 번의 시도로 끝)
 *  3) 나침반 우클릭은 아무 일도 일어나지 않게 막는다 (다른 자석석에 다시 연결되는 것 방지)
 */
public class InteractListener implements Listener {

    private final GeoHintPlugin plugin;

    public InteractListener(GeoHintPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        Block clicked = e.getClickedBlock();

        if (a == Action.RIGHT_CLICK_BLOCK && clicked != null && clicked.getType() == Material.LODESTONE) {
            if (plugin.fossils().discover(p, clicked)) {
                e.setCancelled(true);
                return;
            }
        }

        PlayerState st = plugin.data().state(p);
        if (st.hintWaiting) {
            e.setCancelled(true);
            plugin.hints().analyze(p, e.getItem());
            return;
        }

        ItemStack item = e.getItem();
        if (item != null && item.getType() == Material.COMPASS) {
            e.setCancelled(true);
        }
    }
}
