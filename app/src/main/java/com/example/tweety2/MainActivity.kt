package com.example.tweety2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.twojpakiet.app.ui.theme.TwojaAplikacjaTheme
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TwojaAplikacjaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavigationComponent()
                }
            }
        }
    }
}

@Composable
fun NavigationComponent() {
    val navController = rememberNavController()
    val viewModel: TransportViewModel = viewModel()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginScreen(navController, viewModel) }
        composable("register") { RegisterScreen(navController, viewModel) }
        composable("map") { MapScreen(navController, viewModel) }
        composable("tramStops") { TramStopsScreen(navController, viewModel) }
        composable("reportForm/{stopId}/{stopName}") { backStackEntry ->
            val stopId = backStackEntry.arguments?.getString("stopId") ?: ""
            val stopName = backStackEntry.arguments?.getString("stopName") ?: ""
            ReportFormScreen(stopId, stopName, navController, viewModel)
        }
        composable("reports") { ReportsScreen(navController, viewModel) }
    }
}

class TransportViewModel : ViewModel() {
    private val _stops = mutableStateListOf<TramStop>()
    val stops: List<TramStop> get() = _stops

    private val _reports = mutableStateListOf<Report>()
    val reports: List<Report> get() = _reports

    private var _selectedStop by mutableStateOf<TramStop?>(null)
    val selectedStop: TramStop? get() = _selectedStop

    private var _currentUser by mutableStateOf<User?>(null)
    val currentUser: User? get() = _currentUser

    private val _registeredUsers = mutableStateListOf<User>()
    val registeredUsers: List<User> get() = _registeredUsers

    init {
        loadSampleStops()
    }

    private fun loadSampleStops() {
        _stops.clear()
        _stops.addAll(
            listOf(
                TramStop("1", "Rondo Kaponiera", 52.4064, 16.9252),
                TramStop("2", "Most Teatralny", 52.4081, 16.9325),
                TramStop("3", "Dworzec Zachodni", 52.4025, 16.9138),
                TramStop("4", "Zawady", 52.4123, 16.9456),
                TramStop("5", "Górczyn", 52.3987, 16.9012),
                TramStop("6", "Półwiejska", 52.4095, 16.9231),
                TramStop("7", "Plac Wielkopolski", 52.4078, 16.9194),
                TramStop("8", "Garbary", 52.4042, 16.9357),
                TramStop("9", "Rataje", 52.3976, 16.9532),
                TramStop("10", "Staroleka", 52.4167, 16.9389)
            )
        )
    }

    fun selectStop(stop: TramStop) {
        _selectedStop = stop
    }

    fun clearSelectedStop() {
        _selectedStop = null
    }

    fun addReport(report: Report) {
        _reports.add(report.copy(id = UUID.randomUUID().toString()))
    }

    fun login(username: String, password: String): Boolean {
        val user = _registeredUsers.find { it.username == username && it.password == password }
        _currentUser = user
        return user != null
    }

    fun register(email: String, username: String, password: String): Boolean {
        if (_registeredUsers.any { it.username == username }) {
            return false
        }
        _registeredUsers.add(User(email, username, password))
        return true
    }

    fun logout() {
        _currentUser = null
    }
}

data class User(
    val email: String,
    val username: String,
    val password: String
)

data class TramStop(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
)

