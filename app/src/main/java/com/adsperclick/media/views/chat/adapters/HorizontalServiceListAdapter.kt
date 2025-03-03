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
    var selectedPosition = 0
    interface OnServiceClickListener{
        fun onItemClick(service: Service)
    }

    inner class MyViewHolder(val binding: HorizontalCompanyListItemBinding): RecyclerView.ViewHolder(binding.root)
    {
        fun bind(service: Service, position: Int)
        {
            binding.tvItemHorizontalCompanyList.text = service.serviceName

            binding.root.isSelected = (selectedPosition == position)

            binding.root.setOnClickListener{
                val previousSelectedPosition = selectedPosition
                selectedPosition = position
                notifyItemChanged(previousSelectedPosition)
                notifyItemChanged(selectedPosition)
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
        holder.bind(service, position)
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

