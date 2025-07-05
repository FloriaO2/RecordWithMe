package com.example.recordwithme.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.flowlayout.FlowRow

data class Photo(val url: String, val date: String)

data class Group(val name: String, val photos: List<Photo> = emptyList())

@Composable
fun HomeScreen() {
    val groupList = listOf(
        Group("가족", listOf(
            Photo("url1", "2024년 5월 15일"),
            Photo("url2", "2024년 5월 15일"),
            Photo("url33", "2024년 5월 15일"),
            Photo("url22", "2024년 5월 15일"),
            Photo("url3", "2024년 5월 14일"),
            Photo("url4", "2024년 5월 14일"),
            Photo("url5", "2024년 5월 13일"),
            Photo("url6", "2024년 5월 13일"),
            Photo("url8", "2024년 5월 12일"),
            Photo("url9", "2024년 5월 11일"),
            Photo("url10", "2024년 5월 11일"),
            Photo("url11", "2024년 5월 10일"),
            Photo("url12", "2024년 5월 10일")
        )),
        Group("친구", listOf(
            Photo("url4", "2024년 5월 13일"),
            Photo("url5", "2024년 5월 13일")
        )),
        Group("회사"),
        Group("동아리"),
        Group("스터디"),
        Group("운동"),
        Group("여행")
    )

    var selectedGroup by remember { mutableStateOf<Group?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // (상단바는 MainActivity에서 이미 Scaffold로 포함되어 있음)

        Spacer(modifier = Modifier.height(8.dp))

        // 1. 인스타 스토리처럼 동그란 그룹 리스트 공간
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (groupList.isEmpty()) {
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.LightGray, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {}
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("빈 그룹", color = Color.DarkGray)
                    }
                }
            } else {
                items(groupList) { group ->
                    val isSelected = group == selectedGroup
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            selectedGroup = if (selectedGroup == group) null else group
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    color = Color.LightGray,
                                    shape = CircleShape
                                )
                                .then(
                                    if (isSelected) Modifier
                                        .border(
                                            width = 3.dp,
                                            color = Color(0xFF1976D2), // 파란색 등 강조색
                                            shape = CircleShape
                                        )
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // 원 안 내용
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(group.name, color = Color.DarkGray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val photosToShow = selectedGroup?.photos ?: groupList.flatMap { it.photos }
        if (photosToShow.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("가장 먼저 사진을 업로드해보세요!", color = Color.Gray)
            }
        } else {
            val photosByDate = photosToShow.groupBy { it.date }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // 대표사진 아이템
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            elevation = 4.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFE0E0E0)), // 연한 회색
                                contentAlignment = Alignment.Center
                            ) {
                                Text("대표사진 자리", color = Color.Gray)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 날짜별 사진들
                photosByDate.forEach { (date, photos) ->
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(date, color = Color.Black, fontSize = 25.sp, modifier = Modifier.padding(start = 16.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            modifier = Modifier.padding(start = 16.dp),
                            mainAxisSpacing = 12.dp,
                            crossAxisSpacing = 12.dp
                        ) {
                            photos.forEach { photo ->
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .background(Color.LightGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("사진", color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
