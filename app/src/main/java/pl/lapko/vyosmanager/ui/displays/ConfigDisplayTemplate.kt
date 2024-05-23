package pl.lapko.vyosmanager.ui.displays

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fasterxml.jackson.databind.JsonNode
import pl.lapko.vyosmanager.VyOSConnection
import pl.lapko.vyosmanager.data.VyOSResults

@Composable
fun DisplayConfig(results : VyOSResults, rootPath: String, onRequest: () -> Unit, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
    val createdElements = createListElements(results.data!!, rootPath,
        onRequest = {
            onRequest()
        }, onSuccess = {
            onSuccess()
        }, onError = {
            onError(it)
        })
    val listItems = remember { createdElements }
    LazyColumn(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        items(items = listItems) { listItem ->
            listItem()
        }
    }
}

@Composable
fun createListElements(root: JsonNode, rootPath: String, onRequest: () -> Unit, onSuccess: () -> Unit, onError: (Exception) -> Unit): List<@Composable () -> Unit> {
    val composables = mutableListOf<@Composable () -> Unit>()

    @Composable
    fun createListItems(node: JsonNode, parentNode: JsonNode, indentation : Int, indentationChange : Int, currentPath: String, onRequest: () -> Unit, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        if (node.isObject) {
            val fieldNames = node.fieldNames()
            while (fieldNames.hasNext()) {
                val fieldName = fieldNames.next()
                val fieldValue = node.get(fieldName)
                var fieldValueText = ""
                if(!fieldValue.isObject && !fieldValue.isArray) fieldValueText = fieldValue.textValue()
                composables.add {
                    ListItemTemplate(node = node, fieldName = fieldName, value = fieldValueText, indentation = indentation, currentPath = currentPath,
                        onRequest = {
                            onRequest()
                        }, onSuccess = {
                            onSuccess()
                        }, onError = {
                            onError(it)
                        })
                }
                if(fieldValue.isObject || fieldValue.isArray){
                    createListItems(
                        node = fieldValue,
                        parentNode = node,
                        indentation = indentation + indentationChange,
                        indentationChange = indentationChange,
                        currentPath = "$currentPath\"$fieldName\", ",
                        onRequest = {
                            onRequest()
                        }, onSuccess = {
                            onSuccess()
                        }, onError = {
                            onError(it)
                        })
                }
            }
        } else if (node.isArray) {
            node.forEach { item ->
                val fieldName = parentNode.fieldNames().next()
                val fieldValue = item.textValue()
                composables.add {
                    ListItemTemplate(node = node, fieldName = fieldName, value = fieldValue, indentation = indentation, currentPath = currentPath,
                        onRequest = {
                            onRequest()
                        },
                        onSuccess = {
                            onSuccess()
                        }, onError = {
                            onError(it)
                        })
                }
            }
        }
    }
    createListItems(root, root, 0, 20, rootPath,
        onRequest = {
            onRequest()
        }, onSuccess = {
            onSuccess()
        }, onError = {
            onError(it)
        })
    return composables
}

