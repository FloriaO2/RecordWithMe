package com.example.recordwithme.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recordwithme.MainActivity
import com.example.recordwithme.R
import kotlinx.coroutines.delay

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            SplashScreen()
        }
    }
}

@Composable
fun SplashScreen() {
    val context = LocalContext.current
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "alpha"
    )
    
    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.3f,
        animationSpec = tween(durationMillis = 1000),
        label = "scale"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(3000L) // 2초 대기
        context.startActivity(Intent(context, MainActivity::class.java))
        (context as? SplashActivity)?.finish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8F0)),
        contentAlignment = Alignment.Center

    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 로고 이미지
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "앱 로고",
                modifier = Modifier
                    .size(200.dp)
                    .graphicsLayer(
                        alpha = alphaAnim.value,
                        scaleX = scaleAnim.value,
                        scaleY = scaleAnim.value
                    )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 앱 이름
            Text(
                text = "RecordWithMe",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Cursive,
                color = Color(0xFF1976D2),
                modifier = Modifier.graphicsLayer(alpha = alphaAnim.value)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 부제목 임시로 해봄..
//            Text(
//                text = "함께 기록하는 순간들",
//                fontSize = 16.sp,
//                color = Color.Gray,
//                modifier = Modifier.graphicsLayer(alpha = alphaAnim.value)
//            )
        }
    }
}