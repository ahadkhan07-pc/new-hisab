package com.mydailyhisab.app.ui.navigation

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseUser
import com.mydailyhisab.app.data.firebase.AndroidSyncStatus
import com.mydailyhisab.app.data.firebase.FirebaseAuthManager
import com.mydailyhisab.app.data.firebase.FirestoreSyncManager
import com.mydailyhisab.app.data.local.CategoryEntity
import com.mydailyhisab.app.data.local.PaymentMethod
import com.mydailyhisab.app.data.local.TransactionEntity
import com.mydailyhisab.app.data.local.TransactionType
import com.mydailyhisab.app.data.network.NetworkMonitor
import com.mydailyhisab.app.data.preferences.ThemePreferences
import com.mydailyhisab.app.data.repository.HisabRepository
import com.mydailyhisab.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

enum class AppTab {
    HOME, TRANSACTIONS, REPORTS, PROFILE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppNavHost(
    repository: HisabRepository,
    themePreferences: ThemePreferences,
    isDarkMode: Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val networkMonitor = remember { NetworkMonitor(context) }
    val isOnline by networkMonitor.isOnline.collectAsState(initial = true)

    val authManager = remember { FirebaseAuthManager(context) }
    val currentUser by authManager.currentUser.collectAsState()

    val syncManager = remember { FirestoreSyncManager(repository) }
    val syncStatus by syncManager.syncStatus.collectAsState()

    val allTransactions by repository.allTransactions.collectAsState(initial = emptyList())
    val allCategories by repository.allCategories.collectAsState(initial = emptyList())

    var currentTab by remember { mutableStateOf(AppTab.HOME) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    // Auto-sync when network returns or auth changes
    LaunchedEffect(isOnline, currentUser) {
        if (isOnline && currentUser != null) {
            syncManager.performFullSync()
        }
    }

    val primaryAccent = if (isDarkMode) AccentPurple else AccentVelvet
    val magentaAccent = AccentMagenta
    val orangeAccent = if (isDarkMode) AccentOrange else AccentOrangeDeep

    Scaffold(
        topBar = {
            // App Header (PART 2: strictly only "আমার দৈনিক হিসাব" and connection dot)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Title and Network Connection Dot
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "আমার দৈনিক হিসাব",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Network Connectivity Status Dot (Green=Online, Red=Offline)
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) Color(0xFF22C55E) else Color(0xFFEF4444))
                        )

                        // Cloud Sync Status Pill (Separate from network connectivity)
                        if (currentUser != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when (syncStatus) {
                                    AndroidSyncStatus.SYNCED -> Color(0xFF10B981).copy(alpha = 0.15f)
                                    AndroidSyncStatus.SYNCING -> primaryAccent.copy(alpha = 0.15f)
                                    AndroidSyncStatus.FAILED -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                    AndroidSyncStatus.IDLE -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = when (syncStatus) {
                                            AndroidSyncStatus.SYNCED -> "Synced"
                                            AndroidSyncStatus.SYNCING -> "Syncing..."
                                            AndroidSyncStatus.FAILED -> "Sync Failed"
                                            AndroidSyncStatus.IDLE -> "Cloud Idle"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = when (syncStatus) {
                                            AndroidSyncStatus.SYNCED -> Color(0xFF10B981)
                                            AndroidSyncStatus.SYNCING -> primaryAccent
                                            AndroidSyncStatus.FAILED -> Color(0xFFEF4444)
                                            AndroidSyncStatus.IDLE -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Action Icons (Sun/Moon Theme Switcher & Profile)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Theme Toggle (Sun and Moon icons)
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    val nextMode = if (isDarkMode) ThemePreferences.THEME_LIGHT else ThemePreferences.THEME_DARK
                                    themePreferences.setThemeMode(nextMode)
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.WbSunny else Icons.Default.NightlightRound,
                                contentDescription = if (isDarkMode) "Switch to Light Mode" else "Switch to Dark Mode",
                                tint = if (isDarkMode) orangeAccent else magentaAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Profile Avatar / Button
                        IconButton(
                            onClick = { currentTab = AppTab.PROFILE },
                            modifier = Modifier.size(36.dp)
                        ) {
                            if (currentUser?.photoUrl != null) {
                                AsyncImage(
                                    model = currentUser?.photoUrl,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, primaryAccent, CircleShape)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Profile",
                                    tint = primaryAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Material 3 Bottom Navigation with Center Purple FAB (+)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    NavigationBarItem(
                        selected = currentTab == AppTab.HOME,
                        onClick = { currentTab = AppTab.HOME },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("হোম") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = primaryAccent,
                            selectedTextColor = primaryAccent,
                            indicatorColor = primaryAccent.copy(alpha = 0.15f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.TRANSACTIONS,
                        onClick = { currentTab = AppTab.TRANSACTIONS },
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Transactions") },
                        label = { Text("লেনদেন") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = magentaAccent,
                            selectedTextColor = magentaAccent,
                            indicatorColor = magentaAccent.copy(alpha = 0.15f)
                        )
                    )

                    // Floating Center Quick Add Button (PURPLE ACCENT as requested)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FloatingActionButton(
                            onClick = {
                                editingTransaction = null
                                showAddDialog = true
                            },
                            containerColor = primaryAccent,
                            contentColor = Color.White,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .size(50.dp)
                                .shadow(8.dp, RoundedCornerShape(16.dp))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Quick Add", modifier = Modifier.size(28.dp))
                        }
                    }

                    NavigationBarItem(
                        selected = currentTab == AppTab.REPORTS,
                        onClick = { currentTab = AppTab.REPORTS },
                        icon = { Icon(Icons.Default.PieChart, contentDescription = "Reports") },
                        label = { Text("রিপোর্ট") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = orangeAccent,
                            selectedTextColor = orangeAccent,
                            indicatorColor = orangeAccent.copy(alpha = 0.15f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentTab == AppTab.PROFILE,
                        onClick = { currentTab = AppTab.PROFILE },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("প্রোফাইল") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = primaryAccent,
                            selectedTextColor = primaryAccent,
                            indicatorColor = primaryAccent.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentTab) {
                AppTab.HOME -> ComposeHomeScreen(
                    transactions = allTransactions,
                    categories = allCategories,
                    primaryAccent = primaryAccent,
                    magentaAccent = magentaAccent,
                    orangeAccent = orangeAccent,
                    onAddClick = {
                        editingTransaction = null
                        showAddDialog = true
                    },
                    onEditClick = { tx ->
                        editingTransaction = tx
                        showAddDialog = true
                    }
                )
                AppTab.TRANSACTIONS -> ComposeTransactionsScreen(
                    transactions = allTransactions,
                    categories = allCategories,
                    primaryAccent = primaryAccent,
                    magentaAccent = magentaAccent,
                    orangeAccent = orangeAccent,
                    onEditClick = { tx ->
                        editingTransaction = tx
                        showAddDialog = true
                    },
                    onDeleteClick = { id ->
                        coroutineScope.launch { repository.deleteTransaction(id) }
                    }
                )
                AppTab.REPORTS -> ComposeReportsScreen(
                    transactions = allTransactions,
                    categories = allCategories,
                    primaryAccent = primaryAccent,
                    magentaAccent = magentaAccent,
                    orangeAccent = orangeAccent
                )
                AppTab.PROFILE -> ComposeProfileScreen(
                    currentUser = currentUser,
                    authManager = authManager,
                    syncManager = syncManager,
                    syncStatus = syncStatus,
                    primaryAccent = primaryAccent,
                    orangeAccent = orangeAccent
                )
            }
        }
    }

    // Add / Edit Transaction Sheet Dialog
    if (showAddDialog) {
        ComposeAddTransactionDialog(
            transaction = editingTransaction,
            categories = allCategories,
            primaryAccent = primaryAccent,
            magentaAccent = magentaAccent,
            orangeAccent = orangeAccent,
            onDismiss = { showAddDialog = false },
            onSave = { tx ->
                coroutineScope.launch {
                    if (editingTransaction != null) {
                        repository.updateTransaction(tx)
                    } else {
                        repository.addTransaction(tx)
                    }
                    if (isOnline && currentUser != null) {
                        syncManager.performFullSync()
                    }
                }
                showAddDialog = false
            }
        )
    }
}

// --------------------------------------------------------------------------
// Compose Home Screen with Income (Purple), Expense (Orange), Savings (Pink)
// --------------------------------------------------------------------------
@Composable
fun ComposeHomeScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    primaryAccent: Color,
    magentaAccent: Color,
    orangeAccent: Color,
    onAddClick: () -> Unit,
    onEditClick: (TransactionEntity) -> Unit
) {
    val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val totalSavings = transactions.filter { it.type == TransactionType.SAVINGS }.sumOf { it.amount }
    val netBalance = totalIncome - totalExpense

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("bn", "BD"))

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Balance Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "বর্তমান মোট স্থিতি",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "৳ ${String.format("%,.0f", netBalance)}",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Income (Purple Accent)
                        Column {
                            Text("আয় (Income)", style = MaterialTheme.typography.labelSmall, color = primaryAccent)
                            Text("৳ ${String.format("%,.0f", totalIncome)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = primaryAccent)
                        }

                        // Expense (Orange Accent)
                        Column {
                            Text("ব্যয় (Expense)", style = MaterialTheme.typography.labelSmall, color = orangeAccent)
                            Text("৳ ${String.format("%,.0f", totalExpense)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = orangeAccent)
                        }

                        // Savings (Pink Accent)
                        Column {
                            Text("সঞ্চয় (Savings)", style = MaterialTheme.typography.labelSmall, color = magentaAccent)
                            Text("৳ ${String.format("%,.0f", totalSavings)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = magentaAccent)
                        }
                    }
                }
            }
        }

        // Recent Transactions Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "সাম্প্রতিক লেনদেন",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = onAddClick) {
                    Text("+ হিসাব যোগ করুন", color = primaryAccent)
                }
            }
        }

