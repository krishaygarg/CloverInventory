package com.example.helloworld

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.helloworld.models.FlashDeal
import com.example.helloworld.models.FlashItem
import com.example.helloworld.models.FlashCombo
import com.example.helloworld.services.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun formatPrice(cents: Long): String {
    val dollars = cents / 100
    val remainder = cents % 100
    return "$dollars.${remainder.toString().padStart(2, '0')}"
}

@Composable
fun App(aiService: AIService? = null) {
    val lightColors = lightColors(
        primary = Color(0xFF007A33),
        primaryVariant = Color(0xFF004B1A),
        secondary = Color(0xFF1A1A1A),
        background = Color(0xFFF5F5F5),
        surface = Color.White,
        error = Color(0xFFB00020)
    )

    MaterialTheme(colors = lightColors) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
            val inventoryService = remember { InventoryService(Constants.CLOVER_MERCHANT_ID, Constants.CLOVER_API_TOKEN) }
            val resolvedAiService = remember { aiService ?: AIService() }
            
            var items by remember { mutableStateOf(emptyList<FlashItem>()) }
            var insights by remember { mutableStateOf(emptyList<AIInsight>()) }
            
            val scope = rememberCoroutineScope()
            var isLoading by remember { mutableStateOf(false) }
            var showAddDialog by remember { mutableStateOf(false) }
            val scaffoldState = rememberScaffoldState()

            val activeDeals by DealService.activeDeals.collectAsState()
            var selectedItemForSale by remember { mutableStateOf<FlashItem?>(null) }
            val itemDescriptions = remember { mutableStateMapOf<String, String>() }

            var showApiKeyDialog by remember { mutableStateOf(!resolvedAiService.isAiReady) }

            fun loadInventory() {
                scope.launch {
                    isLoading = true
                    items = inventoryService.getInventory()
                    isLoading = false

                    if (resolvedAiService.isAiReady) {
                        insights = resolvedAiService.getMerchantInsights(items)
                        
                        // Generate descriptions for all items in memory with rate limit pacing
                        items.forEach { item ->
                            if (!itemDescriptions.containsKey(item.id)) {
                                itemDescriptions[item.id] = resolvedAiService.generateDescription(item.name, item.id)
                                delay(400)
                            }
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                loadInventory()
            }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                scaffoldState = scaffoldState,
                topBar = {
                    TopAppBar(
                        title = {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text("Clover Sales", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Merchant Inventory Portal", style = MaterialTheme.typography.caption, color = Color.Gray)
                                    Spacer(Modifier.width(12.dp))
                                    if (resolvedAiService.isAiReady) {
                                        Surface(
                                            color = Color(0xFF007A33).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "GEMINI 3.1 FLASH LITE", 
                                                color = Color(0xFF007A33), 
                                                fontSize = 9.sp, 
                                                fontWeight = FontWeight.Black, 
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else {
                                        Surface(
                                            color = Color(0xFFB00020).copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        ) {
                                            TextButton(
                                                onClick = { showApiKeyDialog = true },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                                modifier = Modifier.height(20.dp)
                                            ) {
                                                Text(
                                                    "ENTER GEMINI API KEY", 
                                                    color = Color(0xFFB00020), 
                                                    fontSize = 9.sp, 
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        backgroundColor = MaterialTheme.colors.surface,
                        contentColor = MaterialTheme.colors.primary,
                        elevation = 0.dp,
                        actions = {
                            IconButton(onClick = { loadInventory() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                            if (activeDeals.isNotEmpty()) {
                                TextButton(onClick = { DealService.clearAllDeals() }) {
                                    Text("Clear All Sales", color = MaterialTheme.colors.error)
                                }
                            }
                        }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        backgroundColor = MaterialTheme.colors.primary,
                        contentColor = Color.White,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Item")
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (showApiKeyDialog || !resolvedAiService.isAiReady) {
                        var keyInput by remember { mutableStateOf("") }
                        AlertDialog(
                            onDismissRequest = { if (resolvedAiService.isAiReady) showApiKeyDialog = false },
                            title = { Text("Enter Google Gemini API Key", fontWeight = FontWeight.Bold) },
                            text = {
                                Column(modifier = Modifier.width(320.dp)) {
                                    Text(
                                        "Please provide your Google Gemini API key from Google AI Studio to generate AI merchant recommendations and product descriptions:",
                                        style = MaterialTheme.typography.body2
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    OutlinedTextField(
                                        value = keyInput,
                                        onValueChange = { keyInput = it },
                                        label = { Text("Gemini API Key (AIzaSy...)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (keyInput.isNotBlank()) {
                                            resolvedAiService.updateApiKey(keyInput.trim())
                                            showApiKeyDialog = false
                                            loadInventory()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF007A33), contentColor = Color.White)
                                ) {
                                    Text("SAVE & INITIALIZE")
                                }
                            },
                            dismissButton = {
                                if (resolvedAiService.isAiReady) {
                                    TextButton(onClick = { showApiKeyDialog = false }) {
                                        Text("CANCEL")
                                    }
                                }
                            }
                        )
                    }
                    if (isLoading && items.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colors.primary)
                        }
                    } else if (items.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No items found. Click + to add one.", style = MaterialTheme.typography.h6, color = Color.Gray)
                        }
                    } else {
                        Box(modifier = Modifier.widthIn(max = 1400.dp).fillMaxSize()) {
                            MerchantInventoryView(
                                items = items,
                                insights = insights,
                                itemDescriptions = itemDescriptions,
                                activeDeals = activeDeals,
                                apiError = resolvedAiService.apiError,
                                onDelete = { item ->
                                    scope.launch {
                                        val success = inventoryService.deleteItem(item.id)
                                        if (success) {
                                            loadInventory()
                                            scaffoldState.snackbarHostState.showSnackbar("Deleted ${item.name}")
                                        } else {
                                            scaffoldState.snackbarHostState.showSnackbar("Error deleting item")
                                        }
                                    }
                                },
                                onSaleClick = { selectedItemForSale = it },
                                onRemoveDeal = { deal ->
                                    DealService.removeDeal(deal.itemId)
                                    scope.launch {
                                        scaffoldState.snackbarHostState.showSnackbar("Ended sale for ${deal.itemName}")
                                    }
                                },
                                onApplyInsight = { insight, item, combo ->
                                    if (combo != null) {
                                        DealService.publishDeal(
                                            FlashDeal(
                                                itemId = combo.id,
                                                itemName = combo.name,
                                                originalPrice = items.filter { it.id in combo.itemIds }.sumOf { it.price },
                                                flashPrice = combo.bundlePrice,
                                                expiryTimestamp = 600000L,
                                                description = combo.description,
                                                isCombo = true
                                            )
                                        )
                                        scope.launch {
                                            scaffoldState.snackbarHostState.showSnackbar("Combo Sale Activated: ${combo.name}")
                                        }
                                    } else if (item != null) {
                                        val flashPrice = (item.price * (1 - insight.suggestedDiscount)).toLong()
                                        DealService.publishDeal(
                                            FlashDeal(
                                                itemId = item.id,
                                                itemName = item.name,
                                                originalPrice = item.price,
                                                flashPrice = flashPrice,
                                                expiryTimestamp = 600000L
                                            )
                                        )
                                        scope.launch {
                                            scaffoldState.snackbarHostState.showSnackbar("Sale Applied: ${item.name} at \$${formatPrice(flashPrice)}")
                                        }
                                    }
                                }
                            )
                        }
                    }

                    if (showAddDialog) {
                        AddItemDialog(
                            onDismiss = { showAddDialog = false },
                            onConfirm = { name, price ->
                                scope.launch {
                                    val success = inventoryService.addItem(name, price)
                                    if (success) {
                                        loadInventory()
                                        showAddDialog = false
                                        scaffoldState.snackbarHostState.showSnackbar("Item added: $name")
                                    } else {
                                        scaffoldState.snackbarHostState.showSnackbar("Failed to add item")
                                    }
                                }
                            }
                        )
                    }

                    selectedItemForSale?.let { item ->
                        SalePriceDialog(
                            item = item,
                            onDismiss = { selectedItemForSale = null },
                            onConfirm = { flashPrice, durationMinutes ->
                                DealService.publishDeal(
                                    FlashDeal(
                                        itemId = item.id,
                                        itemName = item.name,
                                        originalPrice = item.price,
                                        flashPrice = flashPrice,
                                        expiryTimestamp = (durationMinutes * 60 * 1000).toLong()
                                    )
                                )
                                scope.launch {
                                    scaffoldState.snackbarHostState.showSnackbar("Sale active for ${item.name}!")
                                }
                                selectedItemForSale = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MerchantInventoryView(
    items: List<FlashItem>,
    insights: List<AIInsight>,
    itemDescriptions: Map<String, String>,
    activeDeals: List<FlashDeal>,
    apiError: String?,
    onDelete: (FlashItem) -> Unit,
    onSaleClick: (FlashItem) -> Unit,
    onRemoveDeal: (FlashDeal) -> Unit,
    onApplyInsight: (AIInsight, FlashItem?, FlashCombo?) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 350.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (apiError != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = Color(0xFFB00020).copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Color(0xFFB00020).copy(alpha = 0.4f)),
                    elevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFFB00020),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "GEMINI API ERROR",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("Raw Debug Error Output", fontWeight = FontWeight.Bold, color = Color(0xFFB00020))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            apiError,
                            style = MaterialTheme.typography.caption.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            color = Color(0xFFB00020)
                        )
                    }
                }
            }
        }
        if (activeDeals.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = Color(0xFF007A33).copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color(0xFF007A33).copy(alpha = 0.3f)),
                    elevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0xFF007A33),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "LIVE",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Active Item Sales (${activeDeals.size})",
                                    style = MaterialTheme.typography.h6,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF007A33)
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            activeDeals.forEach { deal ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    backgroundColor = Color.White,
                                    elevation = 2.dp,
                                    modifier = Modifier.width(260.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(deal.itemName, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Spacer(Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "$" + formatPrice(deal.originalPrice),
                                                style = MaterialTheme.typography.caption.copy(textDecoration = TextDecoration.LineThrough),
                                                color = Color.Gray
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "$" + formatPrice(deal.flashPrice),
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF007A33),
                                                fontSize = 16.sp
                                            )
                                        }
                                        Spacer(Modifier.height(10.dp))
                                        OutlinedButton(
                                            onClick = { onRemoveDeal(deal) },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB00020)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().height(32.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("End Sale", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (insights.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text(
                        "AI Recommendations",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF007A33),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        insights.forEach { insight ->
                            RecommendationCard(
                                insight = insight,
                                items = items,
                                activeDeals = activeDeals,
                                onApplyInsight = onApplyInsight
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    "Store Inventory",
                    style = MaterialTheme.typography.h4,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colors.secondary
                )
                Text(
                    "Manage your products and configure item sales & discounts",
                    style = MaterialTheme.typography.subtitle1,
                    color = Color.Gray
                )
                Spacer(Modifier.height(16.dp))
                Divider(thickness = 1.dp, color = Color.LightGray.copy(alpha = 0.5f))
            }
        }

        items(items) { item ->
            val activeDeal = activeDeals.find { it.itemId == item.id }
            InventoryItemRow(
                item = item,
                description = itemDescriptions[item.id] ?: "Generating description...",
                activeDeal = activeDeal,
                onDelete = onDelete,
                onSaleClick = onSaleClick,
                onRemoveDeal = onRemoveDeal
            )
        }
    }
}

@Composable
fun RecommendationCard(
    insight: AIInsight,
    items: List<FlashItem>,
    activeDeals: List<FlashDeal>,
    onApplyInsight: (AIInsight, FlashItem?, FlashCombo?) -> Unit
) {
    val item = items.find { it.id == insight.suggestedItemId }
    val combo = insight.suggestedCombo
    val targetId = combo?.id ?: item?.id
    val isAlreadyActive = activeDeals.any { it.itemId == targetId }
    
    Card(
        modifier = Modifier.width(300.dp),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = Color(0xFF007A33).copy(alpha = 0.05f),
        elevation = 0.dp,
        border = BorderStroke(1.dp, Color(0xFF007A33).copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (combo != null) Color(0xFF6200EE) else Color(0xFF007A33),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        if (combo != null) "COMBO" else insight.type.name,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(insight.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.subtitle1)
            }
            Spacer(Modifier.height(8.dp))
            Text(insight.description, style = MaterialTheme.typography.body2, color = Color.DarkGray)
            
            if (item != null || combo != null) {
                Spacer(Modifier.height(12.dp))
                Divider(color = Color(0xFF007A33).copy(alpha = 0.1f))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        val suggestedPrice = combo?.bundlePrice ?: (item!!.price * (1 - insight.suggestedDiscount)).toLong()
                        Text(combo?.name ?: item!!.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "Sale: $" + formatPrice(suggestedPrice),
                            color = Color(0xFF007A33),
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                    Button(
                        onClick = { onApplyInsight(insight, item, combo) },
                        enabled = !isAlreadyActive,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (isAlreadyActive) Color.Gray else (if (combo != null) Color(0xFF6200EE) else Color(0xFF007A33)),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 0.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(if (isAlreadyActive) "ACTIVE" else "APPLY", fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryItemRow(
    item: FlashItem, 
    description: String,
    activeDeal: FlashDeal?,
    onDelete: (FlashItem) -> Unit, 
    onSaleClick: (FlashItem) -> Unit,
    onRemoveDeal: (FlashDeal) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = 0.dp,
        backgroundColor = if (activeDeal != null) Color(0xFF007A33).copy(alpha = 0.04f) else MaterialTheme.colors.surface,
        border = if (activeDeal != null) BorderStroke(1.dp, Color(0xFF007A33).copy(alpha = 0.3f)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(end = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colors.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (activeDeal != null) {
                        Text(
                            text = "$" + formatPrice(item.price),
                            style = MaterialTheme.typography.body2.copy(textDecoration = TextDecoration.LineThrough),
                            color = Color.Gray
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "$" + formatPrice(activeDeal.flashPrice),
                            style = MaterialTheme.typography.body1,
                            color = Color(0xFF007A33),
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFF007A33).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "ON SALE",
                                color = Color(0xFF007A33),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "$" + formatPrice(item.price),
                            style = MaterialTheme.typography.body1,
                            color = Color(0xFF007A33),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                if (activeDeal != null) {
                    OutlinedButton(
                        onClick = { onRemoveDeal(activeDeal) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB00020)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(38.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("END SALE", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                } else {
                    Button(
                        onClick = { onSaleClick(item) },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF007A33).copy(alpha = 0.12f), 
                            contentColor = Color(0xFF007A33)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.elevation(0.dp),
                        modifier = Modifier.height(38.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("SALE", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = { onDelete(item) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Gray.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddItemDialog(onDismiss: () -> Unit, onConfirm: (String, Long) -> Unit) {
    var name by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text("Add New Product", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.padding(top = 8.dp).width(300.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    placeholder = { Text("e.g. Latte") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting,
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { if (it.all { char -> char.isDigit() }) priceInput = it },
                    label = { Text("Price (in cents)") },
                    placeholder = { Text("e.g. 450 for $4.50") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceInput.toLongOrNull()
                    if (name.isNotBlank() && price != null) {
                        isSubmitting = true
                        onConfirm(name, price)
                    }
                },
                enabled = name.isNotBlank() && priceInput.isNotBlank() && !isSubmitting,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF007A33), contentColor = Color.White)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("ADD ITEM")
                }
            }
        },
        dismissButton = {
            if (!isSubmitting) {
                TextButton(onClick = onDismiss) {
                    Text("CANCEL")
                }
            }
        }
    )
}

@Composable
fun SalePriceDialog(item: FlashItem, onDismiss: () -> Unit, onConfirm: (Long, Int) -> Unit) {
    var priceInput by remember { mutableStateOf((item.price * 0.8).toLong().toString()) }
    var durationMinutes by remember { mutableStateOf("10") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Sale: ${item.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.width(300.dp)) {
                Text("Set the sale price and how long it should last.")
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { if (it.all { char -> char.isDigit() }) priceInput = it },
                    label = { Text("Sale Price (cents)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = durationMinutes,
                    onValueChange = { if (it.all { char -> char.isDigit() }) durationMinutes = it },
                    label = { Text("Duration (minutes)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        priceInput.toLongOrNull() ?: 0L,
                        durationMinutes.toIntOrNull() ?: 10
                    )
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF007A33), contentColor = Color.White)
            ) {
                Text("ACTIVATE SALE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}

