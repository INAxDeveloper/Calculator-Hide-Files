package com.example.calculator.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.calculator.R
import java.io.File

class ListFolderAdapter(
    private val onFolderClick: (File) -> Unit,
    private val onFolderLongClick: (File) -> Unit,
    private val onSelectionModeChanged: (Boolean) -> Unit,
    private val onSelectionCountChanged: (Int) -> Unit
) : ListAdapter<File, ListFolderAdapter.FolderViewHolder>(FolderDiffCallback()) {

    private val selectedItems = mutableSetOf<Int>()
    private var isSelectionMode = false

    inner class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val folderNameTextView: TextView = itemView.findViewById(R.id.folderName)

        private val selectedLayer: View = itemView.findViewById(R.id.selectedLayer)

        fun bind(folder: File, onFolderClick: (File) -> Unit, onFolderLongClick: (File) -> Unit, isSelected: Boolean) {
            folderNameTextView.text = folder.name

            selectedLayer.visibility = if (isSelected) View.VISIBLE else View.GONE

            itemView.setOnClickListener { 
                if (isSelectionMode) {
                    toggleSelection(adapterPosition)
                } else {
                    onFolderClick(folder)
                }
            }
            
            itemView.setOnLongClickListener {
                if (!isSelectionMode) {
                    enterSelectionMode()
                    onFolderLongClick(folder)
                    toggleSelection(adapterPosition)
                }
                true
            }
        }

        private fun toggleSelection(position: Int) {
            if (selectedItems.contains(position)) {
                selectedItems.remove(position)
                if (selectedItems.isEmpty()) {
                    exitSelectionMode()
                }
            } else {
                selectedItems.add(position)
            }
            onSelectionCountChanged(selectedItems.size)
            notifyItemChanged(position)
        }
    }

    private fun enterSelectionMode() {
        isSelectionMode = true
        onSelectionModeChanged(true)
    }

    private fun exitSelectionMode() {
        isSelectionMode = false
        onSelectionModeChanged(false)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_folder_list_style, parent, false)
        return FolderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folder = getItem(position)
        holder.bind(folder, onFolderClick, onFolderLongClick, selectedItems.contains(position))
    }

    fun getSelectedItems(): List<File> {
        return selectedItems.mapNotNull { position ->
            if (position < itemCount) getItem(position) else null
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearSelection() {
        val wasInSelectionMode = isSelectionMode
        selectedItems.clear()
        if (wasInSelectionMode) {
            exitSelectionMode()
        }
        onSelectionCountChanged(0)
        notifyDataSetChanged()
    }

    fun onBackPressed(): Boolean {
        return if (isInSelectionMode()) {
            clearSelection()
            true
        } else {
            false
        }
    }

    fun isInSelectionMode(): Boolean {
        return isSelectionMode
    }

    private class FolderDiffCallback : DiffUtil.ItemCallback<File>() {
        override fun areItemsTheSame(oldItem: File, newItem: File): Boolean {
            return oldItem.absolutePath == newItem.absolutePath
        }

        override fun areContentsTheSame(oldItem: File, newItem: File): Boolean {
            return oldItem.name == newItem.name
        }
    }
}