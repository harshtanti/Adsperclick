package com.adsperclick.media.views.user.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adsperclick.media.data.dataModels.CommonData
import com.adsperclick.media.databinding.UserListItemBinding
import com.adsperclick.media.utils.Constants
import com.adsperclick.media.utils.UtilityFunctions
import com.adsperclick.media.utils.gone
import com.adsperclick.media.utils.visible

class CommonAdapter:ListAdapter<CommonData,CommonAdapter.ViewHolder>(DiffUtil()) {

    var bucketName:String?=null
    var listener:CommunityListener?=null

    fun setData(bucketName:String?,listener:CommunityListener?){
        this.bucketName=bucketName
        this.listener=listener
    }

    inner class ViewHolder(val binding: UserListItemBinding): RecyclerView.ViewHolder(binding.root)
    {
        fun bind(data: CommonData)
        {
            with(binding){
                tvName.text = data.name
                data.tagName?.let {
                    tvTagName.visible()
                    tvTagName.text=it
                } ?: run { tvTagName.gone() }
                data.imgUrl?.let {  }?: run{
                    val drawable = UtilityFunctions.generateInitialsDrawable(
                        imgProfileDp.context, data.name ?: "A")
                    imgProfileDp.setImageDrawable(drawable)
                }
                when(bucketName){
                    Constants.EMPLOYEES_SEMI_CAPS, Constants.CLIENTS_SEMI_CAPS -> {
                        btnInfo.visible()
                    }
                    Constants.SERVICES_SEMI_CAPS, Constants.COMPANIES_SEMI_CAPS -> {
                        btnDelete.visible()
                    }
                    else ->{
                        btnInfo.gone()
                        btnDelete.gone()
                    }
                }
                btnDelete.setOnClickListener{
                    listener?.btnDelete(bucketName.toString(),data.id.toString())
                }
                btnInfo.setOnClickListener{
                    listener?.btnInfo(bucketName.toString(),data.id.toString(),data.name.toString())
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = UserListItemBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffUtil : androidx.recyclerview.widget.DiffUtil.ItemCallback<CommonData>()
    {
        override fun areItemsTheSame(oldItem: CommonData, newItem: CommonData): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CommonData, newItem: CommonData): Boolean {
            return oldItem == newItem
        }
    }

    interface CommunityListener{
        fun btnDelete(bucketName: String,id:String)
        fun btnInfo(bucketName: String, id: String, name: String)
    }
}