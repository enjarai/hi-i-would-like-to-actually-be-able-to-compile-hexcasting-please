package at.petrak.hexcasting.fabric.mixin.textpatterns;

import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.client.render.RenderLib;
import at.petrak.hexcasting.gloopy.Glooptastic;
import at.petrak.hexcasting.gloopy.PatternStyle;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.Tesselator;
import kotlin.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FastColor;
import net.minecraft.world.phys.Vec2;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// inspired by https://github.com/Snownee/TextAnimator/blob/1.19.2-fabric/src/main/java/snownee/textanimator/mixin/client/StringRenderOutputMixin.java
@Mixin( targets = "net.minecraft.client.gui.Font$StringRenderOutput")
public class MixinTextDrawerPatSty {
    @Shadow
	float x;
	@Shadow
	float y;
    @Shadow
    private Matrix4f pose;

    @Shadow
    @Final
    MultiBufferSource bufferSource;

    private static final float RENDER_SIZE = 128f;

    @Inject(method = "accept", at = @At("HEAD"), cancellable = true)
	private void PatStyDrawerAccept(int index, Style style, int codepoint, CallbackInfoReturnable<Boolean> cir) {
        PatternStyle pStyle = (PatternStyle) style;
        if(pStyle.isHidden()){
            cir.setReturnValue(true);
            return;
        }
        
        if(pStyle.getPattern() == null){
            return;
        } else {
            HexPattern pattern = pStyle.getPattern();
            Pair<Float, List<Vec2> > pair = RenderLib.getCenteredPattern(pattern, RENDER_SIZE, RENDER_SIZE, 16f);
            Float patScale = pair.getFirst();
            List<Vec2> dots = pair.getSecond();

            float speed = 0;
            float variance = 0;

            // have it in stages so italics is kinda wobbly, obfuscated is really wobbly, and both is really really wobbly

            if(style.isItalic() && !style.isObfuscated()){
                speed = 0.05f;
                variance = 0.2f;
            } else if(style.isObfuscated() && !style.isItalic()){
                speed = 0.1f;
                variance = 0.8f;
            } else if(style.isObfuscated() && style.isItalic()){
                speed = 0.15f;
                variance = 3f;
            }

            List<Vec2> zappyPointsCentered = RenderLib.makeZappy(
                dots, RenderLib.findDupIndices(pattern.positions()),
                10, variance, speed, 0f, RenderLib.DEFAULT_READABILITY_OFFSET, RenderLib.DEFAULT_LAST_SEGMENT_LEN_PROP,
                0.0);
            List<Vec2> zappyPointsCenteredStill = pStyle.getZappyPoints();
            List<Vec2> pathfinderDotsCentered = pStyle.getPathfinderDots();
            List<Vec2> zappyPoints = new ArrayList<Vec2>();
            List<Vec2> pathfinderDots = new ArrayList<Vec2>();

            float minY = 1000000;
            float maxY = -1000000;
            float minX = 1000000;
            float maxX = -1000000;

            // get the ranges of the points
            for(Vec2 p : zappyPointsCenteredStill){
                minY = Math.min(minY, p.y);
                maxY = Math.max(maxY, p.y);
                minX = Math.min(minX, p.x);
                maxX = Math.max(maxX, p.x);
            }

            float lineWidth = 1.8f;
            float innerWidth = 1f; // not *really* sure how this changes it
            float dotWidth = 0.6f;
            float startingDotWidth = 1.25f; // big red one

            // putting these out here since we might want to account for line thickness?
            int patWidth = (int)(maxX - minX);
            int patHeight = (int)(maxY - minY);

            // ideally fit into normal height ? maybe *slightly* taller?
            // I think it's 10f for both?
            float scale = (9f-(lineWidth*0.75f)) / Math.max(patHeight, 48); // don't let it get absurdly long
            float lineScale = 1f;

            // done weird like this so it's in steps, probably a way to do it more mathematically but oh well
            if(scale / 0.5 < 0.5){
                lineScale /= 1.5;
            }
            if(scale / 0.5 < 0.25){
                lineScale /= 1.5;
            }
            if(scale / 0.5 < 0.125){
                lineScale /= 1.5;
            }
            if(scale / 0.5 < 0.0625){
                lineScale /= 1.5;
            }

            if((style.isStrikethrough() && Objects.equals(style.getColor(), TextColor.fromLegacyFormat(ChatFormatting.WHITE))) || style.getColor() == null){
                lineScale *= 0.75;
            }


            for(Vec2 p : zappyPointsCentered){
                zappyPoints.add(new Vec2((scale*p.x) + x + (scale*patWidth/2), (scale*p.y) + y + (scale*patHeight/2)));
            }

            for(Vec2 p : pathfinderDotsCentered){
                pathfinderDots.add(new Vec2((scale*p.x) + x + (scale*patWidth/2), (scale*p.y) + y + (scale*patHeight/2)));
            }

            // yoinked and adapted from pattern tooltip
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.disableCull();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

            // no clue how this will go !
            
            var outer = 0xff_d2c8c8;
            var innerLight = 0xc8_aba2a2;
            var innerDark = 0xc8_322b33;
            Matrix4f mat = new Matrix4f(this.pose);

            mat.translate(0f,0f,0.011f);

            int color = 0xffffffff;
            if(style.getColor() != null) color = style.getColor().getValue();
            int innerColorLight = color & 0x00ffffff | 0xc8000000;
            int innerColorDark = color & 0x00ffffff | 0x80000000; // ish

            // store what the tessellator was before
            Tesselator tessHold = Tesselator.getInstance();
            // make a new tessellator for our rendering functions to use
            MixinSetTessBuffer.setInstance(Glooptastic.specialTesselator);

            // VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getText(new Identifier("")));

            RenderLib.drawLineSeq(mat, zappyPoints, lineWidth * lineScale, 0,
                0xffffffff, 0xffffffff);
            RenderLib.drawLineSeq(mat, zappyPoints, lineWidth * 0.4f * lineScale, 0.01f,
                // style.isStrikethrough() && style.getColor() != null ? innerColorDark : innerDark, 
                // style.isStrikethrough() && style.getColor() != null ? innerColorLight : innerLight);
                innerColorDark, innerColorLight);

            Matrix4f dotMat = new Matrix4f(mat);
            dotMat.translate(0f, 0f, -(+1f-0.02f)); // dot renders 1F forward for some reason, push it back a bit so it doesn't poke out on signs

            RenderLib.drawSpot(dotMat, zappyPoints.get(0), startingDotWidth*lineScale, FastColor.ARGB32.red(color)/255f, FastColor.ARGB32.green(color)/255f, FastColor.ARGB32.blue(color)/255f, style.isBold() ? 0.7f : 0f);

            dotMat.translate(0, 0, 0.005f); // move the other dots just a tiny bit forwards

            for (var dot : pathfinderDots) {
                RenderLib.drawSpot(dotMat, dot, dotWidth*lineScale, 0.82f, 0.8f, 0.8f, 0.5f);
            }

            // return tessellator instance back to what it was before
            MixinSetTessBuffer.setInstance(tessHold);

            this.x += patWidth * scale + 1f;

            // not my business what happens after this?
            cir.setReturnValue(true);
        }
    }
}
