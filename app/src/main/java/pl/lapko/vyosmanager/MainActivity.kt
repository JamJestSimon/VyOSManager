package pl.lapko.vyosmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pl.lapko.vyosmanager.ui.screens.ConfigScreen
import pl.lapko.vyosmanager.ui.screens.LoginScreen
import pl.lapko.vyosmanager.ui.screens.MainScreen
import pl.lapko.vyosmanager.ui.theme.VyOSManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VyOSConnection.createRequestQueue(this)
        setContent {
            val navController = rememberNavController()
            VyOSManagerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(navController = navController, startDestination = "LoginScreen"){
                        composable("LoginScreen") { LoginScreen(navController) }
                        composable("MainScreen") { MainScreen(navController) }
                        composable(
                            "ConfigScreen/{param}",
                            arguments = listOf(navArgument("param") { type = NavType.StringType} )
                        ) {backStackEntry ->
                            val param = backStackEntry.arguments?.getString("param")
                            ConfigScreen(navController, param!!)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppPreview(){
    VyOSManagerTheme {
        MainScreen(rememberNavController())
    }
}