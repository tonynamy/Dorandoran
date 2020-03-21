package com.iseokchan.dorandoran.adapters

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.iseokchan.dorandoran.R
import com.iseokchan.dorandoran.models.ChatRoom
import com.iseokchan.dorandoran.models.User

class ChatRoomAdapter(var chatRooms: ArrayList<ChatRoom>, var my_uid: String?) :
    RecyclerView.Adapter<ChatRoomAdapter.MyViewHolder>() {

    interface onItemClicked {
        fun onChatRoomClicked(view: View, position: Int, chatroom: ChatRoom)
    }

    var itemClick: onItemClicked? = null

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder.
    // Each data item is just a string in this case that is shown in a TextView.
    class MyViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        var ivChatRoomThumbnail: ImageView = view.findViewById(R.id.iv_chatroom_thumbnail)
        var tvChatroomname: TextView = view.findViewById(R.id.tv_chatroom_name)
        var tvRecentMessage: TextView = view.findViewById(R.id.tv_recent_messsage)
        var tvUnreads: TextView = view.findViewById(R.id.tv_unreads)
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.chatroom_item, parent, false) as View

        return MyViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        itemClick?.let {
            holder.view.setOnClickListener { v ->
                it.onChatRoomClicked(v, position, chatRooms[position])
            }
        }

        //1:1 채팅 전용 - 그룹 채팅 구현 시 변경 필요
        holder.tvChatroomname.text = chatRooms[position].userModels?.find { !it.uid.equals(my_uid) }?.displayName
            ?: holder.view.context.getString(R.string.unknownUser)

        holder.tvRecentMessage.text = chatRooms[position].messages?.last()?.content ?: ""

        val profileImage = chatRooms[position].userModels?.find { !it.uid.equals(my_uid) }?.profileImage

        if(profileImage.isNullOrBlank()) {

            holder.ivChatRoomThumbnail.setImageDrawable(
                ContextCompat.getDrawable(
                    holder.view.context,
                    R.drawable.ic_message_purple_24dp
                )
            )

        } else{

            Glide
                .with(holder.view.context)
                .load(profileImage)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .apply(RequestOptions.circleCropTransform())
                .into(holder.ivChatRoomThumbnail)

        }

        // unreads
        chatRooms[position].messages?.let {

            if( it.size.minus(1) > chatRooms[position].seen?.get(my_uid) ?: 0 ) {

                val unreadMessageCount = it.size.minus(chatRooms[position].seen?.get(my_uid) ?: 0)

                holder.tvUnreads.visibility = View.VISIBLE
                holder.tvUnreads.text = unreadMessageCount.toString()

            } else {
                holder.tvUnreads.visibility = View.GONE
            }

        }


        //--------------------
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = chatRooms.size

    fun updateList(chatRooms: ArrayList<ChatRoom>, my_uid: String? = null) {
        this.chatRooms = chatRooms
        this.my_uid = my_uid
        notifyDataSetChanged()
    }
}