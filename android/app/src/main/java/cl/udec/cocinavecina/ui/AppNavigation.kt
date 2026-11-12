package cl.udec.cocinavecina.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.udec.cocinavecina.AppContainer
import cl.udec.cocinavecina.data.Repository
import cl.udec.cocinavecina.data.session.SessionState
import cl.udec.cocinavecina.ui.common.Loading
import cl.udec.cocinavecina.ui.explore.ExploreScreen
import cl.udec.cocinavecina.ui.login.LoginRoute
import cl.udec.cocinavecina.ui.moderation.ModerationScreen
import cl.udec.cocinavecina.ui.offer.OfferDetailScreen
import cl.udec.cocinavecina.ui.orders.OrderScreen
import cl.udec.cocinavecina.ui.orders.OrdersScreen
import cl.udec.cocinavecina.ui.publish.PublishScreen
import cl.udec.cocinavecina.ui.requests.RequestsScreen
import cl.udec.cocinavecina.ui.session.SignUpScreen

private data class Destination(val route: String, val title: String, val icon: ImageVector)

@Composable
fun AppNavigation(container: AppContainer) {
    val sessionState by container.session.sessionState.collectAsStateWithLifecycle(initialValue = SessionState.Loading)
    when (val session = sessionState) {
        SessionState.Loading -> Loading()
        SessionState.Anonymous -> AnonymousFlow(container.repository)
        is SessionState.Active -> MainScaffold(container, session)
    }
}

@Composable
private fun AnonymousFlow(repository: Repository) {
    var signingUp by rememberSaveable { mutableStateOf(false) }
    if (signingUp) {
        SignUpScreen(repository, onBackToLogin = { signingUp = false })
    } else {
        LoginRoute(
            repository = repository,
            onForgotPasswordClick = {},
            onCreateAccountClick = { signingUp = true },
        )
    }
}

@Composable
private fun MainScaffold(
    container: AppContainer,
    session: SessionState.Active,
) {
    val nav = rememberNavController()
    val destinations =
        buildList {
            add(Destination("explore", "Explorar", Icons.Filled.Search))
            if (session.isSeller) add(Destination("publish", "Publicar", Icons.Filled.Add))
            add(Destination("requests", "Solicitudes", Icons.AutoMirrored.Filled.List))
            add(Destination("orders", "Pedidos", Icons.Filled.ShoppingCart))
            if (session.isModerator) add(Destination("moderation", "Moderación", Icons.Filled.Warning))
        }
    LaunchedEffect(session.userId) { container.events.connect() }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val current by nav.currentBackStackEntryAsState()
                destinations.forEach { d ->
                    NavigationBarItem(
                        selected = current?.destination?.route == d.route,
                        onClick = {
                            nav.navigate(d.route) {
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(d.icon, contentDescription = null) },
                        label = { Text(d.title) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "explore", modifier = Modifier.padding(padding)) {
            composable("explore") {
                ExploreScreen(container.repository, onOpenOffer = { nav.navigate("offer/$it") })
            }
            composable("offer/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                OfferDetailScreen(
                    repository = container.repository,
                    offerId = entry.arguments?.getLong("id") ?: 0L,
                    onSubmitted = { nav.navigate("requests") },
                )
            }
            composable("publish") {
                PublishScreen(container.repository, onPublished = { nav.navigate("requests") })
            }
            composable("requests") {
                RequestsScreen(container, session, onOpenOrder = { nav.navigate("order/$it") })
            }
            composable("orders") {
                OrdersScreen(container.repository, onOpen = { nav.navigate("order/$it") })
            }
            composable("order/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                OrderScreen(container, session, entry.arguments?.getLong("id") ?: 0L)
            }
            composable("moderation") { ModerationScreen(container.repository) }
        }
    }
}
