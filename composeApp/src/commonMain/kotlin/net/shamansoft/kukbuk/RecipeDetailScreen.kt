package net.shamansoft.kukbuk

import androidx.compose.foundation.background
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
import net.shamansoft.kukbuk.recipe.Recipe
import net.shamansoft.kukbuk.recipe.RecipeDetailState
import net.shamansoft.kukbuk.recipe.RecipeDetailViewModel
import net.shamansoft.kukbuk.util.RecipeImage

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
                    RecipeContent(recipe = state.recipe)
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
private fun RecipeContent(recipe: Recipe) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
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

        // 5. Nutrition Info Section (if available)
        recipe.nutrition?.let { nutrition ->
            item {
                NutritionSection(nutrition = nutrition)
            }
        }

        // 6. Storage Instructions (if available)
        recipe.storage?.let { storage ->
            item {
                StorageSection(storage = storage)
            }
        }

        // 7. Equipment List (if available)
        if (recipe.equipment.isNotEmpty()) {
            item {
                EquipmentSection(equipment = recipe.equipment)
            }
        }

        // 8. Notes Section
        recipe.notes?.let { notes ->
            if (notes.isNotBlank()) {
                item {
                    NotesSection(notes = notes)
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

        // Servings info (if available)
        recipe.servings?.let { servings ->
            Text(
                text = "Servings: $servings",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

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
private fun IngredientRow(ingredient: net.shamansoft.kukbuk.recipe.Ingredient) {
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
                text = ingredient.toDisplayString(),
                fontSize = 18.sp,
                lineHeight = 28.sp
            )

            // Show substitutions if available
            if (ingredient.substitutions.isNotEmpty()) {
                Text(
                    text = "Alternative: ${ingredient.substitutions.first().item}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

/**
 * Steps Section - Primary cooking instructions
 */
@Composable
private fun StepsSection(instructions: List<net.shamansoft.kukbuk.recipe.Instruction>) {
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
        instructions.forEach { instruction ->
            InstructionStep(instruction = instruction)
        }
    }
}

/**
 * Instruction step with time and temperature
 */
@Composable
private fun InstructionStep(instruction: net.shamansoft.kukbuk.recipe.Instruction) {
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
                    text = instruction.step.toString(),
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
            url = recipe.coverImage?.path ?: recipe.imageUrl,
            contentDescription = "Recipe: ${recipe.title}",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            cornerRadius = 12.dp
        )

        // Title
        Text(
            text = recipe.title,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 40.sp
        )

        // Description
        recipe.description?.let { description ->
            Text(
                text = description,
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
            recipe.prepTime?.let { prepTime ->
                MetadataChip(label = "Prep", value = prepTime)
            }

            recipe.cookTime?.let { cookTime ->
                MetadataChip(label = "Cook", value = cookTime)
            }

            recipe.totalTime?.let { totalTime ->
                MetadataChip(label = "Total", value = totalTime)
            }

            recipe.difficulty?.let { difficulty ->
                MetadataChip(label = "Difficulty", value = difficulty)
            }

            recipe.cuisine?.let { cuisine ->
                MetadataChip(label = "Cuisine", value = cuisine)
            }
        }

        // Categories/Tags
        if (recipe.categories.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recipe.categories.forEach { category ->
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
        } else if (recipe.tags.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recipe.tags.forEach { tag ->
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
                recipe.author?.let { author ->
                    CreditsRow(label = "Author", value = author)
                }

                recipe.sourceUrl?.let { sourceUrl ->
                    CreditsRow(label = "Source", value = sourceUrl)
                }

                recipe.dateCreated?.let { dateCreated ->
                    CreditsRow(label = "Date Created", value = dateCreated)
                }

                recipe.lastModified?.let { lastModified ->
                    CreditsRow(label = "Last Modified", value = formatDate(lastModified))
                }

                recipe.language?.let { language ->
                    CreditsRow(label = "Language", value = language)
                }
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
 * Nutrition Info Section
 */
@Composable
private fun NutritionSection(nutrition: net.shamansoft.kukbuk.recipe.NutritionInfo) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Nutrition Information",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

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
                    NutritionRow(label = "Protein", value = protein)
                }

                nutrition.carbs?.let { carbs ->
                    NutritionRow(label = "Carbohydrates", value = carbs)
                }

                nutrition.fat?.let { fat ->
                    NutritionRow(label = "Fat", value = fat)
                }

                nutrition.fiber?.let { fiber ->
                    NutritionRow(label = "Fiber", value = fiber)
                }

                nutrition.sugar?.let { sugar ->
                    NutritionRow(label = "Sugar", value = sugar)
                }

                nutrition.sodium?.let { sodium ->
                    NutritionRow(label = "Sodium", value = sodium)
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
 * Storage Section
 */
@Composable
private fun StorageSection(storage: net.shamansoft.kukbuk.recipe.StorageInfo) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Storage Instructions",
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
 * Equipment Section
 */
@Composable
private fun EquipmentSection(equipment: List<String>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Equipment Needed",
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
}

@Composable
private fun NotesSection(notes: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Notes",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

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

private fun formatDate(dateString: String): String {
    return try {
        val parts = dateString.take(10)
        parts.replace("-", "/")
    } catch (e: Exception) {
        dateString
    }
}
