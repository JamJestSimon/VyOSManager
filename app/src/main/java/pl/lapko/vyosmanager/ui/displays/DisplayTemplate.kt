package pl.lapko.vyosmanager.ui.displays

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import pl.lapko.vyosmanager.VyOSResults

@Composable
fun DisplayConfig(results : VyOSResults, rootPath: String) {
    val createdElements = createListElements(results.data!!, rootPath)
    val listItems = remember { createdElements }
    LazyColumn(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        items(items = listItems) { listItem ->
            listItem()
        }
    }
}

@Composable
fun createListElements(root: JsonNode, rootPath: String): List<@Composable () -> Unit> {
    val composables = mutableListOf<@Composable () -> Unit>()

    @Composable
    fun createListItems(node: JsonNode, parentNode: JsonNode, indentation : Int, indentationChange : Int, currentPath: String) {
        if (node.isObject) {
            val fieldNames = node.fieldNames()
            while (fieldNames.hasNext()) {
                val fieldName = fieldNames.next()
                val fieldValue = node.get(fieldName)
                var fieldValueText = ""
                if(!fieldValue.isObject && !fieldValue.isArray) fieldValueText = fieldValue.textValue()
                composables.add {
                    ListItemTemplate(node = node, fieldName = fieldName, value = fieldValueText, indentation = indentation, currentPath = currentPath)
                }
                if(fieldValue.isObject || fieldValue.isArray){
                    createListItems(
                        node = fieldValue,
                        parentNode = node,
                        indentation = indentation + indentationChange,
                        indentationChange = indentationChange,
                        currentPath = "$currentPath\"$fieldName\", "
                    )
                }
            }
        } else if (node.isArray) {
            node.forEach { item ->
                val fieldName = parentNode.fieldNames().next()
                val fieldValue = item.textValue()
                composables.add {
                    ListItemTemplate(node = node, fieldName = fieldName, value = fieldValue, indentation = indentation, currentPath = currentPath)
                }
            }
        }
    }
    createListItems(root, root, 0, 20, rootPath)
    return composables
}

@Composable
fun ListItemTemplate(node: JsonNode, fieldName: String, value: String, indentation: Int, currentPath: String){
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
                            Button(
                                onClick = {
                                    editNodeKey(node, path, fieldValue, editModeValue)
                                    isInEditMode = false
                                },
                                shape = CircleShape,
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
                            Button(
                                onClick = {
                                    isInEditMode = false
                                },
                                shape = CircleShape,
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
                            Button(
                                onClick = { isInEditMode = true },
                                shape = CircleShape,
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
                            Button(
                                onClick = {
                                    VyOSConnection.deleteVyOSData("$path\"$fieldValue\"",
                                        onSuccess = {
                                            /*TODO*/
                                        },
                                        onError = {
                                            /*TODO*/
                                        }
                                    )
                                },
                                shape = CircleShape,
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

fun editNodeKey(node: JsonNode, rootPath: String, oldKey: String, newKey: String) {

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
                    VyOSConnection.setVyOSData("$rootPath\"$fieldName\", \"$value\"",
                        onSuccess = {
                            /*TODO*/
                        },
                        onError = {
                            /*TODO*/
                        })
                }
            }
        } else if (node.isArray) {
            node.forEach { item ->
                val fieldValue = item.textValue()
                VyOSConnection.setVyOSData("$rootPath\"$fieldValue\"",
                    onSuccess = {
                        /*TODO*/
                    },
                    onError = {
                        /*TODO*/
                    })
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
                //To do in callback of addNewNode
                VyOSConnection.deleteVyOSData("$rootPath\"$oldKey\"",
                    onSuccess = {
                        /*TODO*/
                    },
                    onError = {
                        /*TODO*/
                    })
            } else {
                VyOSConnection.setVyOSData("$rootPath\"$newKey\"",
                    onSuccess = {
                        /*TODO*/
                    },
                    onError = {
                        /*TODO*/
                    })
                VyOSConnection.deleteVyOSData("$rootPath\"$oldKey\"",
                    onSuccess = {
                        /*TODO*/
                    },
                    onError = {
                        /*TODO*/
                    })
            }
        }
    } else if (node.isArray) {
        node.forEach { item ->
            val fieldValue = item.textValue()
            VyOSConnection.setVyOSData("$rootPath\"$newKey\", \"$fieldValue\"",
                onSuccess = {
                    /*TODO*/
                },
                onError = {
                    /*TODO*/
                })
            VyOSConnection.deleteVyOSData("$rootPath\"$oldKey\"",
                onSuccess = {
                    /*TODO*/
                },
                onError = {
                    /*TODO*/
                })
        }
    }
}

