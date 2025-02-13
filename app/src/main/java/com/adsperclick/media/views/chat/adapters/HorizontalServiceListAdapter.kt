package com.adsperclick.media.views.chat.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adsperclick.media.data.dataModels.Company
import com.adsperclick.media.data.dataModels.Service
import com.adsperclick.media.databinding.HorizontalCompanyListItemBinding


class HorizontalServiceListAdapter(val onServiceClickListener: OnServiceClickListener) : ListAdapter<Service, HorizontalServiceListAdapter.MyViewHolder>(DiffUtil())
{

    interface OnServiceClickListener{
        fun onItemClick(service: Service)
    }

    inner class MyViewHolder(val binding: HorizontalCompanyListItemBinding): RecyclerView.ViewHolder(binding.root)
    {
        fun bind(service: Service)
        {
//            binding.tvGroupName.text = chatGroup.groupName
//            binding.tvLastMsg.text= chatGroup.lastSentMsg?.message ?: ""
//            binding.tvLastMsgDateTime.text = chatGroup.lastSentMsg?.timestamp.toString()


            binding.tvItemHorizontalCompanyList.text = service.serviceName

            binding.root.setOnClickListener{
                onServiceClickListener.onItemClick(service)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding= HorizontalCompanyListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val service = getItem(position)
        holder.bind(service)
    }

    class DiffUtil : androidx.recyclerview.widget.DiffUtil.ItemCallback<Service>()
    {
        override fun areItemsTheSame(oldItem: Service, newItem: Service): Boolean {
            return oldItem.serviceId == newItem.serviceId
        }

        override fun areContentsTheSame(oldItem: Service, newItem: Service): Boolean {
            return oldItem == newItem
        }
    }
}

