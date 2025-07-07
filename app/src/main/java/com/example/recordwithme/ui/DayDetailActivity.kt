package com.example.recordwithme.ui

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DayDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val day = intent.getIntExtra("day", -1)
        val year = intent.getIntExtra("year", -1)
        val month = intent.getIntExtra("month", -1)
        val receivedTransitionName = intent.getStringExtra("transitionName") ?: ""
        
        // 동적으로 레이아웃 생성
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setPadding(32, 64, 32, 64)
            
            // Shared Element Transition을 위한 transitionName 설정
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                transitionName = receivedTransitionName
            }
        }
        
        // 날짜 표시
        val dateText = TextView(this).apply {
            text = "${year}년 ${month}월 ${day}일"
            textSize = 36f
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 32)
        }
        
        // 뒤로가기 버튼
        val backButton = Button(this).apply {
            text = "뒤로가기"
            textSize = 16f
            setOnClickListener {
                finish()
            }
        }
        
        layout.addView(dateText)
        layout.addView(backButton)
        
        setContentView(layout)
        
        // Shared Element Transition 설정 - 속도 조절 및 배경 유지
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val transition = android.transition.TransitionInflater.from(this)
                .inflateTransition(android.R.transition.move)
            
            // 애니메이션 지속 시간을 늘려서 더 부드럽게
            transition.duration = 800
            
            window.sharedElementEnterTransition = transition
            
            // 배경이 사라지지 않도록 설정
            window.allowEnterTransitionOverlap = false
            window.allowReturnTransitionOverlap = false
        }
    }
}