package com.iseokchan.dorandoran.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.iseokchan.dorandoran.R
import com.iseokchan.dorandoran.models.Chat
import com.iseokchan.dorandoran.models.ChatRoom
import com.iseokchan.dorandoran.models.EmoticonPack
import com.iseokchan.dorandoran.models.User
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class ChatAdapter(
    var chatRoom: ChatRoom,
    val my_uid: String,
    emoticonPacks: List<EmoticonPack> = ArrayList()
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var me: User? = null
    private var notMe: User? = null

    private val IMAGEVIEW_SIZE = 300

    var emoticonPacks = emoticonPacks
        set(value) {
            field = value
            this.notifyDataSetChanged()
        }

    interface chatAdapterCallback {
        fun onMessageLongClicked(view: View, position: Int, chat: Chat)
        fun onGlideLoadFin()
    }

    var callBack
            : chatAdapterCallback? = null

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder.
    // Each data item is just a string in this case that is shown in a TextView.
    inner class MyTextChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var tvMessageBody: TextView = view.findViewById(R.id.message_body)
        var tvTime: TextView = view.findViewById(R.id.message_time)
        var tvSeen: TextView = view.findViewById(R.id.message_seen)
    }

    inner class NotMyTextChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var tvMessageBody: TextView = view.findViewById(R.id.message_body)
        var vAvatar: ImageView = view.findViewById(R.id.avatar)
        var tvName: TextView = view.findViewById(R.id.name)
        var tvTime: TextView = view.findViewById(R.id.message_time)
        var tvSeen: TextView = view.findViewById(R.id.message_seen)
    }

    inner class MyImageChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var ivMessageImage: ImageView = view.findViewById(R.id.message_image)
        var tvTime: TextView = view.findViewById(R.id.message_time)
        var tvSeen: TextView = view.findViewById(R.id.message_seen)

        init {
            ivMessageImage.layoutParams.width = IMAGEVIEW_SIZE
            ivMessageImage.layoutParams.height = IMAGEVIEW_SIZE
        }
    }

    inner class NotMyImageChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var ivMessageImage: ImageView = view.findViewById(R.id.message_image)
        var vAvatar: ImageView = view.findViewById(R.id.avatar)
        var tvName: TextView = view.findViewById(R.id.name)
        var tvTime: TextView = view.findViewById(R.id.message_time)
        var tvSeen: TextView = view.findViewById(R.id.message_seen)
        init {
            ivMessageImage.layoutParams.width = IMAGEVIEW_SIZE
            ivMessageImage.layoutParams.height = IMAGEVIEW_SIZE
        }
    }

    override fun getItemViewType(position: Int): Int {


        val currentChat = chatRoom.messages?.get(position) ?: return 0

        val userFlag = if (currentChat.uid == my_uid) 1 else 2
        val emoticonFlag = if (currentChat.emoticon == null) 1 else -2

        return userFlag * emoticonFlag
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            -4 -> NotMyImageChatViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.chat_item_image_not_my, parent, false) as View
            )
            -2 -> MyImageChatViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.chat_item_image_my, parent, false) as View
            )
            1 -> MyTextChatViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.chat_item_my, parent, false) as View
            )
            2 -> NotMyTextChatViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.chat_item_not_my, parent, false) as View
            )
            else -> MyTextChatViewHolder(
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

        callBack?.let {
            holder.itemView.setOnLongClickListener { _ ->
                it.onMessageLongClicked(holder.itemView, position, currentChat)
                true
            }
        }

        val timeFormat = SimpleDateFormat("MM/dd a hh:mm", Locale.KOREA)

        val isEmoticon = currentChat.emoticon != null

        when (holder.itemViewType) {

            1 -> { // my text

                val myHolder = holder as MyTextChatViewHolder

                myHolder.tvMessageBody.text = currentChat.content

                myHolder.tvTime.text =
                    if (currentChat.createdAt != null) timeFormat.format(currentChat.createdAt) else "알 수 없는 시간"

                chatRoom.seen?.let {

                    if (position <= it[notMe?.uid] ?: -1) {
                        myHolder.tvSeen.text = buildSeenText(myHolder.itemView.context, true)
                        myHolder.tvSeen.visibility = View.VISIBLE

                    } else {
                        myHolder.tvSeen.visibility = View.GONE
                    }

                }


            }

            2 -> { // not my text

                val notMyHolder = holder as NotMyTextChatViewHolder

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

                notMyHolder.tvTime.text =
                    if (currentChat.createdAt != null) timeFormat.format(currentChat.createdAt) else "알 수 없는 시간"

                chatRoom.seen?.let {

                    if (position <= it[me?.uid] ?: -1) {

                        notMyHolder.tvSeen.text = buildSeenText(notMyHolder.itemView.context, false)
                        notMyHolder.tvSeen.visibility = View.VISIBLE

                    } else {
                        notMyHolder.tvSeen.visibility = View.GONE
                    }

                }

            }

            -2 -> { // my image

                val myHolder = holder as MyImageChatViewHolder

                val emoticon = currentChat.emoticon!!
                val currentEmoticonPack =
                    emoticonPacks.find { it.id == emoticon.emoticonPackId }
                val currentEmoticon =
                    currentEmoticonPack?.emoticons?.find { it.displayName == emoticon.displayName }

                val circularProgressDrawable =
                    CircularProgressDrawable(myHolder.itemView.context)
                circularProgressDrawable.strokeWidth = 5f
                circularProgressDrawable.centerRadius = 30f
                circularProgressDrawable.start()

                Glide
                    .with(myHolder.itemView.context)
                    .load(currentEmoticon?.url)
                    .placeholder(circularProgressDrawable)
                    .error(circularProgressDrawable)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .override(IMAGEVIEW_SIZE)
                    .into(myHolder.ivMessageImage)

                myHolder.tvTime.text =
                    if (currentChat.createdAt != null) timeFormat.format(currentChat.createdAt) else "알 수 없는 시간"

                chatRoom.seen?.let {

                    if (position <= it[notMe?.uid] ?: -1) {
                        myHolder.tvSeen.text = buildSeenText(myHolder.itemView.context, true)
                        myHolder.tvSeen.visibility = View.VISIBLE

                    } else {
                        myHolder.tvSeen.visibility = View.GONE
                    }

                }


            }

            -4 -> { // not my image

                val notMyHolder = holder as NotMyImageChatViewHolder

                val emoticon = currentChat.emoticon!!
                val currentEmoticonPack =
                    emoticonPacks.find { it.id == emoticon.emoticonPackId }
                val currentEmoticon =
                    currentEmoticonPack?.emoticons?.find { it.displayName == emoticon.displayName }

                val circularProgressDrawable =
                    CircularProgressDrawable(notMyHolder.itemView.context)
                circularProgressDrawable.strokeWidth = 5f
                circularProgressDrawable.centerRadius = 30f
                circularProgressDrawable.start()

                Glide
                    .with(notMyHolder.itemView.context)
                    .load(currentEmoticon?.url)
                    .placeholder(circularProgressDrawable)
                    .error(circularProgressDrawable)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .override(IMAGEVIEW_SIZE)
                    .into(notMyHolder.ivMessageImage)

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

                notMyHolder.tvTime.text =
                    if (currentChat.createdAt != null) timeFormat.format(currentChat.createdAt) else "알 수 없는 시간"

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