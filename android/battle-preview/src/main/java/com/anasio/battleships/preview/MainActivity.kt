package com.anasio.battleships.preview

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battle_screen)

        val inflater = LayoutInflater.from(this)
        findViewById<LinearLayout>(R.id.enemy_board_container).addView(
            inflater.inflate(R.layout.preview_battle_enemy_grid, null, false)
        )
        findViewById<LinearLayout>(R.id.fleet_board_container).addView(
            inflater.inflate(R.layout.preview_battle_fleet_grid, null, false)
        )

        val root = findViewById<android.view.View>(R.id.battle_preview_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            windowInsets
        }
        ViewCompat.requestApplyInsets(root)
    }
}