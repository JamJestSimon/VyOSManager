package pl.lapko.vyosmanager.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import pl.lapko.vyosmanager.VyOSConnection
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(navController: NavController, currentConfig: String){
    val context = LocalContext.current
    val formJson = remember { mutableStateOf<JsonNode?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var addedNewData by remember { mutableStateOf(false) }
    var showErrorMessage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val returnCategory = remember { currentConfig.substring(0, 1).uppercase() + currentConfig.substring(1) }
    LaunchedEffect(Unit) {
        formJson.value = loadJson(currentConfig, context)
    }
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Adding $currentConfig configuration") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.navigate("MainScreen/$addedNewData/$returnCategory")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { contentPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            formJson.value?.let {
                LazyColumn(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CreateConfigElements(it, "\"$currentConfig\"", false,
                                onSuccess = {
                                    addedNewData = true
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Data added successfully")
                                    }
                                }, onError = {
                                    showErrorMessage = true
                                    errorMessage = it.message.toString()
                                })
                        }
                    }
                }
            }
            if(showErrorMessage){
                AlertDialog(
                    onDismissRequest = { showErrorMessage = false },
                    title = { Text("Error while executing operation") },
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

/**
 * @param node currently analyzed JsonNode
 * @param rootPath current configuration path
 * @param selectReload defines whether the current selection should be reloaded
 * @param onSuccess callback function to be recursively called on successful data addition
 * @param onError callback function to be recursively called on exception
* */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateConfigElements(node: JsonNode,
                         rootPath: String,
                         selectReload: Boolean,
                         onSuccess: () -> Unit,
                         onError: (Exception) -> Unit)
{
    var expanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf("") }
    var lastSelectedOption by remember { mutableStateOf("") }
    var reloadSelect by remember { mutableStateOf(false) }
    reloadSelect = selectReload
    if(reloadSelect){
        selectedOption = ""
        reloadSelect = false
    }
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
    if (options.size == 1 && options[0].matches(Regex("^<.*>$"))) {
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
                IconButton(onClick = {
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
            CreateConfigElements(
                node = node.get(options[0]),
                rootPath = "$rootPath, \"$input\"",
                selectReload = false,
                onSuccess = {
                    onSuccess()
                }, onError = {
                    onError(it)
                }
            )
        }
    } else {
        if ((node.isObject || node.isArray) && !node.isEmpty) {
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
                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    modifier = Modifier.menuAnchor()
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
            if (selectedOption != "" && node.isObject) {
                if(lastSelectedOption != selectedOption){
                    reloadSelect = true
                    lastSelectedOption = selectedOption
                }
                CreateConfigElements(
                    node = node.get(selectedOption),
                    rootPath = "$rootPath, \"$selectedOption\"",
                    selectReload = reloadSelect,
                    onSuccess = {
                        onSuccess()
                    }, onError = {
                        onError(it)
                    }
                )
                reloadSelect = false
            } else if (selectedOption != "" && node.isArray) {
                var input by remember { mutableStateOf("") }
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("value") }
                )
                IconButton(
                    onClick = {
                        VyOSConnection.setVyOSData("$rootPath, \"$selectedOption\", \"$input\"",
                            onSuccess = {
                                onSuccess()
                            }, onError = {
                                onError(it)
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
        } else {
            var input by remember { mutableStateOf("") }
            TextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("value") }
            )
            IconButton(
                onClick = {
                    VyOSConnection.setVyOSData("$rootPath, \"$input\"",
                        onSuccess = {
                            onSuccess()
                        }, onError = {
                            onError(it)
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

@Preview(showBackground = true)
@Composable
fun ConfigScreenPreview(){
    VyOSManagerTheme {
        ConfigScreen(rememberNavController(), "interfaces")
    }
}