        // Recent items list
        if (transactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "কোনো লেনদেনের রেকর্ড নেই। নতুন হিসাব যোগ করুন!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            items(transactions.take(10)) { tx ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditClick(tx) }
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (tx.note.isNotBlank()) tx.note else tx.type.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "${tx.date} • ${tx.paymentMethod.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        val amountColor = when (tx.type) {
                            TransactionType.INCOME -> primaryAccent
                            TransactionType.SAVINGS -> magentaAccent
                            TransactionType.EXPENSE -> orangeAccent
                        }

                        Text(
                            text = "${if (tx.type == TransactionType.INCOME) "+" else "-"} ৳ ${String.format("%,.0f", tx.amount)}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = amountColor
                        )
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------
// Compose Transactions Screen
// --------------------------------------------------------------------------
@Composable
fun ComposeTransactionsScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    primaryAccent: Color,
    magentaAccent: Color,
    orangeAccent: Color,
    onEditClick: (TransactionEntity) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(transactions) { tx ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEditClick(tx) }
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (tx.note.isNotBlank()) tx.note else tx.type.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${tx.date} • ${tx.time} • ${tx.paymentMethod.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    val color = when (tx.type) {
                        TransactionType.INCOME -> primaryAccent
                        TransactionType.SAVINGS -> magentaAccent
                        TransactionType.EXPENSE -> orangeAccent
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "৳ ${String.format("%,.0f", tx.amount)}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = color,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        IconButton(onClick = { onDeleteClick(tx.id) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------
// Compose Reports Screen
// --------------------------------------------------------------------------
@Composable
fun ComposeReportsScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    primaryAccent: Color,
    magentaAccent: Color,
    orangeAccent: Color
) {
    val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val totalSavings = transactions.filter { it.type == TransactionType.SAVINGS }.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("মাসিক আর্থিক প্রতিবেদন (Ledger Summary)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Divider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("মোট উপার্জিত আয়:")
                    Text("৳ ${String.format("%,.0f", totalIncome)}", color = primaryAccent, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("মোট সর্বমোট ব্যয়:")
                    Text("৳ ${String.format("%,.0f", totalExpense)}", color = orangeAccent, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("নিট মোট সঞ্চয়:")
                    Text("৳ ${String.format("%,.0f", totalSavings)}", color = magentaAccent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --------------------------------------------------------------------------
// Compose Profile Screen (PART 5: Custom Profile & Google Sign-In)
// --------------------------------------------------------------------------
@Composable
fun ComposeProfileScreen(
    currentUser: FirebaseUser?,
    authManager: FirebaseAuthManager,
    syncManager: FirestoreSyncManager,
    syncStatus: AndroidSyncStatus,
    primaryAccent: Color,
    orangeAccent: Color
) {
    val coroutineScope = rememberCoroutineScope()
    var displayName by remember(currentUser) { mutableStateOf(currentUser?.displayName ?: "") }
    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Profile Photo
                if (currentUser?.photoUrl != null) {
                    AsyncImage(
                        model = currentUser.photoUrl,
                        contentDescription = "Profile Photo",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .border(2.dp, primaryAccent, CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(primaryAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Avatar", tint = primaryAccent, modifier = Modifier.size(44.dp))
                    }
                }

                if (currentUser != null) {
                    Text(
                        text = currentUser.displayName ?: "ব্যবহারকারী",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = currentUser.email ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Button(
                        onClick = {
                            coroutineScope.launch { syncManager.performFullSync() }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (syncStatus == AndroidSyncStatus.SYNCING) "সিঙ্ক হচ্ছে..." else "এখনই ক্লাউড সিঙ্ক করুন")
                    }

                    OutlinedButton(
                        onClick = { authManager.signOut() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("সাইন আউট (Sign Out)")
                    }
                } else {
                    Text(
                        text = "গুগল একাউন্ট সংযুক্ত করুন",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "আপনার হিসাব নিরাপদ ক্লাউডে ব্যাকআপ রাখতে এবং ফোন পরিবর্তনের পর পুনরুদ্ধার করতে সাইন-ইন করুন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                // Real Google Sign In via Credential Manager
                                authManager.signInWithGoogle("109344088352-aamcfkkbkpr1486mcmn8se6vh7669m23.apps.googleusercontent.com")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign in with Google")
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------
// Compose Add Transaction Dialog
// --------------------------------------------------------------------------
@Composable
fun ComposeAddTransactionDialog(
    transaction: TransactionEntity?,
    categories: List<CategoryEntity>,
    primaryAccent: Color,
    magentaAccent: Color,
    orangeAccent: Color,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit
) {
    var amountStr by remember { mutableStateOf(transaction?.amount?.toString() ?: "") }
    var note by remember { mutableStateOf(transaction?.note ?: "") }
    var selectedType by remember { mutableStateOf(transaction?.type ?: TransactionType.EXPENSE) }
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val currentTime = SimpleDateFormat("HH:mm", Locale.US).format(Date())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (transaction != null) "হিসাব সম্পাদনা" else "নতুন হিসাব যোগ করুন")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Type selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { selectedType = TransactionType.EXPENSE },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == TransactionType.EXPENSE) orangeAccent else Color.Transparent,
                            contentColor = if (selectedType == TransactionType.EXPENSE) Color.White else orangeAccent
                        ),
                        modifier = Modifier.weight(1f).border(1.dp, orangeAccent, RoundedCornerShape(8.dp)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("ব্যয়")
                    }

                    Button(
                        onClick = { selectedType = TransactionType.INCOME },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == TransactionType.INCOME) primaryAccent else Color.Transparent,
                            contentColor = if (selectedType == TransactionType.INCOME) Color.White else primaryAccent
                        ),
                        modifier = Modifier.weight(1f).border(1.dp, primaryAccent, RoundedCornerShape(8.dp)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("আয়")
                    }

                    Button(
                        onClick = { selectedType = TransactionType.SAVINGS },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == TransactionType.SAVINGS) magentaAccent else Color.Transparent,
                            contentColor = if (selectedType == TransactionType.SAVINGS) Color.White else magentaAccent
                        ),
                        modifier = Modifier.weight(1f).border(1.dp, magentaAccent, RoundedCornerShape(8.dp)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("সঞ্চয়")
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("টাকার পরিমাণ (Amount)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("বিবরণ / নোট (Note)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        val newTx = TransactionEntity(
                            id = transaction?.id ?: ("tx_" + System.currentTimeMillis()),
                            amount = amount,
                            type = selectedType,
                            categoryId = transaction?.categoryId ?: "general",
                            date = transaction?.date ?: today,
                            time = transaction?.time ?: currentTime,
                            paymentMethod = transaction?.paymentMethod ?: PaymentMethod.CASH,
                            note = note
                        )
                        onSave(newTx)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryAccent)
            ) {
                Text("সংরক্ষণ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}
