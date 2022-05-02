package dev.emortal.lobby

import net.minestom.server.instance.block.Block
import net.minestom.server.instance.generator.GenerationUnit
import net.minestom.server.instance.generator.UnitModifier
import java.util.concurrent.ThreadLocalRandom

object BackroomsGenerator {

    fun generateRoom(unit: GenerationUnit, y: Int) {
        val modifier = unit.modifier()
        val start = unit.absoluteStart()
        val yPos = start.blockY() + y

        for (x in 0 until unit.size().blockX()) {
            for (z in 0 until unit.size().blockZ()) {
                val xPos = start.blockX() + x
                val zPos = start.blockZ() + z
                
                modifier.setBlock(xPos, yPos - 1, zPos, Block.BEDROCK)
                modifier.setBlock(xPos, yPos + 5, zPos, Block.BEDROCK)

                modifier.setBlock(xPos, yPos + 1, zPos, Block.GRAY_CARPET)
                modifier.setBlock(xPos, yPos, zPos, Block.OAK_PLANKS)
                modifier.setBlock(xPos, yPos + 4, zPos, Block.SMOOTH_STONE)

                // idk man... dont ask about this
                if (
                    (x - 2) % 4 == 0 && (
                        ((z - 2) % 8 == 0) ||
                        ((z - 2) % 8 == 1) ||
                        ((z - 2) % 8 == 3) ||
                        ((z - 2) % 8 == 4)
                    )
                ) {
                    modifier.setBlock(xPos, yPos + 4, zPos, Block.SEA_LANTERN)
                }
            }
        }

        generateWalls(modifier, start.blockX() + 0, yPos + 1, start.blockZ() + 0)
        generateWalls(modifier, start.blockX() + 8, yPos + 1, start.blockZ() + 0)
        generateWalls(modifier, start.blockX() + 0, yPos + 1, start.blockZ() + 8)
        generateWalls(modifier, start.blockX() + 8, yPos + 1, start.blockZ() + 8)
    }

    private fun generateWalls(modifier: UnitModifier, xPos: Int, yPos: Int, zPos: Int) {
        generatePilar(modifier, xPos, yPos, zPos, Block.SMOOTH_SANDSTONE)

        val rand = ThreadLocalRandom.current()

        val wall1 = rand.nextInt(4) != 0
        val wall2 = rand.nextInt(4) != 0
        val door1 = rand.nextInt(2) != 0
        val door2 = rand.nextInt(2) != 0

        val door1pos: Int = rand.nextInt(2, 6)
        val door2pos: Int = rand.nextInt(2, 6)

        if (wall1) {
            for (x in xPos until xPos + 8) {
                generatePilar(modifier, x, yPos, zPos, Block.SMOOTH_SANDSTONE)
            }
        }

        if (wall2) {
            for (z in zPos until zPos + 8) {
                generatePilar(modifier, xPos, yPos, z, Block.SMOOTH_SANDSTONE)
            }
        }

        if (door1 && wall1) {
            generatePilar(modifier, xPos + door1pos, yPos, zPos, Block.AIR)
            generatePilar(modifier, xPos + door1pos + 1, yPos, zPos, Block.AIR)
        }

        if (door2 && wall2) {
            generatePilar(modifier, xPos, yPos, zPos + door2pos, Block.AIR)
            generatePilar(modifier, xPos, yPos, zPos + door2pos + 1, Block.AIR)
        }
    }

    private fun generatePilar(modifier: UnitModifier, xPos: Int, yPos: Int, zPos: Int, material: Block) {
        for (y in yPos until yPos + 3) {
            modifier.setBlock(xPos, y, zPos, material)
        }
    }
}