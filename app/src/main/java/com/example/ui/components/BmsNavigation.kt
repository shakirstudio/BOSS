package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.theme.BmsPrimaryDark
import com.example.ui.theme.BmsTertiary
import com.example.ui.viewmodel.BmsNavigationItem
import com.example.ui.viewmodel.BmsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BmsTopBar(
    viewModel: BmsViewModel,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val workspace by viewModel.workspace.collectAsState()
    val allWorkspaces by viewModel.allWorkspaces.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount = notifications.count { !it.isRead }
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val authUser by viewModel.authenticatedUser.collectAsState()

    var showWorkspacePicker by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 8.dp)
        ) {
            // Left: Hamburger Menu + Logo + Workspace Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 4.dp)
            ) {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("menu_drawer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Navigation Menu",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = "BOSS",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clickable { showWorkspacePicker = true }
                        .padding(vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = workspace?.name ?: "BMS Studio",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Switch Workspace",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "${workspace?.city ?: "Studio"} • Studio OS",
                        style = MaterialTheme.typography.labelSmall,
                        color = BmsTertiary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right: Actions (Theme, Search, Notifications, Role Chip)
            val syncStatus by viewModel.syncStatus.collectAsState()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = { viewModel.triggerCloudSync() },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("cloud_sync_button")
                ) {
                    Icon(
                        imageVector = if (syncStatus.isSyncing) Icons.Default.Sync else Icons.Default.CloudSync,
                        contentDescription = "Synchronize with Firebase Cloud",
                        tint = if (syncStatus.isSyncing) MaterialTheme.colorScheme.primary else if (syncStatus.lastSyncTime != null) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleDarkMode() },

                    modifier = Modifier
                        .size(36.dp)
                        .testTag("theme_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (isDarkMode) "Switch to Light Mode" else "Switch to Dark Mode",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleSearch(true) },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("global_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Global Search",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(19.dp)
                    )
                }

                BadgedBox(
                    badge = {
                        if (unreadCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ) {
                                Text(text = unreadCount.toString())
                            }
                        }
                    }
                ) {
                    IconButton(
                        onClick = { viewModel.toggleNotifications(true) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("notifications_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                val currentRole = authUser?.actualRole ?: AppRole.ADMIN
                val roleColor = when (currentRole) {
                    AppRole.ADMIN -> Color(0xFFEF4444)
                    AppRole.MANAGER -> MaterialTheme.colorScheme.primary
                    AppRole.EMPLOYEE -> Color(0xFF10B981)
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = roleColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, roleColor.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .clickable { showProfileDialog = true }
                        .testTag("user_role_chip")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = when (currentRole) {
                                AppRole.ADMIN -> Icons.Default.Security
                                AppRole.MANAGER -> Icons.Default.ManageAccounts
                                AppRole.EMPLOYEE -> Icons.Default.Person
                            },
                            contentDescription = "Role Icon",
                            tint = roleColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = currentRole.label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = roleColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    if (showProfileDialog) {
        Dialog(onDismissRequest = { showProfileDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text(
                            text = authUser?.fullName?.take(2)?.uppercase() ?: "ST",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = authUser?.fullName ?: "Shakir Tamboli",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = authUser?.email ?: "admin@bms.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val role = authUser?.actualRole ?: AppRole.ADMIN
                    val badgeColor = when (role) {
                        AppRole.ADMIN -> Color(0xFFEF4444)
                        AppRole.MANAGER -> MaterialTheme.colorScheme.primary
                        AppRole.EMPLOYEE -> Color(0xFF10B981)
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "RBAC Role: ${role.label}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Department: ${authUser?.department ?: "Management"}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            showProfileDialog = false
                            viewModel.logout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("topbar_logout_button")
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Log Out", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out / Switch Account", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Workspace Switcher Dialog
    if (showWorkspacePicker) {
        AlertDialog(
            onDismissRequest = { showWorkspacePicker = false },
            title = { Text("Switch Business Workspace") },
            text = {
                Column {
                    Text(
                        text = "Data is completely isolated per business studio tenant.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    allWorkspaces.forEach { ws ->
                        val isSelected = ws.id == workspace?.id
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.switchWorkspace(ws.id)
                                    showWorkspacePicker = false
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = ws.name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${ws.city} • Plan: ${ws.planId}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Active",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWorkspacePicker = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun BmsBottomNavBar(
    currentTab: BmsNavigationItem,
    onTabSelected: (BmsNavigationItem) -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        val primaryTabs = listOf(
            Triple(BmsNavigationItem.DASHBOARD, Icons.Default.Dashboard, "Dashboard"),
            Triple(BmsNavigationItem.LEADS, Icons.Default.People, "Leads"),
            Triple(BmsNavigationItem.BOOKINGS, Icons.Default.PhotoCamera, "Bookings"),
            Triple(BmsNavigationItem.FINANCE, Icons.Default.AccountBalanceWallet, "Finance")
        )

        primaryTabs.forEach { (tab, icon, label) ->
            val isSelected = currentTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label
                    )
                },
                label = { Text(text = label, fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }

        NavigationBarItem(
            selected = currentTab !in primaryTabs.map { it.first },
            onClick = onMoreClick,
            icon = {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = "More Modules"
                )
            },
            label = { Text(text = "Modules", fontSize = 10.5.sp, maxLines = 1, softWrap = false) }
        )
    }
}

@Composable
fun GlobalSearchDialog(
    viewModel: BmsViewModel,
    onDismiss: () -> Unit
) {
    val query by viewModel.searchQuery.collectAsState()
    val searchResults = remember(query) { viewModel.getSearchResults(query) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search clients, leads, bookings, tasks...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("global_search_input")
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (query.isBlank()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Search across your entire studio database",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else if (searchResults.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Text(
                            text = "No matching records found for '$query'",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(searchResults) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        viewModel.selectTab(item.targetTab)
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
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    ) {
                                        Text(
                                            text = item.module.take(2).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = item.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
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
    }
}
