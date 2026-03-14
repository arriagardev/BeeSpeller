package com.example.beespeller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(this)
        val geminiProvider = GeminiContentProvider()
        repository = WordRepository(database.wordDao(), geminiProvider)
        ttsProvider = TtsProvider(this)
        spellingEngine = SpellingEngine(repository, ttsProvider)

        enableEdgeToEdge()
        setContent {
            BeeSpellerTheme {
                val navController = rememberNavController()
                val words by repository.allWords.collectAsStateWithLifecycle(initialValue = emptyList())
                val scope = rememberCoroutineScope()

                NavHost(navController = navController, startDestination = "dashboard") {
                    composable("dashboard") {
                        DashboardScreen(
                            words = words,
                            onStartPractice = { wordsToPractice ->
                                navController.navigate("spelling")
                            },
                            onAddWord = { newWord ->
                                scope.launch {
                                    repository.addWord(newWord)
                                }
                            }
                        )
                    }
                    composable("spelling") {
                        SpellingScreen(
                            engine = spellingEngine,
                            words = words,
                            onFinish = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsProvider.shutdown()
    }
}
