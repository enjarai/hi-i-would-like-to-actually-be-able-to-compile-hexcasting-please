package at.petrak.hexcasting.gloopy;

import at.petrak.hexcasting.api.casting.math.HexPattern;
import net.minecraft.network.chat.Style;
import net.minecraft.world.phys.Vec2;
import org.joml.Vector2f;

import java.util.List;

// put this on text (maybe a specific character, tbd) and it'll render it as a hex pattern
public interface PatternStyle {

    public HexPattern getPattern();

    // used to hide stuff so i can print nicely
    public boolean isHidden();

    // note that style is meant to be immutable and this mutates it
    public Style setPattern(HexPattern pattern);

    public default Style withPattern(HexPattern pattern){
        return withPattern(pattern, true, true);
    }

    // in case you don't want the angle sigs / larger render to show on hover/click for whatever reason
    public Style withPattern(HexPattern pattern, boolean withPatternHoverEvent, boolean withPatternClickEvent);

    public Style withHidden(boolean hidden);
    
    public Style setHidden(boolean hidden);

    public static Style fromPattern(HexPattern pattern){
        return ((PatternStyle)Style.EMPTY.withBold(null)).setPattern(pattern); // just to get an empty style
    }

    // mimic tooltip rendering
    public List<Vec2> getZappyPoints();

    public List<Vec2> getPathfinderDots();

    
}
