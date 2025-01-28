package com.adsperclick.media.applicationCommonView.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.adsperclick.media.databinding.SpinnerWithErrorLayoutBinding

class SpinnerWithError : ConstraintLayout {

    private lateinit var binding: SpinnerWithErrorLayoutBinding
    private var itemList: ArrayList<String> = ArrayList()

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        init()
    }

    private fun init(){
        binding = SpinnerWithErrorLayoutBinding.inflate(LayoutInflater.from(context),this,true)
        setOnClickListener()
    }

    private fun setOnClickListener() {
        with(binding){
            spinnerLayout.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
                setVisibilityOfError(View.GONE)
            }

            spinnerLayout.threshold = 0

            spinnerLayout.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    binding.spinnerLayout.showDropDown()
                }
            }

            spinnerLayout.setOnClickListener {
                binding.spinnerLayout.showDropDown()
            }
        }
    }

    fun showList(){
        binding.spinnerLayout.showDropDown()
    }

    fun getSpinnerView(): AutoCompleteTextView {
        return binding.spinnerLayout
    }

    fun setErrorText(value: String, view: Int) {
        binding.error.text = value
        setVisibilityOfError(view)
    }

    fun setVisibilityOfError(visibility: Int) {
        binding.error.visibility = visibility
    }

    fun setHint(value: Int) {
        binding.inputLayout.setHint(value)
    }

    fun setDataItemList(list: List<Any>) {
        setDataItemList(list, android.R.layout.simple_spinner_dropdown_item)
    }

    fun setDataItemList(list: List<Any>, res: Int) {
        if (list.isEmpty()) {
            return
        }

        itemList.clear()
        for (item in list) {
            itemList.add(item.toString())
        }
        val adapter = ArrayAdapter(this.context, res, itemList)
        with(binding){
            spinnerLayout.setAdapter(adapter)
        }
    }

    fun getSelectedItemPosition(): Int {
        return binding.spinnerLayout.listSelection
    }

    fun getSelectedItem(): String? {
        return binding.spinnerLayout.text?.toString()
    }

    fun selectItemAt(position: Int) {
        if (position >= 0 && position < itemList.size) {
            binding.spinnerLayout.setText(itemList[position], false)
        }
    }
}