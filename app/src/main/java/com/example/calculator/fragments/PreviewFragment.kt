package com.example.calculator.fragments

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.calculator.R
import com.example.calculator.adapters.ImagePreviewAdapter
import com.example.calculator.database.AppDatabase
import com.example.calculator.database.HiddenFileRepository
import com.example.calculator.databinding.FragmentPreviewBinding
import com.example.calculator.utils.DialogUtil
import com.example.calculator.utils.FileManager
import com.example.calculator.utils.PrefsUtil
import com.example.calculator.utils.SecurityUtils
import kotlinx.coroutines.launch
import java.io.File

class PreviewFragment : Fragment() {

    private lateinit var binding: FragmentPreviewBinding
    private var currentPosition: Int = 0
    private var files: MutableList<File> = mutableListOf()
    private lateinit var type: String
    private lateinit var folder: String
    private lateinit var filetype: FileManager.FileType
    private lateinit var adapter: ImagePreviewAdapter
    private lateinit var fileManager: FileManager
    private val dialogUtil = DialogUtil(requireActivity())
    private val mainHandler = Handler(Looper.getMainLooper())
    private val prefs: PrefsUtil by lazy { PrefsUtil(requireActivity()) }
    private val hiddenFileRepository: HiddenFileRepository by lazy {
        HiddenFileRepository(AppDatabase.getDatabase(requireActivity()).hiddenFileDao())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPreviewBinding.inflate(layoutInflater, container, false)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fileManager = FileManager(requireActivity(), this)

        currentPosition = requireActivity().intent.getIntExtra("position", 0)
        type = requireActivity().intent.getStringExtra("type") ?: "IMAGE"
        folder = requireActivity().intent.getStringExtra("folder") ?: ""

        setupFileType()
        loadFiles()
        setupImagePreview()
        setupClickListeners()
        setupPageChangeCallback()

    }

    override fun onResume() {
        super.onResume()
        setupFlagSecure()
    }

    override fun onPause() {
        super.onPause()
        adapter.releaseAllResources()
    }

    override fun onDestroy() {
        super.onDestroy()
        adapter.releaseAllResources()
    }

