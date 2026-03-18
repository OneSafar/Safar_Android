package com.safar.app.ui.ekagra


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.safar.app.ui.components.*

@Composable
fun EkagraScreen(onBack: () -> Unit) {

    Scaffold(
        topBar = {
            SafarTopBar(
                title = "Ekagra",
                onBack = onBack,
                actions = {

                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

        }
    }
}
