package net.shamansoft.recipe.serialization

import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import net.shamansoft.recipe.model.Ingredient
import net.shamansoft.recipe.model.IngredientGroup

/**
 * Custom serializer that handles both flat and grouped ingredient structures.
 *
 * Supports two YAML formats:
 * 1. Flat format: List<Ingredient>
 *    ```yaml
 *    ingredients:
 *      - item: "onion"
 *        amount: 1
 *    ```
 *
 * 2. Grouped format: List<IngredientGroup>
 *    ```yaml
 *    ingredients:
 *      - component: "Main"
 *        items:
 *          - item: "onion"
 *            amount: 1
 *    ```
 *
 * The grouped format is automatically flattened to a single list,
 * with each ingredient's `component` field set to the group name.
 */
object FlexibleIngredientListSerializer : KSerializer<List<Ingredient>> {

    private val surrogateListSerializer = ListSerializer(IngredientEntry.serializer())
    private val ingredientListSerializer = ListSerializer(Ingredient.serializer())

    override val descriptor: SerialDescriptor = surrogateListSerializer.descriptor

    override fun serialize(encoder: Encoder, value: List<Ingredient>) {
        // Always serialize as flat list
        ingredientListSerializer.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): List<Ingredient> {
        // Deserialize as list of IngredientEntry (which can be either format)
        val entries = surrogateListSerializer.deserialize(decoder)

        // Flatten the result
        return entries.flatMap { entry ->
            when {
                // Grouped format: has items field
                entry.items != null -> {
                    val component = entry.component ?: "main"
                    entry.items.map { it.copy(component = component) }
                }
                // Flat format: has item field
                entry.item != null -> {
                    listOf(
                        Ingredient(
                            item = entry.item,
                            amount = entry.amount,
                            unit = entry.unit,
                            notes = entry.notes,
                            optional = entry.optional ?: false,
                            substitutions = entry.substitutions,
                            component = entry.component ?: "main"
                        )
                    )
                }
                else -> emptyList()
            }
        }
    }
}

/**
 * Surrogate class that represents either format.
 * Fields from both Ingredient and IngredientGroup are optional.
 */
@Serializable
private data class IngredientEntry(
    // Fields from Ingredient (flat format)
    @SerialName("item")
    val item: String? = null,
    @SerialName("amount")
    val amount: String? = null,
    @SerialName("unit")
    val unit: String? = null,
    @SerialName("notes")
    val notes: String? = null,
    @SerialName("optional")
    val optional: Boolean? = null,
    @SerialName("substitutions")
    val substitutions: List<net.shamansoft.recipe.model.Substitution>? = null,

    // Fields from both formats
    @SerialName("component")
    val component: String? = null,

    // Field from IngredientGroup (grouped format)
    @SerialName("items")
    val items: List<Ingredient>? = null
)
