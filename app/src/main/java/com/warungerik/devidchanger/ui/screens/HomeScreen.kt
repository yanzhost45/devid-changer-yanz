package com.warungerik.devidchanger.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warungerik.devidchanger.MainViewModel
import com.warungerik.devidchanger.overlay.DeviceIdOverlayService
import com.warungerik.devidchanger.R
import com.warungerik.devidchanger.root.RootExecutor
import com.warungerik.devidchanger.ui.components.ProcessingDialog
import com.warungerik.devidchanger.ui.components.RootStatusCard
import com.warungerik.devidchanger.ui.components.SaveIdDialog
import com.warungerik.devidchanger.ui.components.VersionSelector
import com.warungerik.devidchanger.ui.theme.AppStrings
import com.warungerik.devidchanger.ui.theme.DarkSuccess
import com.warungerik.devidchanger.ui.theme.LightSuccess

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val strings = AppStrings(uiState.currentLanguage)
    val context = LocalContext.current

    val deviceIdFilePicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                viewModel.loadDeviceIdFile(context, uri)
            }
        }

    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()
    val successColor = if (MaterialTheme.colorScheme.background.red < 0.5f) DarkSuccess else LightSuccess

    var idToSave by remember { mutableStateOf<String?>(null) }
    var wasProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadSavedDevIds(context)
    }

    LaunchedEffect(uiState.isProcessing) {
        if (wasProcessing && !uiState.isProcessing) {
            val lastLog = uiState.logs.lastOrNull()
            if (lastLog?.message?.contains("Selesai") == true || lastLog?.message?.contains("Done") == true) {
                Toast.makeText(context, strings.toastInjectionSuccess, Toast.LENGTH_LONG).show()
            }
        }
        wasProcessing = uiState.isProcessing
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        RootStatusCard(
            rootInfo = uiState.rootInfo,
            strings = strings,
            onRefresh = { viewModel.checkRootAndFetch() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        VersionSelector(
            selectedVersion = uiState.selectedVersion,
            onVersionSelected = { viewModel.selectVersion(it) },
            title = strings.versionSelectorTitle
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = strings.currentJsonIdTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (uiState.currentJsonId != null) {
                        Row {
                            OutlinedButton(
                                onClick = { idToSave = uiState.currentJsonId },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = strings.btnSaveId,
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = strings.btnSaveId,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(uiState.currentJsonId!!))
                                    Toast.makeText(context, strings.toastCopySuccess, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = strings.btnCopy,
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = strings.btnCopy,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                SelectionContainer {
                    Text(
                        text = when {
                            !uiState.isGameFolderExists -> strings.fileNotFound
                            uiState.currentJsonId == null -> strings.notFound
                            else -> uiState.currentJsonId!!
                        },
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = when {
                            !uiState.isGameFolderExists -> MaterialTheme.colorScheme.error
                            uiState.currentJsonId == null -> MaterialTheme.colorScheme.primary
                            else -> successColor
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = strings.newJsonIdTitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))

                OutlinedButton(
                    onClick = {
                        deviceIdFilePicker.launch(
                            arrayOf("text/plain", "text/*")
                        )
                    },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "PICK TXT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                OutlinedButton(
                    onClick = {
                        if (!Settings.canDrawOverlays(context)) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } else {
                            val intent = Intent(
                                context,
                                DeviceIdOverlayService::class.java
                            )

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                        }
                    },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "FLOAT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                if (uiState.newJsonId.isNotBlank()) {
                    OutlinedButton(
                        onClick = { idToSave = uiState.newJsonId.trim() },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = strings.btnSaveId,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = strings.btnSaveId,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                }

                OutlinedButton(
                    onClick = {
                        viewModel.generateNewGuestId()
                        Toast.makeText(context, strings.toastRandomSuccess, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = strings.btnRandomGuest,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = strings.btnRandomGuest,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                OutlinedButton(
                    onClick = {
                        val clipText = clipboardManager.getText()?.text
                        if (!clipText.isNullOrEmpty()) {
                            viewModel.updateNewJsonId(clipText)
                            Toast.makeText(context, strings.toastPasteSuccess, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, strings.toastPasteEmpty, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = strings.btnPaste,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = strings.btnPaste,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.newJsonId,
                onValueChange = { viewModel.updateNewJsonId(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = strings.placeholderNewJsonId,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                },
                singleLine = false,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(8.dp)
            )

            if (uiState.deviceIdCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { viewModel.previousDeviceId() },
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "◀ PREV",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${uiState.deviceIdPosition} / ${uiState.deviceIdCount}",
                        modifier = Modifier.weight(1f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = { viewModel.nextDeviceId() },
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "NEXT ▶",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.idLength(uiState.newJsonId.length),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.weight(1f))
                if (uiState.selectedVersion.isUS) {
                    val is84 = uiState.newJsonId.length == 84
                    Text(
                        text = if (is84) strings.usaCharOk else strings.usaCharNeed,
                        fontSize = 11.sp,
                        color = if (is84) successColor else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { viewModel.executeInjection() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !uiState.isProcessing && uiState.rootInfo.isRooted,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            ) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = strings.processing,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = strings.btnExecute,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.rootInfo.isRooted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    launchMobileLegends(
                        context = context,
                        packageName = uiState.selectedVersion.packageName,
                        displayName = uiState.selectedVersion.displayName,
                        strings = strings
                    )
                },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_mlbb),
                    contentDescription = strings.btnOpenGame,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${strings.btnOpenGame} (${uiState.selectedVersion.displayName})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (uiState.isProcessing) {
            ProcessingDialog(
                strings = strings,
                currentStep = uiState.currentProcessStep
            )
        }

        if (uiState.showSuccessDialog) {
            com.warungerik.devidchanger.ui.components.SuccessDialog(
                strings = strings,
                onDismiss = { viewModel.dismissSuccessDialog() },
                onOpenGame = {
                    launchMobileLegends(
                        context = context,
                        packageName = uiState.selectedVersion.packageName,
                        displayName = uiState.selectedVersion.displayName,
                        strings = strings
                    )
                }
            )
        }

        if (idToSave != null) {
            SaveIdDialog(
                devIdToSave = idToSave!!,
                strings = strings,
                onSave = { label ->
                    viewModel.saveDevId(context, label, idToSave!!)
                    Toast.makeText(context, strings.toastSaveSuccess, Toast.LENGTH_SHORT).show()
                    idToSave = null
                },
                onDismiss = { idToSave = null }
            )
        }
    }
}


private fun launchMobileLegends(
    context: Context,
    packageName: String,
    displayName: String,
    strings: AppStrings
) {
    try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        } else {
            val rootRes = RootExecutor.execute("monkey -p $packageName -c android.intent.category.LAUNCHER 1")
            if (!rootRes.success || rootRes.output.contains("No activities found") || rootRes.output.contains("error")) {
                Toast.makeText(context, strings.toastGameNotInstalled(displayName), Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, strings.toastGameNotInstalled(displayName), Toast.LENGTH_SHORT).show()
    }
}
