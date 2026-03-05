package com.example.accounting.ui.screen

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.accounting.model.PaymentViewType
import com.example.accounting.model.TransactionInfo
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * A screen for viewing and editing a [TransactionInfo] record.
 * @param transaction The existing transaction to edit.
 * @param onDismiss navigation back handler. Called with updated transaction, if valid.
 * @param onUpload upload button handler. Called with updated transaction, only when valid.
 * @param onDelete delete button handler. Called with original transaction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(
    transaction: TransactionInfo,
    onDismiss: (TransactionInfo?) -> Unit,
    onUpload: (TransactionInfo) -> Unit,
    onDelete: (TransactionInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Form state
    var amountText by rememberSaveable { mutableStateOf(transaction.amount.toPlainString()) }
    var divisorText by rememberSaveable { mutableStateOf("") }
    var merchant by rememberSaveable { mutableStateOf(transaction.merchant ?: "") }
    var paymentMethod by rememberSaveable { mutableStateOf(transaction.paymentMethod ?: "") }
    var timestamp by rememberSaveable { mutableLongStateOf(transaction.timestamp) }
    var viewTypeIndex by rememberSaveable(transaction.viewType) {
        mutableIntStateOf(
            when (transaction.viewType) {
                is PaymentViewType.Success -> 0
                is PaymentViewType.Request -> 1
                else -> 0
            }
        )
    }

    // Validation state
    var amountError by rememberSaveable { mutableStateOf<String?>(null) }
    var divisorError by rememberSaveable { mutableStateOf<String?>(null) }

    // Calculate final amount with divisor
    val finalAmount = remember(amountText, divisorText) {
        val amount = amountText.toBigDecimalOrNull()
        val divisor = divisorText.toIntOrNull()
        if (amount != null && divisor != null && divisor > 0) {
            amount.divide(BigDecimal(divisor), 2, RoundingMode.HALF_UP)
        } else {
            amount
        }
    }

    // Validate amount
    fun validateAmount(): Boolean {
        val amount = amountText.toBigDecimalOrNull()
        return if (amount == null || amount < BigDecimal.ZERO) {
            amountError = "Enter a valid non-negative number"
            false
        } else {
            amountError = null
            true
        }
    }

    // Validate divisor
    fun validateDivisor(): Boolean {
        if (divisorText.isBlank()) {
            divisorError = null
            return true
        }
        val divisor = divisorText.toIntOrNull()
        return if (divisor == null || divisor <= 0) {
            divisorError = "Enter a positive integer"
            false
        } else {
            divisorError = null
            true
        }
    }

    // Save and return updated transaction
    fun saveTransaction(): TransactionInfo? {
        if (!validateAmount() || !validateDivisor()) {
            return null
        }
        return TransactionInfo(
            id = transaction.id,
            amount = finalAmount ?: BigDecimal.ZERO,
            merchant = merchant.ifBlank { null },
            paymentMethod = paymentMethod.ifBlank { null },
            timestamp = timestamp,
            viewType = when (viewTypeIndex) {
                0 -> PaymentViewType.Success
                else -> PaymentViewType.Request
            }
        )
    }

    // Back handler - save and dismiss
    androidx.activity.compose.BackHandler {
        onDismiss(saveTransaction())
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Edit Transaction",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Amount + Divisor
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Amount field
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = {
                                amountText = it
                                validateAmount()
                            },
                            label = { Text("Amount") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = amountError != null,
                            modifier = Modifier.weight(3f),
                            singleLine = true
                        )

                        // Divisor symbol
                        Text(
                            text = "÷",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        // Divisor field
                        OutlinedTextField(
                            value = divisorText,
                            onValueChange = {
                                divisorText = it
                                validateDivisor()
                            },
                            label = { Text("Divisor") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = divisorError != null,
                            modifier = Modifier.weight(2f),
                            singleLine = true
                        )
                    }

                    // Error messages
                    if (amountError != null) {
                        Text(
                            text = amountError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (divisorError != null) {
                        Text(
                            text = divisorError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Show calculated result when divisor is set
                    if (divisorText.isNotBlank() &&
                        divisorText.toIntOrNull()?.let { it > 0 } == true
                    ) {
                        val calculated = finalAmount
                        if (calculated != null) {
                            Text(
                                text = "Result: ¥${calculated.toPlainString()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Merchant
            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text("Merchant") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Method
            OutlinedTextField(
                value = paymentMethod,
                onValueChange = { paymentMethod = it },
                label = { Text("Payment Method") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Timestamp Picker
            val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

            OutlinedTextField(
                value = "${dateFormatter.format(Date(timestamp))} " +
                        "${timeFormatter.format(Date(timestamp))}",
                onValueChange = {},
                label = { Text("Timestamp") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    Row {
                        // Date picker
                        IconButton(onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    calendar.set(year, month, day)
                                    timestamp = calendar.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_my_calendar),
                                contentDescription = "Pick date"
                            )
                        }
                        // Time picker
                        IconButton(onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                                    calendar.set(Calendar.MINUTE, minute)
                                    timestamp = calendar.timeInMillis
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                true
                            ).show()
                        }) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_recent_history),
                                contentDescription = "Pick time"
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // View Type Selector (only Success and Request)
            Text(
                text = "View Type",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedButton(
                    selected = viewTypeIndex == 0,
                    onClick = { viewTypeIndex = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Success")
                }
                SegmentedButton(
                    selected = viewTypeIndex == 1,
                    onClick = { viewTypeIndex = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Request")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Upload Button (Green Check)
                Button(
                    enabled = saveTransaction() != null,
                    onClick = {
                        saveTransaction()?.let { updated ->
                            onUpload(updated)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Upload")
                }

                // Delete Button (Red Delete)
                Button(
                    onClick = {
                        onDelete(transaction)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Delete")
                }
            }
        }
    }
}