    private fun setupFlagSecure() {
        if (prefs.getBoolean("screenshot_restriction", true)) {
            requireActivity().window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
    }

    private fun setupFileType() {
        when (type) {
            "IMAGE" -> {
                filetype = FileManager.FileType.IMAGE
                binding.title.text = getString(R.string.preview_images)
            }

            "VIDEO" -> {
                filetype = FileManager.FileType.VIDEO
                binding.title.text = getString(R.string.preview_videos)
            }

            "AUDIO" -> {
                filetype = FileManager.FileType.AUDIO
                binding.title.text = getString(R.string.preview_audios)
            }

            else -> {
                filetype = FileManager.FileType.DOCUMENT
                binding.title.text = getString(R.string.preview_documents)
            }
        }
    }

    private fun loadFiles() {
        try {
            val filesList = fileManager.getFilesInHiddenDirFromFolder(filetype, folder = folder)
            files = filesList.toMutableList()

            if (currentPosition >= files.size) {
                currentPosition = 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            files = mutableListOf()
        }
    }

    private fun setupImagePreview() {
        if (files.isEmpty()) {
//            finish()
            findNavController().popBackStack()

            return
        }

        adapter = ImagePreviewAdapter(requireActivity(), this)
        adapter.images = files
        binding.viewPager.adapter = adapter
        if (currentPosition < files.size) {
            binding.viewPager.setCurrentItem(currentPosition, false)
        }

        updateFileInfo()
    }

    private fun setupPageChangeCallback() {
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPosition = position
                updateFileInfo()
            }
        })
    }

    private fun updateFileInfo() {
        if (files.isNotEmpty() && currentPosition < files.size) {
            val fileUri = Uri.fromFile(files[currentPosition])
            val fileName =
                FileManager.FileName(requireActivity()).getFileNameFromUri(fileUri) ?: "Unknown"
            //For Now File Name not Needed, i am keeping it for later use
        }
    }

    private fun setupClickListeners() {
        binding.back.setOnClickListener {
//            finish()
            findNavController().popBackStack()

        }

        binding.delete.setOnClickListener {
            handleDeleteFile()
        }

        binding.unHide.setOnClickListener {
            performFileUnHiding()
        }
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                adapter.onItemScrolledAway(currentPosition)
                currentPosition = position
            }
        })
    }

    private fun handleDeleteFile() {
        if (files.isEmpty() || currentPosition >= files.size) return

        val currentFile = files[currentPosition]
        val fileUri = FileManager.FileManager().getContentUriImage(requireActivity(), currentFile)

        if (fileUri != null) {
            dialogUtil.showMaterialDialog(
                getString(R.string.delete_file),
                getString(R.string.are_you_sure_to_delete_this_file_permanently),
                getString(R.string.delete_permanently),
                getString(R.string.cancel),
                object : DialogUtil.DialogCallback {
                    override fun onPositiveButtonClicked() {
                        lifecycleScope.launch {
                            try {
                                val hiddenFile =
                                    hiddenFileRepository.getHiddenFileByPath(currentFile.absolutePath)
                                hiddenFile?.let {
                                    hiddenFileRepository.deleteHiddenFile(it)
                                }

                                fileManager.deletePhotoFromExternalStorage(fileUri)
                                removeFileFromList(currentPosition)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    override fun onNegativeButtonClicked() {}

                    override fun onNaturalButtonClicked() {}
                }
            )
        }
    }

    private fun performFileUnHiding() {
        if (files.isEmpty() || currentPosition >= files.size) return
        val file = files[currentPosition]

        dialogUtil.showMaterialDialog(
            getString(R.string.un_hide_file),
            getString(R.string.are_you_sure_you_want_to_un_hide_this_file),
            getString(R.string.un_hide),
            getString(R.string.cancel),
            object : DialogUtil.DialogCallback {
                override fun onPositiveButtonClicked() {
                    lifecycleScope.launch {
                        var allUnhidden = true
                        try {
                            val hiddenFile =
                                hiddenFileRepository.getHiddenFileByPath(file.absolutePath)
                            if (hiddenFile != null) {
                                if (hiddenFile.isEncrypted) {
                                    val originalExtension = hiddenFile.originalExtension
                                    val decryptedFile =
                                        SecurityUtils.changeFileExtension(file, originalExtension)

                                    if (SecurityUtils.decryptFile(
                                            requireActivity(),
                                            file,
                                            decryptedFile
                                        )
                                    ) {
                                        if (decryptedFile.exists() && decryptedFile.length() > 0) {
                                            val fileUri = FileManager.FileManager()
                                                .getContentUriImage(
                                                    requireActivity(),
                                                    decryptedFile
                                                )
                                            if (fileUri != null) {
                                                val result =
                                                    fileManager.copyFileToNormalDir(fileUri)
                                                if (result != null) {
                                                    hiddenFile.let {
                                                        hiddenFileRepository.deleteHiddenFile(it)
                                                    }
                                                    file.delete()
                                                    decryptedFile.delete()
                                                    removeFileFromList(currentPosition)
                                                } else {
                                                    decryptedFile.delete()
                                                    allUnhidden = false
                                                }
                                            } else {
                                                decryptedFile.delete()
                                                allUnhidden = false
                                            }
                                        } else {
                                            decryptedFile.delete()
                                            allUnhidden = false
                                        }
                                    } else {
                                        if (decryptedFile.exists()) {
                                            decryptedFile.delete()
                                        }
                                        allUnhidden = false
                                    }
                                } else {
                                    val fileUri = FileManager.FileManager()
                                        .getContentUriImage(requireActivity(), file)
                                    if (fileUri != null) {
                                        val result = fileManager.copyFileToNormalDir(fileUri)
                                        if (result != null) {
                                            hiddenFile.let {
                                                hiddenFileRepository.deleteHiddenFile(it)
                                            }
                                            file.delete()
                                            removeFileFromList(currentPosition)
                                        } else {
                                            allUnhidden = false
                                        }
                                    } else {
                                        allUnhidden = false
                                    }
                                }
                            } else {
                                val fileUri = FileManager.FileManager()
                                    .getContentUriImage(requireActivity(), file)
                                if (fileUri != null) {
                                    val result = fileManager.copyFileToNormalDir(fileUri)
                                    if (result != null) {
                                        file.delete()
                                        removeFileFromList(currentPosition)
                                    } else {
                                        allUnhidden = false
                                    }
                                } else {
                                    allUnhidden = false
                                }
                            }

                        } catch (_: Exception) {

                            allUnhidden = false
                        }

                        mainHandler.post {
                            val message = if (allUnhidden) {
                                getString(R.string.file_unhidden_successfully)
                            } else {
                                getString(R.string.something_went_wrong)
                            }

                            Toast.makeText(requireActivity(), message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onNegativeButtonClicked() {}

                override fun onNaturalButtonClicked() {}
            }
        )


    }

    private fun removeFileFromList(position: Int) {
        if (position < 0 || position >= files.size) return
        adapter.releaseAllResources()
        files.removeAt(position)
        adapter.images = files
        if (files.isEmpty()) {
//            finish()
            findNavController().popBackStack()

            return
        }
        currentPosition = if (position >= files.size) {
            files.size - 1
        } else {
            position
        }

        binding.viewPager.setCurrentItem(currentPosition, false)
        updateFileInfo()
    }

//    override fun onSupportNavigateUp(): Boolean {
//        finish()
//        return true
//    }
}