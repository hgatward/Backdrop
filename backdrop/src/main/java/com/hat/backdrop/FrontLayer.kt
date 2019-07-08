package com.hat.backdrop

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.layout_front_subheader.view.*

class FrontLayer(context: Context, attrs: AttributeSet? = null): LinearLayout(context, attrs){
    var subheader: String
        get() = subheaderText.text.toString()
        set(value) {subheaderText.text = value}

    private val onActivateLayerListener: (Backdrop.Layer) -> Unit = {
        when (it){
            Backdrop.Layer.BACK -> revealButton.visibility = View.VISIBLE
            Backdrop.Layer.FRONT -> revealButton.visibility = View.GONE
        }
    }

    init {
        orientation = VERTICAL
        ViewCompat.setBackground(this, ContextCompat.getDrawable(context, R.drawable.rounded_top))

        View.inflate(context, R.layout.layout_front_subheader, this)

        val a = context.obtainStyledAttributes(attrs, R.styleable.FrontLayer)
        subheader = a.getString(R.styleable.FrontLayer_subheader) ?: ""
        a.recycle()

        revealButton.setOnClickListener { (parent as Backdrop).activateLayer(Backdrop.Layer.FRONT) }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        (parent as Backdrop).addOnActivateLayerListener(onActivateLayerListener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        (parent as Backdrop).removeOnActivateLayerListener(onActivateLayerListener)
    }
}