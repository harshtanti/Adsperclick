package com.example.adsperclick.views.chat.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.adsperclick.data.dataModels.Company
import com.example.adsperclick.databinding.HorizontalCompanyListItemBinding
import com.google.android.material.textview.MaterialTextView


class HorizontalCompanyListAdapter(
    private var items: List<Company>?,
    private var companyId: String?,
    private val onItemClick: (data: Company?) -> Unit
) : RecyclerView.Adapter<HorizontalCompanyListAdapter.MyViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = HorizontalCompanyListItemBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding.root)
    }

    override fun getItemCount() = items?.size?:0

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val company = items?.elementAt(position)
        holder.bindItems(company, companyId)
        val binding = HorizontalCompanyListItemBinding.bind(holder.itemView)
        binding.root.setOnClickListener {
            companyId = company?.companyId
//            holder.changeView(binding.textName,isSelected = true)
            onItemClick(company)
        }
    }

    inner class MyViewHolder(
        itemView: View,
    ) : RecyclerView.ViewHolder(itemView) {
        fun bindItems(data: Company?, companyId: String?) {
            val binding = HorizontalCompanyListItemBinding.bind(itemView)
            with(binding) {
//                textName.text = data?.warehouseName?.takeIf { it.isNotEmpty() } ?: data?.warehouseName
//                if (wareHouseId == data?.warehouseId) {
//                    changeView(textName,isSelected = true)
//                }else{
//                    changeView(textName,isSelected = false)
//                }

                tvItemHorizontalCompanyList.text = data?.companyName
            }
        }
//        fun changeView(view: MaterialTextView, isSelected: Boolean = false) {
//            val textColor = if (isSelected) {
//                R.color.blue100
//            } else {
//                R.color.white100
//            }
//            val backgroundColor = if (isSelected) {
//                R.drawable.background_white_button_disabled
//            } else {
//                R.drawable.background_rectangle_blue_corner_circle
//            }
//            view.setTextColor(
//                ContextCompat.getColorStateList(
//                    view.context,
//                    textColor
//                )
//            )
//            view.background =
//                AppCompatResources.getDrawable(view.context, backgroundColor)
//        }
    }
}

