package com.example.beespeller.ui

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.beespeller.data.SpellingEngine
import com.example.beespeller.data.SpeechRecognizerManager
import com.example.beespeller.model.Word
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpellingScreen(
    engine: SpellingEngine,
    speechManager: SpeechRecognizerManager,
    words: List<Word>,
    onMicClick: () -> Unit,
    onFinish: () -> Unit
) {
    val currentWord by engine.currentWord.collectAsState()
    val gameState by engine.gameState.collectAsState()
    val sessionWords by engine.sessionWords.collectAsState()
    val recognizedLetter by speechManager.recognizedLetter.collectAsState()
    val isListening by speechManager.isListening.collectAsState()
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

    LaunchedEffect(recognizedLetter) {
        if (recognizedLetter.isNotEmpty()) {
            userInput += recognizedLetter
            speechManager.resetLetter()
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
                        aiAvailable = engine.isAiOptionAvailable(),
                        isListening = isListening,
                        onMicClick = onMicClick,
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
                            val currentIdx = sessionWords.indexOf(state.word)
                            engine.proceedToNext(currentIdx)
                        },
                        onTryAgain = {
                            engine.tryAgain()
                        }
                    )
                }
                is SpellingEngine.GameState.Loading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Thinking...", style = MaterialTheme.typography.bodyLarge)
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
fun SpellingView(
    userInput: String,
    onUserInputChange: (String) -> Unit,
    onRepeat: () -> Unit,
    onRepeatSlow: () -> Unit,
    onDefinition: () -> Unit,
    onExample: () -> Unit,
    aiAvailable: Boolean,
    isListening: Boolean,
    onMicClick: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            "Type the word you hear!",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = userInput,
                onValueChange = onUserInputChange,
                label = { Text("Spell it here") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    textAlign = TextAlign.Center,
                    letterSpacing = 2.sp
                ),
                shape = MaterialTheme.shapes.medium,
                trailingIcon = {
                    if (userInput.isNotEmpty()) {
                        IconButton(onClick = { onUserInputChange("") }) {
                            Icon(Icons.Default.Error, contentDescription = "Clear")
                        }
                    }
                }
            )

            FloatingActionButton(
                onClick = onMicClick,
                containerColor = if (isListening) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Input",
                    tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Improved Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionCard(
                icon = Icons.Default.VolumeUp,
                label = "Listen",
                modifier = Modifier.weight(1f),
                onClick = onRepeat
            )
            ActionCard(
                icon = Icons.Default.SlowMotionVideo,
                label = "Slow",
                modifier = Modifier.weight(1f),
                onClick = onRepeatSlow
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionCard(
                icon = Icons.Default.Lightbulb,
                label = "Meaning",
                enabled = aiAvailable,
                modifier = Modifier.weight(1f),
                onClick = onDefinition
            )
            ActionCard(
                icon = Icons.Default.Help,
                label = "Example",
                enabled = aiAvailable,
                modifier = Modifier.weight(1f),
                onClick = onExample
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = MaterialTheme.shapes.large,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text("Submit Spelling", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ActionCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    ElevatedCard(
        onClick = { if (enabled) onClick() },
        modifier = modifier.height(80.dp),
        enabled = enabled,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
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
            color = Color(0xFF4CAF50),
            fontWeight = FontWeight.Bold
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
            textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
            shape = MaterialTheme.shapes.medium
        )

        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text("Submit Translation", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.fillMaxSize().padding(top = 20.dp)
    ) {
        if (state.isCorrect) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(100.dp)
            )
            Text(
                "Great Job!",
                style = MaterialTheme.typography.headlineLarge,
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))
            MasteryStars(level = (state.word.masteryLevel + 1).coerceAtMost(5))
            
            Spacer(modifier = Modifier.height(30.dp))
            
            // Show word details even on success
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("English: ${state.word.word}", fontWeight = FontWeight.Bold)
                    Text("Spanish: ${state.word.spanishTranslation}")
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Next Word", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(100.dp)
            )
            Text(
                "Keep Trying!",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
            
            val errorMessage = when(state.errorType) {
                SpellingEngine.ErrorType.SPELLING -> "The spelling was incorrect."
                SpellingEngine.ErrorType.TRANSLATION -> "The translation was incorrect."
                null -> ""
            }
            
            Text(
                errorMessage,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Highlight the correct info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row {
                        Text("Word: ", fontWeight = FontWeight.Bold)
                        Text(state.word.word, color = if (state.errorType == SpellingEngine.ErrorType.SPELLING) MaterialTheme.colorScheme.error else Color.Unspecified)
                    }
                    Row {
                        Text("Spanish: ", fontWeight = FontWeight.Bold)
                        Text(state.word.spanishTranslation, color = if (state.errorType == SpellingEngine.ErrorType.TRANSLATION) MaterialTheme.colorScheme.error else Color.Unspecified)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            MasteryStars(level = (state.word.masteryLevel - 1).coerceAtLeast(0))

            Spacer(modifier = Modifier.height(40.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = onTryAgain,
                    modifier = Modifier.weight(1f).height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("Try Again", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onNext,
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("Next Word", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
