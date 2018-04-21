package com.pandulapeter.campfire.feature.shared.widget

import android.content.Context
import android.databinding.DataBindingUtil
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import android.widget.ViewFlipper
import com.pandulapeter.campfire.R
import com.pandulapeter.campfire.databinding.ViewStateLayoutBinding
import com.pandulapeter.campfire.util.drawable
import com.pandulapeter.campfire.util.useStyledAttributes
import com.pandulapeter.campfire.util.visibleOrGone

class StateLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ViewFlipper(context, attrs) {

    private val binding = DataBindingUtil.inflate<ViewStateLayoutBinding>(LayoutInflater.from(context), R.layout.view_state_layout, this, true)
    var state: State = State.LOADING
        set(value) {
            if (field != value) {
                field = value
                displayedChild = value.childIndex
            }
        }
    var onButtonClicked: OnClickListener? = null
    var buttonText: String? = null
        set(value) {
            field = value
            binding.button.run {
                visibleOrGone = value != null
                text = value
            }
        }
    var text = ""
        set(value) {
            field = value
            binding.text.text = value
        }

    init {
        inAnimation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
        binding.button.setOnClickListener { onButtonClicked?.onClick(this) }
        useStyledAttributes(attrs, R.styleable.StateLayout) {
            buttonText = getString(R.styleable.StateLayout_buttonText)
            getString(R.styleable.StateLayout_text)?.let { text = it }
        }
        postDelayed({
            if (isAttachedToWindow) {
                if (displayedChild == 0) {
                    binding.loadingIndicator.animate().alpha(1f).start()
                } else {
                    binding.loadingIndicator.alpha = 1f
                }
            }
        }, 200)
    }

    fun setText(@StringRes stringRes: Int) {
        text = context.getString(stringRes)
    }

    fun setButtonText(@StringRes stringRes: Int) {
        buttonText = if (stringRes == 0) null else context.getString(stringRes)
    }

    fun setButtonIcon(@DrawableRes resourceId: Int) =
        binding.button.setCompoundDrawablesRelativeWithIntrinsicBounds(if (resourceId == 0) null else context.drawable(resourceId), null, null, null)

    enum class State(val childIndex: Int) {
        LOADING(0),
        ERROR(1),
        NORMAL(2)
    }
}