package at.petrak.hexcasting.fabric.mixin.textpatterns;

import at.petrak.hexcasting.api.casting.iota.PatternIota;
import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.client.render.RenderLib;
import at.petrak.hexcasting.common.lib.HexItems;
import at.petrak.hexcasting.gloopy.PatternStyle;
import com.google.gson.*;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import kotlin.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;


// heavy influence/mild copying from https://github.com/Snownee/TextAnimator/blob/1.19.2-fabric/src/main/java/snownee/textanimator/mixin/StyleMixin.java
// note that all of style's style.withProperty methods won't preserve the pattern
@Mixin(Style.class)
public class MixinPatternStyle implements PatternStyle {

    private HexPattern pattern = null;
    private List<Vec2> zappyPoints = null;
    private List<Vec2> pathfinderDots = null;
    private float patScale; // maybe want to have this exposed somewhere?
    private boolean _isHidden = false;

    private static final String PATTERN_KEY = "hexPatternStyle";
    private static final String PATTERN_START_DIR_KEY = "startDir";
    private static final String PATTERN_ANGLE_SIG_KEY = "angleSig";
    private static final String PATTERN_HIDDEN_KEY = "isHidden";
    private static final float RENDER_SIZE = 128f;

    @Override
    public HexPattern getPattern() {
        return pattern;
    }

    @Override
    public boolean isHidden(){
        return _isHidden;
    }

    @Override
    public Style setPattern(HexPattern pattern) {
        // yoinked from PatternTooltipComponent
        this.pattern = pattern;
        Pair<Float, List<Vec2>> pair = RenderLib.getCenteredPattern(pattern, RENDER_SIZE, RENDER_SIZE, 16f);
        this.patScale = pair.getFirst();
        List<Vec2> dots = pair.getSecond();
        this.zappyPoints = RenderLib.makeZappy(
            dots, RenderLib.findDupIndices(pattern.positions()),
            10, 0.8f, 0f, 0f, RenderLib.DEFAULT_READABILITY_OFFSET, RenderLib.DEFAULT_LAST_SEGMENT_LEN_PROP,
            0.0);
        this.pathfinderDots = dots;
        return (Style)(Object)this;
    }

