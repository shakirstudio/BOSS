package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpenseEntity
import com.example.data.model.PaymentEntity
import com.example.ui.components.BmsSectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.BmsViewModel

@Composable
fun FinanceScreen(
    viewModel: BmsViewModel,
    modifier: Modifier = Modifier
) {
    val payments by viewModel.payments.collectAsState()
    val expenses by viewModel.expenses.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Payments Received, 1: Expenses
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var editingPayment by remember { mutableStateOf<PaymentEntity?>(null) }
    var editingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }

    val totalCollected = payments.sumOf { it.amount }
    val totalExpenses = expenses.sumOf { it.amount }
    val netProfit = totalCollected - totalExpenses

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) showPaymentDialog = true else showExpenseDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("finance_fab")
            ) {
                Icon(
                    imageVector = if (selectedTab == 0) Icons.Default.AddCard else Icons.Default.ReceiptLong,
                    contentDescription = "Record Finance Entry"
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Profit & Loss Quick Summary Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Cashflow & Financial Velocity",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(text = "Total Inflow", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(text = "₹${totalCollected.toInt()}", fontWeight = FontWeight.Bold, color = BmsSuccess, style = MaterialTheme.typography.titleMedium)
                        }
                        Column {
                            Text(text = "Studio Expenses", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(text = "₹${totalExpenses.toInt()}", fontWeight = FontWeight.Bold, color = BmsError, style = MaterialTheme.typography.titleMedium)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Net Surplus", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(text = "₹${netProfit.toInt()}", fontWeight = FontWeight.Black, color = BmsTertiary, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Client Payments (${payments.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Studio Expenses (${expenses.size})") })
            }

            if (selectedTab == 0) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(payments, key = { it.id }) { pay ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = pay.clientName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text(text = "${pay.paymentType} • ${pay.paymentDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    Text(text = "Ref: ${pay.transactionRef} (${pay.paymentMethod})", style = MaterialTheme.typography.labelSmall, color = BmsPrimaryDark)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = "₹${pay.amount.toInt()}", fontWeight = FontWeight.Bold, color = BmsSuccess, style = MaterialTheme.typography.titleMedium)
                                        StatusBadge(text = pay.status)
                                    }
                                    IconButton(onClick = { editingPayment = pay }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Payment", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(expenses, key = { it.id }) { exp ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        StatusBadge(text = exp.category)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = exp.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = exp.description, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    Text(text = "Claimed by: ${exp.employeeName} (${exp.paymentMethod})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(text = "-₹${exp.amount.toInt()}", fontWeight = FontWeight.Bold, color = BmsError, style = MaterialTheme.typography.titleMedium)
                                    IconButton(onClick = { editingExpense = exp }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Expense", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPaymentDialog) {
        RecordPaymentDialog(
            onDismiss = { showPaymentDialog = false },
            onAdd = { newPayment ->
                viewModel.recordPayment(newPayment)
                showPaymentDialog = false
            }
        )
    }

    if (showExpenseDialog) {
        RecordExpenseDialog(
            onDismiss = { showExpenseDialog = false },
            onAdd = { newExpense ->
                viewModel.recordExpense(newExpense)
                showExpenseDialog = false
            }
        )
    }

    editingPayment?.let { payToEdit ->
        EditPaymentDialog(
            payment = payToEdit,
            onDismiss = { editingPayment = null },
            onSave = { updated ->
                viewModel.updatePayment(updated)
                editingPayment = null
            }
        )
    }

    editingExpense?.let { expToEdit ->
        EditExpenseDialog(
            expense = expToEdit,
            onDismiss = { editingExpense = null },
            onSave = { updated ->
                viewModel.updateExpense(updated)
                editingExpense = null
            }
        )
    }
}

@Composable
fun RecordPaymentDialog(
    onDismiss: () -> Unit,
    onAdd: (PaymentEntity) -> Unit
) {
    var clientName by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("50000") }
    var method by remember { mutableStateOf("UPI") }
    var ref by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Advance Booking") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Client Payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Client Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount Received (₹) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = method,
                    onValueChange = { method = it },
                    label = { Text("Method (UPI, Bank Transfer, Razorpay, Cash)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ref,
                    onValueChange = { ref = it },
                    label = { Text("Transaction Reference / UTR") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Payment Type (Advance, Second Installment, Final)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 10000.0
                    val payment = PaymentEntity(
                        id = "pay_" + System.currentTimeMillis().toString().takeLast(6),
                        workspaceId = "",
                        bookingId = "manual_booking",
                        clientName = clientName.trim(),
                        amount = amount,
                        paymentDate = "Today",
                        paymentMethod = method.trim(),
                        transactionRef = if (ref.isBlank()) "TXN-${(10000..99999).random()}" else ref.trim(),
                        paymentType = type.trim(),
                        status = "Paid"
                    )
                    onAdd(payment)
                },
                enabled = clientName.isNotBlank() && amountStr.isNotBlank()
            ) {
                Text("Record Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun RecordExpenseDialog(
    onDismiss: () -> Unit,
    onAdd: (ExpenseEntity) -> Unit
) {
    var category by remember { mutableStateOf("Equipment") }
    var amountStr by remember { mutableStateOf("5000") }
    var description by remember { mutableStateOf("") }
    var employee by remember { mutableStateOf("Studio Staff") }
    var method by remember { mutableStateOf("UPI") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Studio Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (Travel, Equipment, Printing, Food, Petrol)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Expense Amount (₹) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Expense Description *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = employee,
                    onValueChange = { employee = it },
                    label = { Text("Claimed By (Employee)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 1000.0
                    val exp = ExpenseEntity(
                        id = "exp_" + System.currentTimeMillis().toString().takeLast(6),
                        workspaceId = "",
                        category = category.trim(),
                        amount = amount,
                        date = "Today",
                        description = description.trim(),
                        employeeName = employee.trim(),
                        paymentMethod = method.trim()
                    )
                    onAdd(exp)
                },
                enabled = description.isNotBlank() && amountStr.isNotBlank()
            ) {
                Text("Save Expense")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditPaymentDialog(
    payment: PaymentEntity,
    onDismiss: () -> Unit,
    onSave: (PaymentEntity) -> Unit
) {
    var clientName by remember { mutableStateOf(payment.clientName) }
    var amountStr by remember { mutableStateOf(payment.amount.toInt().toString()) }
    var paymentDate by remember { mutableStateOf(payment.paymentDate) }
    var paymentMethod by remember { mutableStateOf(payment.paymentMethod) }
    var transactionRef by remember { mutableStateOf(payment.transactionRef) }
    var paymentType by remember { mutableStateOf(payment.paymentType) }
    var status by remember { mutableStateOf(payment.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Payment Record") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                item { OutlinedTextField(value = clientName, onValueChange = { clientName = it }, label = { Text("Client Name") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = amountStr, onValueChange = { amountStr = it }, label = { Text("Amount Received (₹)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = paymentDate, onValueChange = { paymentDate = it }, label = { Text("Payment Date") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = paymentMethod, onValueChange = { paymentMethod = it }, label = { Text("Payment Method (UPI, Bank Transfer, Razorpay, Cash)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = transactionRef, onValueChange = { transactionRef = it }, label = { Text("UTR / Reference ID") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = paymentType, onValueChange = { paymentType = it }, label = { Text("Payment Stage (Advance, Milestone, Final Balance)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = status, onValueChange = { status = it }, label = { Text("Status (Paid, Pending, Failed)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: payment.amount
                    onSave(payment.copy(clientName = clientName.trim(), amount = amt, paymentDate = paymentDate.trim(), paymentMethod = paymentMethod.trim(), transactionRef = transactionRef.trim(), paymentType = paymentType.trim(), status = status.trim()))
                },
                enabled = clientName.isNotBlank()
            ) { Text("Save Changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EditExpenseDialog(
    expense: ExpenseEntity,
    onDismiss: () -> Unit,
    onSave: (ExpenseEntity) -> Unit
) {
    var category by remember { mutableStateOf(expense.category) }
    var amountStr by remember { mutableStateOf(expense.amount.toInt().toString()) }
    var description by remember { mutableStateOf(expense.description) }
    var employeeName by remember { mutableStateOf(expense.employeeName) }
    var paymentMethod by remember { mutableStateOf(expense.paymentMethod) }
    var date by remember { mutableStateOf(expense.date) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Expense Record") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                item { OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (Travel, Gear Rental, Printing, Studio, Catering)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = amountStr, onValueChange = { amountStr = it }, label = { Text("Expense Amount (₹)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Expense Purpose / Description") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = employeeName, onValueChange = { employeeName = it }, label = { Text("Incurred / Claimed By") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = paymentMethod, onValueChange = { paymentMethod = it }, label = { Text("Payment Mode (UPI, Company Card, Petty Cash)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Expense Date") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: expense.amount
                    onSave(expense.copy(category = category.trim(), amount = amt, description = description.trim(), employeeName = employeeName.trim(), paymentMethod = paymentMethod.trim(), date = date.trim()))
                },
                enabled = description.isNotBlank()
            ) { Text("Save Changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
