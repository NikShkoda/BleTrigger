package com.solvek.bletrigger.ui.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.solvek.bletrigger.R
import com.solvek.bletrigger.ui.theme.Typography
import com.solvek.bletrigger.ui.viewmodel.LogViewModel

@Composable
fun MainContent(
    model: LogViewModel
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(all = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val log by model.log.collectAsState()
        val connectionEnabled by model.connectionEnabled.collectAsState()

        val connectionEnabledChecked = remember {
            mutableStateOf(connectionEnabled)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(id = R.string.text_connection_enabled),
                style = Typography.titleMedium
            )
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = connectionEnabled,
                onCheckedChange = { isChecked ->
                    connectionEnabledChecked.value = isChecked
                    model.setConnectionEnabled(isChecked)
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            modifier = Modifier
                .wrapContentSize()
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(
                    all = 4.dp
                ),
            onClick = model::clear
        ) {
            Text(
                text = "Clear logs",
                style = Typography.bodyLarge
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(log)
        }
    }
}