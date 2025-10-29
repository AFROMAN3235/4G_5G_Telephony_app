package com.example.cellinfo

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cellinfo.ui.MainViewModel
import com.example.cellinfo.ui.theme.CellinfoTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {

            Toast.makeText(this, "Разрешения получены!", Toast.LENGTH_SHORT).show()
        } else {

            Toast.makeText(this, "Нужны разрешения для работы приложения", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        requestPermissions()

        setContent {
            CellinfoTheme {
                val viewModel: MainViewModel = viewModel()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding),
                        onRequestPermissions = { requestPermissions() }
                    )
                }
            }
        }
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.READ_PHONE_STATE
        )
        requestPermissionLauncher.launch(permissions)
    }

}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onRequestPermissions: () -> Unit
) {

    val networkState = remember { viewModel.networkState }
    val cellInfo = remember { viewModel.cellCount }
    val isLoading = remember { viewModel.isLoading }
    val tcpConnected = remember { viewModel.isConnected }
    val error = remember { viewModel.lastError }

    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "Network Info",
            style = MaterialTheme.typography.headlineMedium
        )


        error?.let {
            Text(
                text = "Ошибка: $it",
                color = MaterialTheme.colorScheme.error
            )
        }


        Text(
            text = "TCP: ${if (tcpConnected) "Connected" else "Disconnected"}",
            color = if (tcpConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )


        Text("Operator: $networkState")
        Text("Cells found: $cellInfo")


        Button(
            onClick = { viewModel.refreshData() },
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Loading..." else "Refresh")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.sendToServer() },
            enabled = tcpConnected && cellInfo > 0
        ) {
            Text("Send to Server")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onRequestPermissions) {
            Text("Request Permissions")
        }
    }
}