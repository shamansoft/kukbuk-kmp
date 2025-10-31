package net.shamansoft.recipe.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a recipe ingredient.
 *
 * @property item Name of the ingredient (required)
 * @property amount Amount of the ingredient (can be numeric like "2" or descriptive like "handful", "3-4")
 * @property unit Unit of measurement
 * @property notes Additional notes (preparation details, alternatives, "to taste", etc.)
 * @property optional Whether this ingredient is optional (e.g., garnishes)
 * @property substitutions List of possible substitutions for this ingredient
 * @property component Grouping identifier (e.g., "dough", "filling", "sauce", "main")
 */
@Serializable
data class Ingredient(
    @SerialName("item")
    val item: String,

    @SerialName("amount")
    val amount: String? = null,

    @SerialName("unit")
    val unit: String? = null,

    @SerialName("notes")
    val notes: String? = null,

    @SerialName("optional")
    val optional: Boolean = false,

    @SerialName("substitutions")
    val substitutions: List<Substitution>? = null,

    @SerialName("component")
    val component: String = "main"
)

/**
 * Represents a grouped set of ingredients with a component label.
 * Used for YAML formats that group ingredients by component (e.g., "dough", "filling", "sauce").
 *
 * @property component The component name (e.g., "Main", "Dressing", "Sauce")
 * @property items List of ingredients in this component group
 */
@Serializable
data class IngredientGroup(
    @SerialName("component")
    val component: String,

    @SerialName("items")
    val items: List<Ingredient>
) {
    /**
     * Converts this group to a flat list of ingredients with the component set.
     */
    fun toIngredientList(): List<Ingredient> {
        return items.map { it.copy(component = component) }
    }
}
