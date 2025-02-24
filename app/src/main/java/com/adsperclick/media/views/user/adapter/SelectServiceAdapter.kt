package com.adsperclick.media.views.user.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adsperclick.media.data.dataModels.CommonData
import com.adsperclick.media.databinding.ServiceListItemBinding
import com.adsperclick.media.utils.invisible
import com.adsperclick.media.utils.visible


class SelectServiceAdapter: ListAdapter<CommonData, SelectServiceAdapter.ViewHolder>(DiffUtil()) {

    private var bucketName: String? = null
    private var listener: ServiceListener? = null

    fun setData(bucketName: String?, listener: ServiceListener?) {
        this.bucketName = bucketName
        this.listener = listener
    }

    inner class ViewHolder(val binding: ServiceListItemBinding): RecyclerView.ViewHolder(binding.root)
    {
        fun bind(data: CommonData?) {
            data ?: return

            with(binding) {
                tvName.text = data.name
                if (data.isSelected){binding.btnCheck.visible()}
                serviceItem.setOnClickListener {
                    data.isSelected = !data.isSelected
                    listener?.btnCheck(bucketName.toString(), data)
                    if (data.isSelected){
                        binding.btnCheck.visible()
                    }else{
                        binding.btnCheck.invisible()
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ServiceListItemBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffUtil : androidx.recyclerview.widget.DiffUtil.ItemCallback<CommonData>(){
        override fun areItemsTheSame(oldItem: CommonData, newItem: CommonData): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CommonData, newItem: CommonData): Boolean {
            return oldItem == newItem
        }
    }

    interface ServiceListener {
        fun btnCheck(bucketName: String, data: CommonData)
    }

}