package com.example.beespeller.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AdminPanelSettings
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
fun DashboardScreen(
    words: List<Word>,
    onStartPractice: (List<Word>) -> Unit,
    onAddWord: (String) -> Unit,
    onNavigateToGroup: (String, Int) -> Unit,
    onAdmin: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    var newWord by remember { mutableStateOf("") }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BeeSpeller Dashboard", fontWeight = FontWeight.Bold) },
                actions = {
                    Box {
                        IconButton(onClick = { showSettingsMenu = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                        DropdownMenu(
                            expanded = showSettingsMenu,
                            onDismissRequest = { showSettingsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Admin Management") },
                                onClick = {
                                    showSettingsMenu = false
                                    onAdmin()
                                },
                                leadingIcon = { Icon(Icons.Default.AdminPanelSettings, null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (selectedTabIndex == 1) { // My Words tab
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Word")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Preloaded") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("My Words") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTabIndex == 0) {
                PreloadedTabContent(words, onNavigateToGroup)
            } else {
                MyWordsTabContent(words, onStartPractice)
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
fun PreloadedTabContent(words: List<Word>, onNavigateToGroup: (String, Int) -> Unit) {
    val preloadedWords = remember(words) { words.filter { it.isPreloaded }.sortedBy { it.numericId } }
    
    val groups = remember(preloadedWords) {
        preloadedWords.groupBy { (it.numericId - 1) / 10 }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(groups.keys.sorted().toList()) { groupKey ->
            val groupWords = groups[groupKey] ?: emptyList()
            val startId = groupKey * 10 + 1
            val endId = startId + 9
            val title = "Group $startId - $endId"
            
            GroupItem(
                title = title,
                words = groupWords,
                onClick = { onNavigateToGroup(title, startId) }
            )
        }
    }
}

@Composable
fun GroupItem(title: String, words: List<Word>, onClick: () -> Unit) {
    val avgMastery = if (words.isEmpty()) 0f else words.map { it.masteryLevel }.average().toFloat()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { avgMastery / 5f },
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
fun MyWordsTabContent(words: List<Word>, onStartPractice: (List<Word>) -> Unit) {
    val myWords = remember(words) { words.filter { !it.isPreloaded } }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = { onStartPractice(myWords) },
            modifier = Modifier.fillMaxWidth(),
            enabled = myWords.isNotEmpty()
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Practice My Words")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(myWords) { word ->
                WordItem(word)
            }
        }
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
                if (word.spanishTranslation.isNotEmpty()) {
                    Text(word.spanishTranslation, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            MasteryStars(level = word.masteryLevel)
        }
    }
}
