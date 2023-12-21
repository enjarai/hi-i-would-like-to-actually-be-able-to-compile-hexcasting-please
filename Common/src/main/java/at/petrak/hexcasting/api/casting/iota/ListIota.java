package at.petrak.hexcasting.api.casting.iota;

import at.petrak.hexcasting.api.casting.SpellList;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.api.utils.HexUtils;
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a <i>wrapper</i> for {@link SpellList}.
 */
public class ListIota extends Iota {
    public ListIota(@NotNull SpellList list) {
        super(HexIotaTypes.LIST, list);
    }

    public ListIota(@NotNull List<Iota> list) {
        this(new SpellList.LList(list));
    }

    public SpellList getList() {
        return (SpellList) this.payload;
    }

    @Override
    public boolean isTruthy() {
        return this.getList().getNonEmpty();
    }

    @Override
    public boolean toleratesOther(Iota that) {
        if (!typesMatch(this, that)) {
            return false;
        }
        var a = this.getList();
        if (!(that instanceof ListIota list)) {
            return false;
        }
        var b = list.getList();

        SpellList.SpellListIterator aIter = a.iterator(), bIter = b.iterator();
        for (; ; ) {
            if (!aIter.hasNext() && !bIter.hasNext()) {
                // we ran out together!
                return true;
            }
            if (aIter.hasNext() != bIter.hasNext()) {
                // one remains full before the other
                return false;
            }
            Iota x = aIter.next(), y = bIter.next();
            if (!Iota.tolerates(x, y)) {
                return false;
            }
        }
    }

    @Override
    public @NotNull Tag serialize() {
        var out = new ListTag();
        for (var subdatum : this.getList()) {
            out.add(IotaType.serialize(subdatum));
        }
        return out;
    }

    @Override
    public @Nullable Iterable<Iota> subIotas() {
        return this.getList();
    }

    public static IotaType<ListIota> TYPE = new IotaType<ListIota>() {
        @Nullable
        @Override
        public ListIota deserialize(Tag tag, ServerLevel world) throws IllegalArgumentException {
            var listTag = HexUtils.downcast(tag, ListTag.TYPE);
            var out = new ArrayList<Iota>(listTag.size());

            for (var sub : listTag) {
                var csub = HexUtils.downcast(sub, CompoundTag.TYPE);
                var subiota = IotaType.deserialize(csub, world);
                if (subiota == null) {
                    return null;
                }
                out.add(subiota);
            }

            return new ListIota(out);
        }

        @Override
        public Component display(Tag tag) {
            var out = Component.empty();
            var list = HexUtils.downcast(tag, ListTag.TYPE);
            for (int i = 0; i < list.size(); i++) {
                Tag sub = list.get(i);
                var csub = HexUtils.downcast(sub, CompoundTag.TYPE);

                out.append(IotaType.getDisplay(csub));

                if (i < list.size() - 1) {
                    CompoundTag nextCsub = HexUtils.downcast(list.get(i+1), CompoundTag.TYPE);
                    if(IotaType.getTypeFromTag(csub) == HexIotaTypes.PATTERN && IotaType.getTypeFromTag(nextCsub) == HexIotaTypes.PATTERN) {
                        out.append(" ");
                    } else {
                        out.append(", ");
                    }
                }
            }

            String copyText = getCopyText(list);

            MutableComponent listText = Component.translatable("hexcasting.tooltip.list_contents", out).withStyle(ChatFormatting.DARK_PURPLE).copy();
            Style clickEventStyle = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, copyText));
            listText.setStyle(clickEventStyle.applyTo(listText.getStyle()));
            return listText;
        }

        // this is fine
        public String getCopyText(Tag element){
            String copyText = "";
            if(element.getType() == ListTag.TYPE){ // handle list
                ListTag list = HexUtils.downcast(element, ListTag.TYPE);
                copyText = "[";
                for (int i = 0; i < list.size(); i++) {
                    Tag sub = list.get(i);
                    copyText += getCopyText(sub);

                    if (i < list.size() - 1) {
                        copyText += (", ");
                    }
                }
                copyText += "]";
            } else { // handle not list
                CompoundTag csub = HexUtils.downcast(element, CompoundTag.TYPE);
                IotaType<?> type = IotaType.getTypeFromTag(csub);
                if(type == PatternIota.TYPE){
                    // handle pattern:
                    CompoundTag tagData = csub.getCompound(HexIotaTypes.KEY_DATA);
                    if(tagData == null || tagData.isEmpty()) return copyText;
                    HexPattern pattern = HexPattern.fromNBT(tagData);
                    // HexGloop.logPrint(csub.toString() + " => " + pattern.toString() + "\n");
                    copyText += ("<" + pattern.getStartDir().toString().replace("_", "").toLowerCase() + "," + pattern.anglesSignature() + ">");
                } else if(type == ListIota.TYPE){
                    // kinda handle lists again ? mostly just get the data out of here and call it as if we were passing it to the display method
                    var data = csub.get(HexIotaTypes.KEY_DATA);
                    return getCopyText(data);
                } else {
                    copyText += IotaType.getDisplay(csub).getString();
                }
            }

            return copyText;
        }

        @Override
        public int color() {
            return 0xff_aa00aa;
        }
    };
}
