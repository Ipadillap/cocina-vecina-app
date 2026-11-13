package cl.udec.cocinavecina.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
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
import cl.udec.cocinavecina.ui.components.BackTopBar
import cl.udec.cocinavecina.ui.explore.ExploreRoute
import cl.udec.cocinavecina.ui.login.LoginRoute
import cl.udec.cocinavecina.ui.moderation.ModerationScreen
import cl.udec.cocinavecina.ui.offer.OfferDetailRoute
import cl.udec.cocinavecina.ui.offer.ProposalTarget
import cl.udec.cocinavecina.ui.offer.ProposePriceRoute
import cl.udec.cocinavecina.ui.orders.OrderScreen
import cl.udec.cocinavecina.ui.orders.OrdersScreen
import cl.udec.cocinavecina.ui.publish.PublishScreen
import cl.udec.cocinavecina.ui.requests.RequestTrackingRoute
import cl.udec.cocinavecina.ui.requests.RequestsScreen
import cl.udec.cocinavecina.ui.signup.SignUpRoute

private const val NO_DISTANCE = -1L
private const val START_ROUTE = "explore"

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
        SignUpRoute(
            repository = repository,
            onTermsClick = {},
            onPrivacyClick = {},
            onLoginClick = { signingUp = false },
        )
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
    val currentEntry by nav.currentBackStackEntryAsState()
    val selectedTab =
        remember(currentEntry, destinations) {
            destinations.firstOrNull { it.route != START_ROUTE && nav.hasInBackStack(it.route) }?.route ?: START_ROUTE
        }

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { d ->
                    NavigationBarItem(
                        selected = selectedTab == d.route,
                        onClick = {
                            if (d.route == selectedTab) {
                                nav.popBackStack(d.route, inclusive = false)
                            } else {
                                nav.openTab(d.route)
                            }
                        },
                        icon = { Icon(d.icon, contentDescription = null) },
                        label = { Text(d.title) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = START_ROUTE, modifier = Modifier.padding(padding)) {
            composable(START_ROUTE) {
                ExploreRoute(
                    repository = container.repository,
                    onOfferClick = { offerId, distanceMeters -> nav.navigate("offer/$offerId?distance=${distanceMeters ?: NO_DISTANCE}") },
                )
            }
            composable(
                "offer/{id}?distance={distance}",
                arguments =
                    listOf(
                        navArgument("id") { type = NavType.LongType },
                        navArgument("distance") {
                            type = NavType.LongType
                            defaultValue = NO_DISTANCE
                        },
                    ),
            ) { entry ->
                OfferDetailRoute(
                    repository = container.repository,
                    offerId = entry.arguments?.getLong("id") ?: 0L,
                    distanceMeters = entry.arguments?.getLong("distance")?.takeIf { it != NO_DISTANCE },
                    onBack = { nav.popBackStack() },
                    onProposePriceClick = { offerId, portions -> nav.navigate("offer/$offerId/proposal?portions=$portions") },
                    onPurchaseRequested = { requestId -> nav.openRequestTracking(requestId) },
                )
            }
            composable(
                "offer/{id}/proposal?portions={portions}",
                arguments =
                    listOf(
                        navArgument("id") { type = NavType.LongType },
                        navArgument("portions") {
                            type = NavType.IntType
                            defaultValue = 1
                        },
                    ),
            ) { entry ->
                ProposePriceRoute(
                    repository = container.repository,
                    target =
                        ProposalTarget.NewRequest(
                            offerId = entry.arguments?.getLong("id") ?: 0L,
                            initialPortions = entry.arguments?.getInt("portions") ?: 1,
                        ),
                    onBack = { nav.popBackStack() },
                    onRequestCompleted = { requestId -> nav.openRequestTracking(requestId) },
                )
            }
            composable("request/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val requestId = entry.arguments?.getLong("id") ?: 0L
                RequestTrackingRoute(
                    repository = container.repository,
                    events = container.events.events,
                    requestId = requestId,
                    onBack = { nav.popBackStack() },
                    onProposeAgain = { nav.navigate("request/$it/proposal") },
                    onOpenOrder = { orderId ->
                        nav.navigate("order/$orderId") {
                            popUpTo("request/{id}") { inclusive = true }
                        }
                    },
                )
            }
            composable("request/{id}/proposal", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                ProposePriceRoute(
                    repository = container.repository,
                    target = ProposalTarget.ExistingRequest(entry.arguments?.getLong("id") ?: 0L),
                    onBack = { nav.popBackStack() },
                    onRequestCompleted = { nav.popBackStack() },
                )
            }
            composable("publish") {
                TabRootScreen("Publicar", onBack = { nav.openTab(START_ROUTE) }) {
                    PublishScreen(container.repository, onPublished = { nav.finishFlowInTab("requests") })
                }
            }
            composable("requests") {
                TabRootScreen("Solicitudes", onBack = { nav.openTab(START_ROUTE) }) {
                    RequestsScreen(
                        container = container,
                        session = session,
                        onOpenOrder = { nav.navigate("order/$it") },
                        onOpenRequest = { nav.navigate("request/$it") },
                    )
                }
            }
            composable("orders") {
                TabRootScreen("Pedidos", onBack = { nav.openTab(START_ROUTE) }) {
                    OrdersScreen(container.repository, onOpen = { nav.navigate("order/$it") })
                }
            }
            composable("order/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                OrderScreen(
                    container = container,
                    session = session,
                    orderId = entry.arguments?.getLong("id") ?: 0L,
                    onBack = { nav.popBackStack() },
                )
            }
            composable("moderation") {
                TabRootScreen("Moderación", onBack = { nav.openTab(START_ROUTE) }) {
                    ModerationScreen(container.repository)
                }
            }
        }
    }
}

@Composable
private fun TabRootScreen(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        BackTopBar(title, onBack)
        Box(Modifier.weight(1f)) { content() }
    }
}

private fun NavHostController.hasInBackStack(route: String): Boolean = runCatching { getBackStackEntry(route) }.isSuccess

private fun NavHostController.openTab(route: String) {
    if (route == START_ROUTE) {
        popBackStack(START_ROUTE, inclusive = false)
        return
    }
    navigate(route) {
        popUpTo(START_ROUTE) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.openRequestTracking(requestId: Long) {
    finishFlowInTab("requests")
    navigate("request/$requestId")
}

private fun NavHostController.finishFlowInTab(route: String) {
    navigate(route) {
        popUpTo(START_ROUTE)
        launchSingleTop = true
    }
}
