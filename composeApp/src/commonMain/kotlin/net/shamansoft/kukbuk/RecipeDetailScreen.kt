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
        // Recipe Header
        item {
            RecipeHeader(recipe)
        }

        // Ingredients Section
        if (recipe.ingredients.isNotEmpty()) {
            item {
                IngredientsSection(ingredients = recipe.ingredients)
            }
        }

        // Instructions Section
        if (recipe.instructions.isNotEmpty()) {
            item {
                InstructionsSection(instructions = recipe.instructions)
            }
        }

        // Notes Section
        recipe.notes?.let { notes ->
            if (notes.isNotBlank()) {
                item {
                    NotesSection(notes = notes)
                }
            }
        }

        // Metadata Section
        item {
            MetadataSection(recipe)
        }
    }
}

@Composable
private fun RecipeHeader(recipe: Recipe) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Hero Image Placeholder
        RecipeImage(
            url = recipe.imageUrl,
            contentDescription = "Recipe: ${recipe.title}",
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            cornerRadius = 12.dp
        )

        // Title
        Text(
            text = recipe.title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 36.sp
        )

        // Author
        recipe.author?.let { author ->
            Text(
                text = "by $author",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Description
        recipe.description?.let { description ->
            Text(
                text = description,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Metadata Chips
        MetadataChips(recipe)

        // Tags
        if (recipe.tags.isNotEmpty()) {
            TagsRow(tags = recipe.tags)
        }
    }
}

@Composable
private fun MetadataChips(recipe: Recipe) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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

        recipe.servings?.let { servings ->
            MetadataChip(label = "Servings", value = servings)
        }

        recipe.difficulty?.let { difficulty ->
            MetadataChip(label = "Difficulty", value = difficulty)
        }

        recipe.cuisine?.let { cuisine ->
            MetadataChip(label = "Cuisine", value = cuisine)
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

@Composable
private fun TagsRow(tags: List<String>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
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

@Composable
private fun IngredientsSection(ingredients: List<String>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Section Header
        Text(
            text = "Ingredients",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Ingredients List
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "•",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = ingredient,
                            fontSize = 18.sp,
                            lineHeight = 28.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InstructionsSection(instructions: List<String>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Section Header
        Text(
            text = "Instructions",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Instructions List
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            instructions.forEachIndexed { index, instruction ->
                InstructionStep(
                    stepNumber = index + 1,
                    instruction = instruction
                )
            }
        }
    }
}

@Composable
private fun InstructionStep(stepNumber: Int, instruction: String) {
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
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            // Instruction Text
            Text(
                text = instruction,
                fontSize = 18.sp,
                lineHeight = 28.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NotesSection(notes: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
private fun MetadataSection(recipe: Recipe) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        recipe.sourceUrl?.let { sourceUrl ->
            Text(
                text = "Source: $sourceUrl",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }

        recipe.lastModified?.let { lastModified ->
            Text(
                text = "Last updated: ${formatDate(lastModified)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
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
