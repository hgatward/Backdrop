package com.hat.backdrop.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.hat.backdrop.Backdrop
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_back.view.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        backdrop.addOnActivateLayerListener {
            when (it){
                Backdrop.Layer.BACK -> {
                    back.toolbar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_close_black_24dp)
                    back.toolbar.setNavigationOnClickListener { backdrop.activateLayer(Backdrop.Layer.FRONT) }
                }
                Backdrop.Layer.FRONT -> {
                    back.toolbar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_menu_black_24dp)
                    back.toolbar.setNavigationOnClickListener { backdrop.activateLayer(Backdrop.Layer.BACK) }
                }
            }
        }
    }
}
