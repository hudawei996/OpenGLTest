package com.example.openglstudy.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.openglstudy.R
import com.example.openglstudy.model.DayItem

/**
 * 学习天数列表适配器
 */
class DayAdapter(private val dayList: List<DayItem>) :
    RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDay: TextView = itemView.findViewById(R.id.tv_day)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val tvDescription: TextView = itemView.findViewById(R.id.tv_description)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val dayItem = dayList[position]
                    val intent = Intent(itemView.context, dayItem.activityClass)
                    itemView.context.startActivity(intent)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val dayItem = dayList[position]
        holder.tvDay.text = "Day ${dayItem.day.toString().padStart(2, '0')}"
        holder.tvTitle.text = dayItem.title
        holder.tvDescription.text = dayItem.description
    }

    override fun getItemCount(): Int = dayList.size
}
