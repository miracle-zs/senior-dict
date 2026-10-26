package com.seniordict.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.seniordict.app.ui.screen.MainScreen
import com.seniordict.app.ui.theme.SeniorDictTheme
import com.seniordict.app.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SeniorDictTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
