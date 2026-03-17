package com.example.beespeller.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.SlowMotionVideo
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
    val scope = rememberCoroutineScope()
    var userInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        engine.startSession(words)
    }

    LaunchedEffect(gameState) {
        if (gameState is SpellingEngine.GameState.Finished) {
            onFinish()
        }
    }

    Scaffold(
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
                    SpellingView(
                        userInput = userInput,
                        onUserInputChange = { userInput = it },
                        onRepeat = { engine.repeatWord(slow = false) },
                        onRepeatSlow = { engine.repeatWord(slow = true) },
                        onDefinition = { scope.launch { engine.provideDefinition() } },
                        onExample = { scope.launch { engine.provideExample() } },
                        onSubmit = {
                            scope.launch {
                                engine.submitSpelling(userInput)
                                userInput = ""
                            }
                        }
                    )
                }
                is SpellingEngine.GameState.Translating -> {
                    TranslatingView(
                        word = state.word,
                        userInput = userInput,
                        onUserInputChange = { userInput = it },
                        onSubmit = {
                            scope.launch {
                                engine.submitTranslation(userInput)
                                userInput = ""
                            }
                        }
                    )
                }
                is SpellingEngine.GameState.Feedback -> {
                    FeedbackView(
                        state = state,
                        onNext = {
                            val currentIdx = words.indexOf(state.word)
                            engine.proceedToNext(words, currentIdx)
                        },
                        onTryAgain = {
                            engine.tryAgain()
                        }
                    )
                }
                else -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun SpellingView(
    userInput: String,
    onUserInputChange: (String) -> Unit,
    onRepeat: () -> Unit,
    onRepeatSlow: () -> Unit,
    onDefinition: () -> Unit,
    onExample: () -> Unit,
    onSubmit: () -> Unit
) {
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
            onValueChange = onUserInputChange,
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
            IconButton(onClick = onRepeat) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Listen")
                    Text("Listen", style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = onRepeatSlow) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SlowMotionVideo, contentDescription = "Listen slowly")
                    Text("Slow", style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = onDefinition) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Help, contentDescription = "Definition")
                    Text("Meaning", style = MaterialTheme.typography.labelSmall)
                }
            }
            IconButton(onClick = onExample) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Help, contentDescription = "Example")
                    Text("Example", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text("Submit Spelling", fontSize = 18.sp)
        }
    }
}

@Composable
fun TranslatingView(
    word: Word,
    userInput: String,
    onUserInputChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            "Spelling correct!",
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFF4CAF50)
        )
        Text(
            "Now translate '${word.word}' to Spanish:",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        OutlinedTextField(
            value = userInput,
            onValueChange = onUserInputChange,
            label = { Text("Spanish translation") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center)
        )

        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text("Submit Translation", fontSize = 18.sp)
        }
    }
}

@Composable
fun FeedbackView(
    state: SpellingEngine.GameState.Feedback,
    onNext: () -> Unit,
    onTryAgain: () -> Unit
) {
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
            Spacer(modifier = Modifier.height(20.dp))
            MasteryStars(level = (state.word.masteryLevel + 1).coerceAtMost(5))
            
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Next Word", fontSize = 18.sp)
            }
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
            
            val errorMessage = when(state.errorType) {
                SpellingEngine.ErrorType.SPELLING -> "Spelling was incorrect: ${state.word.word}"
                SpellingEngine.ErrorType.TRANSLATION -> "Translation was incorrect: ${state.word.spanishTranslation}"
                null -> ""
            }
            
            Text(
                errorMessage,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))
            MasteryStars(level = (state.word.masteryLevel - 1).coerceAtLeast(0))

            Spacer(modifier = Modifier.height(40.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = onTryAgain,
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Try Again")
                }
                Button(
                    onClick = onNext,
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Text("Next Word")
                }
            }
        }
    }
}
