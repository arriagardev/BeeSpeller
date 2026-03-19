package com.example.beespeller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.beespeller.model.Word

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupTitle: String,
    allWords: List<Word>,
    groupStartId: Int,
    onBack: () -> Unit,
    onStartPractice: (List<Word>) -> Unit
) {
    var randomMode by remember { mutableStateOf(true) }
    
    val groupWords = remember(allWords, groupStartId) {
        allWords.filter { it.isPreloaded && it.numericId >= groupStartId && it.numericId < groupStartId + 10 }
                .sortedBy { it.numericId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(groupTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Practice Order", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(if (randomMode) "Randomized list" else "In numeric order", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = randomMode,
                        onCheckedChange = { randomMode = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    // Rule: Words with > 3 stars always at the end.
                    val learningWords = groupWords.filter { it.masteryLevel <= 3 }
                    val masteringWords = groupWords.filter { it.masteryLevel > 3 }

                    val finalLearning = if (randomMode) learningWords.shuffled() else learningWords.sortedBy { it.numericId }
                    val finalMastering = if (randomMode) masteringWords.shuffled() else masteringWords.sortedBy { it.numericId }

                    onStartPractice(finalLearning + finalMastering)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Group Challenge")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Words in this Group", style = MaterialTheme.typography.titleLarge)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(groupWords) { word ->
                    WordItem(word)
                }
            }
        }
    }
}
