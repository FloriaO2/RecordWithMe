package com.example.recordwithme.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// 데이터 모델
data class UserGroup(
    val name: String = "",
    val membersCount: Int = 0
)

@Composable
fun GroupScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val groups = remember { mutableStateListOf<UserGroup>() }
    val isLoading = remember { mutableStateOf(true) }

    // Firestore에서 데이터 가져오기
    LaunchedEffect(true) {
        try {
            val snapshot = firestore.collection("groups").get().await()
            groups.clear()
            for (document in snapshot.documents) {
                val groupName = document.getString("name") ?: ""

                // 'members' 배열에서 멤버 수 가져오기
                val members = document.get("members") as? List<String> ?: emptyList() // members 배열을 명시적으로 String 리스트로 캐스팅
                val membersCount = members.size // 배열 크기 계산

                // UserGroup에 멤버 수와 그룹 이름을 설정
                val group = UserGroup(name = groupName, membersCount = membersCount)
                groups.add(group)
            }
        } catch (e: Exception) {
            // 에러 처리
            println("Error fetching data: $e")
        }
        isLoading.value = false
    }

    Box(modifier = Modifier.fillMaxSize()) {  // Box로 전체 화면을 감쌈
        Column(modifier = Modifier.padding(25.dp)) {
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "My Groups",
                style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 데이터 로딩 중일 때 텍스트 표시
            if (isLoading.value) {
                Text(text = "Loading...")
            } else {
                // 그룹이 없다면 안내 메시지 표시
                if (groups.isEmpty()) {
                    Spacer(modifier = Modifier.padding(16.dp))
                    Text(
                        text = "그룹이 없습니다.\n지금 바로 그룹을 만들어보세요!",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    groups.forEach { group ->
                        GroupItem(name = group.name, membersCount = group.membersCount)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f)) // 그룹 목록이 끝난 후 남은 공간을 채움
        }

        // FloatingActionButton은 Box로 감싸고, 아래쪽 우측에 고정시킵니다.
        FloatingActionButton(
            onClick = { 
                // ProfileScreen으로 이동하면서 groupMode를 활성화
                println("GroupScreen: Setting groupMode to true and navigating to profile")
                GroupModeState.isGroupMode = true
                navController.navigate("profile") {
                    launchSingleTop = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)  // 항상 우측 하단에 고정
                .padding(16.dp), // 버튼 주변 여백
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
        // 사각형 도형으로 변경하고, 모서리를 둥글게 처리
        Box(
            modifier = Modifier
                .size(40.dp) // 고정 크기
                .clip(RoundedCornerShape(8.dp))  // 모서리 둥글게
                .background(Color.Gray) // 배경 색상
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.bodyLarge)
            Text(text = "$membersCount members", style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray))
        }
    }
}


