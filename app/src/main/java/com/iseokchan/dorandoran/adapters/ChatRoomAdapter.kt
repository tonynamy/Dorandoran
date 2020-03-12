package com.iseokchan.dorandoran.adapters

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iseokchan.dorandoran.R
import com.iseokchan.dorandoran.models.ChatRoom

class ChatRoomAdapter(var chatRooms: ArrayList<ChatRoom>):
    RecyclerView.Adapter<ChatRoomAdapter.MyViewHolder>() {

    interface ItemClick
    {
        fun onClick(view: View, position: Int, chatroom: ChatRoom)
    }
    var itemClick: ItemClick? = null

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder.
    // Each data item is just a string in this case that is shown in a TextView.
    class MyViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        var ivChatRoomThumbnail: ImageView = view.findViewById(R.id.iv_chatroom_thumbnail)
        var tvChatroomname: TextView = view.findViewById(R.id.tv_chatroom_name)
        var tvRecentMessage: TextView = view.findViewById(R.id.tv_recent_messsage)
    }


    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): MyViewHolder {
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
                it.onClick(v, position, chatRooms[position])
            }
        }

        holder.tvChatroomname.text = chatRooms[position].displayName
        holder.tvRecentMessage.text = chatRooms[position].messages?.last()?.content ?: ""

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            holder.ivChatRoomThumbnail.setImageDrawable(holder.view.resources.getDrawable(R.drawable.ic_message_purple_24dp, holder.view.resources.newTheme()))
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = chatRooms.size

    fun updateList(chatRooms: ArrayList<ChatRoom>) {
        this.chatRooms = chatRooms
        notifyDataSetChanged()
    }
}