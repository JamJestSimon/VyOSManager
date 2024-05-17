package pl.lapko.vyosmanager.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import pl.lapko.vyosmanager.VyOSConnection
import pl.lapko.vyosmanager.ui.displays.createListElements
import pl.lapko.vyosmanager.ui.theme.VyOSManagerTheme
import java.io.InputStreamReader

fun loadJson(filename: String, context: Context): JsonNode? {
    try {
        context.assets.open("ConfigFormJsons/$filename.json").use { inputStream ->
            InputStreamReader(inputStream).use {reader ->
                val mapper = jacksonObjectMapper()
                return mapper.readValue<JsonNode>(reader)
            }
        }
    } catch (e: Exception){
        return null
    }
}

@Composable
fun ConfigScreen(navController: NavController, currentConfig: String){
    val context = LocalContext.current
    val formJson = remember { mutableStateOf<JsonNode?>(null) }
    LaunchedEffect(Unit) {
        formJson.value = loadJson(currentConfig, context)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        formJson.value?.let {
            Text("Adding $currentConfig configuration")
            val createdElements = createConfigElements(it, "\"$currentConfig\", ")
            val listItems = remember { createdElements }
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                items(items = listItems) { listItem ->
                    listItem()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun createConfigElements(node: JsonNode, rootPath: String): List<@Composable () -> Unit> {
    val composables = mutableListOf<@Composable () -> Unit>()

    @Composable
    fun createConfigItems(node: JsonNode, rootPath: String){
        composables.add {
            var expanded by remember { mutableStateOf(false) }
            var selectedOption by remember { mutableStateOf("") }
            val options = mutableListOf<String>()
            if (node.isObject) {
                val fieldNames = node.fieldNames()
                fieldNames.forEach { name ->
                    options.add(name)
                }
            } else if (node.isArray) {
                node.forEach { field ->
                    options.add(field.textValue())
                }
            }
            if (options.size == 1 && options[0].matches(Regex("^<.*>\$"))) {
                var input by remember { mutableStateOf("") }
                var showConfirmButton by remember { mutableStateOf(true) }
                var createNextOption by remember { mutableStateOf(false) }
                Row {
                    TextField(
                        value = input,
                        onValueChange = { input = it },
                        label = { Text(options[0]) }
                    )
                    if (showConfirmButton) {
                        Button(onClick = {
                            showConfirmButton = false
                            createNextOption = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Confirm",
                                tint = Color.White,
                                modifier = Modifier.background(color = Color.Transparent)
                            )
                        }
                    }
                }
                if (createNextOption) {
                    createConfigItems(
                        node = node.get(options[0]),
                        rootPath = "$rootPath\"$input\", "
                    )
                }
            } else {
                if (node.isObject || node.isArray) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }) {
                        TextField(
                            readOnly = true,
                            value = selectedOption,
                            onValueChange = {},
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            colors = ExposedDropdownMenuDefaults.textFieldColors()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = {
                            expanded = false
                        }) {
                            options.forEach { selectionOption ->
                                DropdownMenuItem(
                                    onClick = {
                                        selectedOption = selectionOption
                                        expanded = false
                                    },
                                    text = {
                                        Text(text = selectionOption)
                                    }
                                )
                            }
                        }
                    }
                    if (selectedOption != "") {
                        createConfigItems(
                            node = node.get(selectedOption),
                            rootPath = "$rootPath\"$selectedOption\""
                        )
                    }
                } else {
                    var input by remember { mutableStateOf("") }
                    TextField(
                        value = input,
                        onValueChange = { input = it },
                        label = { Text("value") }
                    )
                    Button(
                        onClick = {
                            VyOSConnection.setVyOSData("$rootPath\"$input\"",
                                onSuccess = {

                                }, onError = {

                                })
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirm",
                            tint = Color.White,
                            modifier = Modifier.background(color = Color.Transparent)
                        )
                    }
                }
            }
        }
    }

    createConfigItems(node, rootPath)
    return composables
}

@Preview(showBackground = true)
@Composable
fun ConfigScreenPreview(){
    VyOSManagerTheme {
        ConfigScreen(rememberNavController(), "interfaces")
    }
}