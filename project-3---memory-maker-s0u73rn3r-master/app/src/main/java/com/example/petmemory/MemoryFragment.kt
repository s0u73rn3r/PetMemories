package com.example.petmemory

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.*
import android.widget.*
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.util.*
import kotlin.reflect.KProperty

private const val DIALOG_DATE = "Dialog Date"
private const val REQUEST_DATE = 0
private const val REQUEST_CONTACT = 1
private const val REQUEST_PHOTO = 2
private const val DATE_FORMAT = "EEE, MMM, dd"
private const val ARG_CRIME_ID = "memory_id"

class MemoryFragment : Fragment(), DatePickerFragment.Callbacks {

    interface Callbacks {
        fun onMemoryDeleted()
    }

    private var callbacks: Callbacks? = null

    private lateinit var memoryRecyclerView: RecyclerView

    private val memoryListViewModel : MemoryListViewModel by lazy {
        ViewModelProviders.of(this).get(MemoryListViewModel::class.java)
    }

    private lateinit var memory: Memory
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri

    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var favoriteSwitch: Switch
    private lateinit var photoButton: ImageButton
    private lateinit var photoView: ImageView
    private lateinit var descriptionField: EditText


    private val memoryDetailViewModel: MemoryDetailViewModel by lazy {
        ViewModelProviders.of(this).get(MemoryDetailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        memory = Memory()
        val memoryID: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        memoryDetailViewModel.loadMemory(memoryID)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_memory, container, false)

        titleField = view.findViewById(R.id.memory_title) as EditText
        dateButton = view.findViewById(R.id.memory_date) as Button
        favoriteSwitch = view.findViewById(R.id.favorite_switch) as Switch
        photoButton = view.findViewById(R.id.memory_camera) as ImageButton
        photoView = view.findViewById(R.id.memory_photo) as ImageView
        descriptionField = view.findViewById(R.id.memory_description) as EditText


        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        memoryDetailViewModel.memoryData.observe(
            viewLifecycleOwner,
            Observer { memory ->
                memory?.let {
                    this.memory = memory
                    photoFile = memoryDetailViewModel.getPhotoFile(memory)
                    photoUri = FileProvider.getUriForFile(
                        requireActivity(),
                        "com.example.petmemory.fileprovider",
                        photoFile
                    )
                    updateUI()
                }
            }
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_memory, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.delete_memory -> {
                memoryListViewModel.deleteMemory(this.memory)
                callbacks?.onMemoryDeleted()
                true
            }
            R.id.send_memory -> {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, getMemoryInfo())
                    putExtra(Intent.EXTRA_SUBJECT, getString(R.string.memory_reminder_subject))
                }.also { intent ->
                    val chooserIntent = Intent.createChooser(intent, getString(R.string.send_reminder))
                    startActivity(chooserIntent)
                }
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()

        // this is for getting the changed text.
        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(
                sequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                // this is left blank
            }

            override fun onTextChanged(
                sequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                memory.title = sequence.toString()
            }

            override fun afterTextChanged(s: Editable?) {
                // this is left blank
            }
        }

        titleField.addTextChangedListener(titleWatcher)

        // this is for getting the changed text.
        val descriptionWatcher = object : TextWatcher {
            override fun beforeTextChanged(
                sequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                // this is left blank
            }

            override fun onTextChanged(
                sequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                memory.description = sequence.toString()
            }

            override fun afterTextChanged(s: Editable?) {
                // this is left blank
            }
        }

        descriptionField.addTextChangedListener(descriptionWatcher)

        favoriteSwitch.apply {
            setOnCheckedChangeListener { _,
                                         isChecked ->
                memory.isFavorite = isChecked
            }
        }

        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(memory.date).apply {
                setTargetFragment(this@MemoryFragment, REQUEST_DATE)
                show(this@MemoryFragment.requireFragmentManager(), DIALOG_DATE)
            }
        }

        photoButton.apply {
            val packageManager: PackageManager = requireActivity().packageManager

            val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(captureImage, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolvedActivity == null) {
                isEnabled = false
            }

            setOnClickListener {
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

                val cameraActivities: List<ResolveInfo> = packageManager.queryIntentActivities(
                    captureImage,
                    PackageManager.MATCH_DEFAULT_ONLY
                )
                for (cameraActivity in cameraActivities) {
                    requireActivity().grantUriPermission(
                        cameraActivity.activityInfo.packageName,
                        photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }

                startActivityForResult(captureImage, REQUEST_PHOTO)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        memoryDetailViewModel.saveMemory(memory)
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    fun updateUI() {
        titleField.setText(memory.title)
        dateButton.text = memory.date.toString()
        favoriteSwitch.apply {
            isChecked = memory.isFavorite
            jumpDrawablesToCurrentState()
        }
        descriptionField.setText(memory.description)
        updatePhotoView()
    }

    private fun updatePhotoView() {
        if (photoFile.exists()) {
            val bitmap = getScaledBitMap(photoFile.path, requireActivity())
            photoView.setImageBitmap(bitmap)
            photoView.contentDescription = getString(R.string.memory_photo_image_description)
        } else {
            photoView.setImageBitmap(null)
            photoView.contentDescription = getString(R.string.memory_photo_no_image_description)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            resultCode != Activity.RESULT_OK -> return

            requestCode == REQUEST_CONTACT && data != null -> {
                memoryDetailViewModel.saveMemory(memory)
            }

            requestCode == REQUEST_PHOTO -> {
                requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                updatePhotoView()
            }
        }
    }

    override fun onDateSelected(date: Date) {
        memory.date = date
        updateUI()
    }

    private fun getMemoryInfo(): String {
        val dateString = DateFormat.format(DATE_FORMAT, memory.date).toString()

        return getString(R.string.memory_reminder, memory.title, dateString, memory.description)
    }

    companion object {
        fun newInstance(memoryID: UUID): MemoryFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, memoryID)
            }
            return MemoryFragment().apply {
                arguments = args
            }
        }
    }
}


