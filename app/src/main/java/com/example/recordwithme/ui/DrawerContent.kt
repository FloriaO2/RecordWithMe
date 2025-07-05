package com.example.recordwithme.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recordwithme.AuthViewModel

@Composable
fun AppDrawer(
    onDrawerClose: () -> Unit = {},
    authViewModel: AuthViewModel? = null
) {
    val context = LocalContext.current

    Column(modifier = Modifier.padding(16.dp)) {
        // 프로필 섹션
        Text(
            text = "프로필",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "사용자 정보",
            modifier = Modifier
                .fillMaxWidth()
                .clickable { 
                    Toast.makeText(context, "프로필 클릭", Toast.LENGTH_SHORT).show()
                }
                .padding(vertical = 8.dp)
        )
        
        // 구분선
        Divider(
            modifier = Modifier.padding(vertical = 16.dp),
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
        )
        
        // 설정 섹션
        Text(
            text = "설정",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "앱 설정",
            modifier = Modifier
                .fillMaxWidth()
                .clickable { 
                    Toast.makeText(context, "앱 설정 클릭", Toast.LENGTH_SHORT).show()
                }
                .padding(vertical = 8.dp)
        )
        
        Text(
            text = "알림 설정",
            modifier = Modifier
                .fillMaxWidth()
                .clickable { 
                    Toast.makeText(context, "알림 설정 클릭", Toast.LENGTH_SHORT).show()
                }
                .padding(vertical = 8.dp)
        )
        
        // 구분선
        Divider(
            modifier = Modifier.padding(vertical = 16.dp),
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
        )
        
        // 로그아웃 섹션
        Text(
            text = "로그아웃",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.error,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { 
                    authViewModel?.signOut() ?: run {
                        // AuthViewModel이 없는 경우 직접 Firebase Auth 사용
                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                    }
                    Toast.makeText(context, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show()
                    onDrawerClose()
                }
                .padding(vertical = 8.dp)
        )
    }
}
