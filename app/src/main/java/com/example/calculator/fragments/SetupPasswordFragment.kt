package com.example.calculator.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.calculator.MainActivity
import com.example.calculator.R
import com.example.calculator.databinding.ActivityChangePasswordBinding
import com.example.calculator.databinding.FragmentSetupPasswordBinding
import com.example.calculator.utils.PrefsUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class SetupPasswordFragment : Fragment() {

    private lateinit var binding: FragmentSetupPasswordBinding
    private val args: SetupPasswordFragmentArgs by navArgs()

    private lateinit var binding2: ActivityChangePasswordBinding
    private lateinit var prefsUtil: PrefsUtil
    private var hasPassword = false
    private val prefs: PrefsUtil by lazy { PrefsUtil(requireActivity()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        prefsUtil = PrefsUtil(requireContext())
        hasPassword = prefsUtil.hasPassword()

        return if (hasPassword) {
            binding2 = ActivityChangePasswordBinding.inflate(inflater, container, false)
            binding2.root
        } else {
            binding = FragmentSetupPasswordBinding.inflate(inflater, container, false)
            binding.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefsUtil = PrefsUtil(requireContext())
        hasPassword = prefsUtil.hasPassword()

        clickListeners()
    }

    private fun clickListeners() {
        if (!hasPassword) {
            // When setting up a new password
            binding.btnSavePassword.setOnClickListener {
                val password = binding.etPassword.text.toString()
                val confirmPassword = binding.etConfirmPassword.text.toString()
                val securityQuestion = binding.etSecurityQuestion.text.toString()
                val securityAnswer = binding.etSecurityAnswer.text.toString()

                if (password.isEmpty()) {
                    binding.etPassword.error = getString(R.string.enter_password)
                    return@setOnClickListener
                }
                if (confirmPassword.isEmpty()) {
                    binding.etConfirmPassword.error = getString(R.string.confirm_password)
                    return@setOnClickListener
                }
                if (securityQuestion.isEmpty()) {
                    binding.etSecurityQuestion.error = getString(R.string.enter_security_question)
                    return@setOnClickListener
                }
                if (securityAnswer.isEmpty()) {
                    binding.etSecurityAnswer.error = getString(R.string.enter_security_answer)
                    return@setOnClickListener
                }
                if (password != confirmPassword) {
                    binding.etPassword.error = getString(R.string.passwords_don_t_match)
                    return@setOnClickListener
                }

                prefsUtil.savePassword(password)
                prefsUtil.saveSecurityQA(securityQuestion, securityAnswer)
                Toast.makeText(requireContext(), R.string.password_set_successfully, Toast.LENGTH_SHORT).show()

                findNavController().popBackStack(R.id.mainFragment, false)
            }

            binding.btnResetPassword.setOnClickListener {
                if (prefsUtil.getSecurityQuestion() != null) {
                    showSecurityQuestionDialog(prefsUtil.getSecurityQuestion().toString())
                } else {
                    Toast.makeText(requireContext(), R.string.security_question_not_set_yet, Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // When changing an existing password
            binding2.btnChangePassword.setOnClickListener {
                val oldPassword = binding2.etOldPassword.text.toString()
                val newPassword = binding2.etNewPassword.text.toString()

                if (oldPassword.isEmpty()) {
                    binding2.etOldPassword.error = getString(R.string.this_field_can_t_be_empty)
                    return@setOnClickListener
                }
                if (newPassword.isEmpty()) {
                    binding2.etNewPassword.error = getString(R.string.this_field_can_t_be_empty)
                    return@setOnClickListener
                }

                if (prefsUtil.validatePassword(oldPassword)) {
                    if (oldPassword != newPassword) {
                        prefsUtil.savePassword(newPassword)
                        Toast.makeText(requireContext(), R.string.password_reset_successfully, Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack(R.id.mainFragment, false)
                    } else {
                        binding2.etNewPassword.error = getString(R.string.old_password_and_new_password_not_be_same)
                    }
                } else {
                    binding2.etOldPassword.error = getString(R.string.old_password_not_matching)
                }
            }

            binding2.btnResetPassword.setOnClickListener {
                if (prefsUtil.getSecurityQuestion() != null) {
                    showSecurityQuestionDialog(prefsUtil.getSecurityQuestion().toString())
                } else {
                    Toast.makeText(requireContext(), R.string.security_question_not_set_yet, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showSecurityQuestionDialog(securityQuestion: String) {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_security_question, null)

        val questionTextView: TextView = dialogView.findViewById(R.id.security_question)
        questionTextView.text = securityQuestion


        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.answer_the_security_question))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.verify)) { dialog, _ ->
                val inputEditText: TextInputEditText =
                    dialogView.findViewById(R.id.text_input_edit_text)
                val userAnswer = inputEditText.text.toString().trim()

                if (userAnswer.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.answer_cannot_be_empty), Toast.LENGTH_SHORT
                    ).show()
                } else {
                    if (prefsUtil.validateSecurityAnswer(userAnswer)) {
                        prefsUtil.resetPassword()
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.password_successfully_reset), Toast.LENGTH_SHORT
                        ).show()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.invalid_answer),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                }
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->

                dialog.dismiss()
            }
            .show()
    }
}