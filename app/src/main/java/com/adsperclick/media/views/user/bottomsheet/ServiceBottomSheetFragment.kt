package com.adsperclick.media.views.user.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.adsperclick.media.data.dataModels.CommonData
import com.adsperclick.media.data.dataModels.NetworkResult
import com.adsperclick.media.data.dataModels.Service
import com.adsperclick.media.databinding.FragmentServiceBottomSheetBinding
import com.adsperclick.media.views.user.adapter.SelectServiceAdapter
import com.adsperclick.media.views.user.viewmodel.UserViewModel
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
        setUpClickListener()
        markAlreadySelected()
        setUpAdapter()
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

    override fun onClick(v: View?) {
        when(v){
            binding.btnClose ->{
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