data class Report(
    val id: String,
    val stopId: String,
    val stopName: String,
    val type: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController, viewModel: TransportViewModel) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Logowanie") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showError) {
                Text(
                    text = "Nieprawidłowe dane logowania",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Nazwa użytkownika") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Hasło") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (viewModel.login(username, password)) {
                        navController.navigate("map")
                    } else {
                        showError = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Zaloguj")
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = { navController.navigate("register") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Nie masz konta? Zarejestruj się")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController, viewModel: TransportViewModel) {
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rejestracja") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Wróć")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showError) {
                Text(
                    text = "Nazwa użytkownika jest już zajęta",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (showSuccess) {
                Text(
                    text = "Rejestracja udana! Możesz się zalogować",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Nazwa użytkownika") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Hasło") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (viewModel.register(email, username, password)) {
                        showSuccess = true
                        showError = false
                    } else {
                        showError = true
                        showSuccess = false
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Zarejestruj się")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavController, viewModel: TransportViewModel) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(52.4064, 16.9252))
        }
    }

    LaunchedEffect(Unit) {
        mapView.overlays.clear()
        viewModel.stops.forEach { stop ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(stop.latitude, stop.longitude)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = stop.name
                setOnMarkerClickListener { _, _ ->
                    viewModel.selectStop(stop)
                    true
                }
            }
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }

    viewModel.selectedStop?.let { stop ->
        AlertDialog(
            onDismissRequest = { viewModel.clearSelectedStop() },
            title = { Text(stop.name) },
            text = {
                Column {
                    Text("ID: ${stop.id}")
                    Text("Lokalizacja: ${stop.latitude}, ${stop.longitude}")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        navController.navigate("reportForm/${stop.id}/${stop.name}")
                        viewModel.clearSelectedStop()
                    }
                ) {
                    Text("Dodaj zgłoszenie")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.clearSelectedStop() }
                ) {
                    Text("Anuluj")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapa Poznania") },
                actions = {
                    if (viewModel.currentUser != null) {
                        Text(viewModel.currentUser?.username ?: "", modifier = Modifier.padding(end = 8.dp))
                    } else {
                        TextButton(onClick = { navController.navigate("login") }) {
                            Text("Zaloguj")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("tramStops") },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.Add, "Lista przystanków")
            }
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = { navController.navigate("reports") }) {
                        Text("Zgłoszenia")
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            AndroidView(factory = { mapView })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TramStopsScreen(navController: NavController, viewModel: TransportViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wybierz przystanek") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Wróć")
                    }
                },
                actions = {
                    if (viewModel.currentUser != null) {
                        Text(viewModel.currentUser?.username ?: "", modifier = Modifier.padding(end = 8.dp))
                    } else {
                        TextButton(onClick = { navController.navigate("login") }) {
                            Text("Zaloguj")
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = { navController.navigate("reports") }) {
                        Text("Zgłoszenia")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(viewModel.stops) { stop ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    onClick = {
                        navController.navigate("reportForm/${stop.id}/${stop.name}")
                    }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stop.name, style = MaterialTheme.typography.titleMedium)
                        Text("ID: ${stop.id}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportFormScreen(
    stopId: String,
    stopName: String,
    navController: NavController,
    viewModel: TransportViewModel
) {
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var description by remember { mutableStateOf("") }
    val options = listOf("Awaria", "Wypadek", "Kontrola biletów")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zgłoszenie dla: $stopName") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Wróć")
                    }
                },
                actions = {
                    if (viewModel.currentUser != null) {
                        Text(viewModel.currentUser?.username ?: "", modifier = Modifier.padding(end = 8.dp))
                    } else {
                        TextButton(onClick = { navController.navigate("login") }) {
                            Text("Zaloguj")
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = { navController.navigate("reports") }) {
                        Text("Zgłoszenia")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Wybierz typ zgłoszenia:", style = MaterialTheme.typography.titleMedium)

            options.forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedOption = option }
                        .padding(8.dp)
                ) {
                    RadioButton(
                        selected = (selectedOption == option),
                        onClick = { selectedOption = option }
                    )
                    Text(
                        text = option,
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Opis sytuacji:", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = { Text("Opisz szczegóły...") },
                maxLines = 5
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (selectedOption != null && description.isNotBlank()) {
                        viewModel.addReport(
                            Report(
                                id = "",
                                stopId = stopId,
                                stopName = stopName,
                                type = selectedOption!!,
                                description = description
                            )
                        )
                        navController.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                enabled = selectedOption != null && description.isNotBlank()
            ) {
                Text("Zatwierdź zgłoszenie")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(navController: NavController, viewModel: TransportViewModel) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historia zgłoszeń") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Wróć")
                    }
                },
                actions = {
                    if (viewModel.currentUser != null) {
                        Text(viewModel.currentUser?.username ?: "", modifier = Modifier.padding(end = 8.dp))
                    } else {
                        TextButton(onClick = { navController.navigate("login") }) {
                            Text("Zaloguj")
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = { navController.navigate("reports") }) {
                        Text("Zgłoszenia")
                    }
                }
            }
        }
    ) { padding ->
        if (viewModel.reports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Brak zgłoszeń")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                items(viewModel.reports) { report ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Przystanek: ${report.stopName}", style = MaterialTheme.typography.titleMedium)
                            Text("Typ: ${report.type}", style = MaterialTheme.typography.bodyLarge)
                            Text("Opis: ${report.description}", style = MaterialTheme.typography.bodyMedium)
                            Text("Data: ${Date(report.timestamp)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}