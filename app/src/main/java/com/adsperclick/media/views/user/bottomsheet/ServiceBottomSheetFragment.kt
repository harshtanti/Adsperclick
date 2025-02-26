package com.adsperclick.media.views.user.bottomsheet

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.adsperclick.media.data.dataModels.CommonData
import com.adsperclick.media.databinding.FragmentServiceBottomSheetBinding
import com.adsperclick.media.views.user.adapter.SelectServiceAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


private const val ARG_PARAM1 = "param1"

class ServiceBottomSheetFragment : BottomSheetDialogFragment(),View.OnClickListener {

    private lateinit var binding: FragmentServiceBottomSheetBinding
    private lateinit var adapter: SelectServiceAdapter
    private lateinit var listener: SelectServiceAdapter.ServiceListener
    private lateinit var multiSelectListener: MultiSelectListener
    private var selectedDataList = arrayListOf<CommonData>()
    private lateinit var dataList:List<CommonData>


    private lateinit var bucketName: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            bucketName = it.getString(ARG_PARAM1).toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentServiceBottomSheetBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpBottomSheetState()
        setUpClickListener()
        markAlreadySelected()
        setUpAdapter()
        setupSearch()
    }

    private fun setUpBottomSheetState(){
        dialog?.let { dialog ->
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(it)
                behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = resources.displayMetrics.heightPixels
            }
        }
    }

    private fun setUpClickListener(){
        binding.submitButton.setOnClickListener(this)
        binding.btnClose.setOnClickListener(this)
    }

    private fun setUpAdapter(){
        adapter = SelectServiceAdapter()
        listener = object : SelectServiceAdapter.ServiceListener{
            override fun btnCheck(bucketName: String, data: CommonData) {
                if (data.isSelected){
                    selectedDataList.add(data)
                }else{
                    selectedDataList.remove(data)
                }
            }
        }
        adapter.setData(bucketName,listener)
        binding.rvService.adapter = adapter
        adapter.submitList(dataList)
    }

    private fun markAlreadySelected(){
        selectedDataList.forEach { selectedData ->
            val index = dataList.indexOfFirst { it.id == selectedData.id }
            if (index != -1) {
                dataList[index].isSelected = true
            }
        }
    }

    private fun setupSearch() {
        binding.etSearchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString()) // Call filter function
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterList(query: String) {
        val newFilteredList = if (query.length<3) {
            dataList
        } else {
            dataList.filter { it.name?.contains(query, ignoreCase = true) == true }
        }

        adapter.submitList(newFilteredList.toList())
    }

    override fun onClick(v: View?) {
        when(v){
            binding.btnClose ->{
                multiSelectListener.onMultiSelect(bucketName,selectedDataList)
                this.dismiss()
            }
            binding.submitButton -> {
                multiSelectListener.onMultiSelect(bucketName,selectedDataList)
                this.dismiss()
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String,multiSelectListener: MultiSelectListener,dataList:List<CommonData>,selectedDataList:ArrayList<CommonData>) =
            ServiceBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                }
                this.multiSelectListener=multiSelectListener
                this.dataList=dataList
                this.selectedDataList=selectedDataList
            }
    }

    interface MultiSelectListener{
        fun onMultiSelect(
            bucketName: String,
            selectedList: ArrayList<CommonData>
        )
    }
}