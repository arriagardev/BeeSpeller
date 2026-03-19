package com.example.beespeller

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.beespeller.data.*
import com.example.beespeller.ui.*
import com.example.beespeller.ui.theme.BeeSpellerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var repository: WordRepository
    private lateinit var ttsProvider: TtsProvider
    private lateinit var spellingEngine: SpellingEngine
    private lateinit var settingsManager: SettingsManager
    private lateinit var speechManager: SpeechRecognizerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        settingsManager = SettingsManager(this)
        val database = AppDatabase.getDatabase(this)
        val geminiProvider = GeminiContentProvider()
        repository = WordRepository(database.wordDao(), geminiProvider, this)
        ttsProvider = TtsProvider(this)
        spellingEngine = SpellingEngine(repository, ttsProvider, geminiProvider, settingsManager)
        speechManager = SpeechRecognizerManager(this)

        enableEdgeToEdge()
        setContent {
            BeeSpellerTheme {
                val navController = rememberNavController()
                val words by repository.allWords.collectAsStateWithLifecycle(initialValue = emptyList())
                val scope = rememberCoroutineScope()
                var showPasswordDialog by remember { mutableStateOf(false) }
                var passwordInput by remember { mutableStateOf("") }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        speechManager.startListening()
                    } else {
                        Toast.makeText(this, "Microphone permission required for voice spelling", Toast.LENGTH_SHORT).show()
                    }
                }

                val importLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri: Uri? ->
                    uri?.let {
                        scope.launch {
                            try {
                                repository.importProgress(it)
                                Toast.makeText(this@MainActivity, "Progress imported successfully!", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(this@MainActivity, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    repository.preloadInitialWords(PreloadedWords.list)
                }

                NavHost(navController = navController, startDestination = "dashboard") {
                    composable("dashboard") {
                        DashboardScreen(
                            words = words,
                            onStartPractice = { wordsToPractice ->
                                navController.currentBackStackEntry?.savedStateHandle?.set("practice_words", wordsToPractice)
                                navController.navigate("spelling")
                            },
                            onAddWord = { newWord ->
                                scope.launch {
                                    repository.addWord(newWord)
                                }
                            },
                            onNavigateToGroup = { groupTitle, groupStartId ->
                                navController.currentBackStackEntry?.savedStateHandle?.set("group_title", groupTitle)
                                navController.currentBackStackEntry?.savedStateHandle?.set("group_start_id", groupStartId)
                                navController.navigate("group_detail")
                            },
                            onAdmin = {
                                showPasswordDialog = true
                            }
                        )
                    }
                    composable("admin") {
                        AdminScreen(
                            settingsManager = settingsManager,
                            onBack = { navController.popBackStack() },
                            onExport = {
                                scope.launch {
                                    try {
                                        val path = repository.exportProgress()
                                        Toast.makeText(this@MainActivity, "Exported to Downloads: $path", Toast.LENGTH_LONG).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            onImport = {
                                importLauncher.launch(arrayOf("application/json"))
                            },
                            onClearProgress = {
                                scope.launch {
                                    repository.clearProgress()
                                    Toast.makeText(this@MainActivity, "Progress cleared. Preloaded words restored.", Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    }
                    composable("group_detail") {
                        val groupTitle = navController.previousBackStackEntry?.savedStateHandle?.get<String>("group_title") ?: ""
                        val groupStartId = navController.previousBackStackEntry?.savedStateHandle?.get<Int>("group_start_id") ?: 1
                        
                        GroupDetailScreen(
                            groupTitle = groupTitle,
                            allWords = words,
                            groupStartId = groupStartId,
                            onBack = { navController.popBackStack() },
                            onStartPractice = { practiceList ->
                                navController.currentBackStackEntry?.savedStateHandle?.set("practice_words", practiceList)
                                navController.navigate("spelling")
                            }
                        )
                    }
                    composable("spelling") {
                        val practiceWords = navController.previousBackStackEntry?.savedStateHandle?.get<List<com.example.beespeller.model.Word>>("practice_words") ?: emptyList()
                        
                        SpellingScreen(
                            engine = spellingEngine,
                            speechManager = speechManager,
                            words = practiceWords,
                            onMicClick = {
                                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    speechManager.startListening()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            onFinish = {
                                navController.popBackStack()
                            }
                        )
                    }
                }

                if (showPasswordDialog) {
                    AlertDialog(
                        onDismissRequest = { 
                            showPasswordDialog = false
                            passwordInput = ""
                        },
                        title = { Text("Admin Access") },
                        text = {
                            TextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Enter Password") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (passwordInput == SettingsManager.ADMIN_PASSWORD) {
                                    showPasswordDialog = false
                                    passwordInput = ""
                                    navController.navigate("admin")
                                } else {
                                    Toast.makeText(this@MainActivity, "Incorrect Password", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Text("Enter")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { 
                                showPasswordDialog = false
                                passwordInput = ""
                            }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsProvider.shutdown()
        speechManager.destroy()
    }
}
