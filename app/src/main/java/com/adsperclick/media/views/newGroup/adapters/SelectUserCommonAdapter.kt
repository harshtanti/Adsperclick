package com.adsperclick.media.views.newGroup.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adsperclick.media.data.dataModels.CommonData
import com.adsperclick.media.databinding.UserListItemBinding
import com.adsperclick.media.utils.Utils
import com.adsperclick.media.utils.gone
import com.adsperclick.media.utils.visible

class SelectUserCommonAdapter :
    PagingDataAdapter<CommonData, SelectUserCommonAdapter.ViewHolder>(DiffUtilCallback()) {

    private var bucketName: String? = null
    private var listener: GroupListener? = null

    fun setData(bucketName: String?, listener: GroupListener?) {
        this.bucketName = bucketName
        this.listener = listener
    }

    inner class ViewHolder(private val binding: UserListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(data: CommonData?) {
            data ?: return // If null, return immediately (Paging handles placeholders)

            with(binding) {
                tvName.text = data.name

                // Show or hide tag
                data.tagName?.let {
                    tvTagName.visible()
                    tvTagName.text = it.toString()
                } ?: tvTagName.gone()

                // Set image or default initials
                data.imgUrl?.let { imageUrl ->
                    Utils.loadImageWithGlide(
                        binding.imgProfileDp.context,
                        binding.imgProfileDp,
                        imageUrl
                    )
                } ?: run {
                    Utils.setInitialsDrawable(
                        binding.imgProfileDp,
                        data.name
                    )
                }

                // Handle selection
                if (data.isSelected) btnCheck.visible() else btnCheck.gone()

                userItem.setOnClickListener {
                    data.isSelected = !data.isSelected
                    listener?.btnCheck(bucketName.toString(), data)

                    // Toggle visibility
                    if (data.isSelected) btnCheck.visible() else btnCheck.gone()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = UserListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position)) // Get paginated data
    }

    class DiffUtilCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<CommonData>() {
        override fun areItemsTheSame(oldItem: CommonData, newItem: CommonData) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: CommonData, newItem: CommonData) =
            oldItem == newItem
    }

    interface GroupListener {
        fun btnCheck(bucketName: String, data: CommonData)
    }
}