package com.example.ebird.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ebird.AuthActivity
import com.example.ebird.MainActivity
import com.example.ebird.MyApplication
import com.example.ebird.R
import com.example.ebird.utils.SharedPrefManager
import com.example.ebird.utils.userEmail

@Composable
fun SettingScreenLayout() {

    val context = LocalContext.current
    // Remembering the state of the switch and initializing it with the stored value
    val switchState = remember { mutableStateOf(SharedPrefManager(context).getSwitchStatus()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Image(painter = painterResource(R.drawable.ic_bird_app),
            modifier = Modifier.padding(top = 16.dp).size(56.dp).fillMaxWidth().align(Alignment.CenterHorizontally), contentDescription = "")
        Text(
            "Email: $userEmail",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Switch with label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("Enable distance in kilometers", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = switchState.value,
                onCheckedChange = { status ->
                    // Update the local state and save it in SharedPreferences
                    switchState.value = status
                    SharedPrefManager(context).saveSwitchStatus(status)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sign-out button
        Button(
            onClick = {
                MyApplication.auth.signOut()
                context.startActivity(Intent(context, AuthActivity::class.java))
                if (context is MainActivity) {
                    context.finish()
                    SharedPrefManager(context).clearPreferences()
                }
            },
            modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally)
        ) {
            Text("Sign out", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