    @Override
    public Style withPattern(HexPattern pattern, boolean withPatternHoverEvent, boolean withPatternClickEvent) {
        Style style = (Style)(Object)this;

        if (withPatternHoverEvent) {
            StringBuilder bob = new StringBuilder();
            bob.append(pattern.getStartDir());
            var sig = pattern.anglesSignature();
            if (!sig.isEmpty()) {
                bob.append(" ");
                bob.append(sig);
            }
//            Action action = PatternRegistryManifest.matchPattern(pattern, );
            
            Component hoverText = Component.translatable("hexcasting.tooltip.pattern_iota",
                Component.literal(bob.toString())).withStyle(ChatFormatting.WHITE);
//            if(action != null){
//                hoverText = action.getDisplayName().copy().formatted(Formatting.UNDERLINE).append("\n").append(hoverText);
//            }
            ItemStack scrollStack = new ItemStack(HexItems.SCROLL_LARGE);
            scrollStack.setHoverName(hoverText);
            HexItems.SCROLL_LARGE.writeDatum(scrollStack, new PatternIota(pattern));
            style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(scrollStack)));
        }
        if(withPatternClickEvent){
            style = style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, "<" +
                pattern.getStartDir().toString().replace("_", "").toLowerCase() + "," + pattern.anglesSignature() + ">"));
        }
        return style.applyTo(PatternStyle.fromPattern(pattern));
    }

    
    @Override
    public Style setHidden(boolean hidden){
        this._isHidden = hidden;
        return (Style)(Object)this;
    }

    @Override
    public Style withHidden(boolean hidden){
        return ((Style)(Object)this).applyTo(((PatternStyle)Style.EMPTY.withBold(null)).setHidden(hidden));
    }

    @Override
    public List<Vec2> getZappyPoints(){
        return zappyPoints;
    }

    @Override
    public List<Vec2> getPathfinderDots(){
        return pathfinderDots;
    }

    @Inject(at=@At("TAIL"), method="<init>")
    private void HexPatDefaultStyleConstructor(TextColor textColor, Boolean boolean_, Boolean boolean2, Boolean boolean3, Boolean boolean4, Boolean boolean5, ClickEvent clickEvent, HoverEvent hoverEvent, String string, ResourceLocation resourceLocation, CallbackInfo ci){
        this.pattern = null;
        this.zappyPoints = null;
        this.pathfinderDots = null;
        this._isHidden = false;
        this.patScale = 1f;
    }

    @Inject(method = "applyTo", at = @At("RETURN"), cancellable = true)
	private void HexPatStyWithParent(Style parent, CallbackInfoReturnable<Style> cir) {
        Style rstyle = cir.getReturnValue();
        if(this.getPattern() != null){
            ((PatternStyle) rstyle).setPattern(this.getPattern());
        } else { // no pattern on this style, try falling back to inherit parent
            HexPattern parentPattern = ((PatternStyle) parent).getPattern();
            if(parentPattern != null){
                ((PatternStyle) rstyle).setPattern(parentPattern);
            }
        }
        // i guess?
        if(this.isHidden() || ((PatternStyle) parent).isHidden()){
            ((PatternStyle) rstyle).setHidden(true);
        }
		cir.setReturnValue(rstyle);
	}

	@Inject(method = "equals", at = @At("HEAD"), cancellable = true)
	private void HexPatStyEquals(Object obj, CallbackInfoReturnable<Boolean> cir) {
		if (this != obj && (obj instanceof PatternStyle style)) {
			if (!Objects.equals(this.getPattern(), style.getPattern())) {
				cir.setReturnValue(false);
			}
            if(this.isHidden() != style.isHidden()){
                cir.setReturnValue(false);
            }
		}
	}

	@Mixin(Style.Serializer.class)
	public static class MixinPatternStyleSerializer {
		@ModifyReturnValue(method = "deserialize", at = @At("RETURN"))
		private Object HexPatStyDeserialize(Object initialStyle, JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) {
			if (!jsonElement.isJsonObject() || initialStyle == null) {
				return initialStyle;
			}
			JsonObject json = jsonElement.getAsJsonObject();
			if (!json.has("hexPatternStyle")) {
				return initialStyle;
			}
            Boolean hiddenFromJson = GsonHelper.isBooleanValue(json, PATTERN_HIDDEN_KEY) ? GsonHelper.getAsBoolean(json, PATTERN_HIDDEN_KEY) : false;
            
            JsonObject patternObj = GsonHelper.getAsJsonObject(json, PATTERN_KEY);
            
            String startDirString = GsonHelper.isStringValue(patternObj, PATTERN_START_DIR_KEY) ? GsonHelper.getAsString(patternObj, PATTERN_START_DIR_KEY) : null;
            String angleSigString = GsonHelper.isStringValue(patternObj, PATTERN_ANGLE_SIG_KEY) ? GsonHelper.getAsString(patternObj, PATTERN_ANGLE_SIG_KEY) : null;

            if(startDirString == null || angleSigString == null) return initialStyle;

            HexDir startDir = HexDir.fromString(startDirString);
            HexPattern pattern = HexPattern.fromAngles(angleSigString, startDir);
            return ((PatternStyle) ((PatternStyle) initialStyle).withPattern(pattern)).setHidden(hiddenFromJson);
		}

		@ModifyReturnValue(method = "serialize", at = @At("RETURN"))
		private JsonElement HexPatStySerialize(JsonElement jsonElement, Object style, Type type, JsonSerializationContext jsonSerializationContext) {
			PatternStyle pStyle = (PatternStyle) style;
			if (jsonElement == null || !jsonElement.isJsonObject() || pStyle.getPattern() == null) {
				return jsonElement;
			}
			JsonObject json = jsonElement.getAsJsonObject();
            json.add(PATTERN_HIDDEN_KEY, new JsonPrimitive(pStyle.isHidden()));
            JsonObject patternObj = new JsonObject();
            patternObj.addProperty(PATTERN_START_DIR_KEY, pStyle.getPattern().getStartDir().toString());
            patternObj.addProperty(PATTERN_ANGLE_SIG_KEY, pStyle.getPattern().anglesSignature());
			json.add(PATTERN_KEY, patternObj);
            return json;
		}
	}

    // meant to be called at the 
    private Style keepPattern(Style returnedStyle){
        PatternStyle pStyle = (PatternStyle)(Object)this;
        if(pStyle.getPattern() != null){
            ((PatternStyle) returnedStyle).setPattern(pStyle.getPattern());
        }
        if(pStyle.isHidden()){
            ((PatternStyle) returnedStyle).setHidden(true);
        }
        return returnedStyle;
    }

    @Inject(method = "withColor(Lnet/minecraft/network/chat/TextColor;)Lnet/minecraft/network/chat/Style;",
    at=@At("RETURN"), cancellable = true)
    private void fixWithColor(TextColor color, CallbackInfoReturnable<Style> cir){
        cir.setReturnValue(keepPattern(cir.getReturnValue()));
    }

    @Inject(method = "withBold",
    at=@At("RETURN"), cancellable = true)
    private void fixWithBold(Boolean boldBool, CallbackInfoReturnable<Style> cir){
        cir.setReturnValue(keepPattern(cir.getReturnValue()));
    }
    
    @Inject(method = "withItalic",
    at=@At("RETURN"), cancellable = true)
    private void fixWithItalic(Boolean boldBool, CallbackInfoReturnable<Style> cir){
        cir.setReturnValue(keepPattern(cir.getReturnValue()));
    }

    @Inject(method = "withUnderlined",
    at=@At("RETURN"), cancellable = true)
    private void fixWithUnderline(Boolean boldBool, CallbackInfoReturnable<Style> cir){
        cir.setReturnValue(keepPattern(cir.getReturnValue()));
    }

    @Inject(method = "withStrikethrough",
    at=@At("RETURN"), cancellable = true)
    private void fixWithStrikethrough(Boolean boldBool, CallbackInfoReturnable<Style> cir){
        cir.setReturnValue(keepPattern(cir.getReturnValue()));
    }

    @Inject(method = "withObfuscated",
    at=@At("RETURN"), cancellable = true)
    private void fixWithObfuscated(Boolean boldBool, CallbackInfoReturnable<Style> cir){
        cir.setReturnValue(keepPattern(cir.getReturnValue()));
    }

    @Inject(method = "withClickEvent",
    at=@At("RETURN"), cancellable = true)
    private void fixWithClickEvent(ClickEvent clickEvent, CallbackInfoReturnable<Style> cir){
        cir.setReturnValue(keepPattern(cir.getReturnValue()));
    }

    @Inject(method = "withHoverEvent",
    at=@At("RETURN"), cancellable = true)
    private void fixWithHoverEvent(HoverEvent hoverEvent, CallbackInfoReturnable<Style> cir){
        cir.setReturnValue(keepPattern(cir.getReturnValue()));
    }

    @Inject(method = "withInsertion",
    at=@At("RETURN"), cancellable = true)
    private void fixWithInsertion(String insertionString, CallbackInfoReturnable<Style> cir){
        cir.setReturnValue(keepPattern(cir.getReturnValue()));
    }

    @Inject(method = "withFont",
    at=@At("RETURN"), cancellable = true)
    private void fixWithFont(ResourceLocation fontID, CallbackInfoReturnable<Style> cir){
        cir.setReturnValue(keepPattern(cir.getReturnValue()));
    }

    @Inject(method = "applyFormat",
    at=@At("RETURN"), cancellable = true)
    private void fixWithFormatting(ChatFormatting chatFormatting, CallbackInfoReturnable<Style> cir){
        cir.setReturnValue(keepPattern(cir.getReturnValue()));
    }

    @Inject(method = "applyLegacyFormat",
    at=@At("RETURN"), cancellable = true)
    private void fixWithExclusiveFormatting(ChatFormatting chatFormatting, CallbackInfoReturnable<Style> cir){
        cir.setReturnValue(keepPattern(cir.getReturnValue()));
    }




}
