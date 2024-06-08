package pl.lapko.vyosmanager.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    val addedConfig = remember { mutableStateListOf<String>() }
    var showConfigs by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        formJson.value = loadJson(currentConfig, context)
    }
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Adding $currentConfig configuration", textAlign = TextAlign.Center) },
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
                },
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Request sent to server")
                            }
                            VyOSConnection.setVyOSData(addedConfig,
                                onSuccess = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Configuration added on server")
                                    }
                                    addedNewData = true
                                }, onError = {
                                    errorMessage = it.message.toString()
                                    showErrorMessage = true
                                })
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Set configurations"
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
                                returnConfigValue = {
                                    showConfigs = false
                                    addedConfig += it
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Configuration added to list")
                                    }
                                    showConfigs = true
                                })
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Configuration paths")
                Spacer(modifier = Modifier.height(10.dp))
                if(showConfigs) {
                    addedConfig.forEach {
                        LazyRow(verticalAlignment = Alignment.CenterVertically) {
                            item {
                                Text(text = it)
                                IconButton(
                                    onClick = {
                                        showConfigs = false
                                        addedConfig.remove(it)
                                        showConfigs = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete config path"
                                    )
                                }
                            }
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
 * @param returnConfigValue callback function to be recursively called
 * to return current configuration path
* */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateConfigElements(node: JsonNode,
                         rootPath: String,
                         selectReload: Boolean,
                         returnConfigValue: (String) -> Unit)
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
                returnConfigValue = {
                    returnConfigValue(it)
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
                    returnConfigValue = {
                        returnConfigValue(it)
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
                        returnConfigValue("$rootPath, \"$selectedOption\", \"$input\"")
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
                    returnConfigValue("$rootPath, \"$input\"")
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