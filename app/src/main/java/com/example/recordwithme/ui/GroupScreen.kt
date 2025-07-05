package com.example.recordwithme.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GroupScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "My Groups",
            style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        GroupItem(name = "Hiking Enthusiasts", membersCount = 12)
        GroupItem(name = "Book Club", membersCount = 8)
        GroupItem(name = "Photography Group", membersCount = 15)
        GroupItem(name = "Tech Innovators", membersCount = 20)
        GroupItem(name = "Art Lovers", membersCount = 10)

        Spacer(modifier = Modifier.weight(1f))

        FloatingActionButton(
            onClick = { /* Handle new group action */ },
            modifier = Modifier.align(Alignment.End),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Group")
        }
    }
}

@Composable
fun GroupItem(name: String, membersCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 사각형 도형으로 변경
        Box(
            modifier = Modifier
                .size(40.dp) // 고정 크기
                .background(Color.Gray) // 사각형 배경 색상
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            // typography 수정: body1 -> bodyLarge, body2 -> bodyMedium
            Text(text = name, style = MaterialTheme.typography.bodyLarge)
            Text(text = "$membersCount members", style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray))
        }
    }
}

@Composable
fun GroupScreenApp() {
    // GroupScreenTheme를 제거하고 기본 MaterialTheme 사용
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            GroupScreen()
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GroupScreenApp()
        }
    }
}
