package com.example.scanner

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView


class RecyclerViewAdapter(private val context: Context, private val myListner: MediaClickListener) :
    RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder>() {
    private var photo: ArrayList<String> = ArrayList()
    private var ids: ArrayList<String> = ArrayList()

    fun addFoto(f: ArrayList<String>, id: ArrayList<String>) {
        if (photo.isNotEmpty()) {
            photo.addAll(f)
            ids.addAll(id)
            notifyDataSetChanged()
        } else {
            photo = f
            ids = id
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.single_view, parent, false)
        return MyViewHolder(view, myListner)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.image.setImageBitmap(
            BitmapUtils.decodeSampledBitmapFromResource(
                photo[position],
                100,
                100
            )
        )
    }

    override fun getItemCount(): Int {
        return photo.size
    }

    /**
     * Class that contains the single element of the RecyclerView.
     */
    inner class MyViewHolder(itemView: View, private val myListner: MediaClickListener) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {

        val image: ImageView = itemView.findViewById(R.id.image_view)

        init {
            image.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            myListner.onMediaClick(photo[adapterPosition], ids[adapterPosition])
        }
    }
}
