package com.aliabbas.aliabbasfiledownloader.utils

import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

fun <T> LiveData<T>.observeOnce(owner: LifecycleOwner, observer: Observer<T>) {
    observe(owner, object : Observer<T> {
        override fun onChanged(value: T) {
            observer.onChanged(value)
            removeObserver(this)
        }
    })
}


fun View.debounceOnClick(debounceDelay: Long = 1000L, onClick: (View) -> Unit) {
    var isClickable = true

    setOnClickListener { view ->
        if (isClickable) {
            isClickable = false
            onClick(view)

            // Enable the view after the delay
            view.postDelayed({
                isClickable = true
            }, debounceDelay)
        }
    }
}