package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NotificationEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.BmsNavigationItem
import com.example.ui.viewmodel.BmsViewModel

data class ModuleItem(
    val tab: BmsNavigationItem,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModulePickerSheet(
    currentTab: BmsNavigationItem,
    onTabSelected: (BmsNavigationItem) -> Unit,
    onDismiss: () -> Unit
) {
    val modules = listOf(
        ModuleItem(BmsNavigationItem.DASHBOARD, "Dashboard", "Overview & KPIs", Icons.Default.Dashboard, BmsPrimaryDark),
        ModuleItem(BmsNavigationItem.LEADS, "Leads CRM", "Inquiries & Follow-ups", Icons.Default.People, BmsSecondary),
        ModuleItem(BmsNavigationItem.CLIENTS, "Clients", "Accounts & History", Icons.Default.PersonOutline, BmsTertiary),
        ModuleItem(BmsNavigationItem.QUOTATIONS, "Quotations", "Proposals & GST", Icons.Default.Description, Color(0xFF8B5CF6)),
        ModuleItem(BmsNavigationItem.BOOKINGS, "Bookings & Shoots", "Shoots & Checklist", Icons.Default.PhotoCamera, Color(0xFF10B981)),
        ModuleItem(BmsNavigationItem.WORKFLOWS, "Editing & Albums", "Post-Production", Icons.Default.PhotoLibrary, Color(0xFFEC4899)),
        ModuleItem(BmsNavigationItem.STORAGE, "Storage & Drives", "Cloud, Drives & Links", Icons.Default.CloudQueue, Color(0xFF3B82F6)),
        ModuleItem(BmsNavigationItem.FINANCE, "Payments & Expenses", "Cashflow & Ledgers", Icons.Default.AccountBalanceWallet, Color(0xFF14B8A6)),
        ModuleItem(BmsNavigationItem.FINANCIAL_YEAR, "FY Analytics", "Revenue, Wages & P&L", Icons.Default.QueryStats, Color(0xFFF59E0B)),
        ModuleItem(BmsNavigationItem.TEAM, "Team & HR", "Staff, Leaves & Pay", Icons.Default.Badge, Color(0xFF6366F1)),
        ModuleItem(BmsNavigationItem.SALARY_REPORTS, "Salary Reports", "Slips, Payroll & Exports", Icons.Default.RequestQuote, Color(0xFF10B981)),
        ModuleItem(BmsNavigationItem.TASKS, "Tasks", "To-Dos & Deadlines", Icons.Default.Checklist, Color(0xFFF97316)),
        ModuleItem(BmsNavigationItem.REPORTS, "Reports & Analytics", "Sales & Workload", Icons.Default.Analytics, Color(0xFF06B6D4)),
        ModuleItem(BmsNavigationItem.SUBSCRIPTION, "SaaS Plans", "Pricing & Razorpay", Icons.Default.Stars, BmsTertiary),
        ModuleItem(BmsNavigationItem.SETTINGS, "Settings", "Business & Config", Icons.Default.Settings, Color(0xFF94A3B8)),
        ModuleItem(BmsNavigationItem.ADMIN, "Super Admin", "SaaS Multi-Tenant", Icons.Default.AdminPanelSettings, Color(0xFFE11D48))
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Business Operational Studio System",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "BOSS Module & Operational workflows",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                items(modules) { item ->
                    val isSelected = currentTab == item.tab
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onTabSelected(item.tab)
                                onDismiss()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(item.color.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = item.color,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSheet(
    notifications: List<NotificationEntity>,
    onMarkAllRead: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Notifications",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(text = "${notifications.count { !it.isRead }} unread")
                }
                TextButton(onClick = onMarkAllRead) {
                    Text("Mark all read", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (notifications.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                ) {
                    Text(
                        text = "No notifications right now",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    items(notifications) { notif ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (notif.isRead) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                val priorityColor = when (notif.priority) {
                                    "URGENT" -> BmsError
                                    "WARNING" -> BmsWarning
                                    else -> BmsPrimaryDark
                                }
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(priorityColor)
                                        .align(Alignment.CenterVertically)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = notif.title,
                                            fontWeight = if (notif.isRead) FontWeight.Normal else FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        StatusBadge(text = notif.priority)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = notif.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = notif.timestamp,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
