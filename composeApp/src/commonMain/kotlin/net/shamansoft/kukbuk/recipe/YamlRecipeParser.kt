package net.shamansoft.kukbuk.recipe

import net.shamansoft.kukbuk.util.Logger

class YamlRecipeParser {
    
    fun parseRecipeYaml(yamlContent: String, fileId: String, lastModified: String): Recipe? {
        return try {
            val yamlMap = parseSimpleYaml(yamlContent)

            // Parse metadata section if present
            val metadataMap = yamlMap["metadata"]?.let { parseNestedMap(it) } ?: emptyMap()

            // Extract categories from metadata
            val categories = metadataMap["category"]?.let { parseStringList(it) } ?: emptyList()

            Recipe(
                id = fileId,
                title = yamlMap["title"] ?: "Untitled Recipe",
                author = yamlMap["author"],
                description = yamlMap["description"],
                prepTime = yamlMap["prep_time"] ?: yamlMap["prepTime"],
                cookTime = yamlMap["cook_time"] ?: yamlMap["cookTime"],
                totalTime = yamlMap["total_time"] ?: yamlMap["totalTime"],
                servings = yamlMap["servings"] ?: yamlMap["yield"],
                difficulty = yamlMap["difficulty"],
                cuisine = yamlMap["cuisine"],
                tags = parseStringList(yamlMap["tags"]),

                // Structured ingredients and instructions
                ingredients = parseIngredientsList(yamlMap["ingredients"]),
                instructions = parseInstructionsList(yamlMap["instructions"] ?: yamlMap["method"]),

                // Additional metadata
                dateCreated = metadataMap["date_created"],
                categories = categories,
                language = metadataMap["language"],
                coverImage = metadataMap["cover_image"]?.let { parseCoverImage(it) },

                // Additional sections
                equipment = parseStringList(yamlMap["equipment"]),
                nutrition = yamlMap["nutrition"]?.let { parseNutrition(it) },
                storage = yamlMap["storage"]?.let { parseStorage(it) },

                // Version tracking
                schemaVersion = metadataMap["schema_version"],
                recipeVersion = metadataMap["recipe_version"],

                // Other fields
                notes = yamlMap["notes"],
                imageUrl = yamlMap["image"] ?: yamlMap["image_url"] ?: metadataMap["cover_image"]?.let {
                    parseNestedMap(it)["path"]
                },
                sourceUrl = yamlMap["source"] ?: yamlMap["source_url"],
                driveFileId = fileId,
                lastModified = lastModified
            )
        } catch (e: Exception) {
            Logger.d("YamlParser", "Error parsing recipe: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Parse a nested YAML map from a string representation
     */
    private fun parseNestedMap(value: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val lines = value.lines()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            if (trimmed.contains(":")) {
                val parts = trimmed.split(":", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val valuePart = parts[1].trim()
                    result[key] = valuePart.removePrefix("\"").removeSuffix("\"")
                }
            }
        }

        return result
    }

    /**
     * Parse structured ingredients list
     * Supports both simple strings and object format
     */
    private fun parseIngredientsList(value: String?): List<Ingredient> {
        if (value == null) return emptyList()

        val lines = value.lines()
        val ingredients = mutableListOf<Ingredient>()
        var currentComponent: String? = null

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            // Check if this is a component header (no dash, not a key-value pair)
            if (!trimmed.startsWith("-") && !trimmed.contains(":")) {
                currentComponent = trimmed
                continue
            }

            // Simple string format: "- ingredient text"
            if (trimmed.startsWith("-") && !trimmed.contains(":")) {
                val text = trimmed.substring(1).trim()
                // Try to parse amount/unit from text like "2 cups flour"
                val (amount, unit, item) = parseIngredientText(text)
                ingredients.add(
                    Ingredient(
                        item = item,
                        amount = amount,
                        unit = unit,
                        component = currentComponent
                    )
                )
            }
            // Structured object format: "- item: flour\n  amount: 2\n  unit: cups"
            // For now, we'll just use simple format as parsing nested objects is complex
        }

        return ingredients
    }

    /**
     * Parse ingredient text to extract amount, unit, and item
     * E.g., "2 cups flour" -> (2.0, "cups", "flour")
     */
    private fun parseIngredientText(text: String): Triple<Double?, String?, String> {
        val parts = text.split(" ", limit = 3)

        return when {
            parts.size >= 3 -> {
                val amount = parts[0].toDoubleOrNull()
                if (amount != null) {
                    Triple(amount, parts[1], parts.drop(2).joinToString(" "))
                } else {
                    Triple(null, null, text)
                }
            }
            parts.size == 2 -> {
                val amount = parts[0].toDoubleOrNull()
                if (amount != null) {
                    Triple(amount, null, parts[1])
                } else {
                    Triple(null, null, text)
                }
            }
            else -> Triple(null, null, text)
        }
    }

    /**
     * Parse structured instructions list
     * Supports both simple strings and object format
     */
    private fun parseInstructionsList(value: String?): List<Instruction> {
        if (value == null) return emptyList()

        val simpleList = parseStringList(value)
        return simpleList.mapIndexed { index, description ->
            Instruction(
                step = index + 1,
                description = description
            )
        }
    }

    /**
     * Parse cover image metadata
     */
    private fun parseCoverImage(value: String): CoverImage? {
        return try {
            val map = parseNestedMap(value)
            val path = map["path"] ?: return null

            CoverImage(
                path = path,
                alt = map["alt"],
                width = map["width"]?.toIntOrNull(),
                height = map["height"]?.toIntOrNull()
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse nutrition information
     */
    private fun parseNutrition(value: String): NutritionInfo? {
        return try {
            val map = parseNestedMap(value)

            NutritionInfo(
                servingSize = map["serving_size"] ?: map["servingSize"],
                calories = map["calories"]?.toIntOrNull(),
                protein = map["protein"],
                carbs = map["carbs"] ?: map["carbohydrates"],
                fat = map["fat"],
                fiber = map["fiber"],
                sugar = map["sugar"],
                sodium = map["sodium"]
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse storage information
     */
    private fun parseStorage(value: String): StorageInfo? {
        return try {
            val map = parseNestedMap(value)

            StorageInfo(
                refrigerator = map["refrigerator"] ?: map["fridge"],
                freezer = map["freezer"],
                roomTemperature = map["room_temperature"] ?: map["roomTemperature"] ?: map["counter"]
            )
        } catch (e: Exception) {
            null
        }
    }
    
    fun parseRecipeMetadata(yamlContent: String, fileId: String, lastModified: String, fileName: String): RecipeMetadata? {
        Logger.d("YamlParser", "parseRecipeMetadata() called for file: $fileName")
        return try {
            Logger.d("YamlParser", "Parsing YAML content (${yamlContent.length} chars)")
            val yamlMap = parseSimpleYaml(yamlContent)
            Logger.d("YamlParser", "Parsed ${yamlMap.size} fields from YAML")

            // Parse metadata section if present
            val metadataMap = yamlMap["metadata"]?.let { parseNestedMap(it) } ?: emptyMap()

            // Extract categories from metadata
            val categories = metadataMap["category"]?.let { parseStringList(it) } ?: emptyList()

            val metadata = RecipeMetadata(
                id = fileId,
                title = yamlMap["title"] ?: extractTitleFromFileName(fileName),
                author = yamlMap["author"],
                description = yamlMap["description"],
                imageUrl = yamlMap["image"] ?: yamlMap["image_url"] ?: metadataMap["cover_image"]?.let {
                    parseNestedMap(it)["path"]
                },
                categories = categories,
                dateCreated = metadataMap["date_created"],
                driveFileId = fileId,
                lastModified = lastModified
            )
            Logger.d("YamlParser", "Successfully created metadata: ${metadata.title}")
            metadata
        } catch (e: Exception) {
            Logger.d("YamlParser", "Exception parsing metadata for $fileName: ${e.message}")
            e.printStackTrace()
            val fallback = createFallbackMetadata(fileId, fileName, lastModified)
            Logger.d("YamlParser", "Created fallback metadata: ${fallback.title}")
            fallback
        }
    }
    
    private fun parseSimpleYaml(yamlContent: String): Map<String, String?> {
        val result = mutableMapOf<String, String?>()
        val lines = yamlContent.lines()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            val trimmedLine = line.trim()

            // Skip empty lines and comments
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                i++
                continue
            }

            // Check if this is a key-value pair
            if (trimmedLine.contains(":")) {
                val parts = trimmedLine.split(":", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val valuePart = parts[1].trim()

                    // Handle different value formats
                    val value = when {
                        // Multi-line string starting with |
                        valuePart == "|" || valuePart == "|-" -> {
                            i++
                            parseMultilineString(lines, i).also { (content, newIndex) ->
                                result[key] = content
                                i = newIndex
                            }
                            continue
                        }
                        // List starting with - on next line
                        valuePart.isEmpty() && i + 1 < lines.size && lines[i + 1].trim().startsWith("-") -> {
                            i++
                            parseYamlList(lines, i).also { (list, newIndex) ->
                                result[key] = list
                                i = newIndex
                            }
                            continue
                        }
                        // Regular value
                        else -> valuePart.removePrefix("\"").removeSuffix("\"").trim()
                    }

                    result[key] = if (value.isNotEmpty()) value else null
                }
            }
            i++
        }

        return result
    }

    private fun parseMultilineString(lines: List<String>, startIndex: Int): Pair<String?, Int> {
        val content = mutableListOf<String>()
        var i = startIndex
        val baseIndent = if (i < lines.size) {
            lines[i].takeWhile { it == ' ' }.length
        } else 0

        while (i < lines.size) {
            val line = lines[i]
            val currentIndent = line.takeWhile { it == ' ' }.length

            // Stop when we reach a line with equal or less indentation that's not empty
            if (line.isNotBlank() && currentIndent < baseIndent) {
                break
            }

            if (line.isNotBlank()) {
                content.add(line.substring(minOf(baseIndent, line.length)))
            } else {
                content.add("")
            }
            i++
        }

        return (if (content.isNotEmpty()) content.joinToString("\n") else null) to i - 1
    }

    private fun parseYamlList(lines: List<String>, startIndex: Int): Pair<String, Int> {
        val items = mutableListOf<String>()
        var i = startIndex
        val baseIndent = if (i < lines.size) {
            lines[i].takeWhile { it == ' ' }.length
        } else 0

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()
            val currentIndent = line.takeWhile { it == ' ' }.length

            if (trimmed.startsWith("-")) {
                // Extract the item text after the dash
                val itemFirstLine = trimmed.substring(1).trim()
                val itemLines = mutableListOf<String>()

                if (itemFirstLine.isNotEmpty()) {
                    itemLines.add(itemFirstLine)
                }

                // Check for continuation lines (indented more than the dash)
                i++
                while (i < lines.size) {
                    val nextLine = lines[i]
                    val nextTrimmed = nextLine.trim()
                    val nextIndent = nextLine.takeWhile { it == ' ' }.length

                    // If next line starts with dash or has equal/less indentation, it's a new item
                    if (nextTrimmed.startsWith("-") || (nextTrimmed.isNotEmpty() && nextIndent <= baseIndent)) {
                        break
                    }

                    // If it's indented more and not empty, it's part of this item
                    if (nextTrimmed.isNotEmpty() && nextIndent > baseIndent) {
                        itemLines.add(nextTrimmed)
                        i++
                    } else if (nextTrimmed.isEmpty()) {
                        // Empty line - might be part of multiline, add it
                        if (itemLines.isNotEmpty()) {
                            itemLines.add("")
                        }
                        i++
                    } else {
                        break
                    }
                }

                if (itemLines.isNotEmpty()) {
                    items.add(itemLines.joinToString(" ").trim())
                }
                // Don't increment i here, we already did it in the inner loop
                continue
            } else if (trimmed.isEmpty()) {
                // Skip empty lines
                i++
            } else {
                // Stop when we encounter a line that doesn't start with - and has less indentation
                if (currentIndent < baseIndent) {
                    break
                }
                i++
            }
        }

        Logger.d("YamlParser", "Parsed ${items.size} list items: ${items.take(2).joinToString(", ")}")

        // Return items as comma-separated string for compatibility with parseStringList
        return items.joinToString("|||") to i - 1
    }
    
    private fun parseStringList(value: String?): List<String> {
        if (value == null) return emptyList()

        // Handle simple YAML list formats
        return when {
            value.contains("|||") -> {
                // Our multiline list separator
                value.split("|||").map { it.trim() }.filter { it.isNotEmpty() }
            }
            value.startsWith("[") && value.endsWith("]") -> {
                // JSON-style array: [item1, item2, item3]
                value.substring(1, value.length - 1)
                    .split(",")
                    .map { it.trim().removePrefix("\"").removeSuffix("\"") }
                    .filter { it.isNotEmpty() }
            }
            value.contains(",") -> {
                // Comma-separated: item1, item2, item3
                value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }
            else -> {
                // Single item
                listOf(value)
            }
        }
    }
    
    private fun createFallbackMetadata(fileId: String, fileName: String, lastModified: String): RecipeMetadata {
        return RecipeMetadata(
            id = fileId,
            title = extractTitleFromFileName(fileName),
            author = null,
            description = null,
            imageUrl = null,
            driveFileId = fileId,
            lastModified = lastModified
        )
    }
    
    private fun extractTitleFromFileName(fileName: String): String {
        return fileName
            .removeSuffix(".yaml")
            .removeSuffix(".yml")
            .replace("_", " ")
            .replace("-", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }
}