@Composable
fun ListItemTemplate(node: JsonNode, fieldName: String, value: String, indentation: Int, currentPath: String, onRequest: () -> Unit, onSuccess: () -> Unit, onError: (Exception) -> Unit){
    var isEditable = false
    var fieldValue = value
    var isInEditMode by remember { mutableStateOf(false) }
    var editModeValue by remember { mutableStateOf(fieldValue) }
    var path = currentPath
    if(!node.isArray && !node.get(fieldName).isObject && !node.get(fieldName).isArray){ path = "$currentPath\"$fieldName\", " }
    if((node.isObject && !node.get(fieldName).isObject && !node.get(fieldName).isArray) || node.isArray || fieldName.matches(Regex("(^([0-9]{1,3}\\.){3}([0-9]{1,3})\$)|(^[0-9]{1,6}\$)"))) {
        isEditable = true
        if(!((node.isObject && !node.get(fieldName).isObject && !node.get(fieldName).isArray) || node.isArray)){
            editModeValue = fieldName
            fieldValue = fieldName
        }
    }
    ListItem(
        modifier = Modifier
            .border(width = 1.dp, color = Color.DarkGray),
        headlineContent = {
            LazyRow (
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    if(isEditable) {
                        if(!node.isArray && !node.get(fieldName).isObject) {
                            Text(
                                text = "$fieldName: ",
                                modifier = Modifier.padding(start = indentation.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.padding(start = indentation.dp))
                        }
                        if (isInEditMode) {
                            TextField(
                                value = editModeValue,
                                onValueChange = { editModeValue = it })
                            Spacer(modifier = Modifier.padding(20.dp))

                            //Confirm button
                            IconButton(
                                onClick = {
                                    editNodeKey(node, path, fieldValue, editModeValue,
                                        onRequest = {
                                            onRequest()
                                        }, onSuccess = {
                                            onSuccess()
                                        }, onError = {
                                            onError(it)
                                        })
                                    isInEditMode = false
                                },
                                modifier = Modifier.background(color = Color.Transparent)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Confirm edit",
                                    tint = Color.White,
                                    modifier = Modifier.background(color = Color.Transparent)
                                )
                            }

                            //Cancel button
                            IconButton(
                                onClick = {
                                    isInEditMode = false
                                },
                                modifier = Modifier.background(color = Color.Transparent)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel edit",
                                    tint = Color.White,
                                    modifier = Modifier.background(color = Color.Transparent)
                                )
                            }
                        } else {
                            Text(
                                text = fieldValue
                            )
                            Spacer(modifier = Modifier.padding(20.dp))

                            //Edit button
                            IconButton(
                                onClick = { isInEditMode = true },
                                modifier = Modifier.background(color = Color.Transparent)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit record",
                                    tint = Color.White,
                                    modifier = Modifier.background(color = Color.Transparent)
                                )
                            }

                            //Delete button
                            IconButton(
                                onClick = {
                                    onRequest()
                                    VyOSConnection.deleteVyOSData("$path\"$fieldValue\"",
                                        onSuccess = {
                                            onSuccess()
                                        },
                                        onError = {
                                            onError(it)
                                        }
                                    )
                                },
                                modifier = Modifier.background(color = Color.Transparent)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete record",
                                    tint = Color.White,
                                    modifier = Modifier.background(color = Color.Transparent)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = fieldName,
                            modifier = Modifier.padding(start = indentation.dp)
                        )
                    }
                }
            }
        }
    )
}

fun editNodeKey(node: JsonNode, rootPath: String, oldKey: String, newKey: String, onRequest: () -> Unit, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
    val pathsToEdit = mutableListOf<String>()

    fun addNewNode(node: JsonNode, rootPath: String) {
        if (node.isObject) {
            val fieldNames = node.fieldNames()
            while (fieldNames.hasNext()) {
                val fieldName = fieldNames.next()
                val fieldValue = node.get(fieldName)
                if (fieldValue.isObject || fieldValue.isArray) {
                    addNewNode(fieldValue, "$rootPath\"$fieldName\", ")
                } else {
                    val value = fieldValue.textValue()
                    pathsToEdit.add("$rootPath\"$fieldName\", \"$value\"")
                }
            }
        } else if (node.isArray) {
            node.forEach { item ->
                val fieldValue = item.textValue()
                pathsToEdit.add("$rootPath\"$fieldValue\"")
            }
        }
    }

    if (node.isObject) {
        val fieldNames = node.fieldNames()
        while (fieldNames.hasNext()) {
            val fieldName = fieldNames.next()
            val fieldValue = node.get(fieldName)
            if (fieldValue.isObject || fieldValue.isArray) {
                addNewNode(fieldValue, "$rootPath\"$newKey\", ")
            } else {
                pathsToEdit.add("$rootPath\"$newKey\"")
            }
        }
    } else {
        pathsToEdit.add("$rootPath\"$newKey\"")
    }
    onRequest()
    VyOSConnection.setMultipleVyOSData(pathsToEdit,
        onSuccess = {
            VyOSConnection.deleteVyOSData("$rootPath\"$oldKey\"",
                onSuccess = {
                    onSuccess()
                }, onError = {
                    onError(it)
                })
        }, onError = {
            onError(it)
        })
}

