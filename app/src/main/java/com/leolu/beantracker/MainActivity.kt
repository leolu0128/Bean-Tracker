package com.leolu.beantracker

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.leolu.beantracker.ui.AppViewModel
import com.leolu.beantracker.ui.BeanListScreen
import com.leolu.beantracker.ui.BeanTrackerTheme
import com.leolu.beantracker.ui.EditorScreen
import com.leolu.beantracker.ui.isDark

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val dark = isDark(vm.themeMode)
            // Keep the system bar icons readable when the theme is set by hand.
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            BeanTrackerTheme(dark) {
                // Re-sort by most recent use whenever the app comes back to the foreground.
                LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.refreshOrder() }
                val editor = vm.editor
                if (editor == null) {
                    BeanListScreen(vm)
                } else {
                    BackHandler(onBack = vm::closeEditor)
                    EditorScreen(vm, editor)
                }
            }
        }
    }
}
