package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlbumProjectEntity
import com.example.data.model.DeliveryEntity
import com.example.data.model.PhotoEditEntity
import com.example.data.model.VideoEditEntity
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.BmsViewModel

@Composable
fun WorkflowsScreen(
    viewModel: BmsViewModel,
    modifier: Modifier = Modifier
) {
    val photoEdits by viewModel.visiblePhotoEdits.collectAsState()
    val videoEdits by viewModel.visibleVideoEdits.collectAsState()
    val albums by viewModel.visibleAlbumProjects.collectAsState()
    val deliveries by viewModel.deliveries.collectAsState()
    val currentUserName by viewModel.currentUserName.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()
    val isUserAdminOrOwner by viewModel.isUserAdminOrOwner.collectAsState()

    var selectedWorkflowTab by remember { mutableStateOf(0) } // 0: Photo, 1: Video, 2: Album, 3: Deliveries
    var editingPhoto by remember { mutableStateOf<PhotoEditEntity?>(null) }
    var editingVideo by remember { mutableStateOf<VideoEditEntity?>(null) }
    var editingAlbum by remember { mutableStateOf<AlbumProjectEntity?>(null) }
    var editingDelivery by remember { mutableStateOf<DeliveryEntity?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        if (!isUserAdminOrOwner) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.LockPerson,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Restricted View: $currentUserName ($currentUserRole) • Showing only projects assigned to you",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        ScrollableTabRow(
            selectedTabIndex = selectedWorkflowTab,
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 16.dp
        ) {
            Tab(selected = selectedWorkflowTab == 0, onClick = { selectedWorkflowTab = 0 }, text = { Text("Photo Editing (${photoEdits.size})") })
            Tab(selected = selectedWorkflowTab == 1, onClick = { selectedWorkflowTab = 1 }, text = { Text("Video Editing (${videoEdits.size})") })
            Tab(selected = selectedWorkflowTab == 2, onClick = { selectedWorkflowTab = 2 }, text = { Text("Album Design (${albums.size})") })
            Tab(selected = selectedWorkflowTab == 3, onClick = { selectedWorkflowTab = 3 }, text = { Text("Deliveries (${deliveries.size})") })
        }

        when (selectedWorkflowTab) {
            0 -> PhotoEditingTab(
                edits = photoEdits,
                onEdit = { editingPhoto = it },
                onUpdateProgress = { edit, count, status ->
                    viewModel.updatePhotoEditProgress(edit, count, status)
                }
            )
            1 -> VideoEditingTab(
                edits = videoEdits,
                onEdit = { editingVideo = it },
                onUpdateStatus = { edit, status ->
                    viewModel.updateVideoEditStatus(edit, status)
                }
            )
            2 -> AlbumDesigningTab(
                albums = albums,
                onEdit = { editingAlbum = it },
                onUpdateStatus = { album, status ->
                    viewModel.updateAlbumStatus(album, status)
                }
            )
            3 -> DeliveriesTab(
                deliveries = deliveries,
                onEdit = { editingDelivery = it },
                onUpdateDelivery = { delivery, status, confirmed ->
                    viewModel.updateDelivery(delivery, status, confirmed)
                }
            )
        }
    }

    editingPhoto?.let { photoToEdit ->
        EditPhotoDialog(
            photo = photoToEdit,
            onDismiss = { editingPhoto = null },
            onSave = { updated ->
                viewModel.updatePhotoEdit(updated)
                editingPhoto = null
            }
        )
    }

    editingVideo?.let { videoToEdit ->
        EditVideoDialog(
            video = videoToEdit,
            onDismiss = { editingVideo = null },
            onSave = { updated ->
                viewModel.updateVideoEdit(updated)
                editingVideo = null
            }
        )
    }

    editingAlbum?.let { albumToEdit ->
        EditAlbumDialog(
            album = albumToEdit,
            onDismiss = { editingAlbum = null },
            onSave = { updated ->
                viewModel.updateAlbumProject(updated)
                editingAlbum = null
            }
        )
    }

    editingDelivery?.let { delToEdit ->
        EditDeliveryDialog(
            delivery = delToEdit,
            onDismiss = { editingDelivery = null },
            onSave = { updated ->
                viewModel.updateDelivery(updated, updated.status, updated.confirmedByClient)
                editingDelivery = null
            }
        )
    }
}

