package pl.lapko.vyosmanager.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import pl.lapko.vyosmanager.ui.theme.VyOSManagerTheme

@Composable
fun ConfigScreen(navController: NavController, currentConfig: String){

}

@Preview(showBackground = true)
@Composable
fun ConfigScreenPreview(){
    VyOSManagerTheme {
        ConfigScreen(rememberNavController(), "interfaces")
    }
}