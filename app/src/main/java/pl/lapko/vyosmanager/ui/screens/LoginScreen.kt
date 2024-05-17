package pl.lapko.vyosmanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import pl.lapko.vyosmanager.VyOSConnection
import pl.lapko.vyosmanager.ui.theme.VyOSManagerTheme

@Composable
fun LoginScreen(navController: NavController) {
    var ipAddress by remember { mutableStateOf("192.168.0.50") }
    var password by remember { mutableStateOf("vyos") }
    var showErrorMessage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center){
        TextField(
            value = ipAddress,
            onValueChange = { ipAddress = it },
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
        Button(onClick = {
            VyOSConnection.setupVyOSConnection(ipAddress, password)
            VyOSConnection.verifyVyOSConnection(
                onSuccess = { connectionResponse ->
                    if(connectionResponse != null){
                        if(connectionResponse.success) {
                            navController.navigate("MainScreen")
                        } else {
                            errorMessage = if(connectionResponse.error != null){
                                connectionResponse.error!!
                            } else {
                                "Unknown error"
                            }
                            showErrorMessage = true
                        }
                    } else {
                        errorMessage = "Unknown error"
                        showErrorMessage = true
                    }
                },
                onError = { exception ->
                    errorMessage = exception.message.toString()
                    showErrorMessage = true
                }
            )
        }) {
            Text("Connect")
        }
        if(showErrorMessage){
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

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview(){
    VyOSManagerTheme {
        LoginScreen(rememberNavController())
    }
}