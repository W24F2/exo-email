package com.example.email_client.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.email_client.LiquidMailApp
import com.example.email_client.data.db.AccountEntity
import com.example.email_client.ui.components.GlassCard
import com.example.email_client.ui.components.GlassTopBar
import com.example.email_client.ui.components.LiquidBackdrop
import com.example.email_client.ui.theme.TextSecondary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Composable
fun AccountsScreen(app: LiquidMailApp, onBack: () -> Unit, onAddAccount: () -> Unit) {
    val vm: AccountsViewModel = viewModel(factory = AccountsViewModelFactory(app))
    val accounts by vm.accounts.collectAsState()

    LiquidBackdrop {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = { GlassTopBar(title = "Accounts") },
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                    items(accounts, key = { it.id }) { a ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { vm.switchTo(a.id) },
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(a.displayName, style = MaterialTheme.typography.titleMedium)
                                Text(a.email, color = TextSecondary)
                            }
                        }
                    }
                }
                Button(onClick = onAddAccount, modifier = Modifier.fillMaxWidth()) { Text("Add account") }
            }
        }
    }
}

class AccountsViewModel(private val app: LiquidMailApp) : ViewModel() {
    val accounts = app.database.accountDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun switchTo(id: String) {
        viewModelScope.launch { app.settings.setActiveAccount(id) }
    }
}

class AccountsViewModelFactory(private val app: LiquidMailApp) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AccountsViewModel(app) as T
}
