package net.shamansoft.kukbuk.recipe

class YamlRecipeParser {
    
    fun parseRecipeYaml(yamlContent: String, fileId: String, lastModified: String): Recipe? {
        return try {
            val yamlMap = parseSimpleYaml(yamlContent)
            
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
                ingredients = parseStringList(yamlMap["ingredients"]),
                instructions = parseStringList(yamlMap["instructions"] ?: yamlMap["method"]),
                notes = yamlMap["notes"],
                imageUrl = yamlMap["image"] ?: yamlMap["image_url"],
                sourceUrl = yamlMap["source"] ?: yamlMap["source_url"],
                driveFileId = fileId,
                lastModified = lastModified
            )
        } catch (e: Exception) {
            null
        }
    }
    
    fun parseRecipeMetadata(yamlContent: String, fileId: String, lastModified: String, fileName: String): RecipeMetadata? {
        return try {
            val yamlMap = parseSimpleYaml(yamlContent)
            
            RecipeMetadata(
                id = fileId,
                title = yamlMap["title"] ?: extractTitleFromFileName(fileName),
                author = yamlMap["author"],
                description = yamlMap["description"],
                imageUrl = yamlMap["image"] ?: yamlMap["image_url"],
                driveFileId = fileId,
                lastModified = lastModified
            )
        } catch (e: Exception) {
            createFallbackMetadata(fileId, fileName, lastModified)
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

            if (trimmed.startsWith("-")) {
                // Extract the item text after the dash
                val item = trimmed.substring(1).trim()
                if (item.isNotEmpty()) {
                    items.add(item)
                }
            } else if (trimmed.isEmpty()) {
                // Skip empty lines
            } else {
                // Stop when we encounter a line that doesn't start with -
                val currentIndent = line.takeWhile { it == ' ' }.length
                if (currentIndent < baseIndent) {
                    break
                }
            }
            i++
        }

        // Return items as comma-separated string for compatibility with parseStringList
        return items.joinToString(", ") to i - 1
    }
    
    private fun parseStringList(value: String?): List<String> {
        if (value == null) return emptyList()
        
        // Handle simple YAML list formats
        return when {
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