package com.example.scanner

interface MediaClickListener {
    // Method to handle the onClickListener call of the RecyclerView.
    fun onMediaClick(path: String, id: String)
}