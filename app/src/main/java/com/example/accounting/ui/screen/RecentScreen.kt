package com.example.accounting.ui.screen

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.accounting.model.PaymentViewType
import com.example.accounting.model.TransactionInfo
import com.example.accounting.ui.navigation.Routes
import com.example.accounting.ui.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecentScreen(
    modifier: Modifier = Modifier,
    viewModel: TransactionViewModel = viewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val uploadingIds by viewModel.uploadingIds.collectAsState()
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.Main.Recent.LIST,
        modifier = modifier
    ) {
        composable(Routes.Main.Recent.LIST) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (transactions.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No transactions yet",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = transactions,
                            key = { it.id }
                        ) { transaction ->
                            TransactionItem(
                                transaction = transaction,
                                isUploading = transaction.id in uploadingIds,
                                onDelete = { viewModel.deleteTransaction(transaction) },
                                onUpload = { viewModel.uploadThenDeleteTransaction(transaction) },
                                onEdit = {
                                    navController.navigate(Routes.Main.Recent.editRoute(transaction.id))
                                },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }

        composable(
            route = Routes.Main.Recent.EDIT,
            deepLinks = listOf(
                navDeepLink { uriPattern = "app://com.example.accounting/recent/{transactionId}" }
            ),
            arguments = listOf(
                navArgument("transactionId") { type = NavType.LongType }
            ),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getLong("transactionId")
            val transactions by viewModel.transactions.collectAsState()
            val transaction = transactions.find { it.id == transactionId }

            transaction?.let { transaction ->
                EditTransactionScreen(
                    transaction = transaction,
                    onDismiss = { updatedTransactionOrNull ->
                        updatedTransactionOrNull?.let { viewModel.updateTransaction(it) }
                        navController.popBackStack()
                    },
                    onUpload = { updatedTransaction ->
                        viewModel.updateTransaction(updatedTransaction)
                        viewModel.uploadThenDeleteTransaction(updatedTransaction)
                        navController.popBackStack()
                    },
                    onDelete = { transactionToDelete ->
                        viewModel.deleteTransaction(transactionToDelete)
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } ?: run {
                // Transaction not found
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Transaction id $transactionId not found",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionItem(
    transaction: TransactionInfo,
    isUploading: Boolean,
    onDelete: () -> Unit,
    onUpload: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        initialValue =
            if (isUploading) SwipeToDismissBoxValue.StartToEnd
            else SwipeToDismissBoxValue.Settled,
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> onDelete()
                SwipeToDismissBoxValue.StartToEnd -> onUpload()
                SwipeToDismissBoxValue.Settled -> {}
            }
            true
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.4f }
    )

    LaunchedEffect(isUploading) {
        if (isUploading && dismissState.currentValue != SwipeToDismissBoxValue.StartToEnd)
            dismissState.dismiss(SwipeToDismissBoxValue.StartToEnd)
        if (!isUploading && dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd)
            dismissState.reset()
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        gesturesEnabled = !isUploading,
        backgroundContent = {
            val roundedShape = RoundedCornerShape(12.dp)    // match Card's rounded corners
            val concentration = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.Settled -> 0.3f
                else -> 0.45f
            }
            val backgroundColor = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Color.Green.copy(alpha = concentration)
                SwipeToDismissBoxValue.EndToStart -> Color.Red.copy(alpha = concentration)
                SwipeToDismissBoxValue.Settled -> Color.Transparent
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(roundedShape)
                    .background(backgroundColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                    Alignment.CenterStart
                } else {
                    Alignment.CenterEnd
                }
            ) {
                when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.StartToEnd -> Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Confirm",
                        tint = Color.White
                    )

                    SwipeToDismissBoxValue.EndToStart -> Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White
                    )

                    SwipeToDismissBoxValue.Settled -> {}
                }
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !isUploading,
                    onClick = onEdit
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatAmount(transaction.amount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatViewType(transaction.viewType),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = formatTime(transaction.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.padding(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = transaction.merchant ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = transaction.paymentMethod ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatAmount(amount: java.math.BigDecimal): String {
    return "¥${amount.toPlainString()}"
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatViewType(viewType: PaymentViewType): String {
    return when (viewType) {
        is PaymentViewType.Success -> "Success"
        is PaymentViewType.Request -> "Request"
        is PaymentViewType.None -> ""
    }
}
