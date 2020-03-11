package com.ptktop.downloadmultifileandroid.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ptktop.downloadmultifileandroid.dao.DownloadDao
import com.ptktop.downloadmultifileandroid.view.ItemDownloadViewGroup

class DownloadAdapter(@JvmField private val listDao: ArrayList<DownloadDao>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        @JvmField
        var listener: OnItemClickListener? = null
        var listClick: ArrayList<DownloadDao>? = null
    }

    interface OnItemClickListener {
        fun onItemClick(itemView: View, position: Int)
    }

    fun setOnItemClickListener(listenerParam: OnItemClickListener) {
        listener = listenerParam
        listClick = listDao
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: View = ItemDownloadViewGroup(parent.context)
        view.layoutParams = RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.MATCH_PARENT,
            RecyclerView.LayoutParams.WRAP_CONTENT
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val dao = listDao[position]
        val view = (holder as ViewHolder).itemView
        if (view is ItemDownloadViewGroup) {
            view.setupView(dao, position)
        }
    }

    override fun getItemCount(): Int {
        return listDao.size
    }

    fun updateProgress(dao: DownloadDao) {
        for (i in listDao.indices) {
            if (listDao[i].url == dao.url) {
                listDao.removeAt(i)
                listDao.add(i,dao)
                notifyItemChanged(i)
                break
            }
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            if (listClick!!.size > 0) {
                itemView.setOnClickListener {
                    if (listener != null)
                        listener!!.onItemClick(itemView, layoutPosition)
                }
            }
        }
    }

}