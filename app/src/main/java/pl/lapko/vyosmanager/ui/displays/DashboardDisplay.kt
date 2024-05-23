package pl.lapko.vyosmanager.ui.displays

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.text.font.FontFamily
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
    var displayDashboard by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        VyOSConnection.showVyOSData(
            "\"host\", \"name\"",
            onSuccess = {
                hostname = it.data!!.textValue().removeSuffix("\n")
                VyOSConnection.showVyOSData(
                    "\"interfaces\"",
                    onSuccess = {
                        interfaces = it.data!!.textValue()
                        interfaces = interfaces.replace("IP Address", "IP-Address")
                        displayDashboard = true
                    }, onError = {
                        onError(it)
                    }
                )
            }, onError = {
                onError(it)
            }
        )
    }
    if (displayDashboard) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "DASHBOARD",
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
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
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "INTERFACE STATUS",
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyColumn {
                itemsIndexed(interfaces.split("\n")) { index, value ->
                    if (value.matches(Regex("^Codes:.*"))) {
                        Text(
                            text = value,
                            fontSize = 10.sp
                        )
                    } else if (!value.matches(Regex("^(-+\\s*)+$"))) {
                        LazyRow {
                            itemsIndexed(Regex("\\S*\\s*").findAll(value).map { it.value }
                                .toList()) { index, value ->
                                Text(
                                    text = value,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.border(
                                        width = 1.dp,
                                        color = Color.White
                                    )
                                )
                            }
                        }
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