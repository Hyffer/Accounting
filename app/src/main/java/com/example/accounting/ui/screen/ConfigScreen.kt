package com.example.accounting.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.accounting.ui.viewmodel.ConfigViewModel
import com.example.accounting.ui.viewmodel.SaveStatus

@Composable
fun ConfigScreen(
    modifier: Modifier = Modifier,
    viewModel: ConfigViewModel = viewModel()
) {
    val context = LocalContext.current
    val url by viewModel.url.collectAsState()
    val token by viewModel.token.collectAsState()
    val saveStatus by viewModel.saveStatus.collectAsState()

    LaunchedEffect(saveStatus) {
        when (saveStatus) {
            is SaveStatus.Success -> {
                Toast.makeText(context, "Configuration saved", Toast.LENGTH_SHORT).show()
                viewModel.resetSaveStatus()
            }

            is SaveStatus.Error -> {
                Toast.makeText(
                    context,
                    "Error: ${(saveStatus as SaveStatus.Error).message}",
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.resetSaveStatus()
            }

            SaveStatus.Idle -> {}
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Backend Configuration",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { viewModel.updateUrl(it) },
                    label = { Text("Server URL") },
                    placeholder = { Text("https://example.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = token,
                    onValueChange = { viewModel.updateToken(it) },
                    label = { Text("Auth Token") },
                    placeholder = { Text("Enter your token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.saveConfig() },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save Configuration")
                }
            }
        }
    }
}