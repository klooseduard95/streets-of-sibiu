package ro.klooseduard.streetsofsibiu.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

/**
 * Every copied asset must be byte-identical to the file at the same relative path in the
 * original Godot project, and every asset the original uses at runtime must be present.
 */
class AssetIntegrityTest {
    private val root = File(System.getProperty("sos.repoRoot") ?: error("sos.repoRoot not set"))
    private val assetsDir = File(root, "assets")
    private val originalDir = File(root, "2026-Game_of_the_year-main/newgame")

    /** Runtime-used images and audio from MIGRATION_PLAN.md section 1.5. */
    private val expected = setOf(
        "tilesetbig.png", "cone.png", "menu.png",
        "player/player_assets/playerassets.png", "player/player_assets/diagonals.png",
        "enemy/enemy_assets/Rat Spritesheet.png", "enemy/enemy_assets/PigeonSpritesheet.png",
        "enemy/enemy_assets/thiefAssets.png", "enemy/enemy_assets/criminalAssets.png",
        "enemy/enemy_assets/manhole.png", "enemy/enemy_assets/ghost.png",
        "enemy/enemy_assets/KingRotta.png", "enemy/enemy_assets/floating_crown.png",
        "enemy/enemy_assets/rat_shadow.png",
        "enemy/enemy_projectile/enemy_projectile_assets/claw.png",
        "enemy/enemy_projectile/enemy_projectile_assets/bullet.png",
        "enemy/enemy_projectile/enemy_projectile_assets/the_orb.png",
        "enemy/enemy_projectile/enemy_projectile_assets/the_orb_GHOST.png",
        "item/item_assets/bullet_player.png", "item/item_assets/lines.png",
        "background/finishers/garbagebin.png", "background/background_assets/manhole_portal.png",
        "bucurest_hroma.mp3",
    ) + listOf(
        "rifle", "smg", "negev", "sniper", "Shield", "heal bottle", "newbottle1", "newbottle2",
        "newbottle3", "newbottle4", "dash1", "dash2", "normaldash", "parry1", "parry2", "parry3",
        "bat", "chainsaw", "knife", "punch", "invslot",
    ).map { "item/item_assets/item icons/$it.png" } + listOf(
        "Horse", "Dash", "Parry", "PotionGulp", "PlayerDeath", "Gunshot", "Slash", "Chainsaw",
        "Rat", "Pigeon", "Grunt", "CeTelefonAi", "EnemySpawner",
    ).map { "Music/$it.mp3" }

    private fun copiedOriginalAssets(): Set<String> = assetsDir.walkTopDown()
        .filter { it.isFile && it.extension in setOf("png", "mp3") }
        .map { it.relativeTo(assetsDir).invariantSeparatorsPath }
        .toSet()

    private fun md5(file: File): String =
        MessageDigest.getInstance("MD5").digest(file.readBytes()).joinToString("") { "%02x".format(it) }

    @Test
    fun allRuntimeAssetsAreCopied() {
        assertEquals(expected, copiedOriginalAssets())
    }

    @Test
    fun copiedAssetsAreIdenticalToOriginals() {
        copiedOriginalAssets().forEach { path ->
            val original = File(originalDir, path)
            assertTrue("no original for $path", original.isFile)
            assertEquals("content differs: $path", md5(original), md5(File(assetsDir, path)))
        }
    }
}
