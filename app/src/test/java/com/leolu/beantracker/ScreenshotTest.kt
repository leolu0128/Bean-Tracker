package com.leolu.beantracker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.leolu.beantracker.data.BeanInput
import com.leolu.beantracker.ui.AppViewModel
import com.leolu.beantracker.ui.BeanListScreen
import com.leolu.beantracker.ui.BeanTrackerTheme
import com.leolu.beantracker.ui.EditorScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/**
 * Renders the README screenshots with sample data. Run with
 * `./gradlew recordRoborazziDebug`; images land in docs/screenshots.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<BeanTrackerApp>()

    @Test
    @Config(qualifiers = "zh-rTW-w411dp-h891dp-xhdpi")
    fun listLightZh() = captureList("list_zh", dark = false)

    @Test
    @Config(qualifiers = "zh-rTW-w411dp-h891dp-xhdpi")
    fun listDarkZh() = captureList("list_dark_zh", dark = true)

    @Test
    @Config(qualifiers = "zh-rTW-w411dp-h891dp-xhdpi")
    fun editorZh() = captureEditor("editor_zh")

    @Test
    @Config(qualifiers = "en-w411dp-h891dp-xhdpi")
    fun listLightEn() = captureList("list_en", dark = false)

    @Test
    @Config(qualifiers = "en-w411dp-h891dp-xhdpi")
    fun editorEn() = captureEditor("editor_en")

    private fun captureList(name: String, dark: Boolean) {
        val vm = loadSampleData()
        compose.setContent { BeanTrackerTheme(dark) { BeanListScreen(vm) } }
        capture(name)
    }

    private fun captureEditor(name: String) {
        val vm = loadSampleData()
        vm.openEdit(vm.listState.value.active.first { it.bean.countries.size > 1 })
        compose.setContent { BeanTrackerTheme(false) { EditorScreen(vm, vm.editor!!) } }
        capture(name)
    }

    private fun capture(name: String) {
        // Covers are decoded asynchronously by Coil; give them time to land.
        repeat(3) {
            Thread.sleep(500)
            compose.waitForIdle()
        }
        compose.onRoot().captureRoboImage("../docs/screenshots/$name.png")
    }

    private fun loadSampleData(): AppViewModel {
        val repo = app.repository
        val today = LocalDate.now().toEpochDay()
        val morning = cover("morning", 0xFFB5651D.toInt(), 0xFF6F2C0F.toInt())
        val hill = cover("hill", 0xFF4F7C6B.toInt(), 0xFF1E3B32.toInt())
        val fox = cover("fox", 0xFFD9A441.toInt(), 0xFF8C4A1A.toInt())

        fun bean(
            roaster: String,
            name: String,
            countries: List<String>,
            region: String = "",
            farm: String = "",
            variety: String = "",
            processes: List<String>,
            grams: Int,
            roastedDaysAgo: Long?,
        ) = BeanInput(
            id = 0,
            roasterName = roaster,
            name = name,
            countries = countries,
            region = region,
            farm = farm,
            variety = variety,
            processes = processes,
            grams = grams,
            roastDate = roastedDaysAgo?.let { today - it },
        )

        runBlocking {
            // Saved oldest first, since the list shows the most recently used on top.
            repo.save(bean("Little Fox Coffee", "Yunnan Catimor", listOf("China"), "Pu'er", processes = listOf("Honey"), grams = 0, roastedDaysAgo = 40), fox)
            delay(5)
            repo.save(bean("Hill & Bean", "", listOf("Kenya"), "Nyeri", "Gatomboya", "SL28 / SL34", listOf("Washed"), 120, 18), hill)
            delay(5)
            repo.save(bean("Morning Roasters", "Panama Geisha", listOf("Panama"), "Boquete", "Elida Estate", "Geisha", listOf("Natural"), 35, 7), morning)
            delay(5)
            repo.save(bean("Little Fox Coffee", "House Blend", listOf("Brazil", "Colombia"), variety = "Bourbon / Caturra", processes = listOf("Natural", "Washed"), grams = 240, roastedDaysAgo = 12), null)
            delay(5)
            repo.save(bean("Morning Roasters", "Guji Hambela", listOf("Ethiopia"), "Guji", "Hambela", "Heirloom", listOf("Washed"), 200, 10), null)
            delay(5)
            val top = repo.currentItems().first().bean.id
            repo.adjust(top, -15)
        }

        val vm = AppViewModel(app)
        compose.waitUntil(10_000) {
            val s = vm.listState.value
            s.loaded && s.active.size == 4 && vm.roasterCovers.value.size == 3
        }
        return vm
    }

    /** A simple gradient cover, standing in for a photo of the bag. */
    private fun cover(name: String, from: Int, to: Int): String {
        val size = 512
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawRect(
            0f, 0f, size.toFloat(), size.toFloat(),
            Paint().apply { shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), from, to, Shader.TileMode.CLAMP) },
        )
        val bean = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55FFFFFF }
        canvas.save()
        canvas.rotate(-30f, size / 2f, size / 2f)
        canvas.drawOval(size * 0.32f, size * 0.22f, size * 0.68f, size * 0.78f, bean)
        canvas.restore()
        val file = File(app.filesDir, "covers/$name.jpg").apply { parentFile?.mkdirs() }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return file.absolutePath
    }
}
