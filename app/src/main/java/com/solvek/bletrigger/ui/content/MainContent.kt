package com.solvek.bletrigger.ui.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solvek.bletrigger.ui.viewmodel.LogViewModel

@Composable
fun MainContent(
    model: LogViewModel
) {
    Column(
        Modifier
            .fillMaxSize()
    ) {

        val log by model.log.collectAsState()
        val connectionEnabled by model.connectionEnabled.collectAsState()

        val connectionEnabledChecked = remember {
            mutableStateOf(connectionEnabled)
        }

        Switch(
            checked = connectionEnabled,
            onCheckedChange = { isChecked ->
                connectionEnabledChecked.value = isChecked
                model.setConnectionEnabled(isChecked)
            }
        )

        Button(
            modifier = Modifier
                .wrapContentSize()
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(
                    all = 24.dp
                ),
            onClick = model::clear
        ) {
            Text("Clear logs")
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