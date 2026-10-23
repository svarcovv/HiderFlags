package dev.svarcovv.hiderflags.command;

import dev.svarcovv.hiderflags.item.ItemHider;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class HideCommandTest {

    private ItemHider hider;
    private HideCommand executor;
    private Player player;
    private PlayerInventory inventory;
    private Command command;
    private AtomicReference<Component> message;

    @BeforeEach
    void setUp() {
        hider = mock(ItemHider.class);
        executor = new HideCommand(hider);
        player = mock(Player.class);
        inventory = mock(PlayerInventory.class);
        command = mock(Command.class);
        message = new AtomicReference<>();
        when(player.getInventory()).thenReturn(inventory);
        when(player.hasPermission("hiderflags.use")).thenReturn(true);
        when(command.getName()).thenReturn("hidehand");
        doAnswer(call -> {
            message.set(call.getArgument(0));
            return null;
        }).when(player).sendMessage(any(Component.class));
    }

    @Test
    void rejectsConsole() {
        CommandSender console = mock(CommandSender.class);
        assertTrue(executor.onCommand(console, command, "hidehand", new String[0]));
        verify(console).sendMessage(any(Component.class));
        verifyNoInteractions(hider);
    }

    @Test
    void requiresPermission() {
        when(player.hasPermission("hiderflags.use")).thenReturn(false);
        assertTrue(executor.onCommand(player, command, "hidehand", new String[0]));
        assertEquals("☠ | У вас нет прав на выполнение этой команды", plainMessage());
        verifyNoInteractions(hider);
    }

    @Test
    void rejectsArguments() {
        assertTrue(executor.onCommand(player, command, "hidehand", new String[]{"extra"}));
        assertEquals("☠ | Использование: /hidehand", plainMessage());
        verifyNoInteractions(hider);
    }

    @Test
    void rejectsEmptyHand() {
        ItemStack air = mock(ItemStack.class);
        when(air.isEmpty()).thenReturn(true);
        when(inventory.getItemInMainHand()).thenReturn(air);
        assertTrue(executor.onCommand(player, command, "hidehand", new String[0]));
        assertEquals("☠ | Вы должны держать предмет в руке", plainMessage());
        verifyNoInteractions(hider);
    }

    @Test
    void hidesHeldItemAndWritesItBack() {
        ItemStack item = mock(ItemStack.class);
        when(inventory.getItemInMainHand()).thenReturn(item);
        assertTrue(executor.onCommand(player, command, "hidehand", new String[0]));
        verify(hider).hide(item);
        verify(inventory).setItemInMainHand(item);
        assertEquals("☠ | Флаги предмета успешно скрыты", plainMessage());
    }

    @Test
    void reportsOnlyChangedInventorySlots() {
        when(command.getName()).thenReturn("hideinv");
        when(hider.hideInventory(inventory)).thenReturn(3);
        assertTrue(executor.onCommand(player, command, "hideinv", new String[0]));
        verify(hider).hideInventory(inventory);
        assertEquals("☠ | Флаги скрыты у 3 предметов", plainMessage());
    }

    @Test
    void handlesUnchangedInventory() {
        when(command.getName()).thenReturn("hideinv");
        assertTrue(executor.onCommand(player, command, "hideinv", new String[0]));
        assertEquals("☠ | Флаги скрыты у 0 предметов", plainMessage());
    }

    @Test
    void doesNotSuggestPlayerNames() {
        assertTrue(executor.onTabComplete(player, command, "hidehand", new String[]{""}).isEmpty());
    }

    @Test
    void rejectsUnknownCommand() {
        when(command.getName()).thenReturn("unknown");
        assertFalse(executor.onCommand(player, command, "unknown", new String[0]));
        verifyNoInteractions(hider);
    }

    private String plainMessage() {
        return PlainTextComponentSerializer.plainText().serialize(message.get());
    }
}
