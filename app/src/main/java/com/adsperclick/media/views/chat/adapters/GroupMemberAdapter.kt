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

class GroupMemberAdapter:ListAdapter<CommonData, GroupMemberAdapter.ViewHolder>(DiffUtil()) {

    var listener: AddMemberListener?=null
    var isAdmin:Boolean = false

    fun setData(isAdmin : Boolean, listener: AddMemberListener?){
        this.isAdmin=isAdmin
        this.listener=listener
    }

    inner class ViewHolder(val binding: UserListItemBinding): RecyclerView.ViewHolder(binding.root)
    {
        fun bind(dataItem: CommonData?)
        {
            with(binding){
                dataItem?.let { data ->
                    tvName.text = data.name
                    data.tagName?.let {
                        tvTagName.visible()
                        tvTagName.text=it
                    } ?: run { tvTagName.gone() }
                    data.imgUrl?.let { imageUrl ->
                        UtilityFunctions.loadImageWithGlide(
                            binding.imgProfileDp.context,
                            binding.imgProfileDp,
                            imageUrl
                        )
                    } ?: run {
                        UtilityFunctions.setInitialsDrawable(
                            binding.imgProfileDp,
                            data.name
                        )
                    }
                    if (isAdmin){
                        btnDelete.visible()
                        data.tagName?.let {
                            tvTagName.visible()
                            tvTagName.text=it
                        }
                    }
                    btnDelete.setOnClickListener{
                        listener?.btnDelete(data)
                    }
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

    interface AddMemberListener{
        fun btnDelete(data: CommonData)
    }
}