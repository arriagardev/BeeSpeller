package com.example.beespeller.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.beespeller.data.SpellingEngine
import com.example.beespeller.model.Word
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpellingScreen(
    engine: SpellingEngine,
    words: List<Word>,
    onFinish: () -> Unit
) {
    val currentWord by engine.currentWord.collectAsState()
    val gameState by engine.gameState.collectAsState()
    val scope = rememberCoroutineAsState()
    var userInput by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        engine.startSession(words)
    }

    LaunchedEffect(gameState) {
        if (gameState is SpellingEngine.GameState.Finished) {
            onFinish()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Spelling Challenge") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            when (val state = gameState) {
                is SpellingEngine.GameState.Spelling -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            "Type the word you hear!",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = userInput,
                            onValueChange = { userInput = it },
                            label = { Text("Spell it here") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.headlineMedium.copy(
                                textAlign = TextAlign.Center,
                                letterSpacing = 4.sp
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            IconButton(onClick = { engine.repeatWord() }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Listen")
                                    Text("Listen", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            IconButton(onClick = { engine.provideDefinition() }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Help, contentDescription = "Definition")
                                    Text("Meaning", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            IconButton(onClick = { engine.provideExample() }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Help, contentDescription = "Example")
                                    Text("Example", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val currentIdx = words.indexOf(currentWord)
                                scope.launch {
                                    engine.submitSpelling(userInput, words, currentIdx)
                                    userInput = ""
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Text("Submit Spelling", fontSize = 18.sp)
                        }
                    }
                }
                is SpellingEngine.GameState.Feedback -> {
                    FeedbackView(state) {
                        val currentIdx = words.indexOf(state.word)
                        engine.proceedToNext(words, currentIdx)
                    }
                }
                else -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun FeedbackView(state: SpellingEngine.GameState.Feedback, onNext: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        if (state.isCorrect) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(120.dp)
            )
            Text(
                "Great Job!",
                style = MaterialTheme.typography.headlineLarge,
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Bold
            )
        } else {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(120.dp)
            )
            Text(
                "Keep Trying!",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
            Text(
                "The word was: ${state.word.word}",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Next Word", fontSize = 18.sp)
        }
    }
}

@Composable
fun rememberCoroutineAsState() = rememberCoroutineScope()
