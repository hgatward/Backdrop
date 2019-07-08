package com.hat.backdrop

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Rect
import android.os.Parcel
import android.os.Parcelable
import androidx.core.view.ViewCompat
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator

open class Backdrop(context: Context, attrs: AttributeSet? = null) : ViewGroup(context, attrs) {
    companion object {
        const val NO_ALWAYS_VISIBLE_BACK_VIEW = 0
        const val ACTIVATE_BACK_ANIMATION_DURATION = 200L
        const val ACTIVATE_FRONT_ANIMATION_DURATION = 250L

        @JvmStatic
        val INITIAL_ACTIVE_LAYER = Layer.FRONT
    }

    var visibleBackHeight: Float = 0f
        set(value) {
            if (ViewCompat.isLaidOut(this) && value > visibleBackHeight && activeLayerState == Layer.FRONT) {
                //Front layer might be covering the always visible back height
                animateFrontHeight(height - requestView(Layer.BACK).height.toFloat(), ACTIVATE_BACK_ANIMATION_DURATION)
            }
            field = value
        }

    private var visibleBackViewId: Int
    private var isFirstLayout = true
    var minFrontHeight: Float = 0f
        set(value) {
            if (value > minFrontHeight && activeLayerState == Layer.BACK) {
                //Front layer's current height might be smaller than the new minimum front height
                animateFrontHeight(minFrontHeight, ACTIVATE_FRONT_ANIMATION_DURATION)
            }
            field = value
        }

    var overrideFrontLayerOnClickListener = false
    private val tempLayoutBounds = Rect()
    private val tempFrontBounds = Rect()

    private var frontHeightAnimator: ValueAnimator? = null
    var interpolator = AccelerateDecelerateInterpolator()

    private val onActivateLayerListeners = mutableListOf<(Layer) -> Unit>()
    private var activeLayerState: Backdrop.Layer = INITIAL_ACTIVE_LAYER

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.Backdrop, 0, R.style.Widget_Backdrop)
        visibleBackViewId = a.getResourceId(R.styleable.Backdrop_visible_back_view, NO_ALWAYS_VISIBLE_BACK_VIEW)
        visibleBackHeight = a.getDimension(R.styleable.Backdrop_visible_back_height, 0f)
        minFrontHeight = a.getDimension(R.styleable.Backdrop_min_front_height, 0f)
        a.recycle()
    }

    fun setBackView(view: View) = setView(view, Layer.BACK)
    fun setFrontView(view: View) = setView(view, Layer.FRONT)
    fun setView(view: View, layer: Layer){
        view.layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, layer)
        if (childCount == 0) addView(view)

        removeView(getView(layer)) //Remove the view of the layer being set if it exists
        addView(view)
    }

    override fun onViewAdded(child: View) {
        super.onViewAdded(child)
        if (childCount > 2) throw IllegalStateException("Backdrop can't have more than 2 children, use Backdrop.setView(view, layer) instead of addView(view)")


        if (getLayer(child) == Layer.BACK && childCount == 2){
            //Just added the back view on top of the front view so bring the front view to the top
            try{
                requestView(Layer.FRONT).bringToFront()
            } catch (e: IllegalStateException){
                throw IllegalStateException("Backdrop must have both a back view and a front view, use Backdrop.setView(view, layer) instead of addView(view)")
            }
        }

        //Set reveal click listener on front view
        if (getLayer(child) == Layer.FRONT && (!child.hasOnClickListeners() || overrideFrontLayerOnClickListener)) {
            child.setOnClickListener { activateLayer(Layer.FRONT) }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        validateChildren()

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

                    if (visibleBackViewId != NO_ALWAYS_VISIBLE_BACK_VIEW){
                        visibleBackHeight = view.findViewById<View>(visibleBackViewId).height.toFloat()
                        visibleBackViewId = NO_ALWAYS_VISIBLE_BACK_VIEW
                    }
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

        if (isFirstLayout){
            activateLayer(activeLayerState)
            isFirstLayout = false
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

    private fun validateChildren() {
        if (childCount != 2) throw IllegalStateException("Backdrop must have exactly 2 children")
        if (getLayer(getChildAt(0)) == getLayer(getChildAt(1))) throw IllegalStateException("Each backdrop child must have different 'layer' values, i.e. one 'front' and the other 'back'")
    }

    override fun generateLayoutParams(attrs: AttributeSet?): ViewGroup.LayoutParams
            = Backdrop.LayoutParams(context, attrs)

    override fun generateDefaultLayoutParams(): ViewGroup.LayoutParams? = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean {
        return p is Backdrop.LayoutParams
    }

    private fun getLayer(view: View): Layer? = (view.layoutParams as LayoutParams).layer

    private fun getView(layer: Layer): View?{
        for (i in 0 until childCount){
            if ((getChildAt(i).layoutParams as LayoutParams).layer == layer) return getChildAt(i)
        }
        return null
    }

    private fun requestView(layer: Layer): View = getView(layer) ?: throw IllegalStateException("No view was found with layer $layer")

    override fun shouldDelayChildPressedState(): Boolean = false

    fun activateLayer(layer: Layer){
        activeLayerState = layer

        when (layer){
            Layer.BACK -> animateFrontHeight(Math.max(minFrontHeight, height - requestView(Layer.BACK).height.toFloat()), ACTIVATE_BACK_ANIMATION_DURATION)
            Layer.FRONT -> animateFrontHeight(height - visibleBackHeight, ACTIVATE_FRONT_ANIMATION_DURATION)
        }

        onActivateLayerListeners.forEach { it(layer) }
    }

    private fun animateFrontHeight(newHeight: Float, animationDuration: Long){
        frontHeightAnimator?.cancel()
        frontHeightAnimator = null
        val frontView = requestView(Layer.FRONT)
        frontHeightAnimator = ValueAnimator.ofFloat(frontView.height.toFloat(), newHeight).apply {
            addUpdateListener {
                frontView.setHeight((it.animatedValue as Float).toInt())
            }
            interpolator = interpolator
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
        val layer: Layer?

        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.Backdrop_Layout)
            layer = when (a.getInt(R.styleable.Backdrop_Layout_layout_layer, -1)) {
                0 -> Layer.BACK
                1 -> Layer.FRONT
                else -> null//throw IllegalStateException("Backdrop.LayoutParams must specify layout_layer attribute")
            }
            a.recycle()
        }

        constructor(width: Int, height: Int, layer: Backdrop.Layer) : super(width, height) {
            this.layer = layer
        }

        constructor(source: ViewGroup.LayoutParams, layer: Backdrop.Layer) : super(source) {
            this.layer = layer
        }
    }
}