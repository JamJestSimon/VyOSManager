package pl.lapko.vyosmanager.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.launch
import pl.lapko.vyosmanager.VyOSConnection
import pl.lapko.vyosmanager.data.VyOSDevices
import pl.lapko.vyosmanager.data.VyOSPassword
import pl.lapko.vyosmanager.ui.theme.VyOSManagerTheme
import java.io.File
import java.io.InputStreamReader

@Composable
fun LoginScreen(navController: NavController) {
    var address by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showErrorMessage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var savedConnections = remember { VyOSDevices(mutableMapOf()) }
    val context = LocalContext.current
    var saveConnectionData by remember { mutableStateOf(false) }
    val mapper = jacksonObjectMapper()
    var displaySavedConnections by remember { mutableStateOf(false) }
    displaySavedConnections = false
    if(!File(context.filesDir, "savedConnections.json").exists()){
        context.openFileOutput("savedConnections.json", Context.MODE_PRIVATE)
    } else if(File(context.filesDir, "savedConnections.json").length() > 0L) {
        context.openFileInput("savedConnections.json").use { inputStream ->
            InputStreamReader(inputStream).use { reader ->
                savedConnections = mapper.readValue<VyOSDevices>(reader)
            }
        }
        displaySavedConnections = true
    }
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ){
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(it),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Ip Address/Domain") }
            )
            Spacer(modifier = Modifier.height(20.dp))
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Save connection data")
                Spacer(modifier = Modifier.width(10.dp))
                Switch(checked = saveConnectionData, onCheckedChange = { saveConnectionData = !saveConnectionData })
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = {
                scope.launch {
                    snackbarHostState.showSnackbar("Connecting to router")
                }
                VyOSConnection.setupVyOSConnection(address, password)
                VyOSConnection.verifyVyOSConnection(
                    onSuccess = {
                        if(saveConnectionData){
                            if(!savedConnections.devices.containsKey(address)) {
                                displaySavedConnections = false
                                savedConnections.devices[address] = VyOSPassword(password)
                                val jsonString = mapper.writeValueAsString(savedConnections)
                                context.openFileOutput("savedConnections.json", Context.MODE_PRIVATE).use { file ->
                                    file.write(jsonString.toByteArray())
                                }
                                displaySavedConnections = true
                            }
                        }
                        navController.navigate("MainScreen/false/Dashboard")
                    },
                    onError = { exception ->
                        errorMessage = exception.message.toString()
                        showErrorMessage = true
                    }
                )
            }) {
                Text("Connect")
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("Saved connections:")
            Spacer(modifier = Modifier.height(20.dp))
            if(displaySavedConnections) {
                LazyColumn {
                    itemsIndexed(savedConnections.devices.entries.toList()) { index, entry ->
                        val (entryAddress, entryPassword) = entry
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.clickable {
                                address = entryAddress
                                password = entryPassword.password
                            }) {
                                Text("Address: $entryAddress")
                            }
                            IconButton(
                                onClick = {
                                    displaySavedConnections = false
                                    savedConnections.devices.remove(entryAddress)
                                    val jsonString = mapper.writeValueAsString(savedConnections)
                                    context.openFileOutput(
                                        "savedConnections.json",
                                        Context.MODE_PRIVATE
                                    ).use { file ->
                                        file.write(jsonString.toByteArray())
                                    }
                                    displaySavedConnections = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete"
                                )
                            }
                        }
                    }
                }
            }
            if (showErrorMessage) {
                AlertDialog(
                    onDismissRequest = { showErrorMessage = false },
                    title = { Text("Error while connecting to router") },
                    text = { Text(errorMessage) },
                    confirmButton = {
                        Button(onClick = { showErrorMessage = false }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview(){
    VyOSManagerTheme {
        LoginScreen(rememberNavController())
    }
}