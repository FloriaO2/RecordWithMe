package com.example.recordwithme.ui

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import com.example.recordwithme.R
import java.util.Calendar
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import android.graphics.drawable.Drawable
import com.google.firebase.firestore.FirebaseFirestore
import android.graphics.drawable.ColorDrawable
import android.util.Base64
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.view.Gravity as AndroidGravity
import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import android.widget.FrameLayout
import android.util.TypedValue
import android.widget.ImageView
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.ComposeView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import com.example.recordwithme.MainActivity
import com.example.recordwithme.ui.TopBarIconType

class GroupCalendarActivity : AppCompatActivity() {
    private var year = 2024 
    private var month = 7 // 1~12
    private var groupId: String = ""
    private var groupName: String = ""
    private lateinit var dayDetailLauncher: ActivityResultLauncher<Intent>
    private lateinit var grid: GridLayout
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_calendar)

        // 시스템 UI(상단바, 하단바) 숨기기
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        // TopBar ComposeView에 세팅
        val composeTopBar = findViewById<ComposeView>(R.id.composeTopBar)
        composeTopBar.setContent {
            TopBar(
                iconType = TopBarIconType.Home,
                onMenuClick = {
                    val intent = Intent(this@GroupCalendarActivity, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    startActivity(intent)
                    finish()
                }
            )
        }

        // Intent에서 그룹 정보 가져오기
        groupId = intent.getStringExtra("groupId") ?: ""
        groupName = intent.getStringExtra("groupName") ?: ""
        
        // 디버깅을 위한 로그
        Log.d("GroupCalendarActivity", "받은 groupId: $groupId")
        Log.d("GroupCalendarActivity", "받은 groupName: $groupName")

        val textMonth = findViewById<TextView>(R.id.textMonth)
        val layoutYearMonth = findViewById<LinearLayout>(R.id.layoutYearMonth)
        // 월에 따라 marginEnd 동적 조정
        val params = layoutYearMonth.layoutParams as ViewGroup.MarginLayoutParams
        if (month >= 10) {
            params.marginEnd = (8 * resources.displayMetrics.density).toInt() // 8dp
        } else {
            params.marginEnd = (0 * resources.displayMetrics.density).toInt() // 20dp
        }
        layoutYearMonth.layoutParams = params
        val layoutWeekdays = findViewById<LinearLayout>(R.id.layoutWeekdays)
        grid = findViewById<GridLayout>(R.id.gridCalendar)

        // 월이 두 자리수면 letterSpacing을 더 좁게, 한 자리수면 기본값
        if (month >= 10) {
            textMonth.letterSpacing = -0.08f
        } else {
            textMonth.letterSpacing = 0f
        }

        // 제스처 디텍터 초기화
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                
                Log.d("Gesture", "Fling detected: diffX=$diffX, diffY=$diffY, velocityX=$velocityX")
                
                // 수평 스와이프가 수직 스와이프보다 크고, 최소 거리 조건을 만족할 때
                if (abs(diffX) > abs(diffY) && abs(diffX) > 50) {
                    if (diffX > 0) {
                        // 오른쪽으로 스와이프 - 이전 월
                        Log.d("Gesture", "Swiping right - going to previous month")
                        goToPreviousMonth()
                    } else {
                        // 왼쪽으로 스와이프 - 다음 월
                        Log.d("Gesture", "Swiping left - going to next month")
                        goToNextMonth()
                    }
                    return true
                }
                return false
            }
        })

        // 캘린더 그리드에 제스처 리스너 추가
        grid.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
        }

        // 연/월 변경 버튼 + 연/월 표시 (상단 오른쪽)
        layoutYearMonth.removeAllViews()
        layoutYearMonth.orientation = LinearLayout.HORIZONTAL
        layoutYearMonth.gravity = Gravity.CENTER_VERTICAL

        // Material 아이콘으로 월 이동 버튼 생성
        val ivPrev = ImageView(this).apply {
            setImageResource(com.google.android.material.R.drawable.material_ic_keyboard_arrow_left_black_24dp)
            setPadding(24, 24, 24, 24)
            setOnClickListener { goToPreviousMonth() }
        }
        val tvYearMonth = TextView(this).apply {
            textSize = 32f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(8, 0, 8, 0)
        }
        val ivNext = ImageView(this).apply {
            setImageResource(com.google.android.material.R.drawable.material_ic_keyboard_arrow_right_black_24dp)
            setPadding(24, 24, 24, 24)
            setOnClickListener { goToNextMonth() }
        }
        // yearMonthControls 관련 코드 삭제, layoutYearMonth에 addView 복원
        layoutYearMonth.addView(ivPrev)
        layoutYearMonth.addView(tvYearMonth)
        layoutYearMonth.addView(ivNext)

        // 뒤로가기 버튼 클릭 리스너 연결
        findViewById<Button>(R.id.btnBackToGroup).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        dayDetailLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                updateCalendar()
            }
        }

        updateCalendar()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev != null) {
            gestureDetector.onTouchEvent(ev)
        }
        return super.dispatchTouchEvent(ev)
    }

    // 이전 월로 이동
    private fun goToPreviousMonth() {
        if (month == 1) {
            if (year > 2024) {
                year--
                month = 12
            }
        } else {
            month--
        }
        animateCalendarTransition()
    }

    // 다음 월로 이동
    private fun goToNextMonth() {
        if (month == 12) {
            if (year < 2027) {
                year++
                month = 1
            }
        } else {
            month++
        }
        animateCalendarTransition()
    }

    // 캘린더 전환 애니메이션 함수
    private fun animateCalendarTransition() {
        val textMonth = findViewById<TextView>(R.id.textMonth)
        val layoutYearMonth = findViewById<LinearLayout>(R.id.layoutYearMonth)
        // 월에 따라 marginEnd 동적 조정
        val params = layoutYearMonth.layoutParams as ViewGroup.MarginLayoutParams
        if (month >= 10) {
            params.marginEnd = (8 * resources.displayMetrics.density).toInt() // 8dp
        } else {
            params.marginEnd = (20 * resources.displayMetrics.density).toInt() // 20dp
        }
        layoutYearMonth.layoutParams = params
        val layoutWeekdays = findViewById<LinearLayout>(R.id.layoutWeekdays)
        val grid = findViewById<GridLayout>(R.id.gridCalendar)
        val btnBackToGroup = findViewById<Button>(R.id.btnBackToGroup)

        val uiElements = listOf(textMonth, layoutYearMonth, layoutWeekdays, grid, btnBackToGroup)

        // 현재 UI 요소들에 페이드 아웃 애니메이션 적용
        uiElements.forEach { element ->
            element.animate()
                .alpha(0f)
                .translationX(-100f)
                .setDuration(300)
                .start()
        }

        // 애니메이션 완료 후 캘린더 업데이트
        grid.postDelayed({
            updateCalendar()

            // 새로운 UI 요소들에 페이드 인 애니메이션 적용
            uiElements.forEach { element ->
                element.alpha = 0f
                element.translationX = 100f
                element.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(300)
                    .start()
            }
        }, 300)
    }

    // 캘린더 업데이트 함수
    private fun updateCalendar() {
        val layoutYearMonth = findViewById<LinearLayout>(R.id.layoutYearMonth)
        // 월에 따라 marginEnd 동적 조정
        val params = layoutYearMonth.layoutParams as ViewGroup.MarginLayoutParams
        if (month >= 10) {
            params.marginEnd = (8 * resources.displayMetrics.density).toInt() // 8dp
        } else {
            params.marginEnd = (20 * resources.displayMetrics.density).toInt() // 20dp
        }
        layoutYearMonth.layoutParams = params
        val textMonth = findViewById<TextView>(R.id.textMonth)
        
        // 상단 표시
        textMonth.text = month.toString()
        // 월이 두 자리수면 letterSpacing을 더 좁게, 한 자리수면 기본값
        if (month >= 10) {
            textMonth.letterSpacing = -0.08f
        } else {
            textMonth.letterSpacing = 0f
        }
        // 연/월 표시 업데이트 (layoutYearMonth의 두 번째 TextView)
        if (layoutYearMonth.childCount >= 2) {
            val tvYearMonth = layoutYearMonth.getChildAt(1) as? TextView
            tvYearMonth?.text = "$year / $month"
        }

        // 날짜 그리드 생성
        grid.removeAllViews()
        grid.rowCount = 6
        grid.columnCount = 7

        // 달력 계산
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1:일~7:토
        val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val startOffset = (firstDayOfWeek + 5) % 7 // 월요일 시작
        val days = mutableListOf<Int?>()
        repeat(startOffset) { days.add(null) }
        for (i in 1..lastDay) days.add(i)
        while (days.size < 42) days.add(null)

        val firestore = FirebaseFirestore.getInstance()

        // 날짜 셀 추가
        for ((index, d) in days.withIndex()) {
            val row = index / 7
            val col = index % 7
            val cell = FrameLayout(this)
            val imageView = ImageView(this).apply {
                val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80f, resources.displayMetrics).toInt()
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    px,
                    Gravity.CENTER
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(if (d == null) Color.TRANSPARENT else Color.WHITE)
                alpha = 0.8f
            }
            val textView = TextView(this).apply {
                text = d?.toString() ?: ""
                gravity = Gravity.CENTER
                textSize = 20f
                setTextColor(Color.BLACK)
                setPadding(0, 0, 0, 0)
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            cell.addView(imageView)
            cell.addView(textView)
            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = 0
            params.columnSpec = GridLayout.spec(col, 1f)
            params.rowSpec = GridLayout.spec(row, 0.5f)
            params.setMargins(4, 4, 4, 4)
            cell.layoutParams = params
            
            // 날짜 셀 클릭 시 Shared Element Transition으로 DayDetailActivity 전환
            if (d != null) {
                // transitionName 설정
                textView.transitionName = "calendar_cell_${year}_${month}_$d"

                // Firestore에서 해당 날짜의 최신 사진 1장만 쿼리 (DayDetailActivity와 동일)
                val dateString = "${year}년 ${month}월 ${d}일"
                firestore.collection("groups").document(groupId)
                    .collection("photos")
                    .whereEqualTo("date", dateString)
                    .orderBy("uploadedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        val doc = querySnapshot.documents.firstOrNull()
                        if (doc != null) {
                            val isBase64 = doc.getBoolean("isBase64") ?: false
                            val imageUrl = doc.getString("url")
                            if (!imageUrl.isNullOrEmpty()) {
                                if (isBase64) {
                                    try {
                                        val imageBytes = Base64.decode(imageUrl, Base64.DEFAULT)
                                        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                        imageView.setImageBitmap(bitmap)
                                    } catch (e: Exception) {
                                        imageView.setImageDrawable(null)
                                        imageView.setBackgroundColor(Color.WHITE)
                                    }
                                } else {
                                    Glide.with(this)
                                        .load(imageUrl)
                                        .centerCrop()
                                        .into(imageView)
                                }
                            } else {
                                imageView.setImageDrawable(null)
                                imageView.setBackgroundColor(Color.WHITE)
                            }
                        } else {
                            imageView.setImageDrawable(null)
                            imageView.setBackgroundColor(Color.WHITE)
                        }
                    }
                    .addOnFailureListener {
                        imageView.setImageDrawable(null)
                        imageView.setBackgroundColor(Color.WHITE)
                    }

                cell.setOnClickListener {
                    val intent = Intent(this, DayDetailActivity::class.java)
                    intent.putExtra("day", d)
                    intent.putExtra("year", year)
                    intent.putExtra("month", month)
                    intent.putExtra("groupId", groupId)
                    intent.putExtra("groupName", groupName)
                    intent.putExtra("transitionName", "calendar_cell_${year}_${month}_$d")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            this,
                            textView,
                            "calendar_cell_${year}_${month}_$d"
                        )
                        dayDetailLauncher.launch(intent, options)
                        window.allowEnterTransitionOverlap = false
                        window.allowReturnTransitionOverlap = false
                    } else {
                        dayDetailLauncher.launch(intent)
                    }
                }
            }
            grid.addView(cell)
        }
    }
} 