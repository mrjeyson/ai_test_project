package com.example.test_ai_project.auth.presentation.login.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.rememberDatePickerState
import com.example.test_ai_project.resource.R as ResR
import com.example.test_ai_project.resource.component.AppText
import com.example.test_ai_project.resource.theme.AppTextStyle
import java.util.Calendar
import java.util.TimeZone

/** The calendar dialog behind the date field's picker button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateOfBirthPicker(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(
        yearRange = EARLIEST_YEAR..Calendar.getInstance().get(Calendar.YEAR),
        selectableDates = PastAndPresentDates,
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { onDateSelected(it.toDateOfBirthDigits()) }
                    onDismiss()
                },
            ) {
                AppText(
                    text = stringResource(id = ResR.string.login_date_picker_confirm),
                    style = AppTextStyle.Label,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                AppText(
                    text = stringResource(id = ResR.string.login_date_picker_dismiss),
                    style = AppTextStyle.Label,
                )
            }
        },
    ) {
        DatePicker(state = state)
    }
}

/** A future date of birth is never valid, so it is not offered in the first place. */
@OptIn(ExperimentalMaterial3Api::class)
private object PastAndPresentDates : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        utcTimeMillis <= System.currentTimeMillis()
}

/**
 * The picker reports UTC midnight, so the calendar fields have to be read back in UTC —
 * doing it in the device zone shifts the date by one day west of Greenwich.
 */
private fun Long.toDateOfBirthDigits(): String {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).also {
        it.timeInMillis = this
    }
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val month = calendar.get(Calendar.MONTH) + 1
    val year = calendar.get(Calendar.YEAR)
    return "%02d%02d%04d".format(day, month, year)
}

private const val EARLIEST_YEAR = 1900
