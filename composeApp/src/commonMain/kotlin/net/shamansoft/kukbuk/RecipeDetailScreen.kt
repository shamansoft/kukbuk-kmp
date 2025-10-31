package net.shamansoft.kukbuk

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.shamansoft.kukbuk.recipe.RecipeDetailState
import net.shamansoft.kukbuk.recipe.RecipeDetailViewModel
import net.shamansoft.kukbuk.util.RecipeImage
import net.shamansoft.recipe.model.Recipe
import net.shamansoft.recipe.model.Ingredient
import net.shamansoft.recipe.model.Instruction
import net.shamansoft.recipe.model.Nutrition
import net.shamansoft.recipe.model.Storage

/**
 * Recipe detail screen showing full recipe information
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: String,
    recipeTitle: String,
    onNavigateBack: () -> Unit,
    viewModel: RecipeDetailViewModel
) {
    val recipeDetailState by viewModel.recipeDetailState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = recipeTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("◀", fontSize = 20.sp)
                    }
                },
                actions = {
                    // Refresh button
                    IconButton(onClick = { viewModel.refresh() }) {
                        Text("🔄", fontSize = 20.sp)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val state = recipeDetailState) {
                is RecipeDetailState.Loading -> {
                    LoadingState()
                }

                is RecipeDetailState.Success -> {
                    RecipeContent(
                        recipe = state.recipe,
                        isOffline = state.isOffline
                    )
                }

                is RecipeDetailState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.retry() }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipeContent(
    recipe: Recipe,
    isOffline: Boolean = false
) {
    // DEBUG: Log recipe data
    net.shamansoft.kukbuk.util.Logger.d("RecipeDetailScreen", "Recipe: ${recipe.metadata.title}")
    net.shamansoft.kukbuk.util.Logger.d("RecipeDetailScreen", "Instructions count: ${recipe.instructions.size}")
    net.shamansoft.kukbuk.util.Logger.d("RecipeDetailScreen", "Ingredients count: ${recipe.ingredients.size}")
    recipe.instructions.forEachIndexed { index, instruction ->
        net.shamansoft.kukbuk.util.Logger.d("RecipeDetailScreen", "Instruction $index: step=${instruction.step}, desc=${instruction.description.take(50)}")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        // Offline indicator banner (if offline)
        if (isOffline) {
            item {
                OfflineIndicatorBanner()
            }
        }

        // 1. Steps Section (PRIMARY - for cooking)
        if (recipe.instructions.isNotEmpty()) {
            item {
                StepsSection(instructions = recipe.instructions)
            }
        }

        // 2. Ingredients Section
        if (recipe.ingredients.isNotEmpty()) {
            item {
                IngredientsSection(recipe = recipe)
            }
        }

        // 3. Description Section
        item {
            DescriptionSection(recipe = recipe)
        }

        // 4. Credits/Metadata Section
        item {
            CreditsSection(recipe = recipe)
        }

        // 5. Nutrition Info Section (if available) - Collapsible
        recipe.nutrition?.let { nutrition ->
            item {
                CollapsibleSection(
                    title = "Nutrition Information",
                    initiallyExpanded = false
                ) {
                    NutritionSectionContent(nutrition = nutrition)
                }
            }
        }

        // 6. Storage Instructions (if available) - Collapsible
        recipe.storage?.let { storage ->
            item {
                CollapsibleSection(
                    title = "Storage Instructions",
                    initiallyExpanded = false
                ) {
                    StorageSectionContent(storage = storage)
                }
            }
        }

        // 7. Equipment List (if available) - Collapsible
        if (recipe.equipment.isNotEmpty()) {
            item {
                CollapsibleSection(
                    title = "Equipment Needed",
                    initiallyExpanded = false
                ) {
                    EquipmentSectionContent(equipment = recipe.equipment)
                }
            }
        }

        // 8. Notes Section - Collapsible
        recipe.notes?.let { notes ->
            if (notes.isNotBlank()) {
                item {
                    CollapsibleSection(
                        title = "Notes",
                        initiallyExpanded = false
                    ) {
                        NotesSectionContent(notes = notes)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataChip(label: String, value: String) {
    AssistChip(
        onClick = { },
        label = {
            Text(
                text = "$label: $value",
                fontSize = 12.sp
            )
        }
    )
}

/**
 * Ingredients Section - Groups ingredients by component
 */
