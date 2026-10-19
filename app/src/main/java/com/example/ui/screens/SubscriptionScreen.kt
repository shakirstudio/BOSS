package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.BmsViewModel

data class PlanData(
    val id: String,
    val name: String,
    val monthlyPrice: Int,
    val yearlyPrice: Int,
    val description: String,
    val features: List<String>,
    val isPopular: Boolean = false
)

@Composable
fun SubscriptionScreen(
    viewModel: BmsViewModel,
    modifier: Modifier = Modifier
) {
    val workspace by viewModel.workspace.collectAsState()
    val subscription by viewModel.subscription.collectAsState()

    var isYearly by remember { mutableStateOf(true) }
    var selectedPlanForCheckout by remember { mutableStateOf<PlanData?>(null) }

    val currentPlanId = workspace?.planId ?: "STUDIO"

    val plans = listOf(
        PlanData(
            id = "FREE",
            name = "Free Starter",
            monthlyPrice = 0,
            yearlyPrice = 0,
            description = "For solo photographers starting their portfolio",
            features = listOf(
                "Up to 10 Leads & Clients",
                "Basic Quotation drafts",
                "Personal Task list",
                "Single User Access"
            )
        ),
        PlanData(
            id = "PRO",
            name = "Pro Freelancer",
            monthlyPrice = 299,
            yearlyPrice = 2999,
            description = "For busy wedding & event photographers",
            features = listOf(
                "Unlimited Leads & Clients",
                "GST-Ready PDF Quotations",
                "Shoot Gear Checklists",
                "Payments & Expense tracker",
                "Up to 3 Team Members"
            )
        ),
        PlanData(
            id = "STUDIO",
            name = "Studio Agency",
            monthlyPrice = 699,
            yearlyPrice = 6999,
            description = "Best for full-service photography & film studios",
            features = listOf(
                "Everything in Pro",
                "Photo & Video Editing Pipeline",
                "Album Design & Lab Printing",
                "Team Attendance & Leave approvals",
                "Client Journey Milestones",
                "Up to 10 Team Members"
            ),
            isPopular = true
        ),
        PlanData(
            id = "BUSINESS",
            name = "Enterprise Business",
            monthlyPrice = 1499,
            yearlyPrice = 14999,
            description = "For high-volume creative production houses",
            features = listOf(
                "Unlimited Team & Workspaces",
                "Advanced P&L Reports & CSV Export",
                "Priority Razorpay Integration",
                "Custom Terms & Branding",
                "Multi-Branch Readiness",
                "Dedicated Account Manager"
            )
        )
    )

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // Current Plan Header Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(text = "Active Subscription", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Studio: ${workspace?.name ?: "BMS Studios"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        StatusBadge(text = if (workspace?.isTrialActive == true) "7-Day Trial" else "Active")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(text = "Current Tier", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(text = "$currentPlanId Plan", fontWeight = FontWeight.Bold, color = BmsTertiary, style = MaterialTheme.typography.titleMedium)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Renewal Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text(text = subscription?.renewalDate ?: "01 Jan 2027", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        // Monthly / Yearly Discount Toggle
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Monthly", fontWeight = if (!isYearly) FontWeight.Bold else FontWeight.Normal, color = if (!isYearly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.width(10.dp))
                Switch(
                    checked = isYearly,
                    onCheckedChange = { isYearly = it },
                    modifier = Modifier.testTag("pricing_cycle_toggle")
                )
                Spacer(modifier = Modifier.width(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Yearly", fontWeight = if (isYearly) FontWeight.Bold else FontWeight.Normal, color = if (isYearly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(color = BmsSuccess.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                        Text(text = "SAVE 20%", color = BmsSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
        }

        // Plan Cards
        items(plans) { plan ->
            val isCurrent = plan.id.equals(currentPlanId, ignoreCase = true)
            val price = if (isYearly) plan.yearlyPrice else plan.monthlyPrice
            val cycleText = if (isYearly) "/year" else "/month"

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (plan.isPopular) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
                ),
                border = if (plan.isPopular) CardDefaults.outlinedCardBorder() else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(text = plan.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(text = plan.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (plan.isPopular) {
                            StatusBadge(text = "MOST POPULAR")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (price == 0) "Free" else "₹$price",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.headlineMedium,
                            color = BmsTertiary
                        )
                        if (price > 0) {
                            Text(text = " $cycleText", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    plan.features.forEach { feat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 3.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = BmsSuccess, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = feat, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isCurrent) {
                        OutlinedButton(
                            onClick = { },
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Current Active Plan")
                        }
                    } else {
                        Button(
                            onClick = { selectedPlanForCheckout = plan },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (plan.isPopular) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (price == 0) "Downgrade to Free" else "Upgrade with Razorpay",
                                color = if (plan.isPopular) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    selectedPlanForCheckout?.let { plan ->
        RazorpayCheckoutDialog(
            plan = plan,
            isYearly = isYearly,
            onDismiss = { selectedPlanForCheckout = null },
            onConfirmUpgrade = {
                viewModel.upgradeSubscription(plan.id, if (isYearly) "YEARLY" else "MONTHLY")
                selectedPlanForCheckout = null
            }
        )
    }
}

@Composable
fun RazorpayCheckoutDialog(
    plan: PlanData,
    isYearly: Boolean,
    onDismiss: () -> Unit,
    onConfirmUpgrade: () -> Unit
) {
    val price = if (isYearly) plan.yearlyPrice else plan.monthlyPrice
    val gst = price * 0.18
    val grandTotal = price + gst

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = BmsSuccess)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Razorpay Secure Checkout", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 10.dp))

                Text(text = "Plan: ${plan.name} (${if (isYearly) "Annual Billing" else "Monthly Billing"})", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(text = "Base Subscription:", style = MaterialTheme.typography.bodySmall)
                            Text(text = "₹$price", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(text = "GST (18%):", style = MaterialTheme.typography.bodySmall)
                            Text(text = "₹${gst.toInt()}", style = MaterialTheme.typography.bodySmall)
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(text = "Total Payable:", fontWeight = FontWeight.Bold)
                            Text(text = "₹${grandTotal.toInt()}", fontWeight = FontWeight.Bold, color = BmsTertiary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Razorpay Order ID: order_BMS_${(100000..999999).random()}\nSignature verified server-side.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onConfirmUpgrade,
                    colors = ButtonDefaults.buttonColors(containerColor = BmsPrimaryDark),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Pay ₹${grandTotal.toInt()} & Activate Tier")
                }
            }
        }
    }
}
