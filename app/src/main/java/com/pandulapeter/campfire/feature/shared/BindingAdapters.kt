package com.pandulapeter.campfire.feature.shared

import android.databinding.BindingAdapter
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.design.widget.AppBarLayout
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.pandulapeter.campfire.util.drawable

@BindingAdapter(value = ["android:drawableStart", "android:drawableTop", "android:drawableEnd", "android:drawableBottom"], requireAll = false)
fun setCompoundDrawables(view: TextView,
                         @DrawableRes drawableStart: Int,
                         @DrawableRes drawableTop: Int,
                         @DrawableRes drawableEnd: Int,
                         @DrawableRes drawableBottom: Int) {
    val drawables = view.compoundDrawables
    view.setCompoundDrawablesWithIntrinsicBounds(
        if (drawableStart == 0) drawables[0] else view.context.drawable(drawableStart),
        if (drawableTop == 0) drawables[1] else view.context.drawable(drawableTop),
        if (drawableEnd == 0) drawables[2] else view.context.drawable(drawableEnd),
        if (drawableBottom == 0) drawables[3] else view.context.drawable(drawableBottom))
}

@BindingAdapter("android:text")
fun setText(view: EditText, text: String?) {
    if (!TextUtils.equals(view.text, text)) {
        view.setText(text)
        if (text != null) {
            view.setSelection(text.length)
        }
    }
}

@BindingAdapter("android:text")
fun setText(view: TextView, @StringRes text: Int?) {
    view.visibility = if (text == null) View.GONE else View.VISIBLE
    text?.let {
        view.setText(text)
    }
}


@BindingAdapter("android:contentDescription")
fun setContentDescription(view: ImageView, @StringRes contentDescription: Int?) {
    contentDescription?.let {
        view.contentDescription = view.context.getString(it)
    }
}

@BindingAdapter("android:drawable")
fun setDrawable(view: ImageView, @DrawableRes drawable: Int?) {
    view.visibility = if (drawable == null) View.GONE else View.VISIBLE
    drawable?.let {
        view.setImageDrawable(view.context.drawable(it))
    }
}

@BindingAdapter("visibility")
fun setVisibility(view: FloatingActionButton, isVisible: Boolean) {
    if (isVisible) {
        view.show()
        view.visibility = View.VISIBLE
    } else {
        view.hide()
    }
}


@BindingAdapter("isScrollEnabled")
fun setScrollEnabled(view: Toolbar, isScrollEnabled: Boolean) {
    (view.layoutParams as AppBarLayout.LayoutParams).scrollFlags = if (isScrollEnabled) {
        AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS or AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
    } else {
        0
    }
}