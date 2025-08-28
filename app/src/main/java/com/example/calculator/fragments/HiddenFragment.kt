package com.example.calculator.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.example.calculator.R
import com.example.calculator.adapters.FolderAdapter
import com.example.calculator.adapters.ListFolderAdapter
import com.example.calculator.databinding.FragmentHiddenBinding
import com.example.calculator.utils.DialogUtil
import com.example.calculator.utils.FileManager
import com.example.calculator.utils.FileManager.Companion.HIDDEN_DIR
import com.example.calculator.utils.FolderManager
import com.example.calculator.utils.PrefsUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class HiddenFragment : Fragment() {

    private lateinit var binding: FragmentHiddenBinding
    private lateinit var fileManager: FileManager
    private var currentFolder: File? = null
    private var folderAdapter: FolderAdapter? = null
    private var listFolderAdapter: ListFolderAdapter? = null
    private val prefs: PrefsUtil by lazy { PrefsUtil(requireContext()) }
    private val args: HiddenFragmentArgs by navArgs()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val hiddenDir by lazy { File(requireContext().getExternalFilesDir(null), FileManager.HIDDEN_DIR) }
    private lateinit var folderManager: FolderManager
    private lateinit var dialogUtil: DialogUtil

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHiddenBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        folderManager = FolderManager()
        dialogUtil = DialogUtil(requireContext())

        setupInitialUIState()
        setupClickListeners()
        setupBackPressedHandler()

        fileManager = FileManager(requireContext(), this)
        fileManager.askPermission(requireActivity())

        refreshCurrentView()
    }
    private fun setupInitialUIState() {

        binding.addFolder.visibility = View.VISIBLE
        binding.settings.visibility = View.VISIBLE
        binding.folderOrientation.visibility = View.VISIBLE
        binding.deleteSelected.visibility = View.GONE
        binding.delete.visibility = View.GONE
        binding.menuButton.visibility = View.GONE
    }

    private fun setupClickListeners() {


        binding.settings.setOnClickListener {
//            startActivity(Intent(requireContext(), SettingsActivity::class.java))
            findNavController().navigate(R.id.action_hiddenFragment_to_settingsFragment)

        }


        binding.back.setOnClickListener {
            handleBackPress()
        }

        binding.addFolder.setOnClickListener {
            createNewFolder()
        }

        binding.deleteSelected.setOnClickListener {
            deleteSelectedItems()
        }

        binding.delete.setOnClickListener {
            deleteSelectedItems()
        }

        binding.edit.setOnClickListener {
            editSelectedFolder()
        }

        binding.folderOrientation.setOnClickListener {
            val currentIsList = PrefsUtil(requireContext()).getBoolean("isList", false)
            val newIsList = !currentIsList

            if (newIsList) {
                showListUI()
                PrefsUtil(requireContext()).setBoolean("isList", true)
                binding.folderOrientation.setIconResource(R.drawable.ic_grid)
            } else {
                showGridUI()
                PrefsUtil(requireContext()).setBoolean("isList", false)
                binding.folderOrientation.setIconResource(R.drawable.ic_list)
            }
        }
    }

    private fun showGridUI() {
        listFoldersInHiddenDirectory()
    }

    private fun showListUI() {
        listFoldersInHiddenDirectoryListStyle()
    }

    private fun listFoldersInHiddenDirectoryListStyle() {
        try {
            if (!hiddenDir.exists()) {
                fileManager.getHiddenDirectory()
            }

            if (hiddenDir.exists() && hiddenDir.isDirectory) {
                val folders = folderManager.getFoldersInDirectory(hiddenDir)

                if (folders.isNotEmpty()) {
                    showFolderListStyle(folders)
                } else {
                    showEmptyState()
                }
            } else {
                showEmptyState()
            }
        } catch (_: Exception) {

            showEmptyState()
        }
    }

    private fun setupBackPressedHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBackPress()
                }
            }
        )
    }


    private fun createNewFolder() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_input, null)
        val inputEditText = dialogView.findViewById<EditText>(R.id.editText)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.enter_folder_name_to_create))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.create)) { dialog, _ ->
                val newName = inputEditText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    val success = try {
                        folderManager.createFolder(hiddenDir, newName)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false
                    }

                    if (success) {
                        refreshCurrentView()
                    } else {
                        Toast.makeText(requireContext(),
                            getString(R.string.failed_to_create_folder),
                            Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        setupFlagSecure()
    }

    private fun setupFlagSecure() {
        if (prefs.getBoolean("screenshot_restriction", true)) {
            requireActivity().window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }
    }


    private fun listFoldersInHiddenDirectory() {
        try {
            if (!hiddenDir.exists()) {
                fileManager.getHiddenDirectory()
            }

            if (hiddenDir.exists() && hiddenDir.isDirectory) {
                val folders = folderManager.getFoldersInDirectory(hiddenDir)

                if (folders.isNotEmpty()) {
                    showFolderList(folders)
                } else {
                    showEmptyState()
                }
            } else {
                showEmptyState()
            }
        } catch (_: Exception) {
            showEmptyState()
        }
    }

    private fun showFolderList(folders: List<File>) {
        binding.noItems.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
        listFolderAdapter = null

        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        folderAdapter = FolderAdapter(
            onFolderClick = { clickedFolder ->
//                startActivity(Intent(requireContext(),ViewFolderActivity::class.java).putExtra("folder",clickedFolder.toString()))
                val action = HiddenFragmentDirections.actionHiddenFragmentToViewFolderFragment(clickedFolder.toString())
                findNavController().navigate(action)
                            },
            onFolderLongClick = {
                enterFolderSelectionMode()
            },
            onSelectionModeChanged = { isSelectionMode ->
                handleFolderSelectionModeChange(isSelectionMode)
            },
            onSelectionCountChanged = { _ ->
                updateEditButtonVisibility()
            }
        )
        binding.recyclerView.adapter = folderAdapter
        folderAdapter?.submitList(folders)

        if (folderAdapter?.isInSelectionMode() != true) {
            showFolderViewIcons()
        }
    }
    private fun showFolderListStyle(folders: List<File>) {
        binding.noItems.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
        folderAdapter = null

        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 1)
        listFolderAdapter = ListFolderAdapter(
            onFolderClick = { clickedFolder ->
//                startActivity(Intent(requireContext(),ViewFolderActivity::class.java).putExtra("folder",clickedFolder.toString()))
                val action = HiddenFragmentDirections
                    .actionHiddenFragmentToViewFolderFragment(clickedFolder.toString())

                findNavController().navigate(action)

            },
            onFolderLongClick = {
                enterFolderSelectionMode()
            },
            onSelectionModeChanged = { isSelectionMode ->
                handleFolderSelectionModeChange(isSelectionMode)
            },
            onSelectionCountChanged = { _ ->
                updateEditButtonVisibility()
            }
        )
        binding.recyclerView.adapter = listFolderAdapter
        listFolderAdapter?.submitList(folders)

        if (listFolderAdapter?.isInSelectionMode() != true) {
            showFolderViewIcons()
        }
    }

    private fun updateEditButtonVisibility() {
        val selectedCount = when {
            folderAdapter != null -> folderAdapter?.getSelectedItems()?.size ?: 0
            listFolderAdapter != null -> listFolderAdapter?.getSelectedItems()?.size ?: 0
            else -> 0
        }
        binding.edit.visibility = if (selectedCount == 1) View.VISIBLE else View.GONE
    }

    private fun showEmptyState() {
        binding.noItems.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
    }

    private fun enterFolderSelectionMode() {
        showFolderSelectionIcons()
    }


    private fun refreshCurrentView() {
        val isList = PrefsUtil(requireContext()).getBoolean("isList", false)
        if (isList) {
            binding.folderOrientation.setIconResource(R.drawable.ic_grid)
            listFoldersInHiddenDirectoryListStyle()
        } else {
            binding.folderOrientation.setIconResource(R.drawable.ic_list)
            listFoldersInHiddenDirectory()
        }
    }


    private fun deleteSelectedItems() {
        deleteSelectedFolders()
    }

    private fun deleteSelectedFolders() {
        val selectedFolders = when {
            folderAdapter != null -> folderAdapter?.getSelectedItems() ?: emptyList()
            listFolderAdapter != null -> listFolderAdapter?.getSelectedItems() ?: emptyList()
            else -> emptyList()
        }

        if (selectedFolders.isNotEmpty()) {
            dialogUtil.showMaterialDialog(
                getString(R.string.delete_items),
                "${getString(R.string.are_you_sure_you_want_to_delete_selected_items)}\n ${getString(R.string.folder_will_be_deleted_permanently)}" ,
                getString(R.string.delete),
                getString(R.string.cancel),
                object : DialogUtil.DialogCallback {
                    override fun onPositiveButtonClicked() {
                        performFolderDeletion(selectedFolders)
                    }

                    override fun onNegativeButtonClicked() {

                    }

                    override fun onNaturalButtonClicked() {

                    }
                }
            )
        }
    }

    private fun performFolderDeletion(selectedFolders: List<File>) {
        var allDeleted = true
        selectedFolders.forEach { folder ->
            if (!folderManager.deleteFolder(folder)) {
                allDeleted = false
            }
        }

        val message = if (allDeleted) {
            getString(R.string.folder_deleted_successfully)
        } else {
            getString(R.string.some_items_could_not_be_deleted)
        }

        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

        folderAdapter?.clearSelection()
        listFolderAdapter?.clearSelection()

        exitFolderSelectionMode()

        refreshCurrentView()
    }

    private fun handleBackPress() {

        if (folderAdapter?.onBackPressed() == true || listFolderAdapter?.onBackPressed() == true) {
            return
        }

        if (currentFolder != null) {
            navigateBackToFolders()
        } else {
//            finish()
            findNavController().popBackStack()
        }
    }

    private fun navigateBackToFolders() {
        currentFolder = null

        refreshCurrentView()

        binding.folderName.text = getString(R.string.hidden_space)

        showFolderViewIcons()
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun showFolderViewIcons() {
        binding.folderOrientation.visibility = View.VISIBLE
        binding.settings.visibility = View.VISIBLE
        binding.delete.visibility = View.GONE
        binding.deleteSelected.visibility = View.GONE
        binding.menuButton.visibility = View.GONE
        binding.addFolder.visibility = View.VISIBLE
        binding.edit.visibility = View.GONE
        if (currentFolder == null) {

            binding.addFolder.visibility = View.VISIBLE
        }
    }
    private fun showFolderSelectionIcons() {
        binding.folderOrientation.visibility = View.GONE
        binding.settings.visibility = View.GONE
        binding.delete.visibility = View.VISIBLE
        binding.deleteSelected.visibility = View.VISIBLE
        binding.menuButton.visibility = View.GONE
        binding.addFolder.visibility = View.GONE
        updateEditButtonVisibility()
    }
    private fun exitFolderSelectionMode() {
        showFolderViewIcons()
    }

    private fun handleFolderSelectionModeChange(isSelectionMode: Boolean) {
        if (!isSelectionMode) {
            exitFolderSelectionMode()
        } else {
            enterFolderSelectionMode()
        }
        updateEditButtonVisibility()
    }

    private fun editSelectedFolder() {
        val selectedFolders = when {
            folderAdapter != null -> folderAdapter?.getSelectedItems() ?: emptyList()
            listFolderAdapter != null -> listFolderAdapter?.getSelectedItems() ?: emptyList()
            else -> emptyList()
        }

        if (selectedFolders.size != 1) {
            Toast.makeText(requireContext(),
                getString(R.string.please_select_exactly_one_folder_to_edit), Toast.LENGTH_SHORT).show()
            return
        }

        val folder = selectedFolders[0]
        showEditFolderDialog(folder)
    }

    private fun showEditFolderDialog(folder: File) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_input, null)
        val inputEditText = dialogView.findViewById<EditText>(R.id.editText)
        inputEditText.setText(folder.name)
        inputEditText.selectAll()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.rename_folder))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.rename)) { dialog, _ ->
                val newName = inputEditText.text.toString().trim()
                if (newName.isNotEmpty() && newName != folder.name) {
                    if (isValidFolderName(newName)) {
                        renameFolder(folder, newName)
                    } else {
                        Toast.makeText(requireContext(),
                            getString(R.string.invalid_folder_name), Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun isValidFolderName(folderName: String): Boolean {
        val forbiddenChars = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
        return folderName.isNotBlank() &&
                folderName.none { it in forbiddenChars } &&
                !folderName.startsWith(".") &&
                folderName.length <= 255
    }

    private fun renameFolder(oldFolder: File, newName: String) {
        val parentDir = oldFolder.parentFile
        if (parentDir != null) {
            val newFolder = File(parentDir, newName)
            if (newFolder.exists()) {
                Toast.makeText(requireContext(),
                    getString(R.string.folder_with_this_name_already_exists), Toast.LENGTH_SHORT).show()
                return
            }

            if (oldFolder.renameTo(newFolder)) {
                folderAdapter?.clearSelection()
                listFolderAdapter?.clearSelection()
                exitFolderSelectionMode()

                refreshCurrentView()
            } else {
                Toast.makeText(requireContext(), getString(R.string.failed_to_rename_folder), Toast.LENGTH_SHORT).show()
            }
        }
    }
}