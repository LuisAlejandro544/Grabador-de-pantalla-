package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Rutas de navegación de las pantallas principales de la aplicación.
 * Cada destino cuenta con sus títulos e íconos activos/inactivos para la barra inferior.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Record : Screen(
        route = "record",
        title = "Grabar",
        selectedIcon = Icons.Filled.Videocam,
        unselectedIcon = Icons.Outlined.Videocam
    )

    data object Library : Screen(
        route = "library",
        title = "Grabaciones",
        selectedIcon = Icons.Filled.FolderSpecial,
        unselectedIcon = Icons.Outlined.FolderSpecial
    )

    data object Settings : Screen(
        route = "settings",
        title = "Ajustes",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}
