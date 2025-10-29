package net.shamansoft.recipe.parser

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import net.shamansoft.recipe.model.Recipe

/**
 * YAML parser and serializer for Recipe objects.
 * Uses kotlinx.serialization with kaml for multiplatform YAML support.
 */
object RecipeYaml {

    /**
     * Default YAML configuration with strict mode disabled
     * to handle optional fields gracefully.
     */
    private val yaml = Yaml(
        configuration = YamlConfiguration(
            strictMode = false,
            encodeDefaults = false
        )
    )

    /**
     * Parses a YAML string into a Recipe object.
     *
     * @param yamlString the YAML string to parse
     * @return the parsed Recipe object
     * @throws RecipeParseException if parsing fails
     */
    fun parse(yamlString: String): Recipe {
        return try {
            yaml.decodeFromString(Recipe.serializer(), yamlString)
        } catch (e: Exception) {
            throw RecipeParseException("Failed to parse YAML content: ${e.message}", e)
        }
    }

    /**
     * Serializes a Recipe object to a YAML string.
     *
     * @param recipe the Recipe to serialize
     * @return YAML string representation
     * @throws RecipeSerializeException if serialization fails
     */
    fun serialize(recipe: Recipe): String {
        return try {
            yaml.encodeToString(Recipe.serializer(), recipe)
        } catch (e: Exception) {
            throw RecipeSerializeException("Failed to serialize recipe to YAML: ${e.message}", e)
        }
    }
}

/**
 * Exception thrown when recipe parsing fails.
 */
class RecipeParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when recipe serialization fails.
 */
class RecipeSerializeException(message: String, cause: Throwable? = null) : Exception(message, cause)
