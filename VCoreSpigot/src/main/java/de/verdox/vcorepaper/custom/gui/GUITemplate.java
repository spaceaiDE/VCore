package de.verdox.vcorepaper.custom.gui;

import de.verdox.vcore.plugin.bukkit.BukkitPlugin;
import de.verdox.vcorepaper.VCorePaper;
import de.verdox.vcorepaper.custom.items.CustomItemManager;
import de.verdox.vcorepaper.custom.items.VCoreItem;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.index.qual.Positive;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class GUITemplate {

    public static void createSelectConfirmationGUI(BukkitPlugin bukkitPlugin, Player player, Function<Boolean, VCoreGUI.Response<?,?>> callback){
        VCoreItem yesItem = VCorePaper.getInstance().getCustomItemManager().createItemBuilder(Material.GREEN_STAINED_GLASS_PANE,1,"&aYes").buildItem();
        VCoreItem noItem = VCorePaper.getInstance().getCustomItemManager().createItemBuilder(Material.RED_STAINED_GLASS_PANE,1,"&cNo").buildItem();
        createSelectConfirmationGUI(bukkitPlugin, player, callback, "&eAre you sure?", yesItem, noItem);
    }

    public static void createSelectConfirmationGUI(BukkitPlugin bukkitPlugin, Player player, Function<Boolean, VCoreGUI.Response<?,?>> callback, String title, String yesTitle, String noTitle){
        VCoreItem yesItem = VCorePaper.getInstance().getCustomItemManager().createItemBuilder(Material.GREEN_STAINED_GLASS_PANE,1, yesTitle).buildItem();
        VCoreItem noItem = VCorePaper.getInstance().getCustomItemManager().createItemBuilder(Material.RED_STAINED_GLASS_PANE,1 ,noTitle).buildItem();
        createSelectConfirmationGUI(bukkitPlugin, player, callback, "&eAre you sure?", yesItem, noItem);
    }

    public static void createSelectConfirmationGUI(BukkitPlugin bukkitPlugin, Player player, Function<Boolean, VCoreGUI.Response<?,?>> callback, String title, VCoreItem yesItem, VCoreItem noItem){
        CustomItemManager customItemManager = VCorePaper.getInstance().getCustomItemManager();
        new VCoreGUI.Builder<String>()
                .plugin(bukkitPlugin)
                .title(ChatColor.translateAlternateColorCodes('&',title))
                .type(InventoryType.HOPPER)
                .content(objectContentBuilder -> {
                    objectContentBuilder.addContent(0,customItemManager.createItemBuilder(Material.BLACK_STAINED_GLASS_PANE,1,"")
                            .buildItem(),"");
                    objectContentBuilder.addContent(1, noItem,"");
                    objectContentBuilder.addContent(2,customItemManager.createItemBuilder(Material.BLACK_STAINED_GLASS_PANE,1,"")
                            .buildItem(),"");
                    objectContentBuilder.addContent(3,yesItem,"");
                    objectContentBuilder.addContent(4,customItemManager.createItemBuilder(Material.BLACK_STAINED_GLASS_PANE,1,"")
                            .buildItem(),"");
                })
                .onItemClick(stringVCoreGUIClick -> {
                    VCoreItem vCoreItem = stringVCoreGUIClick.getClickedItem();
                    if(vCoreItem.getDataHolder().getType().equals(Material.GREEN_STAINED_GLASS_PANE))
                        return callback.apply(true);
                    else
                        return callback.apply(false);
                })
                .open(player);
    }

    public static void selectMaterial(BukkitPlugin bukkitPlugin, Player player, Function<VCoreGUI.VCoreGUIClick<Material>, VCoreGUI.Response<?,?>> onClick, Runnable backCallback){
        new SelectionGUI<Material>("&eMaterial Selection",Arrays.asList(Material.values()),bukkitPlugin, ItemStack::new,1)
                .onClick(onClick)
                .onBack(backCallback)
                .toStringFunc(Enum::name)
                .open(player);
    }

    public static class SelectionGUI<T>{
        private String title;
        private List<T> objectList;
        private final BukkitPlugin bukkitPlugin;
        private final Function<T, ItemStack> objectToItemStack;
        private int page;
        private Supplier<List<T>> listUpdater;
        private String pattern;
        private Function<T,String> toStringFunc;
        private Function<VCoreGUI.VCoreGUIClick<T>, VCoreGUI.Response<?,?>> clickCallback;
        private Runnable backCallback;
        private final Map<VCoreItem, Function<VCoreGUI.VCoreGUIClick<T>, VCoreGUI.Response<?,?>>> additionalItems = new HashMap<>();

        public SelectionGUI(@Nonnull String title, @Nonnull List<T> objectList, @Nonnull BukkitPlugin bukkitPlugin, @Nonnull Function<T, ItemStack> objectToItemStack, @Positive int page){
            this.title = title;
            this.objectList = objectList;
            this.bukkitPlugin = bukkitPlugin;
            this.objectToItemStack = objectToItemStack;
            this.page = page;
        }

        public SelectionGUI<T> updateList(Supplier<List<T>> listUpdater){
            this.listUpdater = listUpdater;
            return this;
        }

        public SelectionGUI<T> addItem(VCoreItem vCoreItem, int slot, Function<VCoreGUI.VCoreGUIClick<T>, VCoreGUI.Response<?,?>> clickCallback){
            if(vCoreItem == null)
                throw new NullPointerException("VCoreItem can't be null!");
            if(clickCallback == null)
                throw new NullPointerException("clickCallback can't be null!");
            vCoreItem.getNBTCompound().setObject("vcore_gui_slot",slot);
            additionalItems.put(vCoreItem,clickCallback);
            return this;
        }

        public SelectionGUI<T> onClick(Function<VCoreGUI.VCoreGUIClick<T>, VCoreGUI.Response<?,?>> clickCallback){
            this.clickCallback = clickCallback;
            return this;
        }

        public SelectionGUI<T> onBack(Runnable backCallback){
            this.backCallback = backCallback;
            return this;
        }

        public SelectionGUI<T> toStringFunc(Function<T,String> toStringFunc){
            this.toStringFunc = toStringFunc;
            return this;
        }

        public SelectionGUI<T> withSearchPattern(String pattern, Function<T,String> toStringFunc){
            this.pattern = pattern;
            this.toStringFunc = toStringFunc;
            return this;
        }

        private void nextPage(){
            if(page+1 > getMaxPage())
                page = getMaxPage();
            else
                page++;
        }

        private void lastPage(){
            if(page-1 <= 0)
                page = 1;
            else
                page--;
        }

        private int getMaxPage(){
            long count = filterWithPattern().count();
            return (int) ((count / 45)+1);
        }

        private Stream<T> filterWithPattern(){
            return objectList.stream().filter(t -> {
                if(t == null)
                    return false;
                ItemStack stack = objectToItemStack.apply(t);
                if(stack == null || stack.getType().equals(Material.AIR) || stack.getType().isAir())
                    return false;
                if(pattern == null || pattern.isEmpty() || toStringFunc == null)
                    return true;
                String string = toStringFunc.apply(t);
                return string.toLowerCase().contains(pattern.toLowerCase());
            });
        }

        public void open(Player player){

            VCoreItem border = VCorePaper.getInstance().getCustomItemManager().createItemBuilder(Material.BLACK_STAINED_GLASS_PANE, 1, "&8").buildItem();
            VCoreItem nextPage = VCorePaper.getInstance().getCustomItemManager().createItemBuilder(Material.PAPER, 1, "&aNext page").buildItem();
            VCoreItem search = VCorePaper.getInstance().getCustomItemManager().createItemBuilder(Material.WRITTEN_BOOK, 1, "&eSearch").buildItem();
            VCoreItem lastPage = VCorePaper.getInstance().getCustomItemManager().createItemBuilder(Material.PAPER, 1, "&cLast page").buildItem();
            VCoreItem back = VCorePaper.getInstance().getCustomItemManager().createItemBuilder(Material.RED_STAINED_GLASS_PANE, 1, "&cBack").buildItem();

            new VCoreGUI.Builder<T>()
                    .plugin(bukkitPlugin)
                    .update()
                    .title(ChatColor.translateAlternateColorCodes('&',title))
                    .type(InventoryType.CHEST)
                    .size(54)
                    .content(contentBuilder -> {
                        if(listUpdater != null)
                            objectList = listUpdater.get();

                        AtomicInteger counter = new AtomicInteger(0);
                        filterWithPattern()
                                .skip((page-1)* 45L)
                                .limit(45)
                                .forEach(t -> {
                                    ItemStack stack = objectToItemStack.apply(t);
                                    if(stack == null)
                                        return;
                                    if(stack.getType().equals(Material.AIR))
                                        return;
                                    if(stack.getType().isEmpty())
                                        return;
                                    VCoreItem vCoreItem = VCorePaper.getInstance().getCustomItemManager().wrap(VCoreItem.class, stack);
                                    contentBuilder.addContent(counter.getAndIncrement(),vCoreItem,t);
                                });

                        if(page > 1)
                            contentBuilder.addContent(45,lastPage,null);
                        else
                            contentBuilder.addContent(45,border,null);
                        contentBuilder.addContent(46,border,null);
                        contentBuilder.addContent(47,back,null);
                        contentBuilder.addContent(48,border,null);
                        contentBuilder.addContent(49,border,null);
                        contentBuilder.addContent(50,border,null);
                        contentBuilder.addContent(51,search,null);
                        contentBuilder.addContent(52,border,null);
                        if(((page)*45)+1 < objectList.size() || counter.get() >= 44)
                            contentBuilder.addContent(53,nextPage,null);
                        else
                            contentBuilder.addContent(53,border,null);


                        additionalItems.forEach((vCoreItem, vCoreItemResponseFunction) -> {
                            if(!vCoreItem.getNBTCompound().hasKey("vcore_gui_slot"))
                                return;
                            int slot = vCoreItem.getNBTCompound().getInteger("vcore_gui_slot");
                            contentBuilder.removeItem(slot);
                            contentBuilder.addContent(slot,vCoreItem,null);
                        });
                    })
                    .onItemClick(vCoreGUIClick -> {
                        VCoreItem vCoreItem = vCoreGUIClick.getClickedItem();
                        if(additionalItems.containsKey(vCoreItem))
                            return additionalItems.get(vCoreItem).apply(vCoreGUIClick);
                        else if(vCoreItem.equals(nextPage))
                            nextPage();
                        else if(vCoreItem.equals(lastPage))
                            lastPage();
                        else if(vCoreItem.equals(back) && backCallback != null) {
                            backCallback.run();
                            return VCoreGUI.Response.nothing();
                        }
                        else if(vCoreItem.equals(search)){
                            return VCoreGUI.Response.input(s -> {
                                this.pattern = s;
                                return AnvilGUI.Response.close();
                            });
                        }
                        T object = vCoreGUIClick.getDataInItemStack();
                        if(object != null && clickCallback != null)
                            return clickCallback.apply(vCoreGUIClick);
                        else
                            return VCoreGUI.Response.nothing();
                    }).open(player);
        }
    }
}
