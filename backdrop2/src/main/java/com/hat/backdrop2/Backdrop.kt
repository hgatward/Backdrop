package com.hat.backdrop2

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator

open class Backdrop(context: Context, attrs: AttributeSet?) : ViewGroup(context, attrs) {
    companion object {
        const val NO_PERSISTENT_BACK_VIEW = 0
        const val ACTIVATE_BACK_ANIMATION_DURATION = 200L
        const val ACTIVATE_FRONT_ANIMATION_DURATION = 250L

        @JvmStatic
        val INITIAL_ACTIVE_LAYER = Layer.FRONT
    }

    private var persistentBackHeight: Float
    private val persistentBackViewId: Int
    private val minFrontHeight: Float

    private val tempLayoutBounds = Rect()
    private val tempFrontBounds = Rect()

    private var frontHeightAnimator: ValueAnimator? = null

    private val onActivateLayerListeners = mutableListOf<(Layer) -> Unit>()

    private var activeLayerState: Backdrop.Layer = INITIAL_ACTIVE_LAYER

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.Backdrop, 0, R.style.Widget_Backdrop)
        persistentBackViewId = a.getResourceId(R.styleable.Backdrop_persistent_back_view, NO_PERSISTENT_BACK_VIEW)
        persistentBackHeight = a.getDimension(R.styleable.Backdrop_persistent_back_height, 0f)
        minFrontHeight = a.getDimension(R.styleable.Backdrop_min_front_height, 0f)
        a.recycle()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        tempLayoutBounds.left = paddingLeft
        tempLayoutBounds.right = r - l - paddingRight
        tempLayoutBounds.top = paddingTop
        tempLayoutBounds.bottom = b - t - paddingBottom

        for (i in 0 until childCount) {
            val view = getChildAt(i)
            when (getLayer(view)) {
                Layer.BACK -> {
                    view.layout(tempLayoutBounds.left,
                            tempLayoutBounds.top,
                            tempLayoutBounds.left + view.measuredWidth,
                            tempLayoutBounds.top + view.measuredHeight)
                }
                Backdrop.Layer.FRONT -> {
                    Gravity.apply(Gravity.BOTTOM, view.measuredWidth, view.measuredHeight, tempLayoutBounds, tempFrontBounds)
                    view.layout(tempFrontBounds.left,
                            tempFrontBounds.top,
                            tempFrontBounds.right,
                            tempFrontBounds.bottom)
                }
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        val heightSize = MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom

        val exactWidth = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY)
        val exactHeight = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY)
        val atMostHeight = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.AT_MOST)

        for (i in 0 until childCount){
            val view = getChildAt(i)
            when (getLayer(view)){
                Backdrop.Layer.BACK -> measureChild(view, exactWidth, exactHeight)
                Backdrop.Layer.FRONT -> measureChild(view, exactWidth, atMostHeight)
            }
        }

        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec))
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        validateChildren()

        if (persistentBackViewId != NO_PERSISTENT_BACK_VIEW){
            val view = findViewById<View>(persistentBackViewId)
            val observer = object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    @Suppress("DEPRECATION")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    else view.viewTreeObserver.removeGlobalOnLayoutListener(this)

                    persistentBackHeight = view.height.toFloat()
                    activateLayer(activeLayerState)
                }
            }
            view.viewTreeObserver.addOnGlobalLayoutListener(observer)
        }
    }

    private fun validateChildren() {
        if (childCount != 2) throw IllegalStateException("Backdrop must have exactly 2 children")
        if (getLayer(getChildAt(0)) == getLayer(getChildAt(1))) throw IllegalStateException("Each backdrop child must have different 'layer' values, i.e. one 'front' and the other 'back'")
    }

    override fun generateLayoutParams(attrs: AttributeSet?): ViewGroup.LayoutParams
            = Backdrop.LayoutParams(context, attrs)

    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams
            = Backdrop.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean {
        return p is Backdrop.LayoutParams
    }

    private fun getLayer(view: View) = (view.layoutParams as LayoutParams).layer

    private fun getView(layer: Layer): View {
        for (i in 0 until childCount){
            if ((getChildAt(i).layoutParams as LayoutParams).layer == layer) return getChildAt(i)
        }
        throw IllegalStateException("No view was found with layer $layer")
    }

    override fun shouldDelayChildPressedState(): Boolean = false

    fun activateLayer(layer: Layer){
        activeLayerState = layer

        when (layer){
            Layer.BACK -> animateFrontHeight(Math.max(minFrontHeight, height - getView(Layer.BACK).height.toFloat()), ACTIVATE_BACK_ANIMATION_DURATION)
            Layer.FRONT -> animateFrontHeight(height - persistentBackHeight, ACTIVATE_FRONT_ANIMATION_DURATION)
        }

        onActivateLayerListeners.forEach { it(layer) }
    }

    private fun animateFrontHeight(newHeight: Float, animationDuration: Long){
        frontHeightAnimator?.cancel()
        frontHeightAnimator = null
        val frontView = getView(Layer.FRONT)
        frontHeightAnimator = ValueAnimator.ofFloat(frontView.height.toFloat(), newHeight).apply {
            addUpdateListener {
                frontView.setHeight((it.animatedValue as Float).toInt())
            }
            interpolator = AccelerateDecelerateInterpolator()
            duration = animationDuration
            start()
        }
    }

    private fun View.setHeight(newHeight: Int){
        layoutParams = layoutParams.apply {
            height = newHeight
        }
    }

    fun addOnActivateLayerListener(listener: (Layer) -> Unit){
        onActivateLayerListeners.add(listener)
    }

    fun removeOnActivateLayerListener(listener: (Layer) -> Unit){
        onActivateLayerListeners.remove(listener)
    }

    override fun onSaveInstanceState(): Parcelable {
        return SavedState(super.onSaveInstanceState()!!).apply {
            activeLayer = activeLayerState
        }
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        super.onRestoreInstanceState(state.superState)
        activeLayerState = state.activeLayer
    }

    class SavedState: BaseSavedState{
        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(parcel: Parcel): SavedState {
                    return SavedState(parcel)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return Array(size) { null }
                }
            }
        }

        lateinit var activeLayer: Backdrop.Layer

        constructor(superState: Parcelable): super(superState)
        constructor(parcelIn: Parcel): super(parcelIn){
            activeLayer = Backdrop.Layer.values()[parcelIn.readInt()]
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(activeLayer.ordinal)
        }
    }

    enum class Layer {
        BACK, FRONT
    }

    class LayoutParams : ViewGroup.LayoutParams {
        val layer: Layer

        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.Backdrop_Layout)
            layer = if (a.getInt(R.styleable.Backdrop_Layout_layout_layer, 0) == 0) Layer.BACK else Layer.FRONT
            a.recycle()
        }

        constructor(width: Int, height: Int) : super(width, height) {
            layer = Layer.BACK
        }

        constructor(source: ViewGroup.LayoutParams) : super(source) {
            layer = Layer.BACK
        }
    }
}