package com.example.tweety2

import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
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
    private var _currentUser by mutableStateOf<User?>(null)
    val currentUser: User? get() = _currentUser

    private var _authToken by mutableStateOf<String?>(null)
    val authToken: String? get() = _authToken

    private val _events = mutableStateListOf<Event>()
    val events: List<Event> get() = _events

    private val _stops = mutableStateListOf<PoznanResponse>()
    val stops: List<PoznanResponse> get() = _stops

    private var _selectedStop by mutableStateOf<PoznanResponse?>(null)
    val selectedStop: PoznanResponse? get() = _selectedStop

    init {
        loadStopsFromApi()
        loadEvents()
    }

    private fun loadStopsFromApi() {
        _stops.clear()
        RetrofitClient.instance.getStopLocation().enqueue(object : Callback<List<PoznanResponse>> {
            override fun onResponse(call: Call<List<PoznanResponse>>, response: Response<List<PoznanResponse>>) {
                if (response.isSuccessful && response.body() != null) {
                    _stops.addAll(response.body()!!)
                }
            }

            override fun onFailure(call: Call<List<PoznanResponse>>, t: Throwable) {
                // Handle error
            }
        })
    }

    fun hasReportForStop(stopId: String): Boolean {
        return _events.any { it.stopId == stopId }
    }

    fun login(usernameOrEmail: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val request = LoginRequest(usernameOrEmail, password)
        RetrofitClient.instance.login(request).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        _authToken = "Bearer ${it.token}"
                        _currentUser = it.user
                        loadEvents()
                        onSuccess()
                    }
                } else {
                    onError("Login failed: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                onError("Network error: ${t.message}")
            }
        })
    }

    fun register(username: String, email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val request = RegisterRequest(username, email, password)
        RetrofitClient.instance.register(request).enqueue(object : Callback<User> {
            override fun onResponse(call: Call<User>, response: Response<User>) {
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onError("Registration failed: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<User>, t: Throwable) {
                onError("Network error: ${t.message}")
            }
        })
    }


    fun logout(navController: NavController) {
        _authToken?.let { token ->
            RetrofitClient.instance.logout(token).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    _authToken = null
                    _currentUser = null
                    navController.navigate("login") {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    // Handle error
                }
            })
        }
    }

    fun addEvent(stopId: String, type: String, description: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        _authToken?.let { token ->
            val request = EventRequest(stopId, type, description)
            RetrofitClient.instance.createEvent(token, request).enqueue(object : Callback<EventResponse> {
                override fun onResponse(call: Call<EventResponse>, response: Response<EventResponse>) {
                    if (response.isSuccessful) {
                        response.body()?.let { eventResponse ->
                            _events.add(Event(
                                eventResponse.id,
                                eventResponse.stopId,
                                stops.find { it.id == eventResponse.stopId }?.stop_name ?: "",
                                eventResponse.type,
                                eventResponse.description,
                                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(eventResponse.timestamp)?.time ?: System.currentTimeMillis(),
                                eventResponse.likes,
                                eventResponse.dislikes
                            ))
                            onSuccess()
                        }
                    } else {
                        onError("Failed to create event: ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<EventResponse>, t: Throwable) {
                    onError("Network error: ${t.message}")
                }
            })
        } ?: onError("Not authenticated")
    }

    fun selectStop(stop: PoznanResponse) {
        _selectedStop = stop
    }

    fun clearSelectedStop() {
        _selectedStop = null
    }

    fun loadEvents() {
        _authToken?.let { token ->
            // First fetch stop locations
            RetrofitClient.instance.getStopLocation().enqueue(object : Callback<List<PoznanResponse>> {
                override fun onResponse(call: Call<List<PoznanResponse>>, response: Response<List<PoznanResponse>>) {
                    if (response.isSuccessful) {
                        val stopLocations = response.body() ?: emptyList()

                        // Then fetch events
                        RetrofitClient.instance.getEvents().enqueue(object : Callback<GetEventsRequest> {
                            override fun onResponse(call: Call<GetEventsRequest>, response: Response<GetEventsRequest>) {
                                if (response.isSuccessful) {
                                    response.body()?.let { eventsResponse ->
                                        _events.clear()

                                        // Map events with stop names
                                        val eventsWithStopNames = eventsResponse.events.map { eventResponse ->
                                            val stopInfo = stopLocations.find { it.id == eventResponse.stopId }
                                            Event(
                                                id = eventResponse.id,
                                                stopId = eventResponse.stopId,
                                                stopName = stopInfo?.stop_name ?: "Unknown Stop",
                                                type = eventResponse.type,
                                                description = eventResponse.description,
                                                timestamp = try {
                                                    eventResponse.timestamp.toLong()
                                                } catch (e: NumberFormatException) {
                                                    System.currentTimeMillis()
                                                },
                                                likes = eventResponse.likes,
                                                dislikes = eventResponse.dislikes
                                            )
                                        }

                                        _events.addAll(eventsWithStopNames)
                                    }
                                }
                            }

                            override fun onFailure(call: Call<GetEventsRequest>, t: Throwable) {
                                // Handle error
                                t.printStackTrace()
                            }
                        })
                    }
                }

                override fun onFailure(call: Call<List<PoznanResponse>>, t: Throwable) {
                    // Handle error
                    t.printStackTrace()
                }
            })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController, viewModel: TransportViewModel) {
    var usernameOrEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

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
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            OutlinedTextField(
                value = usernameOrEmail,
                onValueChange = { usernameOrEmail = it },
                label = { Text("Nazwa użytkownika lub email") },
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
                    if (usernameOrEmail.isBlank() || password.isBlank()) {
                        errorMessage = "Wprowadź dane logowania"
                        return@Button
                    }
                    isLoading = true
                    viewModel.login(
                        usernameOrEmail,
                        password,
                        onSuccess = {
                            isLoading = false
                            navController.navigate("map")
                        },
                        onError = { error ->
                            isLoading = false
                            errorMessage = error
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text("Zaloguj")
                }
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
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

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
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            successMessage?.let {
                Text(
                    text = it,
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
                    if (email.isBlank() || username.isBlank() || password.isBlank()) {
                        errorMessage = "Wypełnij wszystkie pola"
                        return@Button
                    }
                    isLoading = true
                    viewModel.register(
                        username,
                        email,
                        password,
                        onSuccess = {
                            isLoading = false
                            successMessage = "Rejestracja udana! Możesz się zalogować"
                            errorMessage = null
                        },
                        onError = { error ->
                            isLoading = false
                            errorMessage = error
                            successMessage = null
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text("Zarejestruj się")
                }
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
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(52.4064, 16.9252))
        }
    }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf<String?>(null) } // null = wszystkie, "t" = tramwaje, "a" = autobusy

    LaunchedEffect(viewModel.stops, viewModel.events, filter) {
        mapView.overlays.clear()
        viewModel.stops.forEach { stop ->
            // Filtrowanie przystanków
            val isTramStop = stop.headsigns.split(",").any { it.trim().matches(Regex("\\d{1,2}")) }
            val isBusStop = stop.headsigns.split(",").any { it.trim().matches(Regex("\\d{3}")) }   // Zakładając, że route_type istnieje w PoznanResponse

            if (filter == null ||
                (filter == "t" && isTramStop) ||
                (filter == "a" && isBusStop)) {

                val marker = Marker(mapView).apply {
                    position = GeoPoint(stop.latitude, stop.longitude)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = stop.stop_name

                    if (viewModel.hasReportForStop(stop.id)) {
                        setIcon(context.getDrawable(android.R.drawable.presence_busy))
                    } else {
                        setIcon(context.getDrawable(android.R.drawable.presence_online))
                    }

                    setOnMarkerClickListener { _, _ ->
                        viewModel.selectStop(stop)
                        true
                    }
                }
                mapView.overlays.add(marker)
            }
        }
        mapView.invalidate()
    }


    viewModel.selectedStop?.let { stop ->
        AlertDialog(
            onDismissRequest = { viewModel.clearSelectedStop() },
            title = { Text(stop.stop_name) },
            text = {
                Column {
                    Text("ID: ${stop.id}")
                    Text("Lokalizacja: ${stop.latitude}, ${stop.longitude}")
                    Text("Strefa: ${stop.zone}")
                    Text("Kierunek: ${stop.headsigns}")
                    if (viewModel.hasReportForStop(stop.id)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Uwaga: Istnieją zgłoszenia dla tego przystanku!",
                            color = Color.Red
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        navController.navigate("reportForm/${stop.id}/${stop.stop_name}")
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

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Wylogować się?") },
            text = { Text("Czy na pewno chcesz się wylogować?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout(navController)
                    }
                ) {
                    Text("Wyloguj")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
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
                        Text(
                            text = viewModel.currentUser?.username ?: "",
                            modifier = Modifier
                                .clickable { showLogoutDialog = true }
                                .padding(end = 8.dp)
                        )
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
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Przyciski filtrowania
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        // Przycisk tramwajowy
                        FilterChip(
                            selected = filter == "t",
                            onClick = {
                                filter = if (filter == "t") null else "t"
                            },
                            modifier = Modifier.padding(horizontal = 4.dp),
                            label = { Text("T") }
                        )

                        // Przycisk autobusowy
                        FilterChip(
                            selected = filter == "a",
                            onClick = {
                                filter = if (filter == "a") null else "a"
                            },
                            modifier = Modifier.padding(horizontal = 4.dp),
                            label = { Text("A") }
                        )
                    }

                    // Przycisk zgłoszeń
                    Button(
                        onClick = { navController.navigate("reports") },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Zgłoszenia")
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TramStopsScreen(navController: NavController, viewModel: TransportViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val filteredStops = remember(viewModel.stops, searchQuery) {
        if (searchQuery.isBlank()) {
            viewModel.stops
        } else {
            viewModel.stops.filter { stop ->
                stop.stop_name.contains(searchQuery, ignoreCase = true) ||
                        stop.id.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Wylogować się?") },
            text = { Text("Czy na pewno chcesz się wylogować?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout(navController)
                    }
                ) {
                    Text("Wyloguj")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Anuluj")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista przystanków") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Wróć")
                    }
                },
                actions = {
                    if (viewModel.currentUser != null) {
                        Text(
                            text = viewModel.currentUser?.username ?: "",
                            modifier = Modifier
                                .clickable { showLogoutDialog = true }
                                .padding(end = 8.dp)
                        )
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
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Szukaj przystanku") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )

            if (filteredStops.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (viewModel.stops.isEmpty()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Ładowanie przystanków...")
                        }
                    } else {
                        Text("Nie znaleziono przystanków")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    items(filteredStops) { stop ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            onClick = {
                                navController.navigate("reportForm/${stop.id}/${stop.stop_name}")
                            }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stop.stop_name, style = MaterialTheme.typography.titleMedium)
                                Text("ID: ${stop.id}", style = MaterialTheme.typography.bodySmall)
                                Text("Strefa: ${stop.zone}", style = MaterialTheme.typography.bodySmall)
                                if (viewModel.hasReportForStop(stop.id)) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Istnieją zgłoszenia",
                                        color = Color.Red,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
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
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val options = listOf("Awaria", "Wypadek", "Kontrola biletów")

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Wylogować się?") },
            text = { Text("Czy na pewno chcesz się wylogować?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout(navController)
                    }
                ) {
                    Text("Wyloguj")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Anuluj")
                }
            }
        )
    }

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
                        Text(
                            text = viewModel.currentUser?.username ?: "",
                            modifier = Modifier
                                .clickable { showLogoutDialog = true }
                                .padding(end = 8.dp)
                        )
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
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

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
                    if (selectedOption == null) {
                        errorMessage = "Wybierz typ zgłoszenia"
                        return@Button
                    }
                    if (description.isBlank()) {
                        errorMessage = "Wprowadź opis"
                        return@Button
                    }

                    isLoading = true
                    viewModel.addEvent(
                        stopId,
                        selectedOption!!,
                        description,
                        onSuccess = {
                            isLoading = false
                            navController.popBackStack()
                        },
                        onError = { error ->
                            isLoading = false
                            errorMessage = error
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                enabled = !isLoading && selectedOption != null && description.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text("Zatwierdź zgłoszenie")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(navController: NavController, viewModel: TransportViewModel) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    fun handleReaction(eventId: String, isLike: Boolean, currentReaction: String?) {
        val token = viewModel.authToken
        if (token == null) {
            Toast.makeText(context, "Zaloguj się, aby oceniać zgłoszenia", Toast.LENGTH_SHORT).show()
            return
        }

        if ((currentReaction == "like" && isLike) || (currentReaction == "dislike" && !isLike)) {
            return
        }

        val call = if (isLike) {
            RetrofitClient.instance.likeEvent(token, eventId)
        } else {
            RetrofitClient.instance.dislikeEvent(token, eventId)
        }

        call.enqueue(object : Callback<LikeDislikeResponse> {
            override fun onResponse(call: Call<LikeDislikeResponse>, response: Response<LikeDislikeResponse>) {
                if (response.isSuccessful) {
                    viewModel.loadEvents()
                } else {
                    Toast.makeText(context, "Błąd podczas oceniania zgłoszenia", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LikeDislikeResponse>, t: Throwable) {
                Toast.makeText(context, "Błąd sieci: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Wylogować się?") },
            text = { Text("Czy na pewno chcesz się wylogować?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout(navController)
                    }
                ) {
                    Text("Wyloguj")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Anuluj")
                }
            }
        )
    }

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
                        Text(
                            text = viewModel.currentUser?.username ?: "",
                            modifier = Modifier
                                .clickable { showLogoutDialog = true }
                                .padding(end = 8.dp)
                        )
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
        Column(modifier = Modifier.padding(padding)) {
            // Pole wyszukiwania pod TopAppBar
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                placeholder = { Text("Szukaj po nazwie przystanku") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Szukaj") },
                singleLine = true
            )

            val filteredEvents = if (searchQuery.isBlank()) {
                viewModel.events
            } else {
                viewModel.events.filter { event ->
                    event.stopName.contains(searchQuery, ignoreCase = true)
                }
            }

            if (filteredEvents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (searchQuery.isNotBlank()) "Brak zgłoszeń dla podanej nazwy" else "Brak zgłoszeń")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    items(filteredEvents) { event ->
                        var userReaction by remember { mutableStateOf<String?>(null) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Przystanek: ${event.stopName}", style = MaterialTheme.typography.titleMedium)
                                Text("Typ: ${event.type}", style = MaterialTheme.typography.bodyLarge)
                                Text("Opis: ${event.description}", style = MaterialTheme.typography.bodyMedium)
                                Text("Data: ${Date(event.timestamp)}", style = MaterialTheme.typography.bodySmall)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                handleReaction(event.id, true, userReaction)
                                                userReaction = if (userReaction == "like") null else "like"
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.KeyboardArrowUp,
                                                contentDescription = "Like",
                                                tint = if (userReaction == "like") Color.Green else Color.Gray
                                            )
                                        }
                                        Text("${event.likes}")
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                handleReaction(event.id, false, userReaction)
                                                userReaction = if (userReaction == "dislike") null else "dislike"
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Dislike",
                                                tint = if (userReaction == "dislike") Color.Red else Color.Gray
                                            )
                                        }
                                        Text("${event.dislikes}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}