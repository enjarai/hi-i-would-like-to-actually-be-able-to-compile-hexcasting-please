package at.petrak.hexcasting.common.casting.operators.spells

import at.petrak.hexcasting.api.misc.MediaConstants
import at.petrak.hexcasting.api.casting.*
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3

object OpAddMotion : SpellAction {
    override val argc: Int
        get() = 2

    override fun execute(
        args: List<Iota>,
        ctx: CastingEnvironment
    ): Triple<RenderedSpell, Int, List<ParticleSpray>> {
        val target = args.getEntity(0, argc)
        val motion = args.getVec3(1, argc)
        ctx.assertEntityInRange(target)
        var motionForCost = motion.lengthSqr()
        if (ctx.hasBeenGivenMotion(target))
            motionForCost++
        ctx.markEntityAsMotionAdded(target)
        return Triple(
            Spell(target, motion),
            (motionForCost * MediaConstants.DUST_UNIT).toInt(),
            listOf(
                ParticleSpray(
                    target.position().add(0.0, target.eyeHeight / 2.0, 0.0),
                    motion.normalize(),
                    0.0,
                    0.1
                )
            ),
        )
    }

    private data class Spell(val target: Entity, val motion: Vec3) : RenderedSpell {
        override fun cast(ctx: CastingEnvironment) {
            target.push(motion.x, motion.y, motion.z)
            target.hurtMarked = true // Whyyyyy
        }
    }
}
