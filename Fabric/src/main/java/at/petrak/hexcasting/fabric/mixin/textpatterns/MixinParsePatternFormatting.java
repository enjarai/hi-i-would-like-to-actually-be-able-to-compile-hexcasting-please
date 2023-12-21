package at.petrak.hexcasting.fabric.mixin.textpatterns;

import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.gloopy.PatternStyle;
import at.petrak.hexcasting.gloopy.StringsToDirMap;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(StringDecomposer.class)
public class MixinParsePatternFormatting {

    // thx object <3
    private static Pattern PATTERN_PATTERN_REGEX = Pattern.compile("\\A(?<escaped>\\\\?)(HexPattern)?[<(\\[{]\\s*(?<direction>[a-z_-]+)(?:\\s*[, ]\\s*(?<pattern>[aqweds]+))?\\s*[>)\\]}]", Pattern.CASE_INSENSITIVE);

    // want to mixin to start of the loop in visitFormatted
    
    @WrapOperation(method="iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
    at=@At(value="INVOKE", target="Lnet/minecraft/util/StringDecomposer;feedChar(Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;IC)Z"))
    private static boolean parsePatternFormatting(Style style, FormattedCharSink visitor, int index, char c, Operation<Boolean> operation, @Local(ordinal=2) LocalIntRef jref, @Local(ordinal=0) String text){
        int startishIndex = jref.get(); // where we entered the loop
        String remainingText = text.substring(startishIndex);
        Matcher matcher = PATTERN_PATTERN_REGEX.matcher(remainingText);
        if(matcher.find()){ // should only check beginning of text since we have '\A'
            if(matcher.group("escaped").length() > 0){ // it's escaped - so just accept everything through the last match ? not the best for compatability but like, it would've broken without the escape too ?
                int endIndex = matcher.end();
                // skip the escaped character
                // HexGloop.logPrint("escaped pattern for '" + remainingText + "'");
                for(int i = 1; i < endIndex; i++){
                    // HexGloop.logPrint("\t" + i + " -- " + i + startishIndex + "/" + startishIndex + endIndex + " : '" + text.charAt(i + startishIndex) + "'");
                    visitor.accept(startishIndex + i, style, text.charAt(startishIndex + i));
                }
                jref.set(startishIndex + matcher.end()-1);
                return jref.get() < text.length();
            } else { // not escaped
                // need to check if the direction is valid
                // HexGloop.logPrint("not escaped for '" + remainingText + "'");
                String dirString = matcher.group("direction").toLowerCase().strip().replace("_", "");
                HexDir dir = StringsToDirMap.dirMap.get(dirString);
                if(dir == null){ // invalid direction
                    return operation.call(style, visitor, index, c);
                }
                // HexGloop.logPrint("has direction: " + dir.toString());
                // now need to get the pattern
                String angleSigs = matcher.group("pattern");
                HexPattern pattern = parsePattern(angleSigs, dir);
                if(pattern == null){
                    return operation.call(style, visitor, index, c);
                }
                // HexGloop.logPrint("has pattern: " + pattern.toString());
                
                // it's ok to use a single '!' here since it's only for actual rendering
                visitor.accept(startishIndex, ((PatternStyle)style).withPattern(pattern), '!');
                jref.set(startishIndex + matcher.end()-1);
                return (startishIndex + matcher.end()-1) < text.length(); // if there's more or not
            }
        }
        return operation.call(style, visitor, index, c);
    }

    @Nullable
    private static HexPattern parsePattern(String angleSigs, HexDir dir){
        if(angleSigs == null || dir == null){
            return null;
        }
        HexPattern pattern = null;
        try{
            pattern = HexPattern.fromAngles(angleSigs, dir);
        } catch (IllegalStateException e) {
        }
        return pattern;
    }
}
