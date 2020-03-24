package com.iseokchan.dorandoran.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.iseokchan.dorandoran.R
import com.iseokchan.dorandoran.models.Emoticon
import com.iseokchan.dorandoran.models.EmoticonPack


class EmoticonPackAdapter(private var emoticonPacks: List<EmoticonPack>) :
    RecyclerView.Adapter<PagerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagerViewHolder =
        PagerViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.emoticon_pack_item, parent, false)
        )

    override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {

        val emoticonPack = emoticonPacks[position]


        holder.recyclerView.layoutManager = GridLayoutManager(holder.itemView.context, 4)
        holder.recyclerView.adapter = GridAdapter(holder.itemView.context, emoticonPack.emoticons)

    }

    override fun getItemCount(): Int = emoticonPacks.size
}

class GridAdapter(context: Context, var emoticons: ArrayList<Emoticon>) :
    RecyclerView.Adapter<GridAdapter.ViewHolder>() {
    private val layoutInflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(layoutInflater.inflate(R.layout.emoticon_item, parent, false))
    }

    override fun onBindViewHolder(
        viewHolder: ViewHolder,
        position: Int
    ) {

        val circularProgressDrawable = CircularProgressDrawable(viewHolder.itemView.context)
        circularProgressDrawable.strokeWidth = 5f
        circularProgressDrawable.centerRadius = 30f
        circularProgressDrawable.start()

        Glide
            .with(viewHolder.itemView.context)
            .load(emoticons[position].url)
            .override(150, 150)
            .placeholder(circularProgressDrawable)
            .error(R.drawable.ic_error_outline_black_24dp)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(viewHolder.ivEmoticon)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        return emoticons.size
    }

    inner class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var ivEmoticon: ImageView = itemView.findViewById(R.id.iv_emoticon)
    }
}

class PagerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private var id: String = ""
    private var displayName: String = ""
    private var author: String = ""

    val recyclerView: RecyclerView = itemView.findViewById(R.id.rv_emoticon_pack)
}