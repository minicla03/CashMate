package minicla03.cashmate.core.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.graphics.toColorInt
import minicla03.cashmate.R

data class CategoryIconOption(
    val key: String,
    val icon: ImageVector,
    @StringRes val labelResId: Int
)

data class CategoryColorOption(
    val hex: String,
    @StringRes val labelResId: Int
)

object CategoryVisuals {

    val availableIcons: List<CategoryIconOption> = listOf(
        CategoryIconOption("restaurant", Icons.Rounded.Restaurant, R.string.icon_restaurant),
        CategoryIconOption("directions_car", Icons.Rounded.DirectionsCar, R.string.icon_transport),
        CategoryIconOption("home", Icons.Rounded.Home, R.string.icon_home),
        CategoryIconOption("payments", Icons.Rounded.Payments, R.string.icon_payments),
        CategoryIconOption("movie", Icons.Rounded.Movie, R.string.icon_movie),
        CategoryIconOption("medical_services", Icons.Rounded.MedicalServices, R.string.icon_health),
        CategoryIconOption("shopping_bag", Icons.Rounded.ShoppingBag, R.string.icon_shopping),
        CategoryIconOption("flight", Icons.Rounded.Flight, R.string.icon_travel),
        CategoryIconOption("school", Icons.Rounded.School, R.string.icon_education),
        CategoryIconOption("fitness_center", Icons.Rounded.FitnessCenter, R.string.icon_fitness),
        CategoryIconOption("pets", Icons.Rounded.Pets, R.string.icon_pets),
        CategoryIconOption("savings", Icons.Rounded.Savings, R.string.icon_savings),
        CategoryIconOption("work", Icons.Rounded.Work, R.string.icon_work),
        CategoryIconOption("category", Icons.Rounded.Category, R.string.icon_other)
    )

    val availableColors: List<CategoryColorOption> = listOf(
        CategoryColorOption("#5E35B1", R.string.color_purple),
        CategoryColorOption("#00897B", R.string.color_teal),
        CategoryColorOption("#1E88E5", R.string.color_blue),
        CategoryColorOption("#2E7D32", R.string.color_green),
        CategoryColorOption("#FB8C00", R.string.color_orange),
        CategoryColorOption("#E53935", R.string.color_red),
        CategoryColorOption("#8E24AA", R.string.color_plum),
        CategoryColorOption("#546E7A", R.string.color_slate)
    )

    fun iconForKey(key: String): ImageVector =
        availableIcons.firstOrNull { it.key == key }?.icon ?: Icons.Rounded.Category

    fun parseColor(hex: String, fallback: Color = Color(0xFF546E7A)): Color = runCatching {
        Color(hex.toColorInt())
    }.getOrDefault(fallback)
}