@Composable
private fun IngredientsSection(recipe: Recipe) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Header
        Text(
            text = "Ingredients",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Servings info
        Text(
            text = "Servings: ${recipe.metadata.servings}",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Group by component
        val grouped = recipe.ingredients.groupBy { it.component }

        grouped.forEach { (component, ingredients) ->
            // Component header (if present)
            component?.let {
                Text(
                    text = it,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Ingredients card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ingredients.forEach { ingredient ->
                        IngredientRow(ingredient = ingredient)
                    }
                }
            }
        }
    }
}

/**
 * Ingredient row with amount, unit, and notes
 */
@Composable
private fun IngredientRow(ingredient: Ingredient) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildIngredientDisplayString(ingredient),
                fontSize = 18.sp,
                lineHeight = 28.sp
            )

            // Show substitutions if available
            ingredient.substitutions?.let { subs ->
                if (subs.isNotEmpty()) {
                    Text(
                        text = "Alternative: ${subs.first().item}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}

/**
 * Builds a display string for an ingredient
 */
private fun buildIngredientDisplayString(ingredient: Ingredient): String {
    val parts = mutableListOf<String>()

    val amount = ingredient.amount
    val unit = ingredient.unit

    if (amount != null && unit != null) {
        parts.add("$amount $unit")
    } else if (amount != null) {
        parts.add(amount)
    } else if (unit != null) {
        parts.add(unit)
    }

    parts.add(ingredient.item)

    ingredient.notes?.let { notes ->
        parts.add("($notes)")
    }

    if (ingredient.optional) {
        parts.add("(optional)")
    }

    return parts.joinToString(" ")
}

/**
 * Steps Section - Primary cooking instructions
 */
@Composable
private fun StepsSection(instructions: List<Instruction>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Header
        Text(
            text = "Steps",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Instructions List
        instructions.forEachIndexed { index, instruction ->
            InstructionStep(instruction = instruction, stepNumber = index + 1)
        }
    }
}

/**
 * Instruction step with time, temperature, and media
 */
@Composable
private fun InstructionStep(instruction: Instruction, stepNumber: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Step Number Badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (instruction.step ?: stepNumber).toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            // Instruction Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Instruction Text
                Text(
                    text = instruction.description,
                    fontSize = 18.sp,
                    lineHeight = 28.sp
                )

                // Time and Temperature row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    instruction.time?.let { time ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⏱",
                                fontSize = 14.sp
                            )
                            Text(
                                text = time,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    instruction.temperature?.let { temp ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🌡",
                                fontSize = 14.sp
                            )
                            Text(
                                text = temp,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Media Gallery (if media exists)
                instruction.media?.let { mediaList ->
                    if (mediaList.isNotEmpty()) {
                        net.shamansoft.kukbuk.util.MediaGallery(
                            media = mediaList,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Description Section - Recipe overview with hero image and metadata
 */
@Composable
private fun DescriptionSection(recipe: Recipe) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Hero Image
        RecipeImage(
            url = recipe.metadata.coverImage?.path,
            contentDescription = "Recipe: ${recipe.metadata.title}",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            cornerRadius = 12.dp
        )

        // Title
        Text(
            text = recipe.metadata.title,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 40.sp
        )

        // Description
        if (recipe.description.isNotBlank()) {
            Text(
                text = recipe.description,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Metadata Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            recipe.metadata.prepTime?.let { prepTime ->
                MetadataChip(label = "Prep", value = prepTime)
            }

            recipe.metadata.cookTime?.let { cookTime ->
                MetadataChip(label = "Cook", value = cookTime)
            }

            recipe.metadata.totalTime?.let { totalTime ->
                MetadataChip(label = "Total", value = totalTime)
            }

            MetadataChip(label = "Difficulty", value = recipe.metadata.difficulty)
        }

        // Categories/Tags
        if (recipe.metadata.category.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recipe.metadata.category.forEach { category ->
                    SuggestionChip(
                        onClick = { },
                        label = {
                            Text(
                                text = category,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }
        } else if (recipe.metadata.tags.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recipe.metadata.tags.forEach { tag ->
                    SuggestionChip(
                        onClick = { },
                        label = {
                            Text(
                                text = tag,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * Credits/Metadata Section - Author, source, dates
 */
@Composable
private fun CreditsSection(recipe: Recipe) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Credits",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recipe.metadata.author?.let { author ->
                    CreditsRow(label = "Author", value = author)
                }

                recipe.metadata.source?.let { source ->
                    CreditsRow(label = "Source", value = source)
                }

                CreditsRow(label = "Date Created", value = recipe.metadata.dateCreated.toString())

                CreditsRow(label = "Language", value = recipe.metadata.language)
            }
        }
    }
}

@Composable
private fun CreditsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$label:",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.6f)
        )
    }
}

/**
 * Nutrition Info Section Content
 */
@Composable
private fun NutritionSectionContent(nutrition: Nutrition) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        nutrition.servingSize?.let { servingSize ->
            Text(
                text = "Per serving: $servingSize",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                nutrition.calories?.let { calories ->
                    NutritionRow(label = "Calories", value = calories.toString())
                }

                nutrition.protein?.let { protein ->
                    NutritionRow(label = "Protein", value = "${protein}g")
                }

                nutrition.carbohydrates?.let { carbs ->
                    NutritionRow(label = "Carbohydrates", value = "${carbs}g")
                }

                nutrition.fat?.let { fat ->
                    NutritionRow(label = "Fat", value = "${fat}g")
                }

                nutrition.fiber?.let { fiber ->
                    NutritionRow(label = "Fiber", value = "${fiber}g")
                }

                nutrition.sugar?.let { sugar ->
                    NutritionRow(label = "Sugar", value = "${sugar}g")
                }

                nutrition.sodium?.let { sodium ->
                    NutritionRow(label = "Sodium", value = "${sodium}mg")
                }
            }
        }
    }
}

@Composable
private fun NutritionRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 16.sp
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Storage Section Content
 */
@Composable
private fun StorageSectionContent(storage: Storage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            storage.refrigerator?.let { fridge ->
                StorageRow(icon = "❄️", label = "Refrigerator", value = fridge)
            }

            storage.freezer?.let { freezer ->
                StorageRow(icon = "🧊", label = "Freezer", value = freezer)
            }

            storage.roomTemperature?.let { room ->
                StorageRow(icon = "🌡️", label = "Room Temperature", value = room)
            }
        }
    }
}

@Composable
private fun StorageRow(icon: String, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = icon,
            fontSize = 20.sp
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Equipment Section Content
 */
@Composable
private fun EquipmentSectionContent(equipment: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            equipment.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🔧",
                        fontSize = 16.sp
                    )
                    Text(
                        text = item,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun NotesSectionContent(notes: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Text(
            text = notes,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/**
 * Collapsible section with expand/collapse animation
 */
@Composable
private fun CollapsibleSection(
    title: String,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Header with expand/collapse button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = if (isExpanded) "▲" else "▼",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Collapsible content
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            content()
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading recipe...",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "⚠️",
                fontSize = 64.sp
            )

            Text(
                text = "Couldn't load recipe",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Button(onClick = onRetry) {
                Text("Try Again")
            }
        }
    }
}

/**
 * Offline indicator banner shown when recipe is loaded from cache
 */
@Composable
private fun OfflineIndicatorBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📡",
                fontSize = 20.sp
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Offline Mode",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Viewing cached version",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val parts = dateString.take(10)
        parts.replace("-", "/")
    } catch (e: Exception) {
        dateString
    }
}
