package com.warungerik.devidchanger.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warungerik.devidchanger.MainViewModel
import com.warungerik.devidchanger.model.AppTab
import com.warungerik.devidchanger.ui.components.SaveIdDialog
import com.warungerik.devidchanger.ui.components.SavedDevIdCard
import com.warungerik.devidchanger.ui.theme.AppStrings

@Composable
fun ConfigScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val strings = AppStrings(uiState.currentLanguage)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadSavedDevIds(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = strings.configScreenTitle,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.configScreenTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = strings.configScreenSubtitle(uiState.savedDevIds.size),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = strings.btnAddManualDevId,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = strings.btnAddManualDevId,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SavedDevIdCard(
            savedIds = uiState.savedDevIds,
            strings = strings,
            onUse = { item ->
                viewModel.applySavedDevId(item.devId)
                Toast.makeText(context, strings.toastAppliedAndNavigated, Toast.LENGTH_SHORT).show()
                viewModel.selectTab(AppTab.HOME)
            },
            onCopy = { item ->
                clipboardManager.setText(AnnotatedString(item.devId))
                Toast.makeText(context, strings.toastCopySuccess, Toast.LENGTH_SHORT).show()
            },
            onDelete = { item ->
                viewModel.deleteSavedDevId(context, item.id)
                Toast.makeText(context, strings.toastDeleteSuccess, Toast.LENGTH_SHORT).show()
            }
        )
    }


    if (showAddDialog) {
        val clipText = clipboardManager.getText()?.text ?: ""
        val initialId = clipText.ifBlank { uiState.newJsonId.ifBlank { uiState.currentJsonId ?: "" } }
        SaveIdDialog(
            devIdToSave = initialId,
            strings = strings,
            onSave = { label ->
                if (initialId.isNotBlank()) {
                    viewModel.saveDevId(context, label, initialId)
                    Toast.makeText(context, strings.toastSaveSuccess, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, strings.toastPasteEmpty, Toast.LENGTH_SHORT).show()
                }
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}
