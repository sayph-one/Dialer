package com.simplemobiletools.dialer.fragments

import android.content.res.ColorStateList
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.dialer.R
import com.simplemobiletools.dialer.activities.SimpleActivity
import com.simplemobiletools.dialer.databinding.DialogAddContactRequestBinding

class AddContactRequestDialog(
    val activity: SimpleActivity,
    val callback: (firstName: String, lastName: String, phone: String) -> Unit
) {
    private val binding = DialogAddContactRequestBinding.inflate(activity.layoutInflater)
    private val dialog: AlertDialog

    init {
        dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setCancelable(true)
            .create()

        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        setupDialog()
        dialog.show()
    }

    private fun setupDialog() {
        binding.apply {
            val textColor = activity.getProperTextColor()
            val primaryColor = activity.getProperPrimaryColor()
            val backgroundColor = activity.getProperBackgroundColor()

            // Set background color for the root layout
            root.setBackgroundColor(backgroundColor)

            // Set text colors
            dialogTitle.setTextColor(textColor)

            // Style TextInputLayouts with proper colors
            val colorStateList = ColorStateList.valueOf(primaryColor)
            contactFirstNameLayout.apply {
                setBoxStrokeColorStateList(colorStateList)
                setHintTextColor(colorStateList)
                defaultHintTextColor = colorStateList
            }
            contactLastNameLayout.apply {
                setBoxStrokeColorStateList(colorStateList)
                setHintTextColor(colorStateList)
                defaultHintTextColor = colorStateList
            }
            contactPhoneLayout.apply {
                setBoxStrokeColorStateList(colorStateList)
                setHintTextColor(colorStateList)
                defaultHintTextColor = colorStateList
            }

            // Set EditText colors
            contactFirstNameInput.setTextColor(textColor)
            contactLastNameInput.setTextColor(textColor)
            contactPhoneInput.setTextColor(textColor)

            // Set button colors
            cancelButton.setTextColor(textColor)
            submitButton.apply {
                setTextColor(primaryColor.getContrastColor())
                backgroundTintList = ColorStateList.valueOf(primaryColor)
            }

            // Add validation on text change
            contactFirstNameInput.doAfterTextChanged { validateInput() }
            contactLastNameInput.doAfterTextChanged { validateInput() }
            contactPhoneInput.doAfterTextChanged { validateInput() }

            cancelButton.setOnClickListener {
                dialog.dismiss()
            }

            submitButton.setOnClickListener {
                val firstName = contactFirstNameInput.text.toString().trim()
                val lastName = contactLastNameInput.text.toString().trim()
                val phone = contactPhoneInput.text.toString().trim()

                when {
                    firstName.isEmpty() -> {
                        contactFirstNameLayout.error = activity.getString(R.string.please_enter_first_name)
                    }
                    phone.isEmpty() -> {
                        contactPhoneLayout.error = activity.getString(R.string.please_enter_valid_phone)
                    }
                    !isValidPhone(phone) -> {
                        contactPhoneLayout.error = activity.getString(R.string.please_enter_valid_phone)
                    }
                    else -> {
                        callback(firstName, lastName, phone)
                        activity.toast(R.string.contact_request_submitted)
                        dialog.dismiss()
                    }
                }
            }

            // Focus on first name input
            contactFirstNameInput.requestFocus()
        }
    }

    private fun validateInput() {
        binding.apply {
            // Clear errors when user types
            contactFirstNameLayout.error = null
            contactLastNameLayout.error = null
            contactPhoneLayout.error = null
        }
    }

    private fun isValidPhone(phone: String): Boolean {
        // Basic validation: must contain at least 3 digits
        val digits = phone.filter { it.isDigit() }
        return digits.length >= 3
    }
}
