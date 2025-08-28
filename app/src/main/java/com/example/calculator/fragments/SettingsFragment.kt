package com.example.calculator.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.navigation.fragment.findNavController
import com.example.calculator.R
import com.example.calculator.databinding.FragmentSettingsBinding
import com.example.calculator.utils.PrefsUtil
import com.example.calculator.utils.SecurityUtils
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class SettingsFragment : Fragment() {

    private lateinit var binding: FragmentSettingsBinding
    private lateinit var prefs: PrefsUtil
    private var DEV_GITHUB_URL = ""
    private var GITHUB_URL = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSettingsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PrefsUtil(requireContext())

        DEV_GITHUB_URL = getString(R.string.github_profile)
        GITHUB_URL = getString(R.string.github_profile)
        setupUI()
        loadSettings()
        setupListeners()
    }

    private fun setupUI() {
        binding.back.setOnClickListener {
//            onBackPressed()
            findNavController().popBackStack()

        }
    }


    private fun loadSettings() {

        binding.dynamicThemeSwitch.isChecked = prefs.getBoolean("dynamic_theme", true)

        val themeMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        binding.themeModeSwitch.isChecked = themeMode != AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

        when (themeMode) {
            AppCompatDelegate.MODE_NIGHT_YES -> binding.darkThemeRadio.isChecked = true
            AppCompatDelegate.MODE_NIGHT_NO -> binding.lightThemeRadio.isChecked = true
            else -> binding.systemThemeRadio.isChecked = true
        }

        val isUsingCustomKey = SecurityUtils.isUsingCustomKey(requireContext())
        binding.customKeyStatus.isChecked = isUsingCustomKey
        binding.screenshotRestrictionSwitch.isChecked =
            prefs.getBoolean("screenshot_restriction", true)
        binding.showFileNames.isChecked = prefs.getBoolean("showFileName", true)
        binding.encryptionSwitch.isChecked = prefs.getBoolean("encryption", false)

        updateThemeModeVisibility()
    }

    private fun setupListeners() {

        binding.githubButton.setOnClickListener {
            openUrl(GITHUB_URL)
        }

        binding.devGithubButton.setOnClickListener {
            openUrl(DEV_GITHUB_URL)
        }

        binding.dynamicThemeSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.setBoolean("dynamic_theme", isChecked)
            if (!isChecked) {
                showThemeModeDialog()
            } else {
                showThemeModeDialog()
                if (!prefs.getBoolean("isAppReopened", false)) {
                    DynamicColors.applyToActivityIfAvailable(requireContext() as Activity)
                }
            }
        }
        binding.encryptionSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.setBoolean("encryption", isChecked)
        }


        binding.themeModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.themeRadioGroup.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                binding.systemThemeRadio.isChecked = true
                applyThemeMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }

        binding.themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val themeMode = when (checkedId) {
                R.id.lightThemeRadio -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.darkThemeRadio -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            applyThemeMode(themeMode)
        }

        binding.screenshotRestrictionSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.setBoolean("screenshot_restriction", isChecked)
            if (isChecked) {
                enableScreenshotRestriction()
            } else {
                disableScreenshotRestriction()
            }
        }
        binding.showFileNames.setOnCheckedChangeListener { _, isChecked ->
            prefs.setBoolean("showFileName", isChecked)
        }

        binding.customKeyStatus.setOnClickListener {
            showCustomKeyDialog()
        }
    }

    private fun updateThemeModeVisibility() {
        binding.themeRadioGroup.visibility =
            if (binding.themeModeSwitch.isChecked) View.VISIBLE else View.GONE
    }

    private fun showThemeModeDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.attention))
            .setMessage(getString(R.string.if_you_turn_on_off_this_option_dynamic_theme_changes_will_be_visible_after_you_reopen_the_app))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->

            }
            .show()
    }

    private fun applyThemeMode(themeMode: Int) {
        prefs.setInt("theme_mode", themeMode)
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }

    private fun enableScreenshotRestriction() {
        requireActivity().window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

    }

    private fun disableScreenshotRestriction() {
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        } catch (e: Exception) {
            Snackbar.make(
                binding.root,
                getString(R.string.could_not_open_url), Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun showCustomKeyDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_custom_key, null)
        val keyInput = dialogView.findViewById<EditText>(R.id.keyInput)
        val confirmKeyInput = dialogView.findViewById<EditText>(R.id.confirmKeyInput)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.set_custom_encryption_key))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.set)) { dialog, _ ->
                val key = keyInput.text.toString()
                val confirmKey = confirmKeyInput.text.toString()

                if (key.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.key_cannot_be_empty),
                        Toast.LENGTH_SHORT
                    ).show()
                    updateUI()
                    return@setPositiveButton
                }

                if (key != confirmKey) {
                    Toast.makeText(requireContext(), getString(R.string.keys_do_not_match), Toast.LENGTH_SHORT)
                        .show()
                    updateUI()
                    return@setPositiveButton
                }

                if (SecurityUtils.setCustomKey(requireContext(), key)) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.custom_key_set_successfully), Toast.LENGTH_SHORT
                    ).show()
                    updateUI()
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.failed_to_set_custom_key), Toast.LENGTH_SHORT
                    ).show()
                    updateUI()
                }
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                updateUI()
            }
            .setNeutralButton(getString(R.string.delete_key)) { _, _ ->
                SecurityUtils.clearCustomKey(requireContext())
                Toast.makeText(
                    requireContext(),
                    getString(R.string.custom_encryption_key_cleared), Toast.LENGTH_SHORT
                ).show()
                updateUI()
            }
            .show()
    }

    private fun updateUI() {
        binding.showFileNames.isChecked = prefs.getBoolean("showFileName", true)
        binding.encryptionSwitch.isChecked = prefs.getBoolean("encryption", false)
        val isUsingCustomKey = SecurityUtils.isUsingCustomKey(requireContext())
        binding.customKeyStatus.isChecked = isUsingCustomKey
    }
}