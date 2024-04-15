package com.solvek.bletrigger.ui.content

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.solvek.bletrigger.ui.viewmodel.LogViewModel

@Composable
fun MainContent(model: LogViewModel) {
    Column(
        Modifier
            .fillMaxSize()
    ) {

        val log by model.log.collectAsState()

        Button(model::clear, Modifier.fillMaxWidth()) {
            Text("Clear")
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(log)
        }
    }
}