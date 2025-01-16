package com.aliabbas.aliabbasfiledownloader.ui.bottomsheet

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.aliabbas.aliabbasfiledownloader.R
import com.aliabbas.aliabbasfiledownloader.databinding.BottomSheetSettingsBinding
import com.aliabbas.aliabbasfiledownloader.utils.UtilFunctions.getIntFromSharedPreferences
import com.aliabbas.aliabbasfiledownloader.utils.UtilFunctions.saveIntToSharedPreferences
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.slider.Slider

class BottomSheetSettingsFragment : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        val contentView = View.inflate(context, R.layout.bottom_sheet_add_new_download, null)
        dialog.setContentView(contentView)
        (contentView.parent as View).setBackgroundColor(Color.TRANSPARENT)
        val layoutParams = contentView.layoutParams as FrameLayout.LayoutParams
        layoutParams.gravity = Gravity.BOTTOM
        layoutParams.setMargins(0, 0, 0, 0)
        contentView.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.bg_bottom_sheet)
        return dialog
    }

    override fun getTheme(): Int {
        return R.style.BottomSheetDialogTheme
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.seekBar.setCustomThumbDrawable(R.drawable.custom_thumb)

        val csv = getIntFromSharedPreferences(requireContext().applicationContext,"MAX_PARALLEL")
        visibleSeek(csv)

        binding.seekBar.addOnChangeListener{ slider, value, fromUser ->
            visibleSeek(value.toInt())
        }

        binding.updateBtn.setOnClickListener {
            saveIntToSharedPreferences(requireContext().applicationContext,"MAX_PARALLEL",binding.seekBar.value.toInt())
            dismiss()
        }
        binding.closeButton.setOnClickListener {
            dismiss()
        }

    }

    private fun visibleSeek(i: Int) {
        markUnSelected(binding.titleSliderValue1)
        markUnSelected(binding.titleSliderValue2)
        markUnSelected(binding.titleSliderValue3)
        markUnSelected(binding.titleSliderValue4)
        markUnSelected(binding.titleSliderValue5)
        markUnSelected(binding.titleSliderValue6)
        markUnSelected(binding.titleSliderValue7)
        markUnSelected(binding.titleSliderValue8)

        when(i) {
            1 -> {
                markSelected(binding.titleSliderValue1)
            }
            2-> {
                markSelected(binding.titleSliderValue2)
            }
            3-> {
                markSelected(binding.titleSliderValue3)
            }
            4-> {
                markSelected(binding.titleSliderValue4)
            }
            5-> {
                markSelected(binding.titleSliderValue5)
            }
            6-> {
                markSelected(binding.titleSliderValue6)
            }
            7-> {
                markSelected(binding.titleSliderValue7)
            }
            8-> {
                markSelected(binding.titleSliderValue8)
            }
        }
        binding.titleSliderValue1.visibility = VISIBLE
        binding.titleSliderValue8.visibility = VISIBLE
    }

    private fun markSelected(txtView: TextView) {
        txtView.visibility = View.VISIBLE
        txtView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        txtView.setTextColor(ResourcesCompat.getColor(context!!.resources,R.color.slider_green,null))
        txtView.background = (ResourcesCompat.getDrawable(context!!.resources,R.drawable.bg_slider_value,null))
    }

    private fun markUnSelected(txtView: TextView) {
        txtView.visibility = View.INVISIBLE
        txtView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        txtView.setTextColor(ResourcesCompat.getColor(context!!.resources,R.color.black_1,null))
        txtView.background = null
    }
}