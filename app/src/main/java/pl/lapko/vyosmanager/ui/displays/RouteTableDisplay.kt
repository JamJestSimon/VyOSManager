package pl.lapko.vyosmanager.ui.displays

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import pl.lapko.vyosmanager.VyOSConnection

@Composable
fun RouteTableDisplay(onRequest : () -> Unit, onError : (Exception) -> Unit) {
    var routes by remember { mutableStateOf(listOf<String>()) }
    var displayRouteTable by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        onRequest()
        VyOSConnection.showVyOSData(
            "\"ip\", \"route\"",
            onSuccess = {
                routes = it.data!!.textValue().split("\n\n")
                displayRouteTable = true
            }, onError = {
                onError(it)
            }
        )
    }
    if (displayRouteTable) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.padding(10.dp))
            Text(
                text = "ROUTES",
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.padding(10.dp))
            LazyColumn(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                itemsIndexed(routes[1].removeSuffix("\n").split("\n")) { index, value ->
                    val tag = Regex("[^0-9]*\\s+").find(value)!!.value
                    Text(
                        text = value.removePrefix(tag).split(", ").first(),
                        modifier = Modifier.border(
                            width = 1.dp,
                            color = Color.DarkGray
                        ).fillMaxWidth().padding(10.dp)
                    )
                }
            }
        }
    }
}

