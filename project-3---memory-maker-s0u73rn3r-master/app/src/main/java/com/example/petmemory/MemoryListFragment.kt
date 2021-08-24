package com.example.petmemory

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

private const val TAG = "MemoryListFragment"

class MemoryListFragment : Fragment() {

    interface Callbacks {
        fun onMemorySelected(memoryID: UUID)
    }

    private var callbacks: Callbacks? = null
    private var filterByFavorite = false

    private lateinit var memoryRecyclerView: RecyclerView
    private var adpater: MemoryAdapter? = MemoryAdapter(emptyList())

    private val memoryListViewModel : MemoryListViewModel by lazy {
        ViewModelProviders.of(this).get(MemoryListViewModel::class.java)
    }

    private val memoryDetailViewModel: MemoryDetailViewModel by lazy {
        ViewModelProviders.of(this).get(MemoryDetailViewModel::class.java)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_memory_list, container, false)

        memoryRecyclerView = view.findViewById(R.id.memory_recycler_view) as RecyclerView
        memoryRecyclerView.layoutManager = LinearLayoutManager(context)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        memoryListViewModel.memories.observe(
            viewLifecycleOwner,
            Observer { memories ->
                memories?.let {
                    Log.i(TAG, "Got memories ${memories.size}")
                    updateUI(memories)
                }
            }
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_memory_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.new_memory -> {
                val memory = Memory()
                memoryListViewModel.addMemory(memory)
                callbacks?.onMemorySelected(memory.id)
                true
            }
            R.id.filter_favorite -> {
                filterByFavorite = true
                memoryRecyclerView.adapter?.notifyDataSetChanged()
                true
            }
            R.id.filter_regular -> {
                filterByFavorite = false
                memoryRecyclerView.adapter?.notifyDataSetChanged()
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null;
    }

    private fun updateUI(memories: List<Memory>) {
        adpater = MemoryAdapter(memories)
        memoryRecyclerView.adapter = adpater

    }

    private inner class MemoryAdapter(var memories: List<Memory>): RecyclerView.Adapter<MemoryHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoryHolder {
            val view = layoutInflater.inflate(R.layout.list_item, parent, false)
            return MemoryHolder(view)
        }
        override fun getItemCount() = memories.size

        override fun onBindViewHolder(holder:MemoryHolder, position: Int) {
            if (filterByFavorite)
            {
                if (memories[position].isFavorite)
                {
                    val memory = memories[position]
                    holder.bind(memory)
                }
                else
                {
                    holder.filterBy(true)
                }
            }
            else
            {
                val memory = memories[position]
                holder.bind(memory)
                holder.filterBy(false)
            }
        }
    }

    private inner class MemoryHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        private lateinit var memory: Memory
        private val memoryImageView: ImageView = itemView.findViewById(R.id.memory_image)
        private val titleTextView: TextView = itemView.findViewById(R.id.memory_title)
        private val dateTextView: TextView = itemView.findViewById(R.id.memory_date)
        private val favoriteImageView: ImageView = itemView.findViewById(R.id.memory_favorited)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(memory: Memory) {
            this.memory = memory
            val photoFile = memoryDetailViewModel.getPhotoFile(memory)
            if (photoFile.exists()) {
                val bitmap = getScaledBitMap(photoFile.path, requireActivity())
                memoryImageView.setImageBitmap(bitmap)
                memoryImageView.contentDescription = getString(R.string.memory_photo_image_description)
            } else {
                memoryImageView.setImageBitmap(null)
                memoryImageView.contentDescription = getString(R.string.memory_photo_no_image_description)
            }
            titleTextView.text = this.memory.title
            dateTextView.text = this.memory.date.toString()
            favoriteImageView.visibility = if (memory.isFavorite) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        override fun onClick(v: View) {
            callbacks?.onMemorySelected(memory.id)
        }

        fun filterBy(favorites: Boolean)
        {
            if (favorites)
            {
                itemView.visibility = View.GONE
            }
            else
            {
                itemView.visibility = View.VISIBLE
            }
        }
    }

    companion object {
        fun newInstance() : MemoryListFragment {
            return MemoryListFragment()
        }
    }
}