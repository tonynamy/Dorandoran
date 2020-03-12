package com.iseokchan.dorandoran.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iseokchan.dorandoran.R
import com.iseokchan.dorandoran.models.Chat

class ChatAdapter(var chats: List<Chat>, val uid:String):
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder.
    // Each data item is just a string in this case that is shown in a TextView.
    class MyChatViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        var tvMessageBody: TextView = view.findViewById(R.id.message_body)
    }

    class NotMyChatViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        var tvMessageBody: TextView = view.findViewById(R.id.message_body)
        var vAvatar: View = view.findViewById(R.id.avatar)
        var tvName: TextView = view.findViewById(R.id.name)
    }

    override fun getItemViewType(position: Int): Int {
        // Just as an example, return 0 or 2 depending on position
        // Note that unlike in ListView adapters, types don't have to be contiguous
        val currentChat = chats[position]

        return if (currentChat.uid == uid) 0 else 1
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            0 -> MyChatViewHolder( LayoutInflater.from(parent.context).inflate(R.layout.chat_item_my, parent, false) as View )
            1 -> NotMyChatViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.chat_item_not_my, parent, false) as View)
            else -> MyChatViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.chat_item_my, parent, false) as View)
        }
    }
    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        val currentChat = chats[position]

        when(holder.itemViewType) {

            0 -> { // my

                val myHolder = holder as MyChatViewHolder
                myHolder.tvMessageBody.text = currentChat.content


            }

            1 -> { // not my

                val notMyHolder = holder as NotMyChatViewHolder
                notMyHolder.tvMessageBody.text = currentChat.content
                notMyHolder.tvName.text = currentChat.content


            }


        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = chats.size

    fun updateList(chats: ArrayList<Chat>) {
        this.chats = chats
        notifyDataSetChanged()
    }
}