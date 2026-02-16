package net.luis.tracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import net.luis.tracker.R

data class BottomNavItem(
	val route: Any,
	val labelResId: Int,
	val icon: ImageVector
)

val bottomNavItems = listOf(
	BottomNavItem(ExercisesRoute, R.string.nav_exercises, Icons.Default.FitnessCenter),
	BottomNavItem(OverviewRoute, R.string.nav_overview, Icons.Default.Dashboard),
	BottomNavItem(WorkoutsRoute, R.string.nav_workouts, Icons.Default.History)
)

@Composable
fun BottomNavBar(
	selectedIndex: Int,
	onTabSelected: (Int) -> Unit
) {
	NavigationBar {
		bottomNavItems.forEachIndexed { index, item ->
			NavigationBarItem(
				selected = selectedIndex == index,
				onClick = { onTabSelected(index) },
				icon = { Icon(item.icon, contentDescription = stringResource(item.labelResId)) },
				label = { Text(stringResource(item.labelResId)) }
			)
		}
	}
}
