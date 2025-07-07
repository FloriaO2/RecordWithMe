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

class GroupCalendarActivity : AppCompatActivity() {
    private var year = 2024 
    private var month = 7 // 1~12
    private var groupId: String = ""
    private var groupName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_calendar)

        // Intent에서 그룹 정보 가져오기
        groupId = intent.getStringExtra("groupId") ?: ""
        groupName = intent.getStringExtra("groupName") ?: ""

        val textMonth = findViewById<TextView>(R.id.textMonth)
        val layoutYearMonth = findViewById<LinearLayout>(R.id.layoutYearMonth)
        val grid = findViewById<GridLayout>(R.id.gridCalendar)

        // 연/월 변경 버튼 + 연/월 표시 (상단 오른쪽)
        layoutYearMonth.removeAllViews()
        layoutYearMonth.orientation = LinearLayout.HORIZONTAL
        layoutYearMonth.gravity = Gravity.CENTER_VERTICAL

        val tvPrev = TextView(this).apply {
            text = "◀"
            textSize = 28f
            setTextColor(Color.BLACK)
            setPadding(16, 0, 16, 0)
            gravity = Gravity.CENTER
        }
        val tvYearMonth = TextView(this).apply {
            textSize = 32f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(8, 0, 8, 0)
        }
        val tvNext = TextView(this).apply {
            text = "▶"
            textSize = 28f
            setTextColor(Color.BLACK)
            setPadding(16, 0, 16, 0)
            gravity = Gravity.CENTER
        }
        layoutYearMonth.addView(tvPrev)
        layoutYearMonth.addView(tvYearMonth)
        layoutYearMonth.addView(tvNext)

        fun updateCalendar() {
            // 상단 표시
            textMonth.text = month.toString()
            tvYearMonth.text = "$year / $month"

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

            // 날짜 셀 추가
            for (d in days) {
                val tv = TextView(this)
                tv.text = d?.toString() ?: ""
                tv.gravity = Gravity.CENTER
                tv.textSize = 18f
                tv.setTextColor(Color.BLACK)
                tv.setBackgroundColor(if (d == null) Color.TRANSPARENT else Color.WHITE)
                val params = GridLayout.LayoutParams()
                params.width = 0
                params.height = GridLayout.LayoutParams.WRAP_CONTENT
                params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                params.setMargins(4, 4, 4, 4)
                tv.layoutParams = params
                
                // 날짜 셀 클릭 시 Shared Element Transition으로 DayDetailActivity 전환
                if (d != null) {
                    // transitionName 설정
                    tv.transitionName = "calendar_cell_${year}_${month}_$d"
                    
                    tv.setOnClickListener {
                        val intent = Intent(this, DayDetailActivity::class.java)
                        intent.putExtra("day", d)
                        intent.putExtra("year", year)
                        intent.putExtra("month", month)
                        intent.putExtra("groupId", groupId)
                        intent.putExtra("groupName", groupName)
                        intent.putExtra("transitionName", "calendar_cell_${year}_${month}_$d")
                        
                        // Android 5.0 이상에서만 Shared Element Transition 적용
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                this,
                                tv,
                                "calendar_cell_${year}_${month}_$d"
                            )
                            
                            // 전환 애니메이션 설정
                            startActivity(intent, options.toBundle())
                            
                            // 전환 중에 배경이 사라지지 않도록 설정
                            window.allowEnterTransitionOverlap = false
                            window.allowReturnTransitionOverlap = false
                        } else {
                            startActivity(intent)
                        }
                    }
                }
                grid.addView(tv)
            }
        }

        // 이전/다음 버튼 동작
        tvPrev.setOnClickListener {
            if (month == 1) {
                if (year > 2024) {
                    year--
                    month = 12
                }
            } else {
                month--
            }
            updateCalendar()
        }
        tvNext.setOnClickListener {
            if (month == 12) {
                if (year < 2027) {
                    year++
                    month = 1
                }
            } else {
                month++
            }
            updateCalendar()
        }

        // 뒤로가기 버튼 클릭 리스너 연결
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        updateCalendar()
    }
} 