package pl.lapko.vyosmanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import pl.lapko.vyosmanager.VyOSConnection
import pl.lapko.vyosmanager.VyOSResults
import pl.lapko.vyosmanager.ui.displays.DisplayConfig
import pl.lapko.vyosmanager.ui.theme.VyOSManagerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController) {
    var currentVyOSResults : VyOSResults? by remember { mutableStateOf(null) }
    var showConfig by remember { mutableStateOf(false) }
    var configPath by remember { mutableStateOf("interfaces") }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedNavigationItem by remember { mutableStateOf("Interfaces") }
    val navigationItems = listOf("Interfaces", "Protocols", "Firewall", "NAT")
    LaunchedEffect(Unit) {
        VyOSConnection.getVyOSData(
            "\"$configPath\"",
            onSuccess = { vyOSResults ->
                currentVyOSResults = vyOSResults
                showConfig = true
            },
            onError = { /*TODO*/ }
        )
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Configuration sections", modifier = Modifier.padding(16.dp))
                HorizontalDivider()
                for (navigationItem in navigationItems) {
                    NavigationDrawerItem(
                        label = { Text(navigationItem) },
                        selected = navigationItem == selectedNavigationItem,
                        onClick = {
                            scope.launch {
                                drawerState.apply {
                                    if(isOpen) close()
                                }
                            }
                            showConfig = false
                            selectedNavigationItem = navigationItem
                            configPath = navigationItem.lowercase()
                            VyOSConnection.getVyOSData(
                                "\"$configPath\"",
                                onSuccess = { vyOSResults ->
                                    currentVyOSResults = vyOSResults
                                    showConfig = true
                                },
                                onError = { /*TODO*/ }
                            )
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    title = {
                        Text("Connected to " + VyOSConnection.getVyOSAddress())
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    drawerState.apply {
                                        if(isClosed) open() else close()
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Navigation menu"
                            )
                        }
                    },
                    scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { navController.navigate("ConfigScreen/$configPath") }
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                }
            }, content = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(it)
                ) {
                    Text(
                        text = "$configPath configuration".uppercase(),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    if (showConfig) {
                        DisplayConfig(currentVyOSResults!!, "\"$configPath\", ")
                    }
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview(){
    VyOSManagerTheme {
        MainScreen(rememberNavController())
    }
}