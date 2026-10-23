package dev.svarcovv.hiderflags.item;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class HiddenItemListenerTest {

    private Plugin plugin;
    private ItemHider hider;
    private HiddenItemListener listener;
    private BukkitScheduler scheduler;
    private Player player;

    @BeforeEach
    void setUp() {
        plugin = mock(Plugin.class);
        hider = mock(ItemHider.class);
        Server server = mock(Server.class);
        scheduler = mock(BukkitScheduler.class);
        player = mock(Player.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        listener = new HiddenItemListener(plugin, hider);
    }

    @Test
    void refreshesAfterClickIncludingCursor() {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        PlayerInventory inventory = mock(PlayerInventory.class);
        Inventory top = mock(Inventory.class);
        InventoryView view = mock(InventoryView.class);
        ItemStack cursor = mock(ItemStack.class);
        when(player.isOnline()).thenReturn(true);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getOpenInventory()).thenReturn(view);
        when(view.getTopInventory()).thenReturn(top);
        when(player.getItemOnCursor()).thenReturn(cursor);
        when(hider.isHidden(cursor)).thenReturn(true);
        when(hider.hide(cursor)).thenReturn(true);
        listener.onClick(event);
        verifyNoInteractions(hider);
        scheduledTask().run();
        verify(hider).refresh(inventory);
        verify(hider).refresh(top);
        verify(player).setItemOnCursor(cursor);
    }

    @Test
    void skipsPlayerWhoDisconnected() {
        PlayerJoinEvent event = mock(PlayerJoinEvent.class);
        when(event.getPlayer()).thenReturn(player);
        listener.onJoin(event);
        scheduledTask().run();
        verifyNoInteractions(hider);
    }

    @Test
    void schedulesDragRefresh() {
        InventoryDragEvent event = mock(InventoryDragEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        listener.onDrag(event);
        verify(scheduler).runTask(eq(plugin), any(Runnable.class));
        verifyNoInteractions(hider);
    }

    @Test
    void schedulesOpenRefresh() {
        InventoryOpenEvent event = mock(InventoryOpenEvent.class);
        when(event.getPlayer()).thenReturn(player);
        listener.onOpen(event);
        verify(scheduler).runTask(eq(plugin), any(Runnable.class));
        verifyNoInteractions(hider);
    }

    @Test
    void schedulesEnchantRefresh() {
        EnchantItemEvent event = mock(EnchantItemEvent.class);
        when(event.getEnchanter()).thenReturn(player);
        listener.onEnchant(event);
        verify(scheduler).runTask(eq(plugin), any(Runnable.class));
        verifyNoInteractions(hider);
    }

    @Test
    void updatesHiddenAnvilResult() {
        PrepareAnvilEvent event = mock(PrepareAnvilEvent.class);
        ItemStack result = hiddenResult();
        when(event.getResult()).thenReturn(result);
        listener.onAnvil(event);
        verify(event).setResult(result);
    }

    @Test
    void updatesHiddenSmithingResult() {
        PrepareSmithingEvent event = mock(PrepareSmithingEvent.class);
        ItemStack result = hiddenResult();
        when(event.getResult()).thenReturn(result);
        listener.onSmithing(event);
        verify(event).setResult(result);
    }

    @Test
    void updatesHiddenGrindstoneResult() {
        PrepareGrindstoneEvent event = mock(PrepareGrindstoneEvent.class);
        ItemStack result = hiddenResult();
        when(event.getResult()).thenReturn(result);
        listener.onGrindstone(event);
        verify(event).setResult(result);
    }

    @Test
    void leavesForeignResultUntouched() {
        PrepareAnvilEvent event = mock(PrepareAnvilEvent.class);
        ItemStack result = mock(ItemStack.class);
        when(event.getResult()).thenReturn(result);
        listener.onAnvil(event);
        verify(hider, never()).hide(any());
        verify(event, never()).setResult(any());
    }

    private ItemStack hiddenResult() {
        ItemStack result = mock(ItemStack.class);
        when(hider.isHidden(result)).thenReturn(true);
        when(hider.hide(result)).thenReturn(true);
        return result;
    }

    private Runnable scheduledTask() {
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).runTask(eq(plugin), task.capture());
        return task.getValue();
    }
}
