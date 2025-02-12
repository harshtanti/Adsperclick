package com.adsperclick.media.views.chat.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adsperclick.media.data.dataModels.CommonData
import com.adsperclick.media.databinding.UserListItemBinding
import com.adsperclick.media.utils.UtilityFunctions
import com.adsperclick.media.utils.gone
import com.adsperclick.media.utils.visible

class SelectUserCommonAdapter:ListAdapter<CommonData,SelectUserCommonAdapter.ViewHolder>(DiffUtil()) {

    var bucketName:String?=null
    var listener: GroupListener?=null

    fun setData(bucketName:String?,listener: GroupListener?){
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
                if(data.isSelected){
                    btnCheck.visible()
                }else{
                    btnCheck.gone()
                }
                userItem.setOnClickListener{
                    listener?.btnCheck(bucketName.toString(),data.id.toString(),!data.isSelected)
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

    interface GroupListener{
        fun btnCheck(bucketName: String,id:String,isSelected:Boolean)
    }
}