package pl.lapko.vyosmanager.ui.displays

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.lapko.vyosmanager.VyOSConnection
import pl.lapko.vyosmanager.ui.theme.VyOSManagerTheme

@Composable
fun DashboardDisplay(onRequest : () -> Unit, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
    var hostname by remember { mutableStateOf("") }
    var hostnameEditMode by remember { mutableStateOf(false) }
    var interfaces by remember { mutableStateOf("") }
    var memory by remember { mutableStateOf("") }
    var uptime by remember { mutableStateOf("") }
    var cpu by remember { mutableStateOf("") }
    var displayDashboard by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        VyOSConnection.showVyOSData(
            "\"host\", \"name\"",
            onSuccess = {
                hostname = it.data!!.textValue().removeSuffix("\n")
            }, onError = {
                onError(it)
            }
        )
        VyOSConnection.showVyOSData(
            "\"interfaces\", \"summary\"",
            onSuccess = {
                interfaces = it.data!!.textValue().removeSuffix("\n")
                interfaces = interfaces.replace("IP Address", "IP-Address")
            }, onError = {
                onError(it)
            }
        )
        VyOSConnection.showVyOSData(
            "\"system\", \"memory\"",
            onSuccess = {
                memory = it.data!!.textValue().removeSuffix("\n")
            }, onError = {
                onError(it)
            }
        )
        VyOSConnection.showVyOSData(
            "\"system\", \"uptime\"",
            onSuccess = {
                uptime = it.data!!.textValue().removeSuffix("\n")
            }, onError = {
                onError(it)
            }
        )
        VyOSConnection.showVyOSData(
            "\"system\", \"cpu\"",
            onSuccess = {
                cpu = it.data!!.textValue().removeSuffix("\n")
                displayDashboard = true
            }, onError = {
                onError(it)
            }
        )
    }
    if (displayDashboard) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "DASHBOARD",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Hostname: ")
                TextField(
                    value = hostname,
                    onValueChange = { hostname = it },
                    readOnly = !hostnameEditMode,
                    trailingIcon = {
                        if (hostnameEditMode) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Set",
                                modifier = Modifier.clickable {
                                    hostnameEditMode = false
                                    onRequest()
                                    VyOSConnection.setVyOSData("\"system\", \"host-name\", \"$hostname\"",
                                        onSuccess = {
                                            onSuccess()
                                        }, onError = {
                                            onError(it)
                                        }
                                    )
                                }
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.clickable {
                                    hostnameEditMode = true
                                }
                            )
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "INTERFACE STATUS",
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))
            LazyColumn {
                itemsIndexed(interfaces.split("\n")) { index, value ->
                    var content by remember { mutableStateOf(listOf<String>()) }
                    var displayDetails by remember { mutableStateOf(false) }
                    var addresses by remember { mutableStateOf(arrayOf<String>()) }
                    if (index > 2 && !value.first().isWhitespace()) {
                        content = value.split(Regex("\\s+"))
                        TextField(
                            readOnly = true,
                            value = content[0],
                            onValueChange = {},
                            trailingIcon = {
                                if (content[2].contains('d')) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Down",
                                        modifier = Modifier.background(color = Color.Red)
                                    )
                                } else Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Up",
                                    modifier = Modifier.background(color = Color.Green)
                                )
                            },
                            enabled = false,
                            modifier = Modifier.clickable {
                                onRequest()
                                VyOSConnection.getVyOSData("\"interfaces\"",
                                    onSuccess = {
                                        addresses = emptyArray()
                                        if (it.data!!.findValue(content[0]).get("address") != null) {
                                            if (it.data!!.findValue(content[0]).get("address").isArray) {
                                                it.data!!.findValue(content[0]).get("address").forEach { address ->
                                                    addresses += address.textValue()
                                                }
                                            } else {
                                                addresses += it.data!!.findValue(content[0]).get("address").textValue()
                                            }
                                        }
                                        displayDetails = true
                                    }, onError = {
                                        onError(it)
                                    })

                            }
                        )
                        if(displayDetails){
                            AlertDialog(
                                onDismissRequest = { displayDetails = false },
                                confirmButton = {
                                    Button(onClick = { displayDetails = false }) {
                                        Text("Close")
                                    }
                                },
                                title = { Text(text = "Interface addresses") },
                                text = {
                                    Column {
                                        if(addresses.isEmpty()){
                                            Text("No addresses assigned")
                                        }
                                        for (address in addresses) {
                                            Text(text = address)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "MEMORY", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = memory)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "UPTIME", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = uptime.split("\n").first())
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "CPU", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                cpu.split("\n").forEachIndexed { index, s ->
                    if(index > 1){
                        Text(text = s.replace(Regex("\\s+"), " "))
                    }
                }
            }
        }
    }
}


@Preview
@Composable
fun DashboardDisplayPreview(){
    VyOSManagerTheme {
        DashboardDisplay({},{},{})
    }
}