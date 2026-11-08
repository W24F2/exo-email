package com.example.email_client.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.email_client.LiquidMailApp
import com.example.email_client.core.mail.engine.MailEngineFactory
import com.example.email_client.core.security.CredentialStore
import com.example.email_client.data.MailRepository
import com.example.email_client.data.db.AccountEntity
import com.example.email_client.data.db.MessageEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InboxViewModel(
    private val app: LiquidMailApp,
    private val repo: MailRepository = MailRepository(app.database, MailEngineFactory.create()),
    private val credentials: CredentialStore = CredentialStore(app),
) : ViewModel() {

    val activeAccount: StateFlow<AccountEntity?> = app.settings.activeAccountId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        .let { flow ->
            kotlinx.coroutines.flow.combine(flow, repo.observeAccounts()) { id, accounts ->
                accounts.firstOrNull { it.id == id } ?: accounts.firstOrNull()
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: Flow<PagingData<MessageEntity>> = activeAccount
        .flatMapLatest { it?.let { a -> repo.inboxPager(a.id) } ?: flowOf(PagingData.empty()) }
        .cachedIn(viewModelScope)

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing

    fun refresh() {
        val account = activeAccount.value ?: return
        val secret = credentials.get(account.id)?.secret ?: return
        viewModelScope.launch {
            _refreshing.value = true
            repo.refreshInbox(account, secret)
            _refreshing.value = false
        }
    }

    fun markRead(id: String) {
        viewModelScope.launch { repo.markRead(id, true) }
    }

    fun toggleStar(id: String, starred: Boolean) {
        viewModelScope.launch { repo.setStarred(id, starred) }
    }
}

class InboxViewModelFactory(private val app: LiquidMailApp) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = InboxViewModel(app) as T
}
