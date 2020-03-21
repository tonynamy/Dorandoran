package com.iseokchan.dorandoran.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Visibility
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.iseokchan.dorandoran.R
import com.iseokchan.dorandoran.models.Chat
import com.iseokchan.dorandoran.models.ChatRoom
import com.iseokchan.dorandoran.models.User
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ChatAdapter(var chatRoom: ChatRoom, val my_uid: String) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var me: User? = null
    private var notMe: User? = null

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder.
    // Each data item is just a string in this case that is shown in a TextView.
    class MyChatViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        var tvMessageBody: TextView = view.findViewById(R.id.message_body)
        var tvTime: TextView = view.findViewById(R.id.message_time)
        var tvSeen: TextView = view.findViewById(R.id.message_seen)
    }

    class NotMyChatViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        var tvMessageBody: TextView = view.findViewById(R.id.message_body)
        var vAvatar: ImageView = view.findViewById(R.id.avatar)
        var tvName: TextView = view.findViewById(R.id.name)
        var tvTime: TextView = view.findViewById(R.id.message_time)
        var tvSeen: TextView = view.findViewById(R.id.message_seen)
    }

    override fun getItemViewType(position: Int): Int {
        // Just as an example, return 0 or 2 depending on position
        // Note that unlike in ListView adapters, types don't have to be contiguous
        val currentChat = chatRoom.messages?.get(position) ?: return 0

        return if (currentChat.uid == my_uid) 0 else 1
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> MyChatViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.chat_item_my, parent, false) as View
            )
            1 -> NotMyChatViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.chat_item_not_my, parent, false) as View
            )
            else -> MyChatViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.chat_item_my, parent, false) as View
            )
        }
    }

    private fun buildSeenText(context: Context, isMy: Boolean): String = if (isMy) {
        context.getString(R.string.messageSeen) + context.getString(R.string.MIDDLE_DOT)
    } else {
        context.getString(R.string.MIDDLE_DOT) + context.getString(R.string.messageSeen)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        val currentChat = chatRoom.messages?.get(position) ?: return

        val timeFormat = SimpleDateFormat("MM/dd a hh:mm", Locale.KOREA)

        when (holder.itemViewType) {

            0 -> { // my

                val myHolder = holder as MyChatViewHolder

                myHolder.tvMessageBody.text = currentChat.content

                myHolder.tvTime.text = timeFormat.format(currentChat.createdAt)

                chatRoom.seen?.let {

                    if (position <= it[notMe?.uid] ?: -1) {
                            myHolder.tvSeen.text = buildSeenText(myHolder.itemView.context, true)
                            myHolder.tvSeen.visibility = View.VISIBLE

                    } else {
                        myHolder.tvSeen.visibility = View.GONE
                    }

                }


            }

            1 -> { // not my

                val notMyHolder = holder as NotMyChatViewHolder

                notMyHolder.tvMessageBody.text = currentChat.content

                val userName = notMe?.displayName
                    ?: notMyHolder.itemView.context.getString(R.string.unknownUser)
                notMyHolder.tvName.text = userName

                val profileImage = notMe?.profileImage

                notMyHolder.vAvatar.drawable

                profileImage?.let {
                    Glide
                        .with(notMyHolder.itemView.context)
                        .load(it)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .apply(RequestOptions.circleCropTransform())
                        .into(notMyHolder.vAvatar)
                }

                notMyHolder.tvTime.text = timeFormat.format(currentChat.createdAt)

                chatRoom.seen?.let {

                    if (position <= it[me?.uid] ?: -1) {

                        notMyHolder.tvSeen.text = buildSeenText(notMyHolder.itemView.context, false)
                        notMyHolder.tvSeen.visibility = View.VISIBLE

                    } else {
                        notMyHolder.tvSeen.visibility = View.GONE
                    }

                }

            }


        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = chatRoom.messages?.size ?: 0

    fun updateList(chatRoom: ChatRoom) {
        this.chatRoom = chatRoom
        this.me = chatRoom.userModels?.find { it.uid == my_uid }
        this.notMe = chatRoom.userModels?.find { it.uid != my_uid }

        notifyDataSetChanged()
    }
}