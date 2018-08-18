package com.hat.backdrop

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.view.ViewGroup
import com.hat.backdrop2.Backdrop
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        backdrop.addOnActivateLayerListener {
            when (it){
                Backdrop.Layer.BACK -> {
                    toolbar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_close_black_24dp)
                    toolbar.setNavigationOnClickListener { backdrop.activateLayer(Backdrop.Layer.FRONT) }
                }
                Backdrop.Layer.FRONT -> {
                    toolbar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_menu_black_24dp)
                    toolbar.setNavigationOnClickListener { backdrop.activateLayer(Backdrop.Layer.BACK) }
                }
            }
        }
    }
}
