# Recipe SDK (Kotlin Multiplatform)

A Kotlin Multiplatform library for parsing and serializing recipe YAML files according to the recipe schema specification.

## Platforms

- ✅ **Android** (API 24+)
- ✅ **JVM**
- ⚠️ **iOS** and **WASM** - Not currently enabled (kaml library JVM/Android only)

## Features

- **Model classes**: Kotlin data classes with kotlinx.serialization
- **YAML parsing**: Parse YAML files/strings into Recipe objects
- **YAML serialization**: Serialize Recipe objects to YAML format
- **Multiplatform**: Works on Android, iOS, WASM, and JVM
- **Type-safe**: Full Kotlin type safety with nullable fields where appropriate

## Dependencies

- `kotlinx.serialization` (1.8.0) - Serialization framework
- `kotlinx.datetime` (0.6.1) - Date handling
- `kaml` (0.56.0) - YAML support for kotlinx.serialization

## Usage

### Parsing YAML to Recipe

```kotlin
import net.shamansoft.recipe.parser.RecipeYaml
import net.shamansoft.recipe.model.Recipe

// From String
val yamlString = """
  schema_version: "1.0.0"
  recipe_version: "1.0.0"
  metadata:
    title: "Chocolate Chip Cookies"
    source: "https://example.com/recipe"
    date_created: "2025-01-15"
    servings: 12
  ingredients:
    - item: "flour"
      amount: "2"
      unit: "cups"
  instructions:
    - step: 1
      description: "Mix ingredients"
"""

val recipe: Recipe = RecipeYaml.parse(yamlString)
println("Recipe: ${recipe.metadata.title}")
```

### Serializing Recipe to YAML

```kotlin
import net.shamansoft.recipe.parser.RecipeYaml
import net.shamansoft.recipe.model.*
import kotlinx.datetime.LocalDate

val recipe = Recipe(
    schemaVersion = "1.0.0",
    recipeVersion = "1.0.0",
    metadata = RecipeMetadata(
        title = "Chocolate Chip Cookies",
        source = "https://example.com/recipe",
        dateCreated = LocalDate(2025, 1, 15),
        servings = 12,
        prepTime = "15m",
        cookTime = "12m",
        difficulty = "easy"
    ),
    ingredients = listOf(
        Ingredient(item = "flour", amount = "2", unit = "cups"),
        Ingredient(item = "chocolate chips", amount = "1", unit = "cup")
    ),
    instructions = listOf(
        Instruction(step = 1, description = "Mix dry ingredients"),
        Instruction(step = 2, description = "Bake at 350°F", time = "12m", temperature = "350°F")
    )
)

val yaml: String = RecipeYaml.serialize(recipe)
println(yaml)
```

### Building Recipe Objects

```kotlin
import net.shamansoft.recipe.model.*
import kotlinx.datetime.LocalDate

val recipe = Recipe(
    schemaVersion = "1.0.0",
    recipeVersion = "1.0.0",
    metadata = RecipeMetadata(
        title = "Chocolate Chip Cookies",
        source = "https://example.com/recipe",
        author = "John Doe",
        language = "en",
        dateCreated = LocalDate(2025, 1, 15),
        category = listOf("dessert"),
        tags = listOf("cookies", "chocolate"),
        servings = 12,
        prepTime = "15m",
        cookTime = "12m",
        totalTime = "27m",
        difficulty = "easy",
        coverImage = CoverImage(
            path = "/images/cookies.jpg",
            alt = "Delicious cookies"
        )
    ),
    description = "Delicious homemade chocolate chip cookies",
    ingredients = listOf(
        Ingredient(
            item = "flour",
            amount = "2",
            unit = "cups",
            component = "main"
        ),
        Ingredient(
            item = "butter",
            amount = "1",
            unit = "cup",
            substitutions = listOf(
                Substitution(item = "margarine", ratio = "1:1")
            )
        ),
        Ingredient(
            item = "chocolate chips",
            amount = "1",
            unit = "cup"
        )
    ),
    equipment = listOf("mixing bowl", "baking sheet"),
    instructions = listOf(
        Instruction(
            step = 1,
            description = "Mix dry ingredients in a bowl",
            time = "5m"
        ),
        Instruction(
            step = 2,
            description = "Add wet ingredients and mix well",
            time = "5m"
        ),
        Instruction(
            step = 3,
            description = "Bake at 350°F until golden brown",
            time = "12m",
            temperature = "350°F"
        )
    ),
    nutrition = Nutrition(
        servingSize = "1 cookie",
        calories = 150,
        protein = 2.0,
        carbohydrates = 20.0,
        fat = 7.0
    ),
    notes = "Store in airtight container for up to 5 days",
    storage = Storage(
        roomTemperature = "5 days",
        freezer = "3 months"
    )
)
```

## Model Classes

- `Recipe` - Main recipe container
- `RecipeMetadata` - Title, author, source, servings, timing, etc.
- `Ingredient` - Ingredient with optional substitutions and component grouping
- `Instruction` - Step-by-step instructions with optional media
- `Media` - Single media type supporting both images and videos (uses `type` field: "image" or "video")
- `Nutrition` - Nutritional information
- `Storage` - Storage instructions
- `CoverImage` - Cover image metadata

## Adding to Your Project

### Using `gradle/libs.versions.toml`

```toml
[versions]
recipe-sdk = "1.0.0"

[libraries]
recipe-sdk = { module = "net.shamansoft:recipe-sdk", version.ref = "recipe-sdk" }
```

### In your module's `build.gradle.kts`

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":recipe-sdk"))
            // or if published:
            // implementation(libs.recipe.sdk)
        }
    }
}
```

## Testing

The library includes comprehensive tests that run on all platforms:

```bash
./gradlew :recipe-sdk:allTests
```

## License

Same as parent project
