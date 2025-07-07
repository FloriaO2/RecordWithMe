package com.example.recordwithme.ui

import android.content.Intent
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.platform.LocalContext

@Composable
fun GroupScreen() {
    val context = LocalContext.current

    Column {
        Text("그룹 관리화면")
        Button(onClick = {
            // XML 기반 GroupCalendarActivity 실행
            context.startActivity(Intent(context, GroupCalendarActivity::class.java))
        }) {
            Text("그룹 캘린더로 이동")
        }
    }
}