@Composable
fun PhotoEditingTab(
    edits: List<PhotoEditEntity>,
    onEdit: (PhotoEditEntity) -> Unit,
    onUpdateProgress: (PhotoEditEntity, Int, String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(edits, key = { it.id }) { edit ->
            val progress = if (edit.totalPhotos > 0) edit.editedPhotos.toFloat() / edit.totalPhotos.toFloat() else 0f
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = edit.projectName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Client: ${edit.clientName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            StatusBadge(text = edit.status)
                            IconButton(onClick = { onEdit(edit) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Photo Project", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Editor: ${edit.editorName}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text(text = "${edit.editedPhotos} / ${edit.totalPhotos} photos (${(progress * 100).toInt()}%)", style = MaterialTheme.typography.labelMedium, color = BmsPrimaryDark, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        color = BmsSecondary,
                        trackColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Deadline: ${edit.deadline} • Priority: ${edit.priority}", style = MaterialTheme.typography.labelSmall, color = if (edit.priority == "Urgent") BmsError else MaterialTheme.colorScheme.outline)

                    if (edit.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = edit.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Fast progress increment button
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (edit.editedPhotos < edit.totalPhotos) {
                            Button(
                                onClick = {
                                    val nextCount = minOf(edit.totalPhotos, edit.editedPhotos + 100)
                                    val newStatus = if (nextCount == edit.totalPhotos) "Quality Check" else "In Progress"
                                    onUpdateProgress(edit, nextCount, newStatus)
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+100 Photos Graded", fontSize = 11.sp)
                            }
                        } else {
                            Text("All Photos Graded & Approved ✓", color = BmsSuccess, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoEditingTab(
    edits: List<VideoEditEntity>,
    onEdit: (VideoEditEntity) -> Unit,
    onUpdateStatus: (VideoEditEntity, String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(edits, key = { it.id }) { edit ->
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = edit.clientName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = edit.videoType, style = MaterialTheme.typography.bodySmall, color = BmsPrimaryDark, fontWeight = FontWeight.SemiBold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            StatusBadge(text = edit.status)
                            IconButton(onClick = { onEdit(edit) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Video Project", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Target Duration: ${edit.duration}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Lead Editor: ${edit.editorName} • Deadline: ${edit.deadline}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)

                    if (edit.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = edit.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("Rough Cut", "Review", "Revision", "Approved").forEach { status ->
                            FilterChip(
                                selected = edit.status == status,
                                onClick = { onUpdateStatus(edit, status) },
                                label = { Text(status, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumDesigningTab(
    albums: List<AlbumProjectEntity>,
    onEdit: (AlbumProjectEntity) -> Unit,
    onUpdateStatus: (AlbumProjectEntity, String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(albums, key = { it.id }) { album ->
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = album.clientName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "${album.albumSize} • ${album.numberOfPages} Pages", style = MaterialTheme.typography.bodySmall, color = BmsTertiary, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            StatusBadge(text = album.status)
                            IconButton(onClick = { onEdit(album) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Album Project", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Couture Designer: ${album.designerName} • Deadline: ${album.deadline}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)

                    if (album.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = album.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("Designing", "Client Review", "Printing", "Delivered").forEach { status ->
                            FilterChip(
                                selected = album.status == status,
                                onClick = { onUpdateStatus(album, status) },
                                label = { Text(status, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeliveriesTab(
    deliveries: List<DeliveryEntity>,
    onEdit: (DeliveryEntity) -> Unit,
    onUpdateDelivery: (DeliveryEntity, String, Boolean) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(deliveries, key = { it.id }) { del ->
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = del.clientName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = del.deliveryType, style = MaterialTheme.typography.bodySmall, color = BmsPrimaryDark, fontWeight = FontWeight.SemiBold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            StatusBadge(text = del.status)
                            IconButton(onClick = { onEdit(del) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Delivery", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Recipient: ${del.recipient} • Target Date: ${del.deliveryDate}", style = MaterialTheme.typography.bodySmall)

                    if (del.trackingOrLink.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Tracking / Gallery: ${del.trackingOrLink}", style = MaterialTheme.typography.labelSmall, color = BmsSecondary)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = del.confirmedByClient,
                                onCheckedChange = { onUpdateDelivery(del, del.status, it) }
                            )
                            Text(
                                text = if (del.confirmedByClient) "Client Confirmed Receipt ✓" else "Awaiting Client Signoff",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (del.confirmedByClient) BmsSuccess else MaterialTheme.colorScheme.outline
                            )
                        }

                        if (del.status != "Delivered") {
                            Button(
                                onClick = { onUpdateDelivery(del, "Delivered", true) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BmsSuccess)
                            ) {
                                Text("Mark Delivered", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditPhotoDialog(
    photo: PhotoEditEntity,
    onDismiss: () -> Unit,
    onSave: (PhotoEditEntity) -> Unit
) {
    var projectName by remember { mutableStateOf(photo.projectName) }
    var clientName by remember { mutableStateOf(photo.clientName) }
    var editorName by remember { mutableStateOf(photo.editorName) }
    var totalPhotosStr by remember { mutableStateOf(photo.totalPhotos.toString()) }
    var editedPhotosStr by remember { mutableStateOf(photo.editedPhotos.toString()) }
    var status by remember { mutableStateOf(photo.status) }
    var deadline by remember { mutableStateOf(photo.deadline) }
    var notes by remember { mutableStateOf(photo.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Photo Editing Project") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                item { OutlinedTextField(value = projectName, onValueChange = { projectName = it }, label = { Text("Project Title") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = clientName, onValueChange = { clientName = it }, label = { Text("Client Name") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = editorName, onValueChange = { editorName = it }, label = { Text("Assigned Editor") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = editedPhotosStr, onValueChange = { editedPhotosStr = it }, label = { Text("Edited Photos") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = totalPhotosStr, onValueChange = { totalPhotosStr = it }, label = { Text("Total Photos") }, modifier = Modifier.weight(1f))
                    }
                }
                item { OutlinedTextField(value = status, onValueChange = { status = it }, label = { Text("Status (Culling, Color Grading, Retouching, Delivered)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = deadline, onValueChange = { deadline = it }, label = { Text("Deadline") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Client Tone & Style Notes") }, modifier = Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tot = totalPhotosStr.toIntOrNull() ?: photo.totalPhotos
                    val ed = editedPhotosStr.toIntOrNull() ?: photo.editedPhotos
                    onSave(photo.copy(projectName = projectName.trim(), clientName = clientName.trim(), editorName = editorName.trim(), totalPhotos = tot, editedPhotos = ed, status = status.trim(), deadline = deadline.trim(), notes = notes.trim()))
                },
                enabled = projectName.isNotBlank()
            ) { Text("Save Changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EditVideoDialog(
    video: VideoEditEntity,
    onDismiss: () -> Unit,
    onSave: (VideoEditEntity) -> Unit
) {
    var clientName by remember { mutableStateOf(video.clientName) }
    var videoType by remember { mutableStateOf(video.videoType) }
    var duration by remember { mutableStateOf(video.duration) }
    var editorName by remember { mutableStateOf(video.editorName) }
    var status by remember { mutableStateOf(video.status) }
    var deadline by remember { mutableStateOf(video.deadline) }
    var notes by remember { mutableStateOf(video.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Video Project") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                item { OutlinedTextField(value = clientName, onValueChange = { clientName = it }, label = { Text("Client Name") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = videoType, onValueChange = { videoType = it }, label = { Text("Video Type (Teaser, Highlights, Full Film)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Target Duration (e.g. 4 mins, 45 mins)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = editorName, onValueChange = { editorName = it }, label = { Text("Lead Video Editor") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = status, onValueChange = { status = it }, label = { Text("Status (Rough Cut, Review, Revision, Approved)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = deadline, onValueChange = { deadline = it }, label = { Text("Delivery Deadline") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Song Selection & Revisions Notes") }, modifier = Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(video.copy(clientName = clientName.trim(), videoType = videoType.trim(), duration = duration.trim(), editorName = editorName.trim(), status = status.trim(), deadline = deadline.trim(), notes = notes.trim()))
                },
                enabled = clientName.isNotBlank()
            ) { Text("Save Changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EditAlbumDialog(
    album: AlbumProjectEntity,
    onDismiss: () -> Unit,
    onSave: (AlbumProjectEntity) -> Unit
) {
    var clientName by remember { mutableStateOf(album.clientName) }
    var albumSize by remember { mutableStateOf(album.albumSize) }
    var numberOfPagesStr by remember { mutableStateOf(album.numberOfPages.toString()) }
    var designerName by remember { mutableStateOf(album.designerName) }
    var status by remember { mutableStateOf(album.status) }
    var deadline by remember { mutableStateOf(album.deadline) }
    var notes by remember { mutableStateOf(album.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Album Printing Project") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                item { OutlinedTextField(value = clientName, onValueChange = { clientName = it }, label = { Text("Client Name") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = albumSize, onValueChange = { albumSize = it }, label = { Text("Album Dimensions & Paper (e.g. 12x36 Canvera Silk)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = numberOfPagesStr, onValueChange = { numberOfPagesStr = it }, label = { Text("Number of Sheets / Pages") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = designerName, onValueChange = { designerName = it }, label = { Text("Album Designer") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = status, onValueChange = { status = it }, label = { Text("Status (Designing, Client Review, Printing, Delivered)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = deadline, onValueChange = { deadline = it }, label = { Text("Dispatch Deadline") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Cover Embossing & Paper Notes") }, modifier = Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = numberOfPagesStr.toIntOrNull() ?: album.numberOfPages
                    onSave(album.copy(clientName = clientName.trim(), albumSize = albumSize.trim(), numberOfPages = p, designerName = designerName.trim(), status = status.trim(), deadline = deadline.trim(), notes = notes.trim()))
                },
                enabled = clientName.isNotBlank()
            ) { Text("Save Changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EditDeliveryDialog(
    delivery: DeliveryEntity,
    onDismiss: () -> Unit,
    onSave: (DeliveryEntity) -> Unit
) {
    var clientName by remember { mutableStateOf(delivery.clientName) }
    var deliveryType by remember { mutableStateOf(delivery.deliveryType) }
    var recipient by remember { mutableStateOf(delivery.recipient) }
    var deliveryDate by remember { mutableStateOf(delivery.deliveryDate) }
    var trackingOrLink by remember { mutableStateOf(delivery.trackingOrLink) }
    var status by remember { mutableStateOf(delivery.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Client Delivery") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                item { OutlinedTextField(value = clientName, onValueChange = { clientName = it }, label = { Text("Client Name") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = deliveryType, onValueChange = { deliveryType = it }, label = { Text("Delivery Type (Raw Hard Drive, Master USB, Online Cloud)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = recipient, onValueChange = { recipient = it }, label = { Text("Recipient Name & Address") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = deliveryDate, onValueChange = { deliveryDate = it }, label = { Text("Delivery / Handover Date") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = trackingOrLink, onValueChange = { trackingOrLink = it }, label = { Text("Tracking Number / Download Gallery Link") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = status, onValueChange = { status = it }, label = { Text("Status (Pending, Dispatched, Delivered)") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(delivery.copy(clientName = clientName.trim(), deliveryType = deliveryType.trim(), recipient = recipient.trim(), deliveryDate = deliveryDate.trim(), trackingOrLink = trackingOrLink.trim(), status = status.trim()))
                },
                enabled = clientName.isNotBlank()
            ) { Text("Save Changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
