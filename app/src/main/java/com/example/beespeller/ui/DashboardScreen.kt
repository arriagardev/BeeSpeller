package com.example.beespeller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.beespeller.model.Word
import com.example.beespeller.model.SpellingStage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    words: List<Word>,
    onStartPractice: (List<Word>) -> Unit,
    onAddWord: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newWord by remember { mutableStateOf("") }

    val masteredCount = words.count { it.stage == SpellingStage.FINAL && it.repeats >= SpellingStage.FINAL.repeatsRequired }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BeeSpeller Dashboard", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Word")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Progress Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Your Progress", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "$masteredCount / ${words.size} words mastered!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LinearProgressIndicator(
                        progress = if (words.isEmpty()) 0f else masteredCount.toFloat() / words.size,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .height(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onStartPractice(words) },
                modifier = Modifier.fillMaxWidth(),
                enabled = words.isNotEmpty()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Practice Session")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Word List", style = MaterialTheme.typography.titleLarge)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(words) { word ->
                    WordItem(word)
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New Word") },
            text = {
                TextField(
                    value = newWord,
                    onValueChange = { newWord = it },
                    label = { Text("Word") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newWord.isNotBlank()) {
                        onAddWord(newWord)
                        newWord = ""
                        showAddDialog = false
                    }
                }) {
                    Text("Add")
                }
            }
        )
    }
}

@Composable
fun WordItem(word: Word) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(word.word, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("Stage: ${word.stage.name}", style = MaterialTheme.typography.bodySmall)
            }
            
            val progress = word.repeats.toFloat() / (if (word.stage.repeatsRequired == 0) 1 else word.stage.repeatsRequired)
            CircularProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                modifier = Modifier.size(24.dp),
                strokeWidth = 3.dp
            )
        }
    }
}
