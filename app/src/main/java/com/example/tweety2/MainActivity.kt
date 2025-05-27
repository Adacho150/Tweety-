package com.example.tweety2

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.twojpakiet.app.ui.theme.TwojaAplikacjaTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.MoreVert


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TwojaAplikacjaTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // Przekażemy navController do NavHost
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "main"
                    ) {
                        composable("main") {
                            MainScreen(navController)
                        }
                        composable("reports") {
                            ReportsScreen(navController)
                        }
                    }
                }
            }
        }
    }
}

// Teraz MainScreen przyjmuje navController
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController) {
    val showDialog = remember { mutableStateOf(false) }

    // Przykładowa lista przystanków tramwajowych
    val tramStops = listOf(
        "Rondo Kaponiera",
        "Most Teatralny",
        "Dworzec Zachodni",
        "Zawady",
        "Górczyn",
        "Os. Lecha",
        "Pestka - Al. Solidarności"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Użytkownik",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("NazwaUżytkownika")
                    }
                },
                actions = {
                    IconButton(onClick = { /* menu użytkownika */ }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opcje użytkownika"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { navController.navigate("reports") },
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                ) {
                    Text("Zgłoszenia")
                }
                Button(
                    onClick = { /* mapa */ },
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                ) {
                    Text("Mapa")
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog.value = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj", tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            MapScreen(modifier = Modifier.fillMaxSize())
        }

        if (showDialog.value) {
            AlertDialog(
                onDismissRequest = { showDialog.value = false },
                confirmButton = {
                    TextButton(onClick = { showDialog.value = false }) {
                        Text("Zamknij")
                    }
                },
                title = { Text("Przystanki tramwajowe w Poznaniu") },
                text = {
                    Column {
                        tramStops.forEach {
                            Text("• $it", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista zgłoszeń") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            repeat(5) { i ->
                Text(text = "Zgłoszenie #$i", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier,
        factory = {
            MapView(context).apply {
                setMultiTouchControls(true)
                controller.setZoom(12.0)
                controller.setCenter(GeoPoint(52.4064, 16.9252)) // Poznań

                // Po utworzeniu MapView pobierz przystanki i dodaj pineski
                fetchBusStops(context, this)
            }
        }
    )
}