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
        spellingEngine = SpellingEngine(repository, ttsProvider, geminiProvider)

        enableEdgeToEdge()
        setContent {
            BeeSpellerTheme {
                val navController = rememberNavController()
                val words by repository.allWords.collectAsStateWithLifecycle(initialValue = emptyList())
                val scope = rememberCoroutineScope()

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
                            words = practiceWords,
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
