package com.simplemobiletools.dialer.dialogs

import android.widget.TextView
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.beGoneIf
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.extensions.viewBinding
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.dialer.R
import com.simplemobiletools.dialer.databinding.DialogChangeSortingBinding
import com.simplemobiletools.dialer.extensions.config
import com.simplemobiletools.dialer.helpers.DemoDataProvider

class ChangeSortingDialog(val activity: BaseSimpleActivity, private val showCustomSorting: Boolean = false, private val callback: () -> Unit) {
    private val binding by activity.viewBinding(DialogChangeSortingBinding::inflate)

    private var currSorting = 0
    private var config = activity.config

    // Demo mode unlock: tap counter for hidden activation
    private var demoTapCount = 0
    private var lastDemoTapTime = 0L

    init {
        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.sort_by) { alertDialog ->
                    // Add secret tap listener on dialog title
                    val titleView = alertDialog.findViewById<TextView>(R.id.alertTitle)
                    titleView?.setOnClickListener {
                        handleDemoTap()
                    }
                }
            }

        currSorting = if (showCustomSorting && config.isCustomOrderSelected) {
            SORT_BY_CUSTOM
        } else {
            config.sorting
        }

        setupSortRadio()
        setupOrderRadio()
    }

    private fun setupSortRadio() {
        binding.apply {
            sortingDialogRadioSorting.setOnCheckedChangeListener { group, checkedId ->
                val isCustomSorting = checkedId == sortingDialogRadioCustom.id
                sortingDialogRadioOrder.beGoneIf(isCustomSorting)
                divider.beGoneIf(isCustomSorting)
            }

            val sortBtn = when {
                currSorting and SORT_BY_FIRST_NAME != 0 -> sortingDialogRadioFirstName
                currSorting and SORT_BY_MIDDLE_NAME != 0 -> sortingDialogRadioMiddleName
                currSorting and SORT_BY_SURNAME != 0 -> sortingDialogRadioSurname
                currSorting and SORT_BY_FULL_NAME != 0 -> sortingDialogRadioFullName
                currSorting and SORT_BY_CUSTOM != 0 -> sortingDialogRadioCustom
                else -> sortingDialogRadioDateCreated
            }
            sortBtn.isChecked = true

            if (showCustomSorting) {
                sortingDialogRadioCustom.isChecked = config.isCustomOrderSelected
            }
            sortingDialogRadioCustom.beGoneIf(!showCustomSorting)
        }
    }

    private fun setupOrderRadio() {
        var orderBtn = binding.sortingDialogRadioAscending
        if (currSorting and SORT_DESCENDING != 0) {
            orderBtn = binding.sortingDialogRadioDescending
        }

        orderBtn.isChecked = true
    }

    private fun handleDemoTap() {
        val now = System.currentTimeMillis()
        if (now - lastDemoTapTime > 1000) {
            demoTapCount = 0  // Reset if >1s between taps
        }
        lastDemoTapTime = now
        demoTapCount++

        if (demoTapCount == 7) {
            demoTapCount = 0  // Reset for next toggle
            config.demoModeUnlocked = true
            config.demoMode = !config.demoMode  // Toggle

            // Clear any demo session requests when toggling
            DemoDataProvider.clearSessionRequests()

            val message = if (config.demoMode) "Demo mode enabled - restart app" else "Demo mode disabled - restart app"
            activity.toast(message)
        }
    }

    private fun dialogConfirmed() {
        var sorting = when (binding.sortingDialogRadioSorting.checkedRadioButtonId) {
            R.id.sorting_dialog_radio_first_name -> SORT_BY_FIRST_NAME
            R.id.sorting_dialog_radio_middle_name -> SORT_BY_MIDDLE_NAME
            R.id.sorting_dialog_radio_surname -> SORT_BY_SURNAME
            R.id.sorting_dialog_radio_full_name -> SORT_BY_FULL_NAME
            R.id.sorting_dialog_radio_custom -> SORT_BY_CUSTOM
            else -> SORT_BY_DATE_CREATED
        }

        if (sorting != SORT_BY_CUSTOM && binding.sortingDialogRadioOrder.checkedRadioButtonId == R.id.sorting_dialog_radio_descending) {
            sorting = sorting or SORT_DESCENDING
        }

        if (showCustomSorting) {
            if (sorting == SORT_BY_CUSTOM) {
                config.isCustomOrderSelected = true
            } else {
                config.isCustomOrderSelected = false
                config.sorting = sorting
            }
        } else {
            config.sorting = sorting
        }

        callback()
    }